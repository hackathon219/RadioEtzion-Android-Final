package com.hackathon.radioetzion.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class CommentModel {
    private String userName;
    private String message;
    private String userID;
    private long timestamp;


    public CommentModel() {}  // Needed for Firebase

    public CommentModel(String userName, String message, String userID) {
        this.userName = userName;
        this.message = message;
        this.userID = userID;
        this.timestamp = timestamp;
    }

    @Exclude
    public long getTimestampLong(){return timestamp;}

    public Map<String, String> getTimestamp(){return ServerValue.TIMESTAMP;}

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }



}