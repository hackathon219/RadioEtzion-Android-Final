package com.hackathon.radioetzion.fragments;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cloudant.sync.documentstore.DocumentNotFoundException;
import com.cloudant.sync.documentstore.DocumentStoreException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.hackathon.radioetzion.PodcastDBHelper;
import com.hackathon.radioetzion.R;
import com.hackathon.radioetzion.models.PodcastModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hackathon.radioetzion.MainActivity.mIsPlaying;
import static com.hackathon.radioetzion.MainActivity.mMediaBrowserHelper;
import static com.hackathon.radioetzion.MainActivity.mMediaController;


/**
 * A simple {@link Fragment} subclass.
 */
public class ExpandedPlayerFragment extends Fragment {
    private static final String TAG = "Log " + ExpandedPlayerFragment.class.getSimpleName();

    TextView tvTitle, tvBroadcasters, tvParticipants, tvDescription, tvElapsedTime, tvRemainingTime;
    ImageView ivArtwork;
    ImageButton ibComment, ibHeart, ibHeartSelected, ibPlay, ibPause, ibRewind10, ibSkip10;

    View view;
    SeekBar seekBar;
    PlaybackStateCompat state;
    Intent playPausePressed = new Intent("playPausePressed");
    IntentFilter playerState = new IntentFilter("playerState");
    long duration = 0;
    long progress = 0;
    private IntentFilter podcastDuration = new IntentFilter("podcastDuration");
    private boolean mIsTracking = false;
    private boolean userTrack = false;
    PodcastDBHelper dbHelper;

//    MediaControllerCompat mMediaController;
//    MediaBrowserHelper mMediaBrowserHelper;

    Handler mHandler;
    Runnable mRunnable;


    public ExpandedPlayerFragment() {
        // Required empty public constructor
    }

    public static ExpandedPlayerFragment newInstance(String title, String podcastID) {

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("podcastID", podcastID);
        ExpandedPlayerFragment fragment = new ExpandedPlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_expanded_player, container, false);
        findViews(view);

        dbHelper = PodcastDBHelper.getInstance(getContext());

        initFrame();

        ibPlay.setOnClickListener(playPauseListener);
        ibPause.setOnClickListener(playPauseListener);
        ibRewind10.setOnClickListener(fwrRwnListener);
        ibSkip10.setOnClickListener(fwrRwnListener);
        showCorrectPlaybackButton();


        Log.d(TAG, "onCreateView: setup mediaController");

        String podcastID = getArguments().getString("podcastID");


        favoriteListener(podcastID);

        ibHeart.setOnClickListener(v -> {
            List<String> favoritePodcast = getFavoritePodcast();
            favoritePodcast.add(podcastID);
            updateFavoriteList(favoritePodcast);
            ibHeart.setVisibility(View.INVISIBLE);
            ibHeartSelected.setVisibility(View.VISIBLE);
        });

        ibHeartSelected.setOnClickListener(v -> {
            List<String> favoritePodcast = getFavoritePodcast();
            favoritePodcast.remove(podcastID);
            updateFavoriteList(favoritePodcast);
            ibHeart.setVisibility(View.VISIBLE);
            ibHeartSelected.setVisibility(View.INVISIBLE);

        });


        ibComment.setOnClickListener(v -> {
            Intent fragmentTransfer = new Intent("fragmentTransfer");
            fragmentTransfer.putExtra("action", "transferToCommentsFragment");
            fragmentTransfer.putExtra("podcastID", podcastID);
//            Intent transferToCommentsFragment = new Intent("transferToCommentsFragment");
//            transferToCommentsFragment.putExtra("podcastID", podcastID);
            getContext().sendBroadcast(fragmentTransfer);

        });


