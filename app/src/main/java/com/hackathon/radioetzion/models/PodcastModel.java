package com.hackathon.radioetzion.models;

import com.cloudant.sync.documentstore.DocumentRevision;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PodcastModel implements Serializable, Comparable<PodcastModel> {

    private DocumentRevision rev;

    private static final String DOC_TYPE = "podcast";
    private String name;
    private String description;
    private ArrayList<String> broadcasters;
    private ArrayList<String> participants;
    private ArrayList<String> commentsIDs;
    private String urlAddress;
    private Long timestamp;
    private int duration;
    private String type = DOC_TYPE;
    private boolean isFavorite;
    private int imageID;

    public PodcastModel(String name, String description, ArrayList<String> broadcasters, ArrayList<String> participants, ArrayList<String> commentsIDs, String url, int duration, Long timestamp, int imageID) {
        this.name = name;
        this.description = description;
        this.broadcasters = broadcasters;
        this.participants = participants;
        this.commentsIDs = commentsIDs;
        this.urlAddress = url;
        this.duration = duration;
        this.timestamp = timestamp;
        this.imageID = imageID;
    }


    public PodcastModel(String name, String description, ArrayList<String> broadcasters, ArrayList<String> participants, String urlAddress, boolean isFavorite, int duration, Long timestamp, int imageID) {
        this.name = name;
        this.description = description;
        this.broadcasters = broadcasters;
        this.participants = participants;
        this.urlAddress = urlAddress;
        this.isFavorite = isFavorite;
        this.duration = duration;
        this.timestamp = timestamp;
        this.imageID = imageID;

    }

    public PodcastModel(String name, String description, ArrayList<String> broadcasters, ArrayList<String> participants, String url, int duration, long timestamp, int imageID) {
        this.name = name;
        this.description = description;
        this.broadcasters = broadcasters;
        this.participants = participants;
        this.urlAddress = url;
        this.duration = duration;
        this.timestamp = timestamp;
        this.type = DOC_TYPE;
        this.imageID = imageID;
    }

    public PodcastModel() {
    }

    public int getImageID() {
        return imageID;
    }

    public void setImageID(int imageID) {
        this.imageID = imageID;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public DocumentRevision getRev() {
        return rev;
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

    public void setRev(DocumentRevision rev) {
        this.rev = rev;
    }

    public ArrayList<String> getCommentsIDs() {
        return commentsIDs;
    }

    public void setCommentsIDs(ArrayList<String> commentsIDs) {
        this.commentsIDs = commentsIDs;
    }

    public ArrayList<String> getBroadcasters() {
        return broadcasters;
    }

    public void setBroadcasters(ArrayList<String> broadcasters) {
        this.broadcasters = broadcasters;
    }

    public ArrayList<String> getParticipants() {
        return participants;
    }

    public void setParticipants(ArrayList<String> participants) {
        this.participants = participants;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrlAddress() {
        return urlAddress;
    }

    public void setUrlAddress(String urlAddress) {
        this.urlAddress = urlAddress;
    }

    public static PodcastModel fromRevision(DocumentRevision rev) {
        PodcastModel modelShow = new PodcastModel();
        modelShow.rev = rev;
        Map<String, Object> map = rev.getBody().asMap();
        if(map.containsKey("type") && map.get("type").equals(PodcastModel.DOC_TYPE)) {

            modelShow.setName((String) map.get("name"));
            if (map.get("duration") != null){
                modelShow.setDuration((int) map.get("duration"));
            }
            modelShow.setTimestamp((Long) map.get("timestamp"));
            modelShow.setDescription((String) map.get("description"));
            modelShow.setBroadcasters((ArrayList<String>) map.get("broadcasters"));
            modelShow.setParticipants((ArrayList<String>) map.get("participants"));
            modelShow.setCommentsIDs((ArrayList<String>) map.get("commentsIDs"));
            modelShow.setUrlAddress((String) map.get("urlAddress"));

            return modelShow;
        }
        return null;
    }

    public Map<String, Object> asMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("name", name);
        map.put("description", description);
        map.put("broadcasters", broadcasters);
        map.put("participants", participants);
        map.put("commentsIDs", commentsIDs);
        map.put("urlAddress", urlAddress);
        map.put("duration", duration);
        map.put("timestamp", timestamp);
        return map;
    }

    @Override
    public String toString() {
        return "PodcastModel{" +
                "rev=" + rev +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", broadcasters=" + broadcasters +
                ", participants=" + participants +
                ", commentsIDs=" + commentsIDs +
                ", urlAddress='" + urlAddress + '\'' +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", type='" + type + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }

    public String printBroadcastersAndParticipants(){
        String result = "";
        ArrayList<String> wholeList = new ArrayList<>();
        wholeList.addAll(getBroadcasters());
        wholeList.addAll(getParticipants());

        for (int i = 0; i < wholeList.size(); i++) {
            if (i == wholeList.size()-1){
                result += wholeList.get(i);
            }else {
                result += wholeList.get(i) + ", ";
            }
        }
        return result;
    }

    @Override
    public int compareTo(PodcastModel o) {
        return o.getTimestamp().compareTo(getTimestamp());
    }
}
