package com.hackathon.radioetzion.fragments;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cloudant.sync.documentstore.DocumentStoreException;
import com.google.gson.Gson;
import com.hackathon.radioetzion.FavoriteAdapter;
import com.hackathon.radioetzion.PodcastDBHelper;
import com.hackathon.radioetzion.R;
import com.hackathon.radioetzion.mediaplayer.MediaBrowserHelper;
import com.hackathon.radioetzion.models.PodcastModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */
public class FavoriteFragment extends Fragment {

    RecyclerView rvFavorite;
    View view;
    FavoriteAdapter adapter;
    PodcastDBHelper podcastDBHelper;
    Random rand = new Random();
    TextView tvMainTitle, tvDescription, tvTitle, tvParticipants, tvBroadcasters;
    LinearLayout searchLayout;
    EditText etSearch;
    List<String> arrQuery;
    Button btnSearch;





    public FavoriteFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_favorite, container, false);
        setIDs();
        podcastDBHelper = PodcastDBHelper.getInstance(getContext());

        try {
            adapter = new FavoriteAdapter(getFavoritePodcast(), (AppCompatActivity) getActivity(), getImagesFromDrawable());
            rvFavorite.setLayoutManager(new LinearLayoutManager(getContext()));
            rvFavorite.setAdapter(adapter);
        } catch (DocumentStoreException e) {
            e.printStackTrace();
        }

        arrQuery = new ArrayList<>();
        btnSearch.setOnClickListener(searchListener);
        etSearch.addTextChangedListener(searchWatcher);

        tvParticipants.setOnClickListener(categoriesListener);
        tvTitle.setOnClickListener(categoriesListener);
        tvBroadcasters.setOnClickListener(categoriesListener);
        tvDescription.setOnClickListener(categoriesListener);
        return view;
    }

    private void setIDs() {
        tvMainTitle = view.findViewById( R.id.tvMainTitle );
        btnSearch = view.findViewById( R.id.btnSearch );
        rvFavorite = view.findViewById(R.id.rvFavorite);
        searchLayout = view.findViewById(R.id.searchLayout);
        etSearch = view.findViewById(R.id.etSearch);
        tvDescription = view.findViewById(R.id.tvDescriptionCategory);
        tvTitle = view.findViewById(R.id.tvTitleCategory);
        tvBroadcasters = view.findViewById(R.id.tvBroadcastersCategory);
        tvParticipants = view.findViewById(R.id.tvParticipantsCategory);
    }

    private ArrayList<Integer> getImagesFromDrawable() {
        ArrayList<Integer> imagesID = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int identifier = getResources().getIdentifier("p" + (i + 1), "drawable", getActivity().getPackageName());
            if (identifier != 0) {
                imagesID.add(identifier);
            }

        }
        return imagesID;
    }

    private List<PodcastModel> getFavoritePodcast() throws DocumentStoreException {
        Gson gson = new Gson();
        SharedPreferences pref = getActivity().getSharedPreferences("appid_tokens", MODE_PRIVATE);
        String s = pref.getString("favorites", null);
        ArrayList<String> favorites = gson.fromJson(s, ArrayList.class);

        if (favorites == null) favorites = new ArrayList<>();

        List<PodcastModel>favoritesList = new ArrayList<>();

        List<PodcastModel> podcasts = podcastDBHelper.allPodcasts();
        Collections.sort(podcasts);

        for (int i = 0; i < podcasts.size(); i++) {
            if (i < getImagesFromDrawable().size()){
                podcasts.get(i).setImageID(getImagesFromDrawable().get(i));
            }else {
                int randomImage = rand.nextInt(getImagesFromDrawable().size());
                podcasts.get(i).setImageID(randomImage);
            }
        }



        for (String favorite : favorites) {
            for (PodcastModel podcast : podcasts) {
                if (favorite.equals(podcast.getRev().getId())){
                    favoritesList.add(podcast);
                }
            }
        }
        Collections.sort(favoritesList);
        return favoritesList;
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
