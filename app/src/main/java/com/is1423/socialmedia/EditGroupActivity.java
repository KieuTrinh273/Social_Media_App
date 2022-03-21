package com.is1423.socialmedia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
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
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.is1423.socialmedia.common.Constant;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditGroupActivity extends AppCompatActivity {
    private ActionBar actionBar;
    private String groupId;

    //permissions
    private String cameraPermissions[];
    private String storagePermissions[];
    private Uri image_uri;

    //firebase
    private FirebaseAuth firebaseAuth;

    //views
    private ImageView groupIconCiv;
    private EditText groupTitleEt, groupDescriptionEt;
    private FloatingActionButton updateGroupBtn;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Edit Group Info");
        //enable back button in actionBar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        initViews();

        groupId = getIntent().getStringExtra(Constant.COMMON_KEY.GROUPID_INTENT_KEY);

        initArraysOfPermissions();

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        checkUser();
        loadGroupInfo();

        groupIconCiv.setOnClickListener(groupIconOnClickListener);
        updateGroupBtn.setOnClickListener(updateGroupBtnOnClickListener);
    }

    private void loadGroupInfo() {
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

                    //set data
                    groupTitleEt.setText(title);
                    groupDescriptionEt.setText(description);

                    try {
                        Picasso.get().load(icon).placeholder(R.drawable.ic_group_primary).into(groupIconCiv);
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void initViews() {
        groupIconCiv = findViewById(R.id.groupIconCiv);
        groupTitleEt = findViewById(R.id.groupTitleEt);
        groupDescriptionEt = findViewById(R.id.groupDescriptionEt);
        updateGroupBtn = findViewById(R.id.createGroupBtn);
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (Objects.nonNull(user)) {
            actionBar.setSubtitle(user.getEmail());
        }
    }

    private void initArraysOfPermissions() {
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    private View.OnClickListener groupIconOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showImagePickDialog();
        }
    };

    private View.OnClickListener updateGroupBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String title = groupTitleEt.getText().toString().trim();
            String des = groupDescriptionEt.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(EditGroupActivity.this, "Title required", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Updating...");
            progressDialog.show();
            if (Objects.isNull(image_uri)) {
                Map<String, Object> map = new HashMap<>();
                map.put(Constant.GROUP_CHAT_TABLE_FIELD.TITLE, title);
                map.put(Constant.GROUP_CHAT_TABLE_FIELD.DESCRIPTION, des);

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
                reference.child(groupId).updateChildren(map)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                progressDialog.dismiss();
                                Toast.makeText(EditGroupActivity.this, "Done", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(EditGroupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                String time = System.currentTimeMillis() + "";
                String filePathAndName = "Group_Imgs/image_" + time;
                StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
                storageReference.putFile(image_uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful()) ;
                                Uri downloadUri = uriTask.getResult();
                                if (uriTask.isSuccessful()) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(Constant.GROUP_CHAT_TABLE_FIELD.TITLE, title);
                                    map.put(Constant.GROUP_CHAT_TABLE_FIELD.DESCRIPTION, des);
                                    map.put(Constant.GROUP_CHAT_TABLE_FIELD.ICON, downloadUri);

                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
                                    reference.child(groupId).updateChildren(map)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(EditGroupActivity.this, "Done", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(EditGroupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(EditGroupActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    };

    private void showImagePickDialog() {
        //dialog containing options: Camera/Gallery
        String options[] = {Constant.IMAGE_SOURCE_OPTIONS.CAMERA, Constant.IMAGE_SOURCE_OPTIONS.GALLERY};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(EditGroupActivity.this);
        //set title
        builder.setTitle("Pick image from");
        //set items
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle dialog item click
                if (i == 0) {
                    //Camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }

                } else if (i == 1) {
                    //Gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });

        //create and show dialog
        builder.create().show();
    }

    private boolean checkStoragePermission() {
        //check if storage permission is enabled or not
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkCameraPermission() {
        //check if storage permission is enabled or not
        boolean isCameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean isWriteStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return isCameraGranted && isWriteStorageGranted;
    }

    private void requestStoragePermission() {
        //request runtime storage permission
        ActivityCompat.requestPermissions(this, storagePermissions, Constant.REQUEST_CODE.STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        //request runtime storage permission
        ActivityCompat.requestPermissions(this, cameraPermissions, Constant.REQUEST_CODE.CAMERA_REQUEST_CODE);
    }

    private void pickFromCamera() {
        //Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, Constant.REQUEST_CODE.IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Constant.REQUEST_CODE.IMAGE_PICK_GALLERY_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //when user press Allow or Deny from permission request dialog
        switch (requestCode) {
            case Constant.REQUEST_CODE.CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Please enable camera & storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

            case Constant.REQUEST_CODE.STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //called after picking image
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constant.REQUEST_CODE.IMAGE_PICK_GALLERY_CODE) {
                image_uri = data.getData();
                groupIconCiv.setImageURI(image_uri);
            }
            if (requestCode == Constant.REQUEST_CODE.IMAGE_PICK_CAMERA_CODE) {
                groupIconCiv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}