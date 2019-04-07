package com.hackathon.radioetzion;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.MyViewHolder> {


    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Activity activity;
    ImageView profile;

    public ProfileAdapter( Activity activity, ImageView profile) {
        this.activity = activity;
        sharedPref = activity.getSharedPreferences( "imageDoc", Context.MODE_PRIVATE );
        editor = sharedPref.edit();
        this.profile = profile;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_item, parent, false);
        return  new MyViewHolder(mView);
    }
    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        int image = getImage(position);
        holder.ivIcon.setImageResource(image);
        holder.ivIcon.setOnClickListener( v -> {
            editor.clear();
            editor.putInt("image", position).commit();
            profile.setImageResource( image );
        } );

     //   setFadeAnimation( holder.itemView );

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

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIcon;
        private MyViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
    @Override
    public int getItemCount() {
        return 8;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //    public void setFadeAnimation(View view) {
//        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
//        anim.setDuration(500);
//        view.startAnimation(anim);
//    }
}
