package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GroupInfoActivity extends AppCompatActivity {

    private String groupId;
    private String currentUserRole;

    private ActionBar actionBar;

    //views
    private ImageView groupIconIv;
    private TextView descriptionTv, createdByTv, editGroupTv, addParticipantTv, leaveGroupTv, participantsTv;
    private RecyclerView participantRv;

    FirebaseAuth firebaseAuth;
    FirebaseUser fUser;

    List<User> userList;
    AdapterParticipantAdd adapterParticipantAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");
        //enable back button in actionBar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        initViews();

        groupId = getIntent().getStringExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY);

        firebaseAuth = FirebaseAuth.getInstance();
        fUser = firebaseAuth.getCurrentUser();
        getGroupInfo();
        getCurrentUserRole();

        addParticipantTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupInfoActivity.this, GroupParticipantAddActivity.class);
                intent.putExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY, groupId);
                startActivity(intent);
            }
        });

        leaveGroupTv.setOnClickListener(onLeaveGroupClickListener);

        editGroupTv.setOnClickListener(onEditGroupClickListener);
    }

    private void initViews() {
        ;
        groupIconIv = findViewById(R.id.groupIconIv);
        descriptionTv = findViewById(R.id.descriptionTv);
        createdByTv = findViewById(R.id.createdByTv);
        editGroupTv = findViewById(R.id.editGroupTv);
        addParticipantTv = findViewById(R.id.addParticipantTv);
        leaveGroupTv = findViewById(R.id.leaveGroupTv);
        participantsTv = findViewById(R.id.participantsTv);
        participantRv = findViewById(R.id.participantRv);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void getGroupInfo() {
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        //get record which has id = groupId
        groupDbRef.orderByChild(Constant.GROUP_CHAT_TABLE_FIELD.ID).equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get group info from database
                    String groupId = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.ID).getValue();
                    String title = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.TITLE).getValue();
                    String description = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.DESCRIPTION).getValue();
                    String icon = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.ICON).getValue();
                    String createdBy = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_BY).getValue();
                    String time = "" + ds.child(Constant.GROUP_CHAT_TABLE_FIELD.CREATED_DATETIME).getValue();

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(Long.parseLong(time));
                    String createdDate = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

                    getCreatedInfo(createdDate, createdBy);

                    //set data
                    actionBar.setTitle(title);
                    descriptionTv.setText(description);

                    try {
                        Picasso.get().load(icon).placeholder(R.drawable.ic_group_primary).into(groupIconIv);
                    } catch (Exception e) {
                        groupIconIv.setImageResource(R.drawable.ic_group_primary);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getCreatedInfo(String createdDate, String createdBy) {
        DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER);
        userDbRef.orderByChild(Constant.USER_TABLE_FIELD.UID).equalTo(createdBy)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String name = "" + ds.child(Constant.USER_TABLE_FIELD.NAME).getValue();
                            createdByTv.setText("Created by " + name + " on " + createdDate);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void getCurrentUserRole() {
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).orderByChild(Constant.USER_TABLE_FIELD.UID)
                .equalTo(fUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            currentUserRole = "" + ds.child(Constant.PARTICIPANTS_FIELD.ROLE).getValue();
                            actionBar.setSubtitle(fUser.getEmail() + "(" + currentUserRole + ")");

                            if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.PARTICIPANT)) {
                                editGroupTv.setVisibility(View.GONE);
                                addParticipantTv.setVisibility(View.GONE);
                                leaveGroupTv.setText("Leave Group");
                            } else if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)) {
                                editGroupTv.setVisibility(View.GONE);
                                addParticipantTv.setVisibility(View.VISIBLE);
                                leaveGroupTv.setText("Leave Group");
                            } else if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)) {
                                editGroupTv.setVisibility(View.VISIBLE);
                                addParticipantTv.setVisibility(View.VISIBLE);
                                leaveGroupTv.setText("Delete Group");
                            }
                        }
                        loadParticipants();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadParticipants() {
        userList = new ArrayList<>();
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get uid from Group > Participants
                    String uid = "" + ds.child(Constant.USER_TABLE_FIELD.UID).getValue();
                    //get info of user using above uid
                    DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER);
                    userDbRef.orderByChild(Constant.USER_TABLE_FIELD.UID).equalTo(uid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                User user = ds.getValue(User.class);
                                userList.add(user);
                            }
                            adapterParticipantAdd = new AdapterParticipantAdd(GroupInfoActivity.this, userList, groupId, currentUserRole);
                            participantsTv.setText("Participants (" + userList.size() + ")");
                            participantRv.setAdapter(adapterParticipantAdd);
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

    View.OnClickListener onLeaveGroupClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //user is participant/admin => leave group
            //admin => delete group
            String dialogTitle = "";
            String dialogDescription = "";
            String positiveButtonTitle = "";
            if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)) {
                dialogTitle = "Delete Group";
                dialogDescription = "Are you sure to delete group?";
                positiveButtonTitle = "DELETE";
            } else {
                dialogTitle = "Leave Group";
                dialogDescription = "Are you sure to leave group?";
                positiveButtonTitle = "LEAVE";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupInfoActivity.this);
            builder.setTitle(dialogTitle)
                    .setMessage(dialogDescription)
                    .setPositiveButton(positiveButtonTitle, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)) {
                                deleteGroup();
                            } else {
                                leaveGroup();
                            }
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
    };

    private void leaveGroup() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).child(fUser.getUid())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(GroupInfoActivity.this, "Already left", Toast.LENGTH_SHORT);
                        startActivity(new Intent(GroupInfoActivity.this, DashboardActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupInfoActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT);
                    }
                });
    }

    private void deleteGroup() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(GroupInfoActivity.this, "Done", Toast.LENGTH_SHORT);
                        startActivity(new Intent(GroupInfoActivity.this, DashboardActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupInfoActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT);
                    }
                });
    }

    View.OnClickListener onEditGroupClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(GroupInfoActivity.this, EditGroupActivity.class);
            intent.putExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY, groupId);
            startActivity(intent);
        }
    };
}