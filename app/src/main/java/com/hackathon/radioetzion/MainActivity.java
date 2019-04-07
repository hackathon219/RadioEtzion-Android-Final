package com.hackathon.radioetzion;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.cloudant.sync.documentstore.DocumentNotFoundException;
import com.cloudant.sync.documentstore.DocumentStoreException;
import com.google.gson.Gson;
import com.hackathon.radioetzion.auth.AppIdAuthorizationListener;
import com.hackathon.radioetzion.auth.TokensPersistenceManager;
import com.hackathon.radioetzion.fragments.CollapsedPlayerFragment;
import com.hackathon.radioetzion.fragments.CommentFragment;
import com.hackathon.radioetzion.fragments.ExpandedPlayerFragment;
import com.hackathon.radioetzion.fragments.FavoriteFragment;
import com.hackathon.radioetzion.fragments.PodcastFragment;
import com.hackathon.radioetzion.fragments.ProfileFragment;
import com.hackathon.radioetzion.mediaplayer.MediaBrowserHelper;
import com.hackathon.radioetzion.mediaplayer.MusicLibrary;
import com.hackathon.radioetzion.mediaplayer.MusicService;
import com.hackathon.radioetzion.models.PodcastModel;
import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.LoginWidget;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.leo.simplearcloader.ArcConfiguration;
import com.leo.simplearcloader.SimpleArcDialog;
import com.leo.simplearcloader.SimpleArcLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPInternalPushMessage.LOG_TAG;

public class MainActivity extends AppCompatActivity {

    private static final String TENANT_ID = "8b634220-d4f4-4137-9061-f7757c208825";
    private static final String TAG = "Log " + MainActivity.class.getSimpleName();
    private static final String LOG_TAG = "Log " + MainActivity.class.getSimpleName();

    private boolean autoPlayFlag = false; //is initial launch? should autoplay?
    private IntentFilter songSelected = new IntentFilter("songSelected");
    private IntentFilter playPausePressed = new IntentFilter("playPausePressed");
    private IntentFilter updateFBDone = new IntentFilter("updateFBDone");

    public static PlaybackStateCompat playbackStateCompat;

    private SimpleArcDialog mDialog;
    private SimpleArcLoader mSimpleArcLoader;
    private ImageButton ibExpandedPlayerFwd;
    private PodcastDBHelper podcastDbHelper;
    private RecyclerView rv;
    private String podcastTitle;
    public static String podcastID;

    Intent playerState = new Intent("playerState");



    //    private PodcastAdapter adapter;
    public static boolean mIsPlaying;
    public static boolean mIsPause;

    public boolean mIsLoading;
    private boolean isLoadingDone = false;
    boolean isFirstLoading;

    public static MediaBrowserHelper mMediaBrowserHelper;
    public static MediaControllerCompat mMediaController;

    private ExpandedPlayerFragment expandedPlayerFragment;
    private CollapsedPlayerFragment collapsedPlayerFragment;
    public BottomSheetBehavior bottomSheetBehavior;
    public View bottomSheet;
    public View navBottomSheet;
    public BottomNavigationView bottomNav;

    //-----
    Boolean allowHiddenPlayer = false;
    //-----

    AppID appID;

    AppIDAuthorizationManager appIDAuthorizationManager;
    TokensPersistenceManager tokensPersistenceManager;
    IntentFilter fragmentTransfer = new IntentFilter("fragmentTransfer");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        bottomNav = findViewById(R.id.bottomNav);
        ibExpandedPlayerFwd = findViewById(R.id.ibExpandedPlayerFwd);

        initializeBoottomNavigation();
        showProgressBar();

        initializeAppID();
        initializeBottomSheet();

