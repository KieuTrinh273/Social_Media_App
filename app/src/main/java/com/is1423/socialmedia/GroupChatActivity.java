package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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

    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;
    Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        initViews();
        initPermissionsArrays();
        setSupportActionBar(toolbar);
        groupChatMessageList = new ArrayList<>();

        Intent intent = getIntent();
        groupId = intent.getStringExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY);

        firebaseAuth = FirebaseAuth.getInstance();
        fUser = firebaseAuth.getCurrentUser();
        getCurrentUserRole();
        loadGroupInfo();
        loadGroupChatMessage();

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

        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickDialog();
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
                        for (DataSnapshot ds : snapshot.getChildren()) {
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
        attachBtn = findViewById(R.id.attachBtn);
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
        menu.findItem(R.id.action_search).setVisible(false);

//        if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR) ||
//                currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)) {
//            menu.findItem(R.id.action_add_participant).setVisible(true);
//        } else {
//            menu.findItem(R.id.action_add_participant).setVisible(false);
//        }
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
        } else if (id == R.id.action_groupInfo) {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY, groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPermissionsArrays() {
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    private void showImagePickDialog() {
        //dialog containing options: Camera/Gallery
        String options[] = {Constant.IMAGE_SOURCE_OPTIONS.CAMERA, Constant.IMAGE_SOURCE_OPTIONS.GALLERY};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick image from");
        //set items
        builder.setItems(options, onImagePickDialogClickedListener);
        //create and show dialog
        builder.create().show();
    }

    DialogInterface.OnClickListener onImagePickDialogClickedListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission();
                } else {
                    pickFromCamera();
                }
            }
            if (i == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                } else {
                    pickFromGallery();
                }
            }
        }
    };

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, Constant.REQUEST_CODE.STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, Constant.REQUEST_CODE.CAMERA_REQUEST_CODE);
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, Constant.REQUEST_CODE.IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, Constant.REQUEST_CODE.IMAGE_PICK_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQUEST_CODE.CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera & Storage both permissions are necessary...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                }
            }
            break;
            case Constant.REQUEST_CODE.STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is necessary...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == Constant.REQUEST_CODE.IMAGE_PICK_GALLERY_CODE) {
                imageUri = data.getData();
                sendImageMessage(imageUri);
            } else if (requestCode == Constant.REQUEST_CODE.IMAGE_PICK_CAMERA_CODE) {
                sendImageMessage(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImageMessage(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Sending image...");
        progressDialog.show();

        String currentTime = System.currentTimeMillis() + "";
        String fileNameAndPath = "ChatImage/" + "post_" + currentTime;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        storageReference.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()) {
                            //save to database
                            String timestamp = String.valueOf(System.currentTimeMillis());

                            Map<String, Object> hashMap = new HashMap<>();
                            hashMap.put(Constant.MESSAGE_TABLE_FIELD.SENDER, fUser.getUid());
                            hashMap.put(Constant.MESSAGE_TABLE_FIELD.MESSAGE, downloadUri);
                            hashMap.put(Constant.MESSAGE_TABLE_FIELD.SEND_DATETIME, timestamp);
                            hashMap.put(Constant.MESSAGE_TABLE_FIELD.TYPE, Constant.MESSAGE_TYPE.IMAGE);

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);

                            databaseReference.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.MESSAGES).child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            messageEt.setText("");
                                            progressDialog.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });
                        }
                    }
                });
    }
}