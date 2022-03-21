package com.is1423.socialmedia.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.GroupChatActivity;
import com.is1423.socialmedia.R;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.GroupChat;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterGroupChat extends RecyclerView.Adapter<AdapterGroupChat.HolderGroupChat> {
    private Context context;
    private List<GroupChat> groupChatList;

    public AdapterGroupChat(Context context, List<GroupChat> groupChatList) {
        this.context = context;
        this.groupChatList = groupChatList;
    }

    @NonNull
    @Override
    public HolderGroupChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_list, parent, false);
        return new HolderGroupChat(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChat holder, int position) {
        GroupChat groupChat = groupChatList.get(position);
        String groupId = groupChat.getId();
        String groupIcon = groupChat.getIcon();
        String groupTitle = groupChat.getTitle();

        holder.nameTv.setText("");
        holder.timeTv.setText("");
        holder.messageTv.setText("");

        loadLastMessage(groupChat, holder);

        holder.groupTitleTv.setText(groupTitle);
        try {
            Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_primary).into(holder.groupIconCiv);
        } catch (Exception e) {
            holder.groupIconCiv.setImageResource(R.drawable.ic_group_primary);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY, groupId);
                context.startActivity(intent);
            }
        });
    }

    private void loadLastMessage(GroupChat groupChat, HolderGroupChat holder) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupChat.getId()).child(Constant.GROUP_CHAT_TABLE_FIELD.MESSAGES).limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String message = ""+ds.child(Constant.GROUP_CHAT_MESSAGE_TABLE_FIELD.MESSAGE).getValue();
                            String time = ""+ds.child(Constant.GROUP_CHAT_MESSAGE_TABLE_FIELD.SENT_DATETIME).getValue();
                            String sender = ""+ds.child(Constant.GROUP_CHAT_MESSAGE_TABLE_FIELD.SENDER).getValue();

                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(time));
                            String sendDatetime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

                            holder.messageTv.setText(message);
                            holder.timeTv.setText(sendDatetime);

                            DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER);
                            userDbRef.orderByChild(Constant.USER_TABLE_FIELD.UID).equalTo(sender)
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot ds:snapshot.getChildren()){
                                                String name = ""+ds.child(Constant.USER_TABLE_FIELD.NAME).getValue();
                                                holder.nameTv.setText(name);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return groupChatList.size();
    }

    class HolderGroupChat extends RecyclerView.ViewHolder {
        private ImageView groupIconCiv;
        private TextView groupTitleTv, nameTv, messageTv, timeTv;

        public HolderGroupChat(@NonNull View itemView) {
            super(itemView);
            groupIconCiv = itemView.findViewById(R.id.groupIconCiv);
            groupTitleTv = itemView.findViewById(R.id.groupTitleTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}
