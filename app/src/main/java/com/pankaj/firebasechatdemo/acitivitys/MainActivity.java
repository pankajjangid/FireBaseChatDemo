package com.pankaj.firebasechatdemo.acitivitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.model.User;
import com.pankaj.firebasechatdemo.utils.Validations;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public EditText etEmailId, etPasswd,etFirstName,etLastName;
    Button btnSignUp;
    TextView tvSignIn;
   FirebaseAuth mFirebaseAuth;
   FirebaseUser mFirebaseUser;
    private ProgressDialog progressDialog;
    private static final String TAG = "MainActivity";

    private DatabaseReference mUsersDBref;
// Google API Client object.
    public GoogleApiClient googleApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();


        checkIsUserLoggedIn();
    }

    private void checkIsUserLoggedIn() {
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser!=null){
            Intent i = new Intent(MainActivity.this,ChatUsersActivity.class);
            startActivity(i);
        }
    }

    private void initViews() {
        mFirebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        etEmailId = findViewById(R.id.etEmail);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPasswd = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);
        btnSignUp.setOnClickListener(this);
        tvSignIn.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.btnSignUp:

                createUser();

                break;

            case R.id.tvSignIn:
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                break;
        }
    }

    private void createUser() {


        final String Fname= etFirstName.getText().toString();
        final String Lname= etLastName.getText().toString();

        if (Validations.isValidEmail(etEmailId, "Provide your Email first!") && Validations.isValidPassword(etPasswd, "Set correct password")) {
            progressDialog.setMessage("Registering Please Wait...");
            progressDialog.show();

            mFirebaseAuth.createUserWithEmailAndPassword(etEmailId.getText().toString(), etPasswd.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {


                    if (!task.isSuccessful()) {
                        Toast.makeText(MainActivity.this.getApplicationContext(),
                                "SignUp unsuccessful: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    } else {

                        final FirebaseUser newUser = task.getResult().getUser();
                        //success creating user, now set display name as name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(Fname+" "+Lname)
                                .build();

                        newUser.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if (task.isSuccessful()) {
                                    progressDialog.dismiss();

                                    Log.d(TAG, "User profile updated.");
                                    /***CREATE USER IN FIREBASE DB AND REDIRECT ON SUCCESS**/
                                    createUserInDb(newUser.getUid(), newUser.getDisplayName(), newUser.getEmail());

                                }else{
                                    //error
                                    Toast.makeText(MainActivity.this, "Error " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }


                }
            });

        }
    }



    private void createUserInDb(String userId, String displayName, String email){
        mUsersDBref = FirebaseDatabase.getInstance().getReference().child("Users");
        User user = new User(userId, displayName, email);
        mUsersDBref.child(userId).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    //error
                    Toast.makeText(MainActivity.this, "Error " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }else{
                    //success adding user to db as well
                    //go to users chat list
                //    startActivity(new Intent(MainActivity.this, ChatUsersActivity.class));
                    startActivity(new Intent(MainActivity.this, ChatActivity.class));

                }
            }
        });
    }
}