        initializeDBHelpers();

    }

    private void initializeBoottomNavigation() {

        bottomNav.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {


                case R.id.navigation_podcasts:
                    startPodcastFragment();
                    mDialog.dismiss();
                    System.out.println("item selected " + menuItem.getTitle());
                    break;


                case R.id.navigation_favorites:
                    startFavoriteFragment();
                    System.out.println("item selected " + menuItem.getTitle());
                    break;

                case R.id.navigation_log:
                    ProfileFragment profileFragment = new ProfileFragment();
                    profileFragment.show(getSupportFragmentManager(), "profileFragment");

//                    switch (getVisibleContentFragment()){
//                        case "podcastFragment":
//                            System.out.println("podcastFragment");
//                            bottomNav.setSelectedItemId( R.id.navigation_podcasts );
//                            break;
//
//                        case "favoriteFragment":
//                            System.out.println("favoriteFragment");
//                            bottomNav.setSelectedItemId( R.id.navigation_favorites );
//                            break;
//                    }
//                    getSupportFragmentManager().beginTransaction()
//                            .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
//                            .replace(R.id.contentFrame, new SearchFragment(), "searchFragment").commitAllowingStateLoss();
                    break;
            }

            return true;
        });
        bottomNav.setOnNavigationItemReselectedListener(menuItem -> {
            System.out.println("aaa" +  menuItem.getItemId() );
            if (!isFirstLoading) {
                startPodcastFragment();
                mDialog.dismiss();
                isFirstLoading = true;
            }
        });
    }


    private void showProgressBar() {
        mDialog = new SimpleArcDialog(this);

        mSimpleArcLoader = new SimpleArcLoader(getApplicationContext());

        ArcConfiguration configuration = new ArcConfiguration(getApplicationContext());
        configuration.setLoaderStyle(SimpleArcLoader.STYLE.COMPLETE_ARC);
        configuration.setText("טוען... אנא המתן");

// Using this configuration with Dialog
        mDialog.setConfiguration(configuration);

// Using this configuration with ArcLoader
        mSimpleArcLoader.refreshArcLoaderDrawable(configuration);

        mDialog.show();
    }

    private void initializeDBHelpers() {
        podcastDbHelper = PodcastDBHelper.getInstance(getApplicationContext());
        podcastDbHelper.setReplicationListener(this);
        podcastDbHelper.startPodcastPullReplication();
    }

    private void startFavoriteFragment(){
        getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.contentFrame, new FavoriteFragment(), "favoriteFragment")
                .commitAllowingStateLoss();
    }


    private void showCollapsedPlayer() {
//        collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(title);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        ft.replace(R.id.playerFrame, collapsedPlayerFragment, "collapsedPlayerFragment");
        ft.commitAllowingStateLoss();
    }


    private void showExpandedPlayer() {
//        expandedPlayerFragment = ExpandedPlayerFragment.newInstance(title, podcastID);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        ft.replace(R.id.playerFrame, expandedPlayerFragment, "expandedPlayerFragment");
        ft.commitAllowingStateLoss();
    }

    private void showCommentFragment(String podcastID) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        CommentFragment commentFragment = CommentFragment.newInstance(podcastID, false);
        ft.replace(R.id.playerFrame, commentFragment, "commentFragment");
        ft.commitAllowingStateLoss();
    }

    public void collapsePlayer(View view) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void expandPlayer(View view) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    //END persistent bottom sheet

    private ArrayList<Integer> getImagesFromDrawable() {
        ArrayList<Integer> imagesID = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int identifier = getResources().getIdentifier("p" + (i + 1), "drawable", this.getPackageName());
            if (identifier != 0) {
                imagesID.add(identifier);
            }

        }
        return imagesID;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceivers();
        autoPlayFlag = false;
        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
        mMediaBrowserHelper.onStart();


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserHelper.onStop();
        Log.d(TAG, "onStop: mMediaBrowserHelper");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService();
    }

    @Override
    public void onBackPressed() {
        if (getVisiblePlayerFragment() != "empty") {
            switch (getVisiblePlayerFragment()) {
                case "expandedPlayerFragment":
                    System.out.println(getVisiblePlayerFragment());
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return;
            }
            System.out.println(getVisiblePlayerFragment());

        }
        super.onBackPressed();
    }

    private void initializeBottomSheet() {

        navBottomSheet = findViewById(R.id.cardBottomSheet);
        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        if (!mIsPlaying) {
            System.out.println("isPlay");
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {

                navBottomSheet.setVisibility(i == BottomSheetBehavior.STATE_EXPANDED ? View.INVISIBLE : View.VISIBLE);
                getApplicationContext().sendBroadcast(new Intent("playerViewState").putExtra("playerViewState", i));

                switch (i) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        //place miniplayer fragment into playerFrame
//                        if (isLoadingDone) {
//                            showCollapsedPlayer();
//                        }
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:

                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        //place player fragment into playerFrame
                        if (isLoadingDone) {
//                            showExpandedPlayer();
                        }
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        navBottomSheet.setVisibility(View.VISIBLE);
                        MediaControllerCompat.TransportControls controls = mMediaBrowserHelper.getTransportControls();
                        if (controls != null) {
                            mMediaBrowserHelper.getTransportControls().stop();
                        }
//                        if (!allowHiddenPlayer){
//                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        //sheet is released in neither collapsed or expanded state and attempts to go into either
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

//                do something while the player is sliding up/down...
                if (v > 0.1 && isLoadingDone && getVisiblePlayerFragment() != "commentFragment") {
                    navBottomSheet.setVisibility(View.INVISIBLE);
                    showExpandedPlayer();
                } else if (v < 0.1) {
                    showCollapsedPlayer();
                }
            }

        });

        //tap on collapsed player expands it
        bottomSheet.setOnClickListener(v -> {
            switch (bottomSheetBehavior.getState()) {
                case BottomSheetBehavior.STATE_COLLAPSED:
                    if (expandedPlayerFragment != null && isLoadingDone) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
            }
        });

        //fragment stuff:
