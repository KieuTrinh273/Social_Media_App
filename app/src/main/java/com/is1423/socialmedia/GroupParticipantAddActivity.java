package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.adapter.AdapterParticipantAdd;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.User;

import java.util.ArrayList;
import java.util.List;

public class GroupParticipantAddActivity extends AppCompatActivity {

    private RecyclerView userRv;
    private ActionBar actionBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser fUser;
    private String groupId, currentUserRole;
    private List<User> userList;
    private AdapterParticipantAdd adapterParticipantAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_participant_add);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add Participants");
        //enable back button in actionBar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        fUser = firebaseAuth.getCurrentUser();

        userRv = findViewById(R.id.userRv);

        groupId = getIntent().getStringExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY);
        getGroupInfo();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void getGroupInfo() {
        DatabaseReference groupDbRef1 = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);

        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.orderByChild(Constant.GROUP_CHAT_TABLE_FIELD.ID)
                .equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //information of group
                            String groupID = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.ID).getValue();
                            String groupTitle = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.TITLE).getValue();
                            String groupDescription = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.DESCRIPTION).getValue();
                            String icon = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.ICON).getValue();
                            String createdBy = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_BY).getValue();
                            String createdDate = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_DATETIME).getValue();

                            actionBar.setTitle("Add Participants");
                            groupDbRef1.child(groupID)
                                    .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                                    .child(fUser.getUid())
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                currentUserRole = "" + snapshot.child(Constant.PARTICIPANTS_FIELD.ROLE).getValue();
                                                actionBar.setTitle(groupTitle + "(" + currentUserRole + ")");

                                                getAllMembers();
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

    private void getAllMembers() {
        userList = new ArrayList<>();
        DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER);
        userDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    User user = ds.getValue(User.class);
                    if(!fUser.getUid().equals(user.getUid())){
                        userList.add(user);
                    }
                }
                adapterParticipantAdd = new AdapterParticipantAdd(GroupParticipantAddActivity.this, userList, groupId, currentUserRole);
                userRv.setAdapter(adapterParticipantAdd);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}