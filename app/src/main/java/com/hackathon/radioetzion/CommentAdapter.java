package com.hackathon.radioetzion;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.ServerValue;
import com.hackathon.radioetzion.models.CommentModel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentAdapter extends FirebaseRecyclerAdapter<CommentModel, CommentAdapter.CommentHolder> {




    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public CommentAdapter(@NonNull FirebaseRecyclerOptions<CommentModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull CommentHolder holder, int position, @NonNull CommentModel model) {
        holder.bind(model);
    }

    @NonNull
    @Override
    public CommentHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.comment_rv_item, viewGroup, false);
        return new CommentHolder(view);
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        private final TextView mNameField;
        private final TextView mTextField;
        private final TextView mTimeField;
        private final FrameLayout mLeftArrow;
        private final FrameLayout mRightArrow;
        private final RelativeLayout mMessageContainer;
        private final LinearLayout mMessage;;
        private final int mGreen300;
        private final int mGray300;



        public CommentHolder(@NonNull View itemView) {
            super(itemView);
            mGreen300 = ContextCompat.getColor(itemView.getContext(), R.color.material_green_300);
            mGray300 = ContextCompat.getColor(itemView.getContext(), R.color.material_gray_300);
            mNameField = itemView.findViewById(R.id.name_text);
            mTimeField = itemView.findViewById(R.id.time_text);
            mTextField = itemView.findViewById(R.id.message_text);
            mLeftArrow = itemView.findViewById(R.id.left_arrow);
            mRightArrow = itemView.findViewById(R.id.right_arrow);
            mMessageContainer = itemView.findViewById(R.id.message_container);
            mMessage = itemView.findViewById(R.id.message);
        }

        public void bind(CommentModel commentModel) {
            setName(commentModel.getUserName());
            setText(commentModel.getMessage());
            setTime(getDate(commentModel.getTimestampLong()));
            String currentUser = getStoredUserID();
            setIsSender(currentUser != null && commentModel.getUserID().equals(currentUser));
        }

        private void setName(String name) {
            mNameField.setText(name);
        }

        private void setText(String text) {
            mTextField.setText(text);
        }
        private void setTime(String time) {
            mTimeField.setText(time);
        }



        private String getDate(long timestamp){
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM hh:mm");
            String dateString = formatter.format(new Date(timestamp));
            return dateString;
        }

        private void setIsSender(boolean isSender) {
            final int color;
            if (isSender) {
                color = mGreen300;
                mLeftArrow.setVisibility(View.GONE);
                mRightArrow.setVisibility(View.VISIBLE);
                mMessageContainer.setGravity(Gravity.END);
            } else {
                color = mGray300;
                mLeftArrow.setVisibility(View.VISIBLE);
                mRightArrow.setVisibility(View.GONE);
                mMessageContainer.setGravity(Gravity.START);
            }

            ((GradientDrawable) mMessage.getBackground()).setColor(color);
            ((RotateDrawable) mLeftArrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
            ((RotateDrawable) mRightArrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
        }

        public String getStoredUserID(){
            SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("appid_tokens", itemView.getContext().MODE_PRIVATE);
            return sharedPreferences.getString("appid_user_id", null);
        }

    }



}