//        if (collapsedPlayerFragment == null) {
//            getSupportFragmentManager().beginTransaction().replace(R.id.playerFrame, new CollapsedPlayerFragment()).commitAllowingStateLoss();
//        }
    }


    private void initializeAppID() {
        BMSClient bmsClient = BMSClient.getInstance();
        bmsClient.initialize(getApplicationContext(), BMSClient.REGION_UK);
        appID = AppID.getInstance().initialize(this, TENANT_ID, AppID.REGION_UK);

        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(getApplicationContext(), appIDAuthorizationManager);
    }

    private void initializeLogin() {
        String storedRefreshToken = tokensPersistenceManager.getStoredRefreshToken();
        if (storedRefreshToken != null && !storedRefreshToken.isEmpty()) {

            if (!tokensPersistenceManager.isRegisterToNotification()){
                registerNotification(tokensPersistenceManager.getStoredUserID());
            }

            startPodcastFragment();


//            Log.d(TAG, ("refreshTokens ") + "Trying to refresh tokens using a refresh token");
//            boolean storedTokenAnonymous = tokensPersistenceManager.isStoredTokenAnonymous();
//            if (storedTokenAnonymous) {
//            }
//            AppIdAuthorizationListener appIdAuthorizationListener =
//                    new AppIdAuthorizationListener(this, appIDAuthorizationManager, storedTokenAnonymous, false);
//            appID.signinWithRefreshToken(this, storedRefreshToken, appIdAuthorizationListener);

        } else {
            anonymousLogin();
        }
    }


    private void registerNotification(String userID){
        String appGuid = "b53c0e69-b5a2-4a48-a524-9ae474733b37";
        String clientSecret = "232f2f5f-a033-446f-a60b-9994b4f1664c";

        BMSClient.getInstance().initialize(getApplicationContext().getApplicationContext(), BMSClient.REGION_UK);

        MFPPush push = MFPPush.getInstance();
        push.initialize(getApplicationContext().getApplicationContext(),appGuid,clientSecret);
        push.registerDeviceWithUserId(userID, new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                System.out.println("notification listener success: " + response);
                tokensPersistenceManager.saveNotificationStatus();
            }

            @Override
            public void onFailure(MFPPushException exception) {
                System.out.println("notification listener: " + exception.getErrorMessage());

            }
        });

    }

    private void startPodcastFragment() {
        Gson gson = new Gson();
        SharedPreferences pref = getSharedPreferences("appid_tokens", MODE_PRIVATE);
        String s = pref.getString("favorites", null);
        if (s == null) {
            System.out.println("is null");
            return;
        }
        ArrayList<String> favorites = gson.fromJson(s, ArrayList.class);
        PodcastFragment podcastFragment = PodcastFragment.newInstance(favorites, getImagesFromDrawable());

        getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.contentFrame, podcastFragment, "podcastFragment")
                .commitAllowingStateLoss();
        mDialog.dismiss();
    }

    private void startSearchFragment() {
        Gson gson = new Gson();
        SharedPreferences pref = getSharedPreferences("appid_tokens", MODE_PRIVATE);
        String s = pref.getString("favorites", null);
        if (s == null) {
            System.out.println("is null");
            return;
        }
        ArrayList<String> favorites = gson.fromJson(s, ArrayList.class);
        System.out.println("favorites.size()) " + favorites.size());
        for (String favorite : favorites) {
            System.out.println(favorite);
        }
        PodcastFragment podcastFragment = PodcastFragment.newInstance(favorites, getImagesFromDrawable());

        getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.contentFrame, podcastFragment, "podcastFragment")
                .commitAllowingStateLoss();
    }


    private void login() {

        Log.d(TAG, "Attempting identified authorization");
        LoginWidget loginWidget = appID.getLoginWidget();
        final String storedAccessToken;
        storedAccessToken = tokensPersistenceManager.getStoredAccessToken();

        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(this, appIDAuthorizationManager, false, false);

        loginWidget.launch(this, appIdAuthorizationListener);
    }

    private String getVisiblePlayerFragment() {
        Fragment fragmentById = getSupportFragmentManager().findFragmentById(R.id.playerFrame);
        if (fragmentById == null) return "empty";
        else return fragmentById.getTag();
    }

    private String getVisibleContentFragment() {
        Fragment fragmentById = getSupportFragmentManager().findFragmentById(R.id.contentFrame);
        if (fragmentById == null) return null;
        else return fragmentById.getTag();
    }


    private void anonymousLogin() {

//        final String storedAccessToken = tokensPersistenceManager.getStoredAnonymousAccessToken();
        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(this, appIDAuthorizationManager, true, false);


        appID.signinAnonymously(getApplicationContext(), null, appIdAuthorizationListener);

    }

    private String getUserID() {
        SharedPreferences pref = getSharedPreferences("appid_tokens", MODE_PRIVATE);
        return pref.getString("appid_user_id", null);
    }

    public boolean clearStoredTokens() {
        SharedPreferences sharedPreferences = getSharedPreferences("appid_tokens", MODE_PRIVATE);
        return !sharedPreferences.edit().clear().commit();
    }


    private void prepareSelectedSong(PodcastModel podcast) throws IOException {


        if (podcast.getDuration() > 0) {
            playWithDuration(podcast);
        } else {
            playWithoutDuration(podcast);
        }
    }

    private void playWithDuration(PodcastModel podcast) {
        collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(podcast.getName());

        if (mMediaBrowserHelper != null) {
            mMediaBrowserHelper.onStop();
        }

        MusicLibrary.createMediaMetadataCompat(
                podcast.getRev().getId(),
                podcast.getName(),
                podcast.getDuration(),
                podcast.getName(),
                podcast.getUrlAddress()
        );

//        mDialog.dismiss();
        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());

        mMediaBrowserHelper.onStart();
        isLoadingDone = true;

        getSupportFragmentManager().beginTransaction().replace(R.id.playerFrame, collapsedPlayerFragment, "collapsedPlayerFragment").commitAllowingStateLoss();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


    }

    private void playWithoutDuration(PodcastModel podcast) throws IOException {
        if (!mIsPlaying) {
            collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(podcast.getName());
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.playerFrame, collapsedPlayerFragment, "collapsedPlayerFragment").commitAllowingStateLoss();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        if (mMediaBrowserHelper != null) {
            mMediaBrowserHelper.onStop();
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(podcast.getUrlAddress());
        mediaPlayer.prepare();
        int duration = mediaPlayer.getDuration();
        podcast.setDuration(duration);
        mediaPlayer.release();
        mediaPlayer = null;
        MusicLibrary.createMediaMetadataCompat(
                podcast.getRev().getId(),
                podcast.getName(),
                duration,
                podcast.getName(),
                podcast.getUrlAddress()
        );
        if (duration > 0) isLoadingDone = true;


//        mDialog.dismiss();
        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());

        mMediaBrowserHelper.onStart();

    }


    void firstStart() {

        initializeLogin();

//        mDialog.dismiss();


        System.out.println("mIsPlaying: " + mIsPlaying + " title: " + podcastTitle + " podcastID " + podcastID);

    }

    void replicationPodcastComplete() {
        firstStart();
        Log.d(TAG, "replicationPodcastComplete: ");
    }

    void replicationPodcastError() {
        firstStart();
        Log.d(TAG, "replicationPodcastError: ");
    }


    /**
     * Customize the connection to our {@link android.support.v4.media.MediaBrowserServiceCompat}
     * and implement our app specific desires.
     */
    public class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context) {
            super(context, MusicService.class);
        }

        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
            Log.d(TAG, "onConnected: connect - and set mMediaController");
