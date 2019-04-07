/*
 * Copyright 2016, 2017 IBM Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hackathon.radioetzion.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.hackathon.radioetzion.models.UserModel;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.AuthorizationException;
import com.ibm.cloud.appid.android.api.AuthorizationListener;
import com.ibm.cloud.appid.android.api.tokens.AccessToken;
import com.ibm.cloud.appid.android.api.tokens.IdentityToken;
import com.ibm.cloud.appid.android.api.tokens.RefreshToken;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;

import java.util.ArrayList;

/**
 * This listener provides the callback methods that are called at the end of App ID
 * authorization process when using the {@link com.ibm.cloud.appid.android.api.AppID} login APIs
 */
public class AppIdAuthorizationListener implements AuthorizationListener {
    private NoticeHelper noticeHelper;
    private TokensPersistenceManager tokensPersistenceManager;
    private boolean isAnonymous;
    private Activity activity;
    UserCallBack listener;
    boolean isReceive;
    boolean isFromCommentFragment;
    UserModel userModel;

    public AppIdAuthorizationListener(Activity activity, AppIDAuthorizationManager authorizationManager, boolean isAnonymous, boolean isFromCommentFragment) {
        tokensPersistenceManager = new TokensPersistenceManager(activity, authorizationManager);
        noticeHelper = new NoticeHelper(activity, authorizationManager, tokensPersistenceManager);
        this.isAnonymous = isAnonymous;
        this.isFromCommentFragment = isFromCommentFragment;
        this.activity = activity;
    }

    public AppIdAuthorizationListener setOnUserCallbackListener(UserCallBack listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onAuthorizationFailure(AuthorizationException exception) {
        Log.e(logTag("onAuthorizationFailure"), "Authorization failed", exception);
    }

    @Override
    public void onAuthorizationCanceled() {
        Log.w(logTag("onAuthorizationCanceled"), "Authorization canceled");
    }

    @Override
    public void onAuthorizationSuccess(AccessToken accessToken, IdentityToken identityToken, RefreshToken refreshToken) {
        Log.d(logTag("onAuthorizationSuccess"), "Authorization succeeded");
        if (accessToken == null && identityToken == null) {
            Log.d(logTag("onAuthorizationSuccess"), "Finish done flow");

        } else {

            //storing the new token
            tokensPersistenceManager.persistTokensOnDevice();
            String currentUserID = tokensPersistenceManager.getStoredUserID();
            String deviceID = tokensPersistenceManager.getStoredDeviceID();

            if (!tokensPersistenceManager.isRegisterToNotification()){
                registerNotification(currentUserID);
            }

            UserModel user = new UserModel(currentUserID, deviceID, new ArrayList<>());


            listener = userModel -> {
                if (userModel != null){
                    startPodcastFragment(userModel);
                }else if (isAnonymous){
                    saveUser(user);
                }else updateUser(currentUserID);


            };
            getUserFromFB();

        }

    }

    private void startPodcastFragment(UserModel userModel) {
        Intent updateFBDone = new Intent("updateFBDone");
        ArrayList<String> favorites = userModel.getFavoritePodcasts();
        if (favorites == null) favorites = new ArrayList<>();
        saveFavoritesInPref(favorites);
        updateFBDone.putStringArrayListExtra("favorites", favorites);
        System.out.println("isFromCommentFragment " + isFromCommentFragment);
        if (isFromCommentFragment)return;
        activity.sendBroadcast(updateFBDone);
    }


    private void saveFavoritesInPref(ArrayList<String> favorites){
        Gson gson = new Gson();
        String s = gson.toJson(favorites);
        SharedPreferences.Editor editor = activity.getSharedPreferences("appid_tokens", Context.MODE_PRIVATE).edit();
        editor.putString("favorites", s).commit();
    }

    private void updateUser(String userID) {
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(tokensPersistenceManager.getKeyFromFB())
                .child("userID")
                .setValue(userID);
    }


    private void saveUser(UserModel userModel) {
        DatabaseReference push = FirebaseDatabase.getInstance().getReference().child("users").push();
        String key = push.getKey();
        tokensPersistenceManager.saveKeyFromFB(key);
        push.setValue(userModel).addOnSuccessListener(aVoid -> startPodcastFragment(userModel));
    }

    private void getUserFromFB() {
        isReceive = false;
        String currentUserID = tokensPersistenceManager.getStoredUserID();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");
        Query userID = ref.orderByChild("userID").equalTo(currentUserID).limitToFirst(1);

        userID.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChildren()) {
                    listener.onUserReceiveListener(userModel);
                    return;
                }
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String key = child.getKey();
                    tokensPersistenceManager.saveKeyFromFB(key);
                    userModel = dataSnapshot.child(key).getValue(UserModel.class);
                    listener.onUserReceiveListener(userModel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void registerNotification(String userID){
        String appGuid = "b53c0e69-b5a2-4a48-a524-9ae474733b37";
        String clientSecret = "232f2f5f-a033-446f-a60b-9994b4f1664c";

        BMSClient.getInstance().initialize(activity.getApplicationContext(), BMSClient.REGION_UK);

        MFPPush push = MFPPush.getInstance();
        push.initialize(activity.getApplicationContext(),appGuid,clientSecret);
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

        push.listen(new MFPPushNotificationListener() {
            @Override
            public void onReceive(MFPSimplePushNotification message) {
                System.out.println(message.toString());
            }
        });
    }


    private String logTag(String methodName) {
        return this.getClass().getCanonicalName() + "." + methodName;
    }

}

