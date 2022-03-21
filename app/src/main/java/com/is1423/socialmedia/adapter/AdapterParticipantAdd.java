package com.is1423.socialmedia.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.AddPostActivity;
import com.is1423.socialmedia.R;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.User;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdapterParticipantAdd extends RecyclerView.Adapter<AdapterParticipantAdd.MyHolder> {
    private Context context;
    private List<User> userList;
    private String groupId, currentUserRole;

    public AdapterParticipantAdd(Context context, List<User> userList, String groupId, String currentUserRole) {
        this.context = context;
        this.userList = userList;
        this.groupId = groupId;
        this.currentUserRole = currentUserRole;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_participant_add, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        User user = userList.get(position);
        String name = user.getName();
        String email = user.getEmail();
        String image = user.getImage();
        String uid = user.getUid();

        //set data
        holder.nameTv.setText(name);
        holder.emailTv.setText(email);
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_default_img_primary).into(holder.avatarTv);
        } catch (Exception e) {
            holder.avatarTv.setImageResource(R.drawable.ic_default_img_primary);
        }

        isUserExisted(user, holder);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
                groupDbRef.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String memberRole = "" + snapshot.child(Constant.PARTICIPANTS_FIELD.ROLE).getValue();
                                    String[] options;
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Choose option");

                                    if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)) {
                                        if (memberRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)) {
                                            options = new String[]{Constant.ROLE_OPERATION.REMOVE_ADMIN,
                                                    Constant.ROLE_OPERATION.REMOVE_USER};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 0) {
                                                        removeAdmin(user);
                                                    } else {
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        } else if (memberRole.equals(Constant.GROUP_MEMBER_ROLE.PARTICIPANT)) {
                                            options = new String[]{Constant.ROLE_OPERATION.MAKE_ADMIN,
                                                    Constant.ROLE_OPERATION.REMOVE_USER};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 0) {
                                                        makeAdmin(user);
                                                    } else {
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    } else if (currentUserRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)) {
                                        if (memberRole.equals(Constant.GROUP_MEMBER_ROLE.CREATOR)) {
                                            Toast.makeText(context, "He/She is creator...", Toast.LENGTH_SHORT).show();
                                        } else if (memberRole.equals(Constant.GROUP_MEMBER_ROLE.ADMIN)) {
                                            Toast.makeText(context, "He/She is admin like you...", Toast.LENGTH_SHORT).show();
                                        } else if (memberRole.equals(Constant.GROUP_MEMBER_ROLE.PARTICIPANT)) {
                                            options = new String[]{Constant.ROLE_OPERATION.MAKE_ADMIN,
                                                    Constant.ROLE_OPERATION.REMOVE_USER};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 0) {
                                                        makeAdmin(user);
                                                    } else {
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                }
                                //user does not exist/not participant
                                else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Add Participant")
                                            .setMessage("Add this user into group")
                                            .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    //add user
                                                    addParticipant(user);
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
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }
        });
    }

    private void addParticipant(User user) {
        String time = "" + System.currentTimeMillis();

        Map<String, String> map = new HashMap<>();
        map.put(Constant.PARTICIPANTS_FIELD.UID, user.getUid());
        map.put(Constant.PARTICIPANTS_FIELD.ROLE, Constant.GROUP_MEMBER_ROLE.PARTICIPANT);
        map.put(Constant.PARTICIPANTS_FIELD.JOIN_TIME, time);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId)
                .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                .child(user.getUid())
                .setValue(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "Add successfully...", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void makeAdmin(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.PARTICIPANTS_FIELD.ROLE, Constant.GROUP_MEMBER_ROLE.ADMIN);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId)
                .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                .child(user.getUid())
                .updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "The user is admin now", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeParticipant(User user) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId)
                .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                .child(user.getUid())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "Member removed...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeAdmin(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.PARTICIPANTS_FIELD.ROLE, Constant.GROUP_MEMBER_ROLE.PARTICIPANT);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        reference.child(groupId)
                .child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS)
                .child(user.getUid())
                .updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "The user is participant now", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void isUserExisted(User user, MyHolder holder) {
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.child(groupId).child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String role = "" + snapshot.child(Constant.PARTICIPANTS_FIELD.ROLE).getValue();
                            holder.roleTv.setText(role);
                        } else {
                            holder.roleTv.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {
        private ImageView avatarTv;
        private TextView nameTv, emailTv, roleTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            avatarTv = itemView.findViewById(R.id.avatarTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
            roleTv = itemView.findViewById(R.id.roleTv);
        }
    }
}
