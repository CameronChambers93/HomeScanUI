package com.example.homescanui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;



public class HomeScan extends AppCompatActivity {

    private Button button2;
    private Button shadow_button;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_scan);


        final TextView tv = (TextView) findViewById(R.id.gps_history);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String latitude = intent.getStringExtra(GPSTracker.EXTRA_LATITUDE);
                        String longitude = intent.getStringExtra(GPSTracker.EXTRA_LONGITUDE);
                        tv.append("(" + latitude + ",\t" + longitude + ")\n");
                    }
                }, new IntentFilter(GPSTracker.ACTION_LOCATION_BROADCAST)
        );

        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(HomeScan.this, History.class);
                startActivity(intent2);
            }
        });

        gps = new GPSTracker(HomeScan.this);
    }
}