        return view;
    }


    private void updateSeekbar(int duration) {
        System.out.println("the duration is : " + duration);
        if (duration < 1) return;
        tvRemainingTime.setText(millisToMinutes(duration));
        seekBar.setMax(duration);

        Handler mHandler = new Handler();

        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {

                state = mMediaController.getPlaybackState();
                if (state != null && !mIsTracking) {
                    progress = state.getPosition();
                    seekBar.setProgress((int) state.getPosition());
                    tvElapsedTime.setText(millisToMinutes(state.getPosition()));
                    tvRemainingTime.setText(millisToMinutes(duration - state.getPosition()));

                }
                mHandler.postDelayed(this, 1000);
            }

        };
        mRunnable.run();

    }

    private List<String> getFavoritePodcast(){
        List<String> favorites = new ArrayList<>();
        Gson gson = new Gson();
        SharedPreferences pref = getActivity().getSharedPreferences("appid_tokens", Context.MODE_PRIVATE);

        String s = pref.getString("favorites", null);
        if (s == null) return favorites;
        return gson.fromJson(s, ArrayList.class);
    }

    private void updateFavoriteList(List<String> favorites){
        SharedPreferences pref = getActivity().getSharedPreferences("appid_tokens", Context.MODE_PRIVATE);
        String key = pref.getString("key", null);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");
        ref.child(key).child("favoritePodcasts").setValue(favorites);

        saveFavoritesInPref(favorites);
    }

    private void saveFavoritesInPref(List<String> favorites){
        Gson gson = new Gson();
        String s = gson.toJson(favorites);
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("appid_tokens", Context.MODE_PRIVATE).edit();
        editor.putString("favorites", s).commit();
    }

    private void favoriteListener(String podcastID){
        if (getFavoritePodcast().contains(podcastID)){
            ibHeart.setVisibility(View.INVISIBLE);
            ibHeartSelected.setVisibility(View.VISIBLE);
        }else {
            ibHeart.setVisibility(View.VISIBLE);
            ibHeartSelected.setVisibility(View.INVISIBLE);
        }

    }


    private void findViews(View v) {
        tvTitle = v.findViewById(R.id.tvExpandedPlayerTitle);
        tvBroadcasters = v.findViewById(R.id.tvExpandedPlayerBroadcasters);
        tvParticipants = v.findViewById(R.id.tvExpandedPlayerParticipants);
        tvDescription = v.findViewById(R.id.tvExpandedPlayerDescription);
        tvElapsedTime = v.findViewById(R.id.tvExpandedPlayerTimePassed);
        tvRemainingTime = v.findViewById(R.id.tvExpandedPlayerTimeLeft);
        ivArtwork = v.findViewById(R.id.ivExpandedPlayerArtwork);
        ibComment = v.findViewById(R.id.ibExpandedPlayerComment);
        ibHeart = v.findViewById(R.id.ibExpandedPlayerHeart);
        ibHeartSelected = v.findViewById(R.id.ibExpandedPlayerHeartSelected);
        ibPlay = v.findViewById(R.id.ibExpandedPlayerPlay);
        ibPause = v.findViewById(R.id.ibExpandedPlayerPause);
        ibRewind10 = v.findViewById(R.id.ibExpandedPlayerRwd);
        ibSkip10 = v.findViewById(R.id.ibExpandedPlayerFwd);
        seekBar = view.findViewById(R.id.sbExpandedPlayer);
    }

    private void initFrame() {
        try {

            if (getArguments() != null) {
                String title = getArguments().getString("title");
                String podcastID = getArguments().getString("podcastID");

                PodcastModel podcast = dbHelper.getPodcastObjByDocId(podcastID);
                System.out.println("results " + podcast);

                tvTitle.setText(title);
                tvBroadcasters.setText(getBroadcasters(podcast));
                System.out.println(getParticipants(podcast).length());
                if (getParticipants(podcast).length() == 9)tvParticipants.setVisibility(View.GONE);
                tvParticipants.setText(getParticipants(podcast));
                tvDescription.setText(podcast.getDescription());

            }


        } catch (DocumentStoreException | DocumentNotFoundException e) {
            e.printStackTrace();
        }

    }

    View.OnClickListener playPauseListener = v -> {
        getContext().sendBroadcast(playPausePressed);
    };

    View.OnClickListener fwrRwnListener = v -> {
        switch (v.getId()) {

            case R.id.ibExpandedPlayerFwd:
                if (mMediaBrowserHelper != null)
                    mMediaBrowserHelper.getTransportControls().seekTo(progress + 10000);
                break;

            case R.id.ibExpandedPlayerRwd:
                if (mMediaBrowserHelper != null)
                    mMediaBrowserHelper.getTransportControls().seekTo(progress - 10000);
                break;
        }
    };

    BroadcastReceiver playBackStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//                mIsPlaying = intent.getBooleanExtra("mIsPlaying", false);
            showCorrectPlaybackButton();
        }
    };


    private void showCorrectPlaybackButton() {
        if (mIsPlaying) {
            ibPlay.setVisibility(View.INVISIBLE);
            ibPause.setVisibility(View.VISIBLE);

        } else {
            ibPlay.setVisibility(View.VISIBLE);
            ibPause.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        duration = mMediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        updateSeekbar((int) duration);
        tvElapsedTime.setVisibility(View.VISIBLE);
        tvRemainingTime.setVisibility(View.VISIBLE);

        getActivity().registerReceiver(playBackStateReceiver, playerState);


        seekBar.setOnSeekBarChangeListener(seekBarListener);

//        getActivity().registerReceiver(podcastDurationReceiver, podcastDuration);
        showCorrectPlaybackButton();


        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onPause() {
        super.onPause();
        seekBar.setOnSeekBarChangeListener(null);
        getActivity().unregisterReceiver(playBackStateReceiver);
        clearHandlers();
        Log.d(TAG, "onPause: ");

        //        getActivity().unregisterReceiver(podcastDurationReceiver);
    }


    private void clearHandlers() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mIsTracking = true;
                if (mMediaBrowserHelper != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaBrowserHelper.getTransportControls().pause();
                    userTrack = true;
                }

                tvElapsedTime.setText(millisToMinutes(progress));
                tvRemainingTime.setText(millisToMinutes(duration - progress));

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsTracking = false;
            if (mMediaBrowserHelper != null) {
                mMediaBrowserHelper.getTransportControls().seekTo(seekBar.getProgress());
                state = mMediaController.getPlaybackState();
                if (state != null && state.getState() == PlaybackStateCompat.STATE_PAUSED && userTrack) {
                    mMediaBrowserHelper.getTransportControls().play();
                    userTrack = false;
                }

            } else {
                Log.d(TAG, "onProgressChanged: mMediaBrowserHelper is null");
            }
        }
    };

    private String millisToMinutes(long time) {
//

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return (new SimpleDateFormat("mm:ss")).format(new Date(time));
        } else {
            @SuppressLint("DefaultLocale") String format = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(time),
                    TimeUnit.MILLISECONDS.toSeconds(time) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
            );
            return format;

        }


    }

    private StringBuilder getBroadcasters(PodcastModel podcast) {
        StringBuilder builder = new StringBuilder();
        builder.append("שדרנים: ");
        for (String broadcaster : podcast.getBroadcasters()) {
            builder.append(broadcaster + ", ");
        }

        builder.replace(builder.length() - 2, builder.length() - 1, ".");

        return builder;
    }


    private StringBuilder getParticipants(PodcastModel podcast) {
        StringBuilder builder = new StringBuilder();
        builder.append("משתתפים: ");
        for (String participant : podcast.getParticipants()) {
            builder.append(participant + ", ");
        }

        builder.replace(builder.length() - 2, builder.length() - 1, ".");

        return builder;
    }

}
