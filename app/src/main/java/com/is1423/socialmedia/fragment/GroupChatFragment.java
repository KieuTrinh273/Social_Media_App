package com.is1423.socialmedia.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.is1423.socialmedia.CreateGroupActivity;
import com.is1423.socialmedia.MainActivity;
import com.is1423.socialmedia.R;
import com.is1423.socialmedia.adapter.AdapterGroupChat;
import com.is1423.socialmedia.common.Constant;
import com.is1423.socialmedia.domain.GroupChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatFragment extends Fragment {
    RecyclerView groupChatRv;
    FirebaseAuth firebaseAuth;
    FirebaseUser fUser;

    List<GroupChat> groupChatList;
    AdapterGroupChat adapterGroupChat;

    public GroupChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);
        groupChatRv = view.findViewById(R.id.groupChatRv);
        groupChatRv.setHasFixedSize(true);
        groupChatRv.setLayoutManager(new LinearLayoutManager(getActivity()));


        groupChatList = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();

        getAllGroupChatList();
        return view;
    }

    private void getAllGroupChatList() {
        fUser = firebaseAuth.getCurrentUser();
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).child(firebaseAuth.getUid()).exists()) {
                        GroupChat groupChat = ds.getValue(GroupChat.class);
                        groupChatList.add(groupChat);
                    }
                }
                adapterGroupChat = new AdapterGroupChat(getActivity(), groupChatList);
                groupChatRv.setAdapter(adapterGroupChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchGroupChatList(String s) {
        groupChatList = new ArrayList<>();
        DatabaseReference groupDbRef = FirebaseDatabase.getInstance().getReference(Constant.TABLE.GROUP);
        groupDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child(Constant.GROUP_CHAT_TABLE_FIELD.PARTICIPANTS).child(firebaseAuth.getUid()).exists()) {
                        if (ds.child(Constant.GROUP_CHAT_TABLE_FIELD.TITLE).toString().toLowerCase().contains(s.toLowerCase())) {
                            GroupChat groupChat = ds.getValue(GroupChat.class);
                            groupChatList.add(groupChat);
                        }
                    }
                }
                adapterGroupChat = new AdapterGroupChat(getActivity(), groupChatList);
                groupChatRv.setAdapter(adapterGroupChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    /*inflate options menu*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        //inflating menu
        menuInflater.inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupInfo).setVisible(false);

        //SearchView
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!TextUtils.isEmpty(s)) {
                    searchGroupChatList(s);
                } else {
                    getAllGroupChatList();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s)) {
                    searchGroupChatList(s);
                } else {
                    getAllGroupChatList();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /*handle menu item clicks*/
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
            updateOnlineStatus(Constant.USER_STATUS.OFFLINE);
        } else if (id == R.id.action_create_group) {
            startActivity(new Intent(getActivity(), CreateGroupActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUserStatus() {
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //user signed in => stay here
        } else {
            //user not signed in, go main
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    private void updateOnlineStatus(String status) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Constant.TABLE.USER).child(fUser.getUid());
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.USER_TABLE_FIELD.ONLINE_STATUS, status);
        reference.updateChildren(map);
    }
}