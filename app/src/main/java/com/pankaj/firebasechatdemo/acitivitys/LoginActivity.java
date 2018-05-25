package com.pankaj.firebasechatdemo.acitivitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.utils.Validations;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    TextView tvSignIn;
    EditText etPassword, etEmail;
    Button btnLogIn;
    FirebaseAuth firebaseAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        checkIsUserLogin();
    }

    private void checkIsUserLogin() {
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(LoginActivity.this, "User logged in ", Toast.LENGTH_SHORT).show();
                    Intent I = new Intent(LoginActivity.this, ChatUsersActivity.class);
                    startActivity(I);
                } else {
                    Toast.makeText(LoginActivity.this, "Login to continue", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void initView() {
        progressDialog = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();


        tvSignIn = findViewById(R.id.tvSignIn);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        btnLogIn = findViewById(R.id.btnLogIn);

        btnLogIn.setOnClickListener(this);
        tvSignIn.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.tvSignIn:
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                break;
            case R.id.btnLogIn:


                doUserLogin();
                break;
        }
    }

    private void doUserLogin() {

        if (Validations.isValidEmail(etEmail, "Provide your Valid Email first!")
                && Validations.isEditTextFilled(etEmail, "Enter Password!")) {
            progressDialog.setMessage("Loging You ...");
            progressDialog.show();


            firebaseAuth.signInWithEmailAndPassword(etEmail.getText().toString(), etPassword.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Not sucessfull", Toast.LENGTH_SHORT).show();
                    } else {
                        progressDialog.dismiss();

                        startActivity(new Intent(LoginActivity.this, ChatUsersActivity.class));
                    }

                }
            });
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}
