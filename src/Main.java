import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static String latConvert(String lat){

        //get dd
        String dd = lat.substring(0, 2);

        //get minutes
        String mmmm = lat.substring(2,9);
        //convert to ddddd
        Double mConvert = Double.parseDouble(mmmm)/60;
        //round to 5dp
        DecimalFormat df = new DecimalFormat("#.#####");
        String mRounded = df.format(mConvert);
        //remove starting 0.
        String mfinal = mRounded.substring(1);

        String latconverted = dd + mfinal;

        return latconverted;
    }

    public static String lonConvert(String lon){

        //get ddd
        String ddd = lon.substring(0, 3);

        //get minutes
        String mmmm = lon.substring(3,10);
        //Convert to dddd
        Double mConvert = Double.parseDouble(mmmm)/60;
        //round to 5dp
        DecimalFormat df = new DecimalFormat("#.#####");
        String mRounded = df.format(mConvert);
        //remove starting 0.
        String mfinal = mRounded.substring(1);

        String lonConverted = ddd + mfinal;

        return lonConverted;
    }

    //Returns the formatted lat,long data from GPS module
    public static String formattedLocation(String raw_data){

        //Get split sentence at commas.
        String[] split = raw_data.split(",", 0);
        String lat;
        String lon;

        //for converting to +/- format.
        if(split[3].equals("S")){

            split[3] = "-";
            lat = split[3] + split[2];
        }
        else{
            lat = split[2];
        }

        //for converting to +/- format.
        if(split[5].equals("W")){

            split[5] = "-";
            lon = split[5] + split[4];
        }
        else{
            lon = split[4];
        }

        String formattedLat = latConvert(lat);
        String formattedLon = lonConvert(lon);

        String formattedLocationData = formattedLat + "," + formattedLon;
        return formattedLocationData;
    }

    //Returns total drunk or spilled for csv.
    public static int getTotalDrunkorSpilled(ArrayList<Record> reductionList){

        int volume = 0;
        for(Record r: reductionList){

            volume = volume + r.getDecreaseVolume();

        }

        return volume;
    }

    //returns a list with records older than 5 minutes cleared from it.
    public static ArrayList<Record> cleanList(ArrayList<Record> reductionList){

        LocalTime current = LocalTime.now();
        ArrayList<Record> toRemove = new ArrayList<Record>();
        for(Record r : reductionList){

            //System.out.println(current.minus(5, ChronoUnit.MINUTES));
            //System.out.println(r.getTime());
            if(r.getTime().isBefore(current.minus(5, ChronoUnit.MINUTES))){
                System.out.println("Record" + r + "is older than 5 minutes!");
                toRemove.add(r);
            }
        }
        reductionList.removeAll(toRemove);
        return reductionList;

    }

    public static void uploadLocationDataToDB(String latlong){

        //get current runtime.
        Runtime rt = Runtime.getRuntime();
        String data = String.format("\"{\\\"location\\\" : \\\"%s\\\"}\"", latlong);

        String[] command = {"curl", "-X", "PATCH", "-d", data, "https://water-volume-9cd60-default-rtdb.europe-west1.firebasedatabase.app/.json"};

        try{
            rt.exec(command);
            System.out.println("Written location data to database.");

        } catch (IOException e) {
            System.out.println("Cannot write to database!");
            throw new RuntimeException(e);
        }
    }

    public static void uploadLoadCellDataToDB(String bucketID, int watervolume, int fillvolume, int waterdrunk, int waterspilled, int maxcapacity){


        //get current runtime.
        Runtime rt = Runtime.getRuntime();
        //format data to keep with command format
        String data = String.format("\"{\\\"bucketid\\\" : \\\"%s\\\", \\\"watervolume\\\" : %s, \\\"fillvolume\\\" : %s, \\\"drunkvolume\\\" : %s, \\\"spillvolume\\\" : %s , \\\"maxvolume\\\" : %s}\""
                , bucketID, watervolume, fillvolume, waterdrunk, waterspilled, maxcapacity);

        //construct command
        String[] command = {"curl", "-X", "PATCH", "-d", data, "https://water-volume-9cd60-default-rtdb.europe-west1.firebasedatabase.app/.json"};
        //try running command
        try{
            rt.exec(command);
            System.out.println("Written load cell data to database.");

        } catch (IOException e) {
            System.out.println("Cannot write to database!");
            throw new RuntimeException(e);
        }


    }

    public static void updateCSV(String bucketID, int watervolume, int fillvolume, int waterdrunk, int waterspilled, int maxcapacity){

        String filepath = "C:\\Users\\James\\IdeaProjects\\Loadcell\\example_entry_sheet1.csv";
        try{

            FileWriter fw = new FileWriter(filepath);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            pw.println("bucketID,waterLvl,waterRequired,waterDrunk,waterLoss,maxCapcity");
            pw.println(bucketID+ "," +watervolume+ "," +fillvolume+ "," +waterdrunk+ "," +waterspilled + "," + maxcapacity);
            pw.flush();
            pw.close();

            System.out.println("Written to csv.");
        }
        catch (Exception e){

            System.out.println("Cannot write to csv.");
            System.out.println(e);
        }

    }

    public static SerialPort serialComSetup(){

        //port arduino is running on
        SerialPort port = SerialPort.getCommPort("COM3");
        if(port.openPort()){
            System.out.println("Opened port at: " + port.getSystemPortName());
            System.out.println("Preparing to read data from arduino, please wait...");
        }
        else{
            System.out.println("Port cannot be opened.");
        }

        //set parameters
        port.setComPortParameters(57600, Byte.SIZE, 1, 0 );
        port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0 , 0 );

        return port;
    }
    public static void data(SerialPort port, Bucket bucket) {

        int count = 1;
        int totalDrunk = 0;
        int totalSpill = 0;
        ArrayList<Record> drinkList = new ArrayList<Record>();
        ArrayList<Record> spillList = new ArrayList<Record>();
        Scanner scanner = new Scanner(port.getInputStream());

        while (scanner.hasNextLine()) {

            scanner.next();
            //bad system for ignoring first 5 results as they can get "stuck" in port on closure.
            if (count > 5) {

                //get data
                String raw_data = scanner.next();

                //If gps module data.
                if(raw_data.startsWith("$GPGGA") || raw_data.startsWith("$GPRMC")){

                    String latlong = formattedLocation(raw_data);
                    uploadLocationDataToDB(latlong);

                }
                else{ //if load cell data

                    //round it to the nearest positive whole number value.
                    Float float_data = Math.abs(Float.parseFloat(raw_data));
                    int data = Math.round(float_data);

                    //if there is an increase in volume
                    if(bucket.getCurrent_vol() < data){

                        int increase_amount = data - bucket.getCurrent_vol();

                        //if the increase is enough to overfill set to max volume.
                        if(data > bucket.getMax_vol()){
                            System.out.println("Overfilled!");
                            bucket.setCurrent_vol(bucket.getMax_vol());
                        }
                        else{ //update to new volume
                            bucket.setCurrent_vol(data);
                            System.out.println("Bucket volume increased: " + bucket.getCurrent_vol() + "ml. Needed to fill: " + bucket.getNeeded_to_fill_vol() + "ml.");
                            System.out.println("Volume updated.");
                        }
                        updateCSV(bucket.getBucketid(), bucket.getCurrent_vol(), bucket.getNeeded_to_fill_vol(), totalDrunk, totalSpill, bucket.getMax_vol());
                        uploadLoadCellDataToDB(bucket.getBucketid(), bucket.getCurrent_vol(), bucket.getNeeded_to_fill_vol(), totalDrunk, totalSpill, bucket.getMax_vol());


                    }
                    else if(bucket.getCurrent_vol() > data){ //if there is a decrease in volume.

                        int decrease_amount = bucket.getCurrent_vol() - data;
                        System.out.println(decrease_amount);

                        if((bucket.getCurrent_vol() - data) > bucket.getMax_vol()*0.05){ //if the decrease is greater than 5%

                            //class as water drunk by a horse
                            bucket.setCurrent_vol(data);
                            System.out.println("Bucket volume decreased due to drinking: " + bucket.getCurrent_vol() + "ml. Needed to fill: " + bucket.getNeeded_to_fill_vol() + "ml.");
                            System.out.println("Volume updated.");

                            //Add record to drinkList
                            Record record = new Record(decrease_amount);
                            drinkList.add(record);
                            drinkList = cleanList(drinkList);
                            totalDrunk = getTotalDrunkorSpilled(drinkList);

                        }
                        else{
                            //decrease is less than 5% class as water spilled or something
                            bucket.setCurrent_vol(data);
                            System.out.println("Bucket volume decreased due to spill: " + bucket.getCurrent_vol() + "ml. Needed to fill: " + bucket.getNeeded_to_fill_vol() + "ml.");
                            System.out.println("Volume updated.");

                            //Add record to spill list.
                            Record record = new Record(decrease_amount);
                            spillList.add(record);
                            spillList = cleanList(spillList);
                            totalSpill = getTotalDrunkorSpilled(spillList);

                        }
                        updateCSV(bucket.getBucketid(), bucket.getCurrent_vol(), bucket.getNeeded_to_fill_vol(), totalDrunk, totalSpill, bucket.getMax_vol());
                        uploadLoadCellDataToDB(bucket.getBucketid(), bucket.getCurrent_vol(), bucket.getNeeded_to_fill_vol(), totalDrunk, totalSpill, bucket.getMax_vol());
                    }
                    else{ //No change
                        //System.out.println("No change in volume: " + bucket.getCurrent_vol());
                    }

                }

            }
            else {
                String raw_data = scanner.next();
                count += 1;
                if(count == 5){
                    System.out.println("Ready!");
                }
            }
        }
    }

    public static void main(String[] args) {

        //set constants
        String bucket_id = "B001";
        int max_capacity = 500;

        //setting up bucket
        System.out.println("Bucket setup.");
        Bucket bucket = new Bucket(bucket_id, max_capacity);

        //set up serial communication with arduino.
        SerialPort port = serialComSetup();

        //clean data from arduino and send to db.
        data(port, bucket);

    }

}

