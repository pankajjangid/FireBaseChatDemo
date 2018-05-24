package com.pankaj.firebasechatdemo.acitivitys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.adapter.UsersAdapter;
import com.pankaj.firebasechatdemo.model.User;
import com.pankaj.firebasechatdemo.utils.ProgressDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatUsersActivity extends AppCompatActivity {

    FirebaseUser mFirebaseUser;
    FirebaseAuth mFirebaseAuth;
    RecyclerView usersRecyclerView;
    List<User> mUserList = new ArrayList<>();
    Context mContext;
    private DatabaseReference mUsersRef;

   Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);


        initView();
        checkIsUserLogin();
    }

    private void checkIsUserLogin() {

        if (mFirebaseUser != null) {
            ProgressDialogUtil.showProgress(mContext,"Fetching users ...");

            mUsersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {

                        User user = snap.getValue(User.class);
                        if (!mFirebaseUser.getUid().equals(user.getUserId()))
                        mUserList.add(user);
                    }

                    /**populate users**/
                    ProgressDialogUtil.hideProgress();
                    populateUersRecyclerView();

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    private void populateUersRecyclerView() {

        UsersAdapter adapter = new UsersAdapter(mUserList, mContext);
        usersRecyclerView.setAdapter(adapter);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    }

    private void initView() {
        mContext = this;
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();


        //init Firebase
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        usersRecyclerView = findViewById(R.id.usersRecyclerView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logout:
                logOutuser();
                return true;
            case R.id.userProfile:
                goToUpdateUserProfile();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void goToUpdateUserProfile() {

        startActivity(new Intent(mContext,UpdateProfileActivity.class));
    }

    private void logOutuser() {
        if (mFirebaseUser != null) {

            mFirebaseAuth.signOut();

            startActivity(new Intent(ChatUsersActivity.this, LoginActivity.class));

        }
    }
}
