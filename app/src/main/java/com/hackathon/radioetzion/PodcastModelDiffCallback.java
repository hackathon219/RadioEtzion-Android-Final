package com.hackathon.radioetzion;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.hackathon.radioetzion.models.PodcastModel;

import java.util.List;

public class PodcastModelDiffCallback extends DiffUtil.Callback {

    private final List<PodcastModel> mOldPodcastList;
    private final List<PodcastModel> mNewPodcastList;


    public PodcastModelDiffCallback(List<PodcastModel> mOldPodcastList, List<PodcastModel> mNewPodcastList) {
        this.mOldPodcastList = mOldPodcastList;
        this.mNewPodcastList = mNewPodcastList;
    }

    @Override
    public int getOldListSize() {
        return mOldPodcastList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewPodcastList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldPodcastList.get(oldItemPosition).getRev() == mNewPodcastList.get(
                newItemPosition).getRev();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final PodcastModel oldPodcast = mOldPodcastList.get(oldItemPosition);
        final PodcastModel newPodcast = mNewPodcastList.get(newItemPosition);

        return oldPodcast.getName().equals(newPodcast.getName());
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload( oldItemPosition, newItemPosition );
    }
}

