

package com.hackathon.radioetzion.mediaplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;


import com.hackathon.radioetzion.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;


public class MusicLibrary  {

    private static final TreeMap<String, MediaMetadataCompat> music = new TreeMap<>();
    private static final TreeMap<String, MediaMetadataCompat> singleMusic = new TreeMap<>();
    private static final HashMap<String, Integer> albumRes = new HashMap<>();
    private static final HashMap<String, String> musicFileName = new HashMap<>();
    private static final HashMap<String, String> musicUri = new HashMap<>();
    private static final String TAG = "Log " + MusicLibrary.class.getSimpleName();



//    static {
//
//
//        Log.d(TAG, "static initializer: ");
//
//        createMediaMetadataCompat(
//                "Jazz_In_Paris",
//                "Jazz in Paris",
//                "Media Right Productions",
//                "Jazz & Blues",
//                "Jazz",
//                103,
//                TimeUnit.SECONDS,
//                "jazz_in_paris.mp3",
//                R.drawable.album_jazz_blues,
//                "album_jazz_blues",
//                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
//
//        createMediaMetadataCompat(
//                "The_Coldest_Shoulder",
//                "The Coldest Shoulder",
//                "The 126ers",
//                "Youtube Audio Library Rock 2",
//                "Rock",
//                160,
//                TimeUnit.SECONDS,
//                "the_coldest_shoulder.mp3",
//                R.drawable.album_youtube_audio_library_rock_2,
//                "album_youtube_audio_library_rock_2",
//                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3");
//    }

    public static String getRoot() {
        Log.d(TAG, "getRoot: ");
        return "root";
    }

    private static String getAlbumArtUri(String albumArtResName) {
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName;
    }

    public static String getMusicFilename(String mediaId) {
        return musicFileName.containsKey(mediaId) ? musicFileName.get(mediaId) : null;
    }

    public static String getMusicUri(String mediaId) {
        return musicUri.containsKey(mediaId) ? musicUri.get(mediaId) : null;
    }





    public static List<MediaBrowserCompat.MediaItem> getMediaItems() {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (MediaMetadataCompat metadata : music.values()) {
            result.add(
                    new MediaBrowserCompat.MediaItem(
                            metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    public static MediaMetadataCompat getMetadata(Context context, String mediaId) {
        MediaMetadataCompat metadataWithoutBitmap = music.get("i");

        // Since MediaMetadataCompat is immutable, we need to create a copy to set the album art.
        // We don't set it initially on all items so that they don't take unnecessary memory.


//        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
//
//        for (String key : new String[]{
//                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
//                        MediaMetadataCompat.METADATA_KEY_TITLE,
//                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI
//                }) {
//            builder.putString(key, metadataWithoutBitmap.getString(key));
//        }
//        builder.putLong(
//                MediaMetadataCompat.METADATA_KEY_DURATION,
//                metadataWithoutBitmap.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
//        return builder.build();
        return metadataWithoutBitmap;
    }

    public static void createMediaMetadataCompat(
            String mediaId,
            String title,
            long duration,
            String musicFilename,
            String uri) {
        music.clear();
        music.put(
                "i",
                new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri)
                        .build());
        musicFileName.clear();
        musicUri.clear();
        musicFileName.put(mediaId, musicFilename);
        musicUri.put(mediaId, uri);
    }
}