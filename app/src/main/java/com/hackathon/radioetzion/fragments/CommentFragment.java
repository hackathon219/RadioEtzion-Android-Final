package com.hackathon.radioetzion.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.hackathon.radioetzion.CommentAdapter;
import com.hackathon.radioetzion.LiveStreamAsync;
import com.hackathon.radioetzion.MainActivity;
import com.hackathon.radioetzion.PodcastDBHelper;
import com.hackathon.radioetzion.R;
import com.hackathon.radioetzion.auth.AppIdAuthorizationListener;
import com.hackathon.radioetzion.auth.TokensPersistenceManager;
import com.hackathon.radioetzion.models.CommentModel;
import com.hackathon.radioetzion.models.LiveStreamModel;
import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.LoginWidget;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;

import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;
import static android.support.constraint.Constraints.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class CommentFragment extends Fragment {

    //
    private final static int INTERVAL = 1000 * 60; //2 minutes

    private static final String TENANT_ID = "8b634220-d4f4-4137-9061-f7757c208825";
    AppIDAuthorizationManager appIDAuthorizationManager;
    TokensPersistenceManager tokensPersistenceManager;
    AppID appID;

    private Button btnCloseFeedback, btnSendComment;
    private ImageView mEmptyImageView;
    private RecyclerView rvComments;
    private EditText etMessage;
    private AppCompatActivity activity;
    private String podcastID;
    private PodcastDBHelper dbHelper;
    private View view;
    private CommentAdapter adapter;
    private RecyclerView.LayoutManager mManager;
    private RecyclerView.AdapterDataObserver observer;
    private static String ANONYMOUS_USER_NAME = "אורח";
    private boolean fromPreview;
//    private View fillBottomSheet, fillCollapsePlayer;


    public static CommentFragment newInstance(String podcastID, boolean fromPreview) {

        Bundle args = new Bundle();
        args.putSerializable("podcastID", podcastID);
        args.putBoolean("fromPreview", fromPreview);
        CommentFragment fragment = new CommentFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.comment_fragment, container, false);
        initializeAppID();
        findIDs();

        dbHelper = PodcastDBHelper.getInstance(getContext());

        fromPreview = getArguments().getBoolean("fromPreview");

        System.out.println("fromPreview " + fromPreview);


        setAdapter();

        btnSendComment.setOnClickListener(v -> {

            if (isStoredTokenAnonymous()) {
                suggestLogin();
                return;
            }
            if (!getMessage().isEmpty()) {
                sendMessage(new CommentModel(getStoredUserName(), getMessage(), getStoredUserID()));
            }
        });
        btnCloseFeedback.setOnClickListener(v -> getActivity().onBackPressed());
        if (fromPreview){
//            fillBottomSheet.setVisibility(View.VISIBLE);
            boolean playerCollapse = ((MainActivity)getActivity()).bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
//            fillCollapsePlayer.setVisibility(playerCollapse ? View.VISIBLE :  GONE);
        }



        return view;
    }

    private void findIDs() {
        btnCloseFeedback = view.findViewById(R.id.btnCloseComment);
        mEmptyImageView = view.findViewById(R.id.emptyImage);
        etMessage = view.findViewById(R.id.messageEdit);
        btnSendComment = view.findViewById(R.id.sendButton);
        mEmptyImageView = view.findViewById(R.id.emptyImage);
//        fillBottomSheet = view.findViewById(R.id.hideBottomSheet);
//        fillCollapsePlayer = view.findViewById(R.id.hideCollapsePlayer);
    }

    public String getStoredUserID() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("appid_tokens", getContext().MODE_PRIVATE);
        return sharedPreferences.getString("appid_user_id", null);
    }

    private void setAdapter() {
        rvComments = view.findViewById(R.id.rvComments);
        mManager = new LinearLayoutManager(getContext());
        if (getArguments() == null) return;
        String podcastID = getArguments().getString("podcastID");


        Query query = FirebaseDatabase.getInstance().getReference()
                .child("comments")
                .child(podcastID)
                .orderByChild("timestamp")
                .limitToLast(50);

        FirebaseRecyclerOptions<CommentModel> options = new FirebaseRecyclerOptions.Builder<CommentModel>()
                .setQuery(query, CommentModel.class)
                .build();


        adapter = new CommentAdapter(options) {
            @Override
            public void onDataChanged() {
                mEmptyImageView.setVisibility(getItemCount() == 0 ? VISIBLE : View.GONE);
                super.onDataChanged();
            }
        };

        rvComments.setLayoutManager(mManager);
        rvComments.setAdapter(adapter);


        observer = new RecyclerView.AdapterDataObserver() {


            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mManager.smoothScrollToPosition(rvComments, null, adapter.getItemCount());
            }
        };


        adapter.registerAdapterDataObserver(observer);

        rvComments.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

            System.out.println("addOnLayoutChangeListener chat");
