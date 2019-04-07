

package com.hackathon.radioetzion.mediaplayer;

import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;


/**
 * Listener to provide state updates from {@link MediaPlayerAdapter} (the media player)
 * to {@link MusicService} (the service that holds our {@link MediaSessionCompat}.
 */
public abstract class PlaybackInfoListener {

    private static final String TAG = "Log " + PlaybackInfoListener.class.getSimpleName();

    public abstract void onPlaybackStateChange(PlaybackStateCompat state);

    public void onPlaybackCompleted() {
        Log.d(TAG, "onPlaybackCompleted: ");
    }
}