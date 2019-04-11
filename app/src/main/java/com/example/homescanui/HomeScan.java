package com.example.homescanui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.SignOutOptions;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.regions.Regions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


public class HomeScan extends AppCompatActivity {

    static final String LOG_TAG = HomeScan.class.getCanonicalName();
    private static final String IOT_ENDPOINT = "";
    private Button gps_button;
    private static final int REQUEST_CODE_PERMISSION = 0;
    String fPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private LocationManager locationManager;
    private String SHADOW_UPDATE_TOPIC = "$aws/things/CapstonePi/shadow/update";
    String clientId;
    AWSIotMqttManager mqttManager;
    private String lockStatusSend;
    private static final String AWS_REGION = "US_WEST_2";
    String status;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private TextView textView;
    private String gpsJSON;
    private String SHADOW_SUBSCRIBE_TOPIC = "$aws/things/CapstonePi/shadow/update/accepted";
    GPSTracker gps;
    TextView  lockStatusAWS;
    private CognitoUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_scan);

        //Declare Buttons and Views in UI
        boolean gpsCheckActive;
        Button unlockBtn = (Button) findViewById(R.id.unlockBtn);
        Button lockBtn = (Button) findViewById(R.id.lockBtn);
        Button photoActivityBtn = findViewById(R.id.photoActivityBtn);
        lockStatusAWS = (TextView) findViewById(R.id.lockStatusAWS);

        //Set up LocationListener

        Criteria criteria = new  Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);


        CognitoUserPool userPool = new CognitoUserPool(HomeScan.this, new AWSConfiguration(HomeScan.this));
        user = userPool.getCurrentUser();

        // Implement callback handler for get details call
        GetDetailsHandler getDetailsHandler = new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                Map<String,String> map = cognitoUserDetails.getAttributes().getAttributes();
                Log.e("FLKDJ:LKSDF","Phone_number:" + cognitoUserDetails.getAttributes().getAttributes().get("phone_number"));
                //user.signOut();
                // The user detail are in cognitoUserDetails
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e("No","No");
                // Fetch user details failed, check exception for the cause
            }
        };

        // Fetch the user details
        user.getDetailsInBackground(getDetailsHandler);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsCheckActive = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (gpsCheckActive == true){
            Toast.makeText(getApplicationContext(), "GPS Active", Toast.LENGTH_SHORT).show();

        } else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);


            builder.setTitle("GPS Status");
            builder.setMessage("Please turn on GPS location services");

            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();

        }
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
        locationManager.requestLocationUpdates(20000,-1,criteria, gpsListener, null);
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != 0) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:07589be7-8ed8-46b9-89e8-ee97c41cd0c0", // Identity pool ID
                Regions.US_WEST_2 // Region
        );

        //Attempt MQTT connection
        try {
            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"AWS Connection Successful", Toast.LENGTH_LONG).show();
                            subscribe();
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

    public void subscribe(){
        try {
            mqttManager.subscribeToTopic(SHADOW_SUBSCRIBE_TOPIC, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);
                                        Toast.makeText(getApplicationContext(), "Status Update Received", Toast.LENGTH_LONG).show();
                                        JSONObject jOb = new JSONObject(message);
                                        JSONObject statejson = jOb.getJSONObject("state");
                                        JSONObject reportedjson = statejson.getJSONObject("reported");
                                        status = reportedjson.getString("Door Status");
                                        switch(status){
                                            case "Lock":
                                                lockStatusAWS.setText("Locked");
                                            case "Unlock":
                                                lockStatusAWS.setText("Unlocked");
                                        }

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }


    }

    LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String user = "testuser";
            gpsJSON =  "{\"state\":{\"reported\": {\"GPS Location\": [ \"" + latitude +"\",  \"" + longitude +"\", \"" + user +"\" ]  }}}";
            try {
                mqttManager.publishString(gpsJSON, SHADOW_UPDATE_TOPIC, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }
            String msg = "New Latitude: " + latitude + "\n" + "New Longitude: " + longitude;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();


        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}