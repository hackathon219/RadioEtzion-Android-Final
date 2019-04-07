package com.hackathon.radioetzion.models;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

public class PodcastDiffCallback extends DiffUtil.Callback {

    private final List<PodcastModel> mOldPodcastList;
    private final List<PodcastModel> mNewPodcastList;

    public PodcastDiffCallback(List<PodcastModel> oldPodcastList, List<PodcastModel> newEmployeeList) {
        this.mOldPodcastList = oldPodcastList;
        this.mNewPodcastList = newEmployeeList;
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
        return mOldPodcastList.get(oldItemPosition).getRev().getId() == mNewPodcastList.get(
                newItemPosition).getRev().getId();
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
        // Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
