package com.is1423.socialmedia.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.R;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.GroupChatMessage;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterGroupChatMessage extends RecyclerView.Adapter<AdapterGroupChatMessage.MyHolder> {
    private Context context;
    private List<GroupChatMessage> groupChatMessageList;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser fUser;

    public AdapterGroupChatMessage(Context context, List<GroupChatMessage> groupChatMessageList) {
        this.context = context;
        this.groupChatMessageList = groupChatMessageList;

        firebaseAuth = FirebaseAuth.getInstance();
        fUser = firebaseAuth.getCurrentUser();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == Constant.MESSAGE_SIDE.RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_message_right, parent, false);
            return new AdapterGroupChatMessage.MyHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_message_left, parent, false);
            return new AdapterGroupChatMessage.MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        GroupChatMessage groupChatMessage = groupChatMessageList.get(position);
        String message = groupChatMessage.getMessage();
        String type = groupChatMessage.getType();

        if (type.equals(Constant.MESSAGE_TYPE.TEXT)) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            holder.messageTv.setText(message);
        } else {
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            try {
                Picasso.get().load(message).placeholder(R.drawable.ic_image_black).into(holder.messageIv);
            } catch (Exception e) {
                holder.messageIv.setImageResource(R.drawable.ic_image_black);
            }
        }

        if (Objects.nonNull(groupChatMessage.getSendDatetime())) {
            String time = groupChatMessage.getSendDatetime();

            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(Long.parseLong(time));
            String sendDatetime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
            holder.timeTv.setText(sendDatetime);
        }

        holder.messageTv.setText(message);
        setUserName(groupChatMessage, holder);
    }

    private void setUserName(GroupChatMessage groupChatMessage, MyHolder holder) {
        DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER);
        userDbRef.orderByChild(Constant.USER_TABLE_FIELD.UID).equalTo(groupChatMessage.getSender())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String name = "" + ds.child(Constant.USER_TABLE_FIELD.NAME).getValue();
                            holder.nameTv.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemViewType(int position) {
        if (groupChatMessageList.get(position).getSender().equals(fUser.getUid())) {
            return Constant.MESSAGE_SIDE.RIGHT;
        } else {
            return Constant.MESSAGE_SIDE.LEFT;
        }
    }

    @Override
    public int getItemCount() {
        return groupChatMessageList.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private TextView nameTv, messageTv, timeTv;
        private ImageView messageIv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            messageIv = itemView.findViewById(R.id.messageIv);
        }
    }
}
