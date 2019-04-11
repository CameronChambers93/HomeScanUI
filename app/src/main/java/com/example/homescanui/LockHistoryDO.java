package com.example.homescanui;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "History")

public class LockHistoryDO {
    private String _LockStatus;
    private String _Time;

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
}
