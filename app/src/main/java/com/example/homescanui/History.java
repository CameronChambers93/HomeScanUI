package com.example.homescanui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class History extends AppCompatActivity {

    //DO NOT ALLOW THESE TWO ON GITHUB
    private final String KEY = "";
    private final String SECRET = "";

    DynamoDBMapper dynamoDBMapper;
    private BasicAWSCredentials credentials;
    public TextView historyResultText;
    public TextView timeText;
    public  TextView lockText;
    public static final String ACTION_HISTORY_BROADCAST = History.class.getName() + "HistoryBroadcast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //Where the text will be placed
        historyResultText = findViewById(R.id.historyResultText);
        timeText = findViewById(R.id.timeText);
        lockText = findViewById(R.id.lockText);

        //Instantiate a DynamoDB Client using the access KEY and SECRET
        AWSMobileClient.getInstance().initialize(this).execute();
        credentials = new BasicAWSCredentials(KEY,SECRET);
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentials);

        //May not find DB if region not set
        dynamoDBClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        this.dynamoDBMapper = new DynamoDBMapper(dynamoDBClient);

        //The thread that queries the DB can't update the view, so use a BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String time = intent.getStringExtra("Time");
                        String status = intent.getStringExtra("Status");
                        timeText.append("\n" + time);
                        lockText.append("\n" + status);

                    }
                }, new IntentFilter(History.ACTION_HISTORY_BROADCAST)
        );
        readLockHistoryThread();
    }


    //Query must be ran in separate thread
    public void readLockHistoryThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Performs a scan and sends a broadcast to the receiver to update historyResultText
                    List<LockHistoryDO> scanResult = dynamoDBMapper.scan(LockHistoryDO.class, new DynamoDBScanExpression());

                    processResults(scanResult);
                }
                catch(Exception e){
                    Log.e("FAILURE: ", "here", e);
                }
            }
        }).start();
    }

    /* This sends out the results of the Dynamo scan to the history view. The DynamoDB scan iterator does not iterate correctly, so
        we can't sort based on time for now.
     */
    public void processResults(List<LockHistoryDO> scanResult) {
        /*
        ArrayList<LockHistoryDO> cleanResult = new ArrayList<LockHistoryDO>();

        for (LockHistoryDO lock: scanResult) {
            cleanResult.add(lock);
        }

        Collections.sort(cleanResult);

        Intent intent = new Intent(ACTION_HISTORY_BROADCAST);

        for (LockHistoryDO lock: cleanResult) {
            intent.putExtra("Time", lock.getTime());
            intent.putExtra("Status", lock.getLockStatus());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        */

        for (LockHistoryDO lock: scanResult) {
            Intent intent = new Intent(ACTION_HISTORY_BROADCAST);
            intent.putExtra("Time", lock.getTime());
            intent.putExtra("Status", lock.getLockStatus());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

    }
}
