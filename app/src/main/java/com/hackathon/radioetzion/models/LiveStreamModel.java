package com.hackathon.radioetzion.models;

public class LiveStreamModel {
    String name;
    String description;
    String streamUrl;

    public LiveStreamModel(String name, String description, String streamUrl) {
        this.name = name;
        this.description = description;
        this.streamUrl = streamUrl;
    }

    public LiveStreamModel(String name, String streamUrl) {
        this.name = name;
        this.streamUrl = streamUrl;
    }

    public LiveStreamModel(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public LiveStreamModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    @Override
    public String toString() {
        return "LiveStreamModel{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                '}';
    }
}
