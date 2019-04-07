package com.hackathon.radioetzion;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;

import com.hackathon.radioetzion.fragments.PreviewFragment;
import com.hackathon.radioetzion.models.PodcastModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder>{
    private Intent songSelected = new Intent("songSelected");

    private List<PodcastModel> favorites;
    AppCompatActivity activity;
    List<Integer> imagesID;
    Random rand = new Random();
    List<PodcastModel> podcastFull;
    List<String> arrQuery;
    String filterPattern;

    public FavoriteAdapter(List<PodcastModel> favorites, AppCompatActivity activity, List<Integer> imagesID) {
        this.favorites = favorites;
        podcastFull = new ArrayList<>(favorites);
        this.activity = activity;
        this.imagesID = imagesID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.podcast_rv_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        holder.model = favorites.get(i);
        String podcastID = favorites.get(i).getRev().getId();
        holder.heart.setVisibility(View.GONE);
        holder.heartSelected.setVisibility(View.VISIBLE);

        holder.tvName.setText(holder.model.getName());
        holder.podcastDocID = favorites.get(i).getRev().getId();

        holder.ivBackground.setImageResource(holder.model.getImageID());
//
//        if (i < imagesID.size()){
//            int identifier = imagesID.get(i);
//            holder.ivBackground.setImageResource(identifier);
//        }else {
//            int randomImage = rand.nextInt(imagesID.size());
//            holder.ivBackground.setImageResource(imagesID.get(randomImage));
//        }

        setFadeAnimation(holder.itemView);



    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private android.support.v7.widget.AppCompatTextView tvName;
        private PodcastModel model;
        private ImageView heart, heartSelected, ivBackground;
        private Button info;
        private String podcastDocID;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPodcastName);
            heart = itemView.findViewById(R.id.ivFavorite);
            heartSelected = itemView.findViewById(R.id.ivFavoriteSelected);
            info = itemView.findViewById(R.id.btnInfo);
            ivBackground = itemView.findViewById(R.id.ivBackground);

            itemView.setOnClickListener(v -> {
                songSelected.putExtra("podcastID", model.getRev().getId());
                activity.sendBroadcast(songSelected);
            });


            info.setOnClickListener(v ->{
                activity
                        .getSupportFragmentManager()
                        .beginTransaction().setCustomAnimations(R.anim.zoom_in, R.anim.zoom_out, R.anim.zoom_in, R.anim.zoom_out)
                        .replace(R.id.contentFrame, PreviewFragment.newInstance(model.getName(), model.getRev().getId()), "previewFragment")
                        .addToBackStack(null)
                        .commitAllowingStateLoss();

            });


        }
    }


    public void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500);
        view.startAnimation(anim);
    }

    public void updatePodcastListItems(CharSequence constraint) {
        List<PodcastModel> podcasts = getModelsByString( constraint );
        final PodcastModelDiffCallback diffCallback = new PodcastModelDiffCallback(this.favorites, podcasts);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.favorites.clear();
        this.favorites.addAll(podcasts);
        diffResult.dispatchUpdatesTo(this);
    }

    @NotNull
    private List<PodcastModel> getModelsByString(CharSequence constraint) {
        List<PodcastModel> filtered = new ArrayList<>();
        boolean found;
        // arrQuery = Arrays.asList( new String[]{"broadcasters", "description", "title","participants"} );
        if (constraint == null || constraint.length() == 0) {
            return podcastFull;
        } else if (arrQuery.isEmpty()){
            arrQuery = Arrays.asList( new String[]{"broadcasters", "description", "title","participants"} );
            filterPattern = constraint.toString().trim();
            for (PodcastModel item : podcastFull) {
                inner:
                for (int i = 0; i < arrQuery.size(); i++) {
                    found = checkIsContains(item, arrQuery.get(i), filterPattern);
                    if (found) {
                        filtered.add(item);
                        break inner;
                    }
                }
            }
            arrQuery = new ArrayList<>();
        } else {
            filterPattern = constraint.toString().trim();
            for (PodcastModel item : podcastFull) {
                inner:
                for (int i = 0; i < arrQuery.size(); i++) {
                    found = checkIsContains(item, arrQuery.get(i), filterPattern);
                    if (found) {
                        filtered.add(item);
                        break inner;
                    }
                }
            }
        }
        return filtered;
    }
    private boolean checkIsContains(PodcastModel item, String s, String filterPattern) {
        switch (s) {
            case "broadcasters":
                ArrayList<String> broadcasters = item.getBroadcasters();
                for (int i = 0; i < broadcasters.size(); i++) {
                    if (broadcasters.get(i).contains(filterPattern)) return true;
                }
                return false;
            case "description":
                return item.getDescription().contains(filterPattern);
            case "title":
                return item.getName().contains(filterPattern);
            case "participants":
                ArrayList<String> participants = item.getParticipants();
                for (int i = 0; i < participants.size(); i++) {
                    if (participants.get(i).contains(filterPattern)) return true;
                }
                return false;
        }


        return false;
    }

    public void setArrQuery(List<String> arrQuery) {
        this.arrQuery = arrQuery;
    }


}
