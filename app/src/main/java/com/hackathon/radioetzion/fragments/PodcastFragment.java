package com.hackathon.radioetzion.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cloudant.sync.documentstore.DocumentStoreException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hackathon.radioetzion.LiveStreamAsync;
import com.hackathon.radioetzion.MainActivity;
import com.hackathon.radioetzion.PodcastAdapter;
import com.hackathon.radioetzion.PodcastDBHelper;
import com.hackathon.radioetzion.R;
import com.hackathon.radioetzion.auth.AppIdAuthorizationListener;
import com.hackathon.radioetzion.auth.TokensPersistenceManager;
import com.hackathon.radioetzion.models.LiveStreamModel;
import com.hackathon.radioetzion.models.PodcastModel;
import com.hackathon.radioetzion.models.UserModel;
import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.LoginWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;


public class PodcastFragment extends Fragment {
    private static final String TAG = "Log " + PodcastFragment.class.getSimpleName();

    private Button btnLiveStreamInActive, btnLiveStreamActive;
    private static final String TENANT_ID = "8b634220-d4f4-4137-9061-f7757c208825";

    RecyclerView rvPodcasts;
    Button btnSearch;
    View view, invisibleView;
    PodcastDBHelper podcastDBHelper;
    List<String> favorites;
    PodcastAdapter adapter;
    ArrayList<Integer> imagesID;
    boolean isPlayerCollapse;
    PodcastModel podcastModel;
    LiveStreamModel liveStreamModel;
    TextView tvMainTitle, tvDescription, tvTitle, tvParticipants, tvBroadcasters;
    Random rand = new Random();
    LinearLayout searchLayout;
    EditText etSearch;
    List<String> arrQuery;



