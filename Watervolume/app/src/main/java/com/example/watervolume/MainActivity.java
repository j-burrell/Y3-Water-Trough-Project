package com.example.watervolume;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    TextView troughid, maxcapacity, currentvolume, fillvolume, drinkvolume, spillvolume;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting up map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Get database reference.
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://water-volume-9cd60-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference myRef = database.getReference();


        // Read from the database
        myRef.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                HashMap<String, String> data = (HashMap<String, String>)dataSnapshot.getValue();
                System.out.println(data.toString());

                for(Map.Entry<String, String> entry : data.entrySet()){
                    String key = String.valueOf(entry.getKey());
                    String value = String.valueOf(entry.getValue());

                    //debugging puproses
                    //Log.d(TAG, "Key is: " + key);
                    //Log.d(TAG, "Value is: " + value);

                    if(key.equals("bucketid")){

                        troughid = findViewById(R.id.troughid);
                        troughid.setText(value);
                    }
                    if(key.equals("drunkvolume")){
                        drinkvolume = findViewById(R.id.waterdrank);
                        drinkvolume.setText(value+"ml");
                    }
                    if(key.equals("fillvolume")){
                        fillvolume = findViewById(R.id.fillvolume);
                        fillvolume.setText(value+"ml");
                    }
                    if(key.equals("maxvolume")){
                        maxcapacity = findViewById(R.id.maxcapacity);
                        maxcapacity.setText(value+"ml");
                    }
                    if(key.equals("spillvolume")){
                        spillvolume = findViewById(R.id.waterspilled);
                        spillvolume.setText(value+"ml");
                    }
                    if(key.equals("watervolume")){
                        currentvolume = findViewById(R.id.currentvolume);
                        currentvolume.setText(value+"ml");
                    }
                    if(key.equals("location")){

                        String locationinfo = value;
                        String[] latlondata = locationinfo.split("[,]", 0);
                        mapsUpdate(Double.parseDouble(latlondata[0]), Double.parseDouble(latlondata[1]));
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
    }

    private void mapsUpdate(double Lat, double Long) {

        mMap.clear();
        TextView textView = (TextView)findViewById(R.id.troughid);
        String markerText = textView.getText().toString();
        LatLng location = new LatLng(Lat, Long);

        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(markerText));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 18));
    }
}




