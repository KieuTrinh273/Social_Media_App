package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.adapter.AdapterMessage;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.Message;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MessageActivity extends AppCompatActivity {
    //view from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileCiv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn;

    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference userDbRef;

    //for checking if user has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<Message> messageList;
    AdapterMessage adapterMessage;

    String partnerUid;
    String currentUserUid;
    String partnerImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        initView();

        //Layout for RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        /*using intent to pass user's uid
         * from uid => get profile picture, name and start chatting*/
        Intent intent = getIntent();
        partnerUid = intent.getStringExtra("partnerUid");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        userDbRef = firebaseDatabase.getReference(Constant.TABLE.USER);

        Query userQuery = userDbRef.orderByChild(Constant.USER_TABLE_FIELD.UID).equalTo(partnerUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check until required info is received
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get data
                    String name = ds.child(Constant.USER_TABLE_FIELD.NAME).getValue() + "";
                    partnerImg = ds.child(Constant.USER_TABLE_FIELD.IMAGE).getValue() + "";

                    if (Objects.nonNull(ds.child(Constant.USER_TABLE_FIELD.ONLINE_STATUS).getValue())){
                        String onlineStatus = "" + ds.child(Constant.USER_TABLE_FIELD.ONLINE_STATUS).getValue();
                        userStatusTv.setText(onlineStatus);
                    }
                    nameTv.setText(name);
                    try {
                        Picasso.get().load(partnerImg).placeholder(R.drawable.ic_default_img_white).into(profileCiv);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileCiv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //click btn to sent message
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get text from edit text
                String message = messageEt.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(MessageActivity.this, "Cannot send empty message...", Toast.LENGTH_SHORT);
                } else {
                    sendMessage(message);
                }
            }
        });

        readMessage();
        seenMessage();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference(Constant.TABLE.MESSAGE);
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message message = ds.getValue(Message.class);
                    if (message.getReceiver().equals(currentUserUid) && message.getSender().equals(partnerUid)) {
                        Map<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put(Constant.MESSAGE_TABLE_FIELD.IS_SEEN, true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessage() {
        messageList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.MESSAGE);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message message = ds.getValue(Message.class);
                    if ((message.getReceiver().equals(currentUserUid) && message.getSender().equals(partnerUid))
                            || (message.getReceiver().equals(partnerUid) && message.getSender().equals(currentUserUid))) {
                        messageList.add(message);
                    }

                    //adapter
                    adapterMessage = new AdapterMessage(MessageActivity.this, messageList, partnerImg);
                    adapterMessage.notifyDataSetChanged();
                    recyclerView.setAdapter(adapterMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String message) {
        //save to database
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        String sendDatetime = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.SENDER, currentUserUid);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.RECEIVER, partnerUid);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.MESSAGE, message);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.SEND_DATETIME, sendDatetime);
        hashMap.put(Constant.MESSAGE_TABLE_FIELD.IS_SEEN, false);
        reference.child(Constant.TABLE.MESSAGE).push().setValue(hashMap);

        messageEt.setText("");
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.message_recyclerView);
        profileCiv = findViewById(R.id.profileCiv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        updateOnlineStatus(Constant.USER_STATUS.ONLINE);
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateOnlineStatus(Constant.USER_STATUS.OFFLINE);
        userRefForSeen.removeEventListener(seenListener);

    }

    @Override
    protected void onResume() {
        updateOnlineStatus(Constant.USER_STATUS.ONLINE);
        super.onResume();
    }

    private void checkUserStatus() {
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserUid = user.getUid();

        } else {
            //user not signed in, go main
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchview
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
            updateOnlineStatus(Constant.USER_STATUS.OFFLINE);
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateOnlineStatus(String status) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER).child(currentUserUid);
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.USER_TABLE_FIELD.ONLINE_STATUS, status);
        reference.updateChildren(map);
    }
}