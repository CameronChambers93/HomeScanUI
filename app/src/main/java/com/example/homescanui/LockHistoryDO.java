package com.example.homescanui;

import android.util.Log;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@DynamoDBTable(tableName = "History")

public class LockHistoryDO implements Comparable<LockHistoryDO> {
    private String _LockStatus;
    private String _Time;
    private Date date;

    @DynamoDBHashKey(attributeName = "Time")
    @DynamoDBAttribute(attributeName = "Time")
    public String getTime() {
        return _Time;
    }

    public void setTime(final String _Time) {
        this._Time = _Time;
    }

    @DynamoDBAttribute(attributeName = "LockStatus")
    public String getLockStatus() {
        return _LockStatus;
    }

    public void setLockStatus(final String _lockStatus) {
        this._LockStatus = _lockStatus;
    }

    public Date getDate() throws ParseException {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        return new SimpleDateFormat(pattern, Locale.US).parse(_Time);
    }


    @Override
    public int compareTo(LockHistoryDO t) {
        try {
            return getDate().compareTo(t.getDate());
        } catch (ParseException e) {
            Log.e("PARSING ERROR",  _Time, e);
        }
        return -1;
    }
}
