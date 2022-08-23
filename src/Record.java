import java.time.LocalTime;

public class Record {

    private int decreaseVolume;
    private LocalTime time;

    public Record(int decreaseVolume){

        this. decreaseVolume = decreaseVolume;
        this.time = LocalTime.now();
    }

    public int getDecreaseVolume() {
        return decreaseVolume;
    }

    public LocalTime getTime() {
        return time;
    }
}