//            ((MainActivity)getActivity()).navBottomSheet.setVisibility(bottom < oldBottom ? View.GONE : VISIBLE);
//            ((MainActivity)getActivity()).bottomSheet.setVisibility(bottom < oldBottom ? View.GONE : VISIBLE);


            if (bottom < oldBottom) {
                rvComments.postDelayed(() -> rvComments.smoothScrollToPosition(
                        rvComments.getAdapter().getItemCount()), 100);
            }

        });

    }

    BroadcastReceiver playerState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean playerCollapse = ((MainActivity)getActivity()).bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
//            fillCollapsePlayer.setVisibility(playerCollapse ? View.VISIBLE :  GONE);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(playerState, new IntentFilter("playerState"));

        System.out.println("onResume comment");
        if (fromPreview){
            ((MainActivity) getActivity()).bottomSheet.setVisibility(GONE);
            ((MainActivity) getActivity()).navBottomSheet.setVisibility(GONE);
        }else {
            ((MainActivity) getActivity()).navBottomSheet.setVisibility(GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(playerState);
        System.out.println("onPause comment");

        ((MainActivity) getActivity()).bottomSheet.setVisibility(VISIBLE);
        ((MainActivity) getActivity()).navBottomSheet.setVisibility(VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().sendBroadcast(new Intent("CommentFragmentOnStart"));
        adapter.startListening();


    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().sendBroadcast(new Intent("CommentFragmentOnStop"));
        adapter.stopListening();
    }


    private void sendMessage(CommentModel model) {
        podcastID = getArguments().getString("podcastID");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("comments").child(this.podcastID);

        DatabaseReference push = reference.push();
        String key = push.getKey();
        push.setValue(model);
        clearEtMessage();
    }

    private void initializeAppID() {
        BMSClient bmsClient = BMSClient.getInstance();
        bmsClient.initialize(getContext(), BMSClient.REGION_UK);
        appID = AppID.getInstance().initialize(getContext(), TENANT_ID, AppID.REGION_UK);

        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(getContext(), appIDAuthorizationManager);
    }


    private boolean isStoredTokenAnonymous() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("appid_tokens", MODE_PRIVATE);
        return sharedPreferences.getBoolean("appid_is_anonymous", false);
    }

    public String getStoredUserName() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("appid_tokens", getContext().MODE_PRIVATE);
        if (sharedPreferences.getBoolean("appid_is_anonymous", true)) {
            return ANONYMOUS_USER_NAME;
        }
        return sharedPreferences.getString("appid_user_name", null);
    }

    private void suggestLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setPositiveButton("התחבר", (dialog, id) -> {


            login();
//            Intent fragmentTransfer = new Intent("fragmentTransfer");
//            fragmentTransfer.putExtra("action", "login");
//            getContext().sendBroadcast(fragmentTransfer);

        });
        builder.setNegativeButton("איני מעוניין", (dialog, id) -> {

        });

        builder.setMessage("יש להתחבר על מנת להגיב");
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private String getMessage() {
        return etMessage.getText().toString();
    }

    private void clearEtMessage() {
        etMessage.setText("");
    }

    private void login() {

        Log.d(TAG, "Attempting identified authorization");
        LoginWidget loginWidget = appID.getLoginWidget();
        final String storedAccessToken;
        storedAccessToken = tokensPersistenceManager.getStoredAccessToken();

        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(getActivity(), appIDAuthorizationManager, false, true);

        loginWidget.launch(getActivity(), appIdAuthorizationListener);
    }


//
}
