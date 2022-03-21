package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.adapter.AdapterGroupChatMessage;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.GroupChatMessage;
import com.is1423.socialmedia.domain.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseUser fUser;

    private String groupId, currentUserRole;

    private Toolbar toolbar;
    private ImageView groupIconCiv;
    private TextView groupTitleTv;
    private ImageButton attachBtn, sendBtn;
    private EditText messageEt;
    private RecyclerView messageRv;

    private List<GroupChatMessage> groupChatMessageList;
    private AdapterGroupChatMessage adapterGroupChatMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        initViews();
        setSupportActionBar(toolbar);
        groupChatMessageList = new ArrayList<>();

        Intent intent = getIntent();
        groupId = intent.getStringExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY);

        firebaseAuth = FirebaseAuth.getInstance();
        fUser = firebaseAuth.getCurrentUser();
        loadGroupInfo();
        loadGroupChatMessage();
        getCurrentUserRole();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                notify = true;
                //get text from edit text
                String message = messageEt.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(GroupChatActivity.this, "Cannot send empty message...", Toast.LENGTH_SHORT);
                } else {
                    sendMessage(message);
                }
                messageEt.setText("");
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void getCurrentUserRole() {
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.child(groupId)
                .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                .orderByChild(Constant.PARTICIPANTS_FIELD.UID)
                .equalTo(fUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            currentUserRole = "" + ds.child(Constant.PARTICIPANTS_FIELD.ROLE).getValue();
                            //refresh menu items
                            invalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadGroupChatMessage() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        databaseReference.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.MESSAGES)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groupChatMessageList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            GroupChatMessage model = ds.getValue(GroupChatMessage.class);
                            groupChatMessageList.add(model);
                        }
                        adapterGroupChatMessage = new AdapterGroupChatMessage(GroupChatActivity.this, groupChatMessageList);
                        messageRv.setAdapter(adapterGroupChatMessage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        groupIconCiv = findViewById(R.id.groupIconCiv);
        groupTitleTv = findViewById(R.id.groupTitleTv);
        attachBtn = findViewById(R.id.attachBtn);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        messageRv = findViewById(R.id.messageRv);
    }

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.orderByChild(Constant.GROUP_CHAT_TABLE_FIELD.ID).equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String groupTitle = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.TITLE).getValue();
                            String description = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.DESCRIPTION).getValue();
                            String icon = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.ICON).getValue();
                            String createdDate = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_DATETIME).getValue();
                            String createdBy = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_BY).getValue();

                            groupTitleTv.setText(groupTitle);
                            try {
                                Picasso.get().load(icon).placeholder(R.drawable.ic_group_primary);
                            } catch (Exception e) {
                                groupIconCiv.setImageResource(R.drawable.ic_group_primary);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void sendMessage(String message) {
        //save to database
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.SENDER, fUser.getUid());
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.MESSAGE, message);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.SEND_DATETIME, timestamp);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.TYPE, Constant.MESSAGE_TYPE.TEXT);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);

        databaseReference.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.MESSAGES).child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        messageEt.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);

        if(currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)||
        currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)){
            menu.findItem(R.id.action_add_participant).setVisible(true);
        }else {
            menu.findItem(R.id.action_add_participant).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_add_participant) {
            Intent intent = new Intent(this, GroupParticipantAddActivity.class);
            intent.putExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY, groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}