//            if (mediaController != null){
//                mMediaController = getMediaController();
//            }
            mMediaController = getMediaController();
            MediaMetadataCompat metadata = mediaController.getMetadata();
            if (metadata == null) return;


        }

        @Override
        protected void onChildrenLoaded(@NonNull String parentId,
                                        @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            if (getMediaController() == null) return;
            final MediaControllerCompat mediaController = getMediaController();

            // Queue up all media items for this simple sample.
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                mediaController.addQueueItem(mediaItem.getDescription());
            }

            // Call prepare now so pressing play just works.
            mediaController.getTransportControls().prepare();
            if (autoPlayFlag) {
                mediaController.getTransportControls().play();
            }
        }
    }


    private class MediaBrowserListener extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {

            playbackStateCompat = playbackState;


            Log.d(TAG, "onPlaybackStateChanged: " + playbackState);
            mIsPlaying = playbackState != null &&
                    playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;


            mIsPause = playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PAUSED;
            playerState.putExtra("mIsLoading", mIsLoading);
            playerState.putExtra("mIsPlaying", mIsPlaying);
            playerState.putExtra("podcastID", podcastID);
            getApplicationContext().sendBroadcast(playerState);

            if (mIsPlaying && !getVisiblePlayerFragment().equals("expandedPlayerFragment")) {
                collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(podcastTitle);
                expandedPlayerFragment = ExpandedPlayerFragment.newInstance(podcastTitle, podcastID);
                showCollapsedPlayer();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isLoadingDone = true;
            }

        }


        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }

            podcastTitle = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            podcastID = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
