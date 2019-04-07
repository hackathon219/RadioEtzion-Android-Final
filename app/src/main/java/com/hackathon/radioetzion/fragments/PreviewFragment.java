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
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
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
import com.hackathon.radioetzion.MainActivity;
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
import static com.hackathon.radioetzion.MainActivity.playbackStateCompat;
import static com.hackathon.radioetzion.MainActivity.podcastID;


/**
 * A simple {@link Fragment} subclass.
 */
public class PreviewFragment extends Fragment {
    private static final String TAG = "Log " + ExpandedPlayerFragment.class.getSimpleName();

    TextView tvTitle, tvBroadcasters, tvParticipants, tvDescription;
    ImageView ivArtwork;
    ImageButton ibComment, ibHeart, ibHeartSelected, ibExpandedPlayerDismiss;
    private String selfPodcastID;
    View hideCollapsePlayer;
    private boolean playerCollapse;
    private boolean bottomSheetVisible;


    View view;
    SeekBar seekBar;
    long progress = 0;
    PodcastDBHelper dbHelper;

    Handler mHandler;
    Runnable mRunnable;


    public PreviewFragment() {
        // Required empty public constructor
    }

    public static PreviewFragment newInstance(String title, String podcastID) {

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("podcastID", podcastID);
        PreviewFragment fragment = new PreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_item_preview, container, false);
        findViews(view);

        dbHelper = PodcastDBHelper.getInstance(getContext());

        initFrame();
        ibExpandedPlayerDismiss.setOnClickListener(v -> ((MainActivity)getActivity()).onBackPressed());



        Log.d(TAG, "onCreateView: setup mediaController");

        selfPodcastID = getArguments().getString("podcastID");


        ibComment.setOnClickListener(v -> {
            Intent fragmentTransfer = new Intent("fragmentTransfer");
            fragmentTransfer.putExtra("action", "transferToCommentsFragmentFromPreview");
            fragmentTransfer.putExtra("podcastID", selfPodcastID);
            getContext().sendBroadcast(fragmentTransfer);
        });

        playerCollapse = ((MainActivity)getActivity()).bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
        hideCollapsePlayer.setVisibility(playerCollapse ? View.VISIBLE : View.GONE);


        favoriteListener();

        ibHeart.setOnClickListener(v -> {
            List<String> favoritePodcast = getFavoritePodcast();
            favoritePodcast.add(selfPodcastID);
            updateFavoriteList(favoritePodcast);
            ibHeart.setVisibility(View.INVISIBLE);
            ibHeartSelected.setVisibility(View.VISIBLE);
        });

        ibHeartSelected.setOnClickListener(v -> {
            List<String> favoritePodcast = getFavoritePodcast();
            favoritePodcast.remove(selfPodcastID);
            updateFavoriteList(favoritePodcast);
            ibHeart.setVisibility(View.VISIBLE);
            ibHeartSelected.setVisibility(View.INVISIBLE);

        });


        return view;
    }




    BroadcastReceiver playerState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playerCollapse = ((MainActivity)getActivity()).bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
            hideCollapsePlayer.setVisibility(playerCollapse ? View.VISIBLE : View.GONE);
        }
    };

    private void favoriteListener(){
        if (getFavoritePodcast().contains(selfPodcastID)){
            ibHeart.setVisibility(View.INVISIBLE);
            ibHeartSelected.setVisibility(View.VISIBLE);
        }else {
            ibHeart.setVisibility(View.VISIBLE);
            ibHeartSelected.setVisibility(View.INVISIBLE);
        }

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
        if (key == null) key = "tempKey";

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

    private void findViews(View v) {
        tvTitle = v.findViewById(R.id.tvExpandedPlayerTitle);
        tvBroadcasters = v.findViewById(R.id.tvExpandedPlayerBroadcasters);
        tvParticipants = v.findViewById(R.id.tvExpandedPlayerParticipants);
        tvDescription = v.findViewById(R.id.tvExpandedPlayerDescription);
        ivArtwork = v.findViewById(R.id.ivExpandedPlayerArtwork);
        ibComment = v.findViewById(R.id.ibExpandedPlayerComment);
        ibHeart = v.findViewById(R.id.ibExpandedPlayerHeart);
        ibHeartSelected = v.findViewById(R.id.ibExpandedPlayerHeartSelected);
        ibExpandedPlayerDismiss = v.findViewById(R.id.ibExpandedPlayerDismiss);
        hideCollapsePlayer = v.findViewById(R.id.hideCollapsePlayer);
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

    private void heartListener(){
        ibHeart.setOnClickListener(v -> {

        });
    }




    @Override
    public void onResume() {
        super.onResume();

        getContext().registerReceiver(playerState, new IntentFilter("playerState"));
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        getContext().unregisterReceiver(playerState);

        //        getActivity().unregisterReceiver(podcastDurationReceiver);
    }




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
