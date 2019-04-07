package com.hackathon.radioetzion;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hackathon.radioetzion.models.LiveStreamModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LiveStreamAsync extends AsyncTask<Void, Void, LiveStreamModel> {

    String urlAddress = "http://be.repoai.com:5080/WebRTCAppEE/rest/broadcast/get";

    @Override
    protected void onPostExecute(LiveStreamModel liveStreamModel) {
        super.onPostExecute(liveStreamModel);

    }


    @Override
    protected LiveStreamModel doInBackground(Void... params) {

        LiveStreamModel liveStreamModel = new LiveStreamModel();
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlAddress);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
                Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

            }

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(buffer.toString());
            JsonObject model = element.getAsJsonObject();

            String streamUrl = model.get("streamUrl").toString();
            String streamId = model.get("streamId").toString();
            String name = model.get("type").toString();
            String description = model.get("description").toString();

            liveStreamModel.setStreamUrl(streamUrl);
            liveStreamModel.setDescription(description);
            liveStreamModel.setName(name);

//            System.out.println(model.toString());

//            JsonObject type = model.getAsJsonObject("type");
//            System.out.println("type " + type.toString());

            return liveStreamModel;


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}