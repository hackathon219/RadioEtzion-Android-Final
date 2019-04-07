package com.hackathon.radioetzion.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hackathon.radioetzion.MainActivity;
import com.hackathon.radioetzion.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class CollapsedPlayerFragment extends Fragment {
    private static final String TAG = CollapsedPlayerFragment.class.getSimpleName();
    ImageButton ibPlay;
    ImageButton ibPause;
    TextView tvTrackTitle;
    View view;
    ProgressBar progressBar;
    Intent playPausePressed = new Intent("playPausePressed");
    IntentFilter playerState = new IntentFilter("playerState");
    boolean mIsPlaying;
    boolean mIsPause;
    boolean mIsLoading;



    public CollapsedPlayerFragment() {
        // Required empty public constructor
    }

    public static CollapsedPlayerFragment newInstance(String title) {

        Bundle args = new Bundle();
        args.putString("title", title);
        CollapsedPlayerFragment fragment = new CollapsedPlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void findViews(View view) {
        ibPlay = view.findViewById(R.id.ibMinPlayerPlay);
        ibPause = view.findViewById(R.id.ibMinPlayerPause);
        tvTrackTitle = view.findViewById(R.id.tvMinPlayerTrackTitle);
        progressBar = view.findViewById(R.id.progressBar);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
//        mIsPlaying = savedInstanceState.getBoolean("mIsPlaying", false);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_collapsed_player, container, false);

        findViews(view);
        initFrame();

        mIsPlaying = ((MainActivity) getActivity()).mIsPlaying;
        mIsPause = ((MainActivity) getActivity()).mIsPause;
        mIsLoading = ((MainActivity) getActivity()).mIsLoading;
//        showCorrectPlaybackButton();

        ibPlay.setOnClickListener(playPauseListener);
        ibPause.setOnClickListener(playPauseListener);


        if (!mIsPause && !mIsPlaying){
            progressBar.setVisibility(View.VISIBLE);
            ibPlay.setVisibility(View.GONE);
        }else{
            showCorrectPlaybackButton();
        }


        return view;
    }

    View.OnClickListener playPauseListener = v -> {
        getContext().sendBroadcast(playPausePressed);
    };

    BroadcastReceiver playBackStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsPlaying = intent.getBooleanExtra("mIsPlaying", false);
            mIsLoading = intent.getBooleanExtra("mIsLoading", false);
            showCorrectPlaybackButton();

        }
    };


    private void showCorrectPlaybackButton() {
        if (mIsPlaying) {
            progressBar.setVisibility(View.GONE);
            ibPlay.setVisibility(View.GONE);
            ibPause.setVisibility(View.VISIBLE);
        }else if (progressBar.getVisibility() == View.GONE){
            ibPlay.setVisibility(View.VISIBLE);
            ibPause.setVisibility(View.GONE);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(playBackStateReceiver, playerState);
//        if (!mIsPlaying){
//            progressBar.setVisibility(View.VISIBLE);
//        }else {
//            progressBar.setVisibility(View.GONE);
//        }
//        showCorrectPlaybackButton();

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(playBackStateReceiver);
    }

    private void initFrame() {
        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString("title");
            tvTrackTitle.setText(title);
        }
    }


}
