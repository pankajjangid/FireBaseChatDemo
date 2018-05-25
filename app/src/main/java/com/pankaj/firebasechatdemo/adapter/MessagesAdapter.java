package com.pankaj.firebasechatdemo.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.acitivitys.ChatActivity;
import com.pankaj.firebasechatdemo.model.ChatMessage;
import com.pankaj.firebasechatdemo.model.User;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private static final int ITEM_TYPE_SENT = 1;
    private static final int ITEM_TYPE_RECEIVED = 2;
    private List<ChatMessage> mMessagesList;
    private ChatActivity chatActivity;
    public MessagesAdapter(List<ChatMessage> mMessagesList, ChatActivity chatActivity) {
        this.mMessagesList=mMessagesList;
        this.chatActivity=chatActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = null;
        if (viewType == ITEM_TYPE_SENT) {
            v = LayoutInflater.from(chatActivity).inflate(R.layout.sent_message_row, null);
        } else if (viewType == ITEM_TYPE_RECEIVED) {
            v = LayoutInflater.from(chatActivity).inflate(R.layout.received_message_row, null);
        }
        return new ViewHolder(v); // view holder for header items
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        if (mMessagesList.get(position).isSender())
            holder.userName.setText(mMessagesList.get(position).getSenderName());
        else
            holder.userName.setText(mMessagesList.get(position).getReceiverName());
        holder.chatMsgTextView.setText(mMessagesList.get(position).getMessage());

        if (mMessagesList.get(position).getImageUrl().equals("")){
            holder.chatMsgTextView.setVisibility(View.VISIBLE);
            holder.chatMsgImageView.setVisibility(View.GONE);
        }else {

            holder.chatMsgTextView.setVisibility(View.GONE);
            holder.chatMsgImageView.setVisibility(View.VISIBLE);
            Picasso.get().load(mMessagesList.get(position).getImageUrl()).placeholder(R.drawable.progress_animation).into(holder.chatMsgImageView);

        }



    }

    @Override
    public int getItemViewType(int position) {
        if (mMessagesList.get(position).getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return ITEM_TYPE_SENT;
        } else {
            return ITEM_TYPE_RECEIVED;
        }
    }

    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class  ViewHolder  extends RecyclerView.ViewHolder{
        TextView chatMsgTextView,userName;
        ImageView chatMsgImageView;
        public ViewHolder(View itemView) {
            super(itemView);

            chatMsgTextView = itemView.findViewById(R.id.chatMsgTextView);
            userName = itemView.findViewById(R.id.userName);
            chatMsgImageView = itemView.findViewById(R.id.chatMsgImageView);
        }
    }
}
