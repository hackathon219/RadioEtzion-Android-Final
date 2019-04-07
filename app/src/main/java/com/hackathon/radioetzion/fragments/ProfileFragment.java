package com.hackathon.radioetzion.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.hackathon.radioetzion.MainActivity;
import com.hackathon.radioetzion.ProfileAdapter;
import com.hackathon.radioetzion.R;
import com.hackathon.radioetzion.auth.AppIdAuthorizationListener;
import com.hackathon.radioetzion.auth.TokensPersistenceManager;
import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.LoginWidget;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import org.json.JSONObject;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends DialogFragment {
    private static final String TENANT_ID = "8b634220-d4f4-4137-9061-f7757c208825";
    private static final String TAG = "ProfileFragment";

    View view;
    Button btnLogout, btnLogin;
    AppID appID;
    TextView tvFB;
    CardView cvProfile;
    AppIDAuthorizationManager appIDAuthorizationManager;
    TokensPersistenceManager tokensPersistenceManager;
    TextView tvProfileName;
    ImageView ivProfile;
    RecyclerView rvProfile;
    boolean isUp;



    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity)getActivity()).bottomNav.setSelectedItemId( R.id.navigation_podcasts );
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        SharedPreferences preferences = getActivity().getSharedPreferences("imageDoc", MODE_PRIVATE);


        ivProfile = view.findViewById(R.id.ivProfile);
        rvProfile = view.findViewById(R.id.rvProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvProfileName = view.findViewById(R.id.tvProfileName);

        tvFB = view.findViewById(R.id.tvFB);
        cvProfile = view.findViewById( R.id.cvProfile );
        appID = AppID.getInstance().initialize(getContext(), TENANT_ID, AppID.REGION_UK);
        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(getContext(), appIDAuthorizationManager);

        int image = preferences.getInt("image", 0);
        ivProfile.setImageResource( getImage(image) );
        ivProfile.setOnClickListener( profileImageListener );

        setProfileAdapter();

        if (getStoredUserName() != null){
            tvProfileName.setText("שלום " + getStoredUserName());
            btnLogout.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
        }else {
            btnLogout.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        }

        tvFB.setOnClickListener( v -> {
            Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
            try {
                String facebookUrl = getFacebookPageURL(getContext());
                facebookIntent.setData( Uri.parse(facebookUrl));
                getActivity().startActivity(new Intent(facebookIntent));
            } catch (ActivityNotFoundException e) {
                facebookIntent.setData( Uri.parse(FACEBOOK_URL));
                getActivity().startActivity(new Intent(facebookIntent));
            }
        } );

        btnLogin.setOnClickListener(v -> {
            login();
            dismiss();
        });
        btnLogout.setOnClickListener(v -> {
            clearStoredTokens();
            anonymousLogin();
            dismiss();
        });


        return view;
    }


    View.OnClickListener profileImageListener = v -> {

        if(isUp){
            rvProfile.setVisibility( View.GONE );
            tvProfileName.setVisibility( View.VISIBLE );
        }else {
            rvProfile.setVisibility( View.VISIBLE );
            tvProfileName.setVisibility( View.INVISIBLE );
        }

//        if (isUp == true) {
//            viewRv.animate().alpha( 1.0f ).setDuration( 500 ).setListener( new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    super.onAnimationEnd( animation );
//                    viewRv.setVisibility( View.GONE);
//                }
//            } );
//        } else {
//            viewRv.animate().alpha( 0.0f ).setDuration( 500 ).setListener( new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    super.onAnimationEnd( animation );
//                    viewRv.setVisibility( View.VISIBLE );
//                }
//            } );
//        }



        isUp = !isUp;
    };


    private void setProfileAdapter() {
        ProfileAdapter adapter = new ProfileAdapter(getActivity(), ivProfile);
        rvProfile.setAdapter( adapter );
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager( this.getContext(), LinearLayoutManager.HORIZONTAL, false );
        rvProfile.setLayoutManager( linearLayoutManager );

    }

    public String getStoredUserName(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("appid_tokens", MODE_PRIVATE);
        if(sharedPreferences.getBoolean("appid_is_anonymous", true)){
            return null;
        }
        return sharedPreferences.getString("appid_user_name", null);
    }

    private void login() {

        Log.d(TAG, "Attempting identified authorization");
        LoginWidget loginWidget = appID.getLoginWidget();
        final String storedAccessToken;
        storedAccessToken = tokensPersistenceManager.getStoredAccessToken();

        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(getActivity(), appIDAuthorizationManager, false, false);

        loginWidget.launch(getActivity(), appIdAuthorizationListener);
    }

    private void logout() {


        appIDAuthorizationManager.logout(getContext(), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                clearStoredTokens();
                anonymousLogin();
                dismiss();
                System.out.println("success");
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                System.out.println("Fail");
            }
        });
    }

    public boolean clearStoredTokens() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("appid_tokens", MODE_PRIVATE);
        return !sharedPreferences.edit().clear().commit();
    }

    private void anonymousLogin() {

//        final String storedAccessToken = tokensPersistenceManager.getStoredAnonymousAccessToken();
        AppIdAuthorizationListener appIdAuthorizationListener =
                new AppIdAuthorizationListener(getActivity(), appIDAuthorizationManager, true, false);

        appID.signinAnonymously(getContext(), null, appIdAuthorizationListener);

    }

    public static String FACEBOOK_URL = "https://www.facebook.com/beitekstein";
    public static String FACEBOOK_PAGE_ID = "beitekstein";

    //method to get the right URL to use in the intent
    public String getFacebookPageURL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.katana", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://facewebmodal/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }
    private int getImage(int position) {
        switch (position){
            case 0:
                return R.drawable.icon0;
            case 1:
                return R.drawable.icon1;
            case 2:
                return R.drawable.icon2;
            case 3:
                return R.drawable.icon3;
            case 4:
                return R.drawable.icon4;
            case 5:
                return R.drawable.icon5;
            case 6:
                return R.drawable.icon6;
            case 7:
                return R.drawable.icon7;
        }
        return R.drawable.icon0;
    }


}