    private IntentFilter dbListener = new IntentFilter("updateFavoriteList");


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_podcasts, container, false);
        setIDs();
        setTitle();
        podcastDBHelper = PodcastDBHelper.getInstance(getContext());
        favorites = getArguments().getStringArrayList("favorites");
        imagesID = getArguments().getIntegerArrayList("imagesID");
        setAdapter();

        btnLiveStreamActive.setOnClickListener(liveStreamListener);
        btnLiveStreamInActive.setOnClickListener(liveStreamListener);

        isPlayerCollapse = ((MainActivity)getActivity()).bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;

        arrQuery = new ArrayList<>();
        btnSearch.setOnClickListener(searchListener);
        etSearch.addTextChangedListener(searchWatcher);

        tvParticipants.setOnClickListener(categoriesListener);
        tvTitle.setOnClickListener(categoriesListener);
        tvBroadcasters.setOnClickListener(categoriesListener);
        tvDescription.setOnClickListener(categoriesListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLiveStream();
        adjustLayout();
        getContext().registerReceiver(playerViewStateReceiver, new IntentFilter("playerViewState"));
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(playerViewStateReceiver);
    }

    BroadcastReceiver playerViewStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int playerViewState = intent.getIntExtra("playerViewState", 0);
            isPlayerCollapse = playerViewState == BottomSheetBehavior.STATE_COLLAPSED;
            adjustLayout();
        }
    };

    private void adjustLayout(){
        invisibleView.setVisibility(isPlayerCollapse ? View.INVISIBLE : View.GONE);
        System.out.println("invisibleView " + isPlayerCollapse);
    }





    public static PodcastFragment newInstance(ArrayList<String> favorites, ArrayList<Integer> imagesID) {

        Bundle args = new Bundle();
        args.putStringArrayList("favorites", favorites);
        args.putIntegerArrayList("imagesID", imagesID);
        PodcastFragment fragment = new PodcastFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void setTitle(){
        if (!isStoredTokenAnonymous()){
            if (getStoredUserName() != null){
                tvMainTitle.setText("שלום " + getStoredUserName());
            }
        }
    }

    public String getStoredUserName(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("appid_access_token", MODE_PRIVATE);
        return sharedPreferences.getString("appid_user_name", null);
    }

    public boolean isStoredTokenAnonymous(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("appid_access_token", MODE_PRIVATE);
        return sharedPreferences.getBoolean("appid_is_anonymous", false);
    }

    View.OnClickListener searchListener = v -> {
//            ProfileFragment profileFragment = new ProfileFragment();
//            profileFragment.show(getFragmentManager(), "profileFragment");
        switch (searchLayout.getVisibility()){
            case View.VISIBLE:
//                btnLiveStreamActive.setVisibility( View.GONE );
//                btnLiveStreamInActive.setVisibility( View.VISIBLE );
                searchLayout.setVisibility( View.GONE );
                etSearch.setVisibility( View.GONE );
                tvMainTitle.setVisibility( View.VISIBLE );
                break;

            case View.GONE:
//                btnLiveStreamInActive.setVisibility( View.GONE );
//                btnLiveStreamActive.setVisibility( View.VISIBLE );
                searchLayout.setVisibility( View.VISIBLE );
                etSearch.setVisibility( View.VISIBLE );
                tvMainTitle.setVisibility( View.GONE );
                break;
        }

    };

    View.OnClickListener liveStreamListener = v -> {
        switch (v.getId()){
            case R.id.btnLiveStreamActive:

                break;

            case R.id.btnLiveStreamInActive:

                break;
        }
    };

    private void setAdapter() {
        try {
            adapter = new PodcastAdapter(getPodcastList(),favorites ,(AppCompatActivity) getActivity());
            rvPodcasts.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPodcasts.setAdapter(adapter);


            rvPodcasts.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> adjustLayout());

        } catch (DocumentStoreException e) {
            Log.d(TAG, "setAdapter: error: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void setIDs() {
        tvMainTitle = view.findViewById(R.id.tvMainTitle);
        rvPodcasts = view.findViewById(R.id.rvPodcasts);
        btnSearch = view.findViewById(R.id.btnSearch);
        invisibleView = view.findViewById(R.id.invisibleView);
        btnLiveStreamActive = view.findViewById(R.id.btnLiveStreamActive);
        btnLiveStreamInActive = view.findViewById(R.id.btnLiveStreamInActive);
        searchLayout = view.findViewById(R.id.searchLayout);
        etSearch = view.findViewById(R.id.etSearch);
        tvDescription = view.findViewById(R.id.tvDescriptionCategory);
        tvTitle = view.findViewById(R.id.tvTitleCategory);
        tvBroadcasters = view.findViewById(R.id.tvBroadcastersCategory);
        tvParticipants = view.findViewById(R.id.tvParticipantsCategory);

    }

    private void updateFirebaseDB(UserModel userModel){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userModel.getUserID());
        ref.setValue(userModel);
    }

    private List<PodcastModel> getPodcastList() throws DocumentStoreException {
        List<PodcastModel> podcasts = podcastDBHelper.allPodcasts();
        Collections.sort(podcasts);


        for (String favorite : favorites) {
            for (PodcastModel podcast : podcasts) {
                if (favorite.equals(podcast.getRev().getId())) podcast.setFavorite(true);
            }
        }


        for (int i = 0; i < podcasts.size(); i++) {
            if (i < imagesID.size()){
                podcasts.get(i).setImageID(imagesID.get(i));
            }else {
                int randomImage = rand.nextInt(imagesID.size());
                podcasts.get(i).setImageID(randomImage);
            }
        }
        return podcasts;
    }

    private void updateLiveStream(){
        try {

            AsyncTask<Void, Void, LiveStreamModel> execute = new LiveStreamAsync().execute();
            liveStreamModel = execute.get();
            if (!execute.get().getStreamUrl().equals("null")){
                System.out.println("is not null " + execute.get().getStreamUrl());

                btnLiveStreamActive.setVisibility(View.VISIBLE);
                btnLiveStreamInActive.setVisibility(View.GONE);
            }else {
                System.out.println("is null");
                btnLiveStreamActive.setVisibility(View.GONE);
                btnLiveStreamInActive.setVisibility(View.VISIBLE);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }




    }


    private void login() {
        AppID appID = AppID.getInstance().initialize(getContext(),TENANT_ID, AppID.REGION_UK);
        AppIDAuthorizationManager appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        TokensPersistenceManager tokensPersistenceManager = new TokensPersistenceManager(getContext(), appIDAuthorizationManager);


        Log.d(TAG, "Attempting identified authorization");
        LoginWidget loginWidget = appID.getLoginWidget();
        final String storedAccessToken;
        storedAccessToken = tokensPersistenceManager.getStoredAccessToken();

        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(getActivity(), appIDAuthorizationManager, false, false);

        loginWidget.launch(getActivity(), appIdAuthorizationListener);
    }


    public String getStoredLoginAccessToken(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("appid_tokens", MODE_PRIVATE);
        if(sharedPreferences.getBoolean("appid_is_anonymous", true)){
            return null;
        }
        return sharedPreferences.getString("appid_access_token", null);
    }

    TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            adapter.setArrQuery( arrQuery );
            adapter.updatePodcastListItems(s.toString());

        }
    };

    View.OnClickListener categoriesListener = v -> {
        switch (v.getId()) {
            case R.id.tvBroadcastersCategory:
                updateArrayCategory(tvBroadcasters, "broadcasters");
                break;
            case R.id.tvDescriptionCategory:
                updateArrayCategory(tvDescription, "description");
                break;
            case R.id.tvTitleCategory:
                updateArrayCategory(tvTitle, "title");
                break;
            case R.id.tvParticipantsCategory:
                updateArrayCategory(tvParticipants, "participants");
                break;
        }
        adapter.setArrQuery( arrQuery );
        adapter.updatePodcastListItems(etSearch.getText().toString());
        System.out.println(arrQuery.toString() + ": " + arrQuery.size());

    };


    private void updateArrayCategory(TextView tv, String category) {
        boolean contains = arrQuery.contains(category);
        if (contains) {
            tv.setTextColor( Color.BLACK);
            arrQuery.remove(category);
        } else {
            tv.setTextColor(Color.RED);
            arrQuery.add(category);
        }
    }


}
