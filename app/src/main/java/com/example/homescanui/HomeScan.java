package com.example.homescanui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


public class HomeScan extends AppCompatActivity {

    static final String LOG_TAG = HomeScan.class.getCanonicalName();
    private static final String IOT_ENDPOINT = "a2vd7uyfqfuks5-ats.iot.us-west-2.amazonaws.com";
    private Button gps_button;
    private static final int REQUEST_CODE_PERMISSION = 0;
    String fPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private LocationManager locationManager;
    private String SHADOW_UPDATE_TOPIC = "$aws/things/CapstonePi/shadow/update";
    String clientId;
    AWSIotMqttManager mqttManager;
    private String lockStatusSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_scan);

        //Declare Buttons and Views in UI
        final TextView gpsView = (TextView) findViewById(R.id.gpsView);
        Button unlockBtn = (Button) findViewById(R.id.unlockBtn);
        Button lockBtn = (Button) findViewById(R.id.lockBtn);
        Button photoActivityBtn = findViewById(R.id.photoActivityBtn);

        //Set up LocationListener
        LocationListener gpsListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String msg = "New Latitude: " + latitude + "New Longitude: " + longitude;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                gpsView.setText("New Latitude: " + latitude + "New Longitude: " + longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onProviderDisabled(String provider) { }
        };
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1, gpsListener);

        clientId = UUID.randomUUID().toString();

        //Initialize latch
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(
                getApplicationContext(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        Log.e(LOG_TAG, "onError: ", e);
                    }
                }
        );
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mqttManager = new AWSIotMqttManager(clientId,IOT_ENDPOINT);

        unlockBtn.setEnabled(true);
        lockBtn.setEnabled(true);

        //Attempt MQTT connection
        try {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"AWS Connection Successful", Toast.LENGTH_LONG).show();
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            Toast.makeText(getApplicationContext(),"AWS Connection Error",Toast.LENGTH_LONG ).show();
        }

        //Setup the buttons
        lockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                lockStatusSend = "{\"state\":{\"desired\": {\"Door Status\": \"Lock\"}}}";
                try {
                    mqttManager.publishString(lockStatusSend, SHADOW_UPDATE_TOPIC, AWSIotMqttQos.QOS0);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Publish error.", e);
                }
        }});

        unlockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                lockStatusSend = "{\"state\":{\"desired\": {\"Door Status\": \"Unlock\"}}}";
                try {
                    mqttManager.publishString(lockStatusSend, SHADOW_UPDATE_TOPIC, AWSIotMqttQos.QOS0);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Publish error.", e);
                }
            }});

        gps_button = findViewById(R.id.button2);
        gps_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeScan.this, History.class);
                startActivity(intent);
            }
        });

        photoActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeScan.this, PhotoActivity.class);
                startActivity(intent);
            }
        });



        //Prompt for Storage permission. If both prompts are on same page, second prompt might not activate
        try {
            if (ActivityCompat.checkSelfPermission(this, fPermission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{fPermission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
