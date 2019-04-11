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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class History extends AppCompatActivity {

    //DO NOT ALLOW THESE TWO ON GITHUB
    private final String KEY = "";
    private final String SECRET = "";

    DynamoDBMapper dynamoDBMapper;
    private BasicAWSCredentials credentials;
    public TextView historyResultText;
    public static final String
            ACTION_HISTORY_BROADCAST = History.class.getName() + "HistoryBroadcast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

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
                        ArrayList<String> text = intent.getStringArrayListExtra("History");
                        for (String s: text) {
                            historyResultText.append(s);
                        }
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
                    //Where the text will be placed
                    historyResultText = findViewById(R.id.historyResultText);

                    //Creates the query
                    Map<String, AttributeValue> eav = new HashMap<>();

                    /*
                    eav.put(":val1", new AttributeValue().withN("4446"));
                    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                            .withFilterExpression("PhoneNumDoorStatus = :val1").withExpressionAttributeValues(eav);
                            */

                    //Performs a scan and sends a broadcast to the receiver to update historyResultText
                    ArrayList<String> scanResultToArrayList = new ArrayList<>();
                    List<LockHistoryDO> scanResult = dynamoDBMapper.scan(LockHistoryDO.class, new DynamoDBScanExpression());
                    for (LockHistoryDO lock: scanResult) {
                        scanResultToArrayList.add(lock.getTime() + "  -  " + lock.getLockStatus() + "\n");
                    }
                    /*
                    for (LockHistoryDO lockHistoryItem: scanResult) {
                        Intent intent = new Intent(ACTION_HISTORY_BROADCAST);
                        intent.putExtra("History", lockHistoryItem.getTime()+"\t"+lockHistoryItem.getLockStatus()+"\n");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }*/
                    Intent intent = new Intent(ACTION_HISTORY_BROADCAST);
                    intent.putStringArrayListExtra("History", scanResultToArrayList);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
                catch(Exception e){
                    Log.e("FAILURE: ", e.toString());
                }
            }
        }).start();
    }
}
