package com.example.homescanui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.time.LocalTime;

public class History extends AppCompatActivity {

    DynamoDBMapper dynamoDBMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // AWSMobileClient enables AWS user credentials to access your table
        AWSMobileClient.getInstance().initialize(this).execute();

        //This credentialsProvider allows users to access the DynamoDB
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:5f5ebaab-898a-4a97-a21b-fa40128de072", // Identity pool ID
                Regions.US_WEST_2 // Region
        );
        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();

        // Add code to instantiate a AmazonDynamoDBClient
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();
        createHistory();
        try {
            wait(3000);
        }
        catch (Exception e){
            Log.e("Error", "idk");
        }
        readHistory();
    }

    public void readUser() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Go: ", "Going");
                try {
                    UsersDO newsItem = dynamoDBMapper.load(
                            UsersDO.class,
                            "Article1",
                            "This is the article content");
                    // Item read
                    Log.d("SUCCESS, ITEM GENERATED: ", "\n"+newsItem.getPiId()+"\n"+newsItem.getUserId());
                }
                catch(Exception e){
                    Log.e("FAILURE: ", e.toString());
                }
            }
        }).start();
    }

    public void createUser() {
        final UsersDO userItem= new UsersDO();

        userItem.setUserId("Article1");
        userItem.setPiId("This is the article content");

        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(userItem);
                Log.e("Test", "Success");
                // Item saved
            }
        }).start();
    }


    public void readHistory() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Go: ", "Going");
                try {
                    HisroryDO historyItem = dynamoDBMapper.load(
                            HisroryDO.class,
                            "User1",
                            "Current time");

                    // Item read
                    Log.d("SUCCESS, ITEM GENERATED: ", "\n"+historyItem.getUserId()+"\n"+historyItem.getTime()+"\n"+historyItem.getLockAction());
                }
                catch(Exception e){
                    Log.e("FAILURE: ", e.toString());
                }
            }
        }).start();
    }

    public void createHistory() {
        final HisroryDO historyItem = new HisroryDO();

        historyItem.setUserId("User1");
        historyItem.setTime("Current time");
        historyItem.setLockAction("Locked");

        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(historyItem);
                Log.e("Test", "Success");
                // Item saved
            }
        }).start();
    }
}
