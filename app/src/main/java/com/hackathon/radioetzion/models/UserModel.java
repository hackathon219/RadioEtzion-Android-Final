package com.hackathon.radioetzion.models;

import java.util.ArrayList;

public class UserModel {
    private String userID;
    private String lastDeviceID;
    private ArrayList<String> favoritePodcasts;

    public UserModel(String userID, String lastDeviceID, ArrayList<String> favoritePodcasts) {
        this.userID = userID;
        this.lastDeviceID = lastDeviceID;
        this.favoritePodcasts = favoritePodcasts;
    }

    public UserModel(String userID, ArrayList<String> favoritePodcasts) {
        this.userID = userID;
        this.favoritePodcasts = favoritePodcasts;
    }


    public UserModel() {
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getLastDeviceID() {
        return lastDeviceID;
    }

    public void setLastDeviceID(String lastDeviceID) {
        this.lastDeviceID = lastDeviceID;
    }

    public ArrayList<String> getFavoritePodcasts() {
        return favoritePodcasts;
    }

    public void setFavoritePodcasts(ArrayList<String> favoritePodcasts) {
        this.favoritePodcasts = favoritePodcasts;
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "userID='" + userID + '\'' +
                ", lastDeviceID='" + lastDeviceID + '\'' +
                ", favoritePodcasts=" + favoritePodcasts +
                '}';
    }
}