//            collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(podcastTitle);
//            expandedPlayerFragment = ExpandedPlayerFragment.newInstance(podcastTitle, podcastID);
//
//            isLoadingDone = true;
//            if (mIsPlaying) {
////                getSupportFragmentManager().beginTransaction().replace(R.id.playerFrame, collapsedPlayerFragment, "collapsedPlayerFragment").commitAllowingStateLoss();
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//            }


//            mMediaBrowserHelper.getTransportControls().play();
//            mTitleTextView.setText(
//                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
//            mArtistTextView.setText(
//                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
//            mAlbumArt.setImageBitmap(MusicLibrary.getAlbumBitmap(
//                    MainActivity.this,
//                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)));
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            Log.d(TAG, "onQueueChanged: " + queue.size());
            super.onQueueChanged(queue);
        }
    }

    private void registerReceivers() {
        getApplicationContext().registerReceiver(songSelectedReceiver, songSelected);
        getApplicationContext().registerReceiver(playPausePressedReceiver, playPausePressed);

        getApplicationContext().registerReceiver(updateFBDoneReceiver, updateFBDone);
        getApplicationContext().registerReceiver(fragmentTransferReceiver, fragmentTransfer);


    }

    private void unregisterReceivers() {
        getApplicationContext().unregisterReceiver(songSelectedReceiver);
        getApplicationContext().unregisterReceiver(playPausePressedReceiver);

        getApplicationContext().unregisterReceiver(updateFBDoneReceiver);
        getApplicationContext().unregisterReceiver(fragmentTransferReceiver);
    }


    BroadcastReceiver songSelectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String podcastID = intent.getStringExtra("podcastID");
            boolean preventAutoPlay = intent.getBooleanExtra("preventAutoPlay", false);
            if (!preventAutoPlay) {
                autoPlayFlag = true;
            }
            try {
                PodcastModel podcastModel = PodcastDBHelper.getInstance(MainActivity.this).getPodcastObjByDocId(podcastID);
                expandedPlayerFragment = ExpandedPlayerFragment.newInstance(podcastModel.getName(), podcastID);
                collapsedPlayerFragment = CollapsedPlayerFragment.newInstance(podcastModel.getName());

                mIsLoading = true;
                playerState.putExtra("mIsLoading", mIsLoading);
//                getApplicationContext().sendBroadcast(playerState);

                MediaControllerCompat.TransportControls controls = mMediaBrowserHelper.getTransportControls();
                if (controls != null) {
                    mMediaBrowserHelper.getTransportControls().stop();
                }

                prepareSelectedSong(podcastModel);
            } catch (DocumentNotFoundException | DocumentStoreException | IOException e) {
                e.printStackTrace();
            }
        }
    };

    BroadcastReceiver playPausePressedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMediaBrowserHelper == null) {
                return;
            }
            if (mIsPlaying) {
                mMediaBrowserHelper.getTransportControls().pause();
            } else if (isLoadingDone) {
                mMediaBrowserHelper.getTransportControls().play();

            }

        }
    };

    BroadcastReceiver updateFBDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Gson gson = new Gson();
            SharedPreferences.Editor editor = getSharedPreferences("appid_tokens", MODE_PRIVATE).edit();

            ArrayList<String> favorites = intent.getStringArrayListExtra("favorites");
            String s = gson.toJson(favorites);

            editor.putString("favorites", s).commit();
            bottomNav.setSelectedItemId(R.id.navigation_podcasts);

        }
    };

    BroadcastReceiver fragmentTransferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            String podcastID = intent.getStringExtra("podcastID");
            System.out.println("pressed");
            if (action == null) return;
            switch (action) {
                case "transferToCommentsFragment":
                    CommentFragment commentFragment = CommentFragment.newInstance(podcastID, false);
                    getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
                            .addToBackStack(null)
                            .replace(R.id.playerFrame, commentFragment, "commentFragment")
                            .commitAllowingStateLoss();

                    break;

                case "login":
                    login();
                    break;

                case "transferToCommentsFragmentFromPreview":
                    commentFragment = CommentFragment.newInstance(podcastID, true);
                    getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
                            .addToBackStack(null)
                            .replace(R.id.contentFrame, commentFragment, "commentFragment")
                            .commitAllowingStateLoss();

                    break;

            }
        }
    };




}