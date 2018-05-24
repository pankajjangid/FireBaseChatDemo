package com.pankaj.firebasechatdemo.acitivitys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.model.User;
import com.pankaj.firebasechatdemo.utils.AppPermissions;
import com.pankaj.firebasechatdemo.utils.ProgressDialogUtil;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_PICK = 2;
    private static final int ALL_PERMISSION_REQUEST_CODE = 10;

    private ImageView mUserPhotoImageView;
    private EditText mUserNameEdit;
    private Button mUpdateProfileBtn;

    private byte[] byteArray = null;
    private DatabaseReference mUserDBRef;
    private StorageReference mStorageRef;
    private String mCurrentUserID;

    private static final String[] CAMERA_ALL_PERMISSION = {
            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private AppPermissions mRuntimePermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);


        //init
        mRuntimePermission = new AppPermissions(this);
        mUserPhotoImageView = (ImageView)findViewById(R.id.userPhotoUpdate);
        mUserNameEdit = (EditText)findViewById(R.id.userNameUpdate);
        mUpdateProfileBtn = (Button)findViewById(R.id.updateUserProfileBtn);

        mCurrentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //init firebase
        mUserDBRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mStorageRef = FirebaseStorage.getInstance().getReference().child("Photos").child("Users");

        /**populate views initially**/
        populateTheViews();

        /**listen to imageview click**/
        mUserPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mRuntimePermission.hasPermission(CAMERA_ALL_PERMISSION)) {
                   openCameraDialog();
                } else {
                    mRuntimePermission.requestPermission(UpdateProfileActivity.this, CAMERA_ALL_PERMISSION, ALL_PERMISSION_REQUEST_CODE);
                }


            }
        });

        /**listen to update btn click**/
        mUpdateProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userDisplayName = mUserNameEdit.getText().toString().trim();

                /**Call the Firebase methods**/
                try {
                    updateUserName(userDisplayName);
                    updateUserPhoto(byteArray);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void openCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(UpdateProfileActivity.this);
        builder.setTitle("Change photo");
        builder.setMessage("Choose a method to change photo");
        builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pickPhotoFromGallery();
            }
        });
        builder.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dispatchTakePictureIntent();
            }
        });
        builder.create().show();
    }


    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                     @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ALL_PERMISSION_REQUEST_CODE:
                List<Integer> permissionResults = new ArrayList<>();
                for (int grantResult : grantResults) {
                    permissionResults.add(grantResult);
                }
                if (permissionResults.contains(PackageManager.PERMISSION_DENIED)) {
                    Toast.makeText(this, "Please Allow All Permissions", Toast.LENGTH_SHORT).show();
                } else {
                    openCameraDialog();
                }
                break;

        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        /**populate views initially**/
        populateTheViews();
    }


    private void populateTheViews(){
        mUserDBRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User currentuser = dataSnapshot.getValue(User.class);
                try {
                    String userPhoto = currentuser.getImage();
                    String userName = currentuser.getDisplayName();

                    Picasso.get().load(userPhoto).placeholder(R.mipmap.ic_launcher).into(mUserPhotoImageView);
                    mUserNameEdit.setText(userName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickPhotoFromGallery(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mUserPhotoImageView.setImageBitmap(imageBitmap);

            /**convert bitmap to byte array to store in firebase storage**/
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            assert imageBitmap != null;
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();

        }else if(requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mUserPhotoImageView.setImageBitmap(imageBitmap);

            /** convert bitmap to byte array to store in firebase storage**/
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            assert imageBitmap != null;
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
        }
    }

    private void updateUserName(String newDisplayName){
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("displayName", newDisplayName);
        mUserDBRef.child(mCurrentUserID).updateChildren(childUpdates);
    }

    private void updateUserPhoto(byte[] photoByteArray){
        ProgressDialogUtil.showProgress(UpdateProfileActivity.this,"Uploading ...please wait");
        // Create file metadata with property to delete
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(null)
                .setContentLanguage("en")
                .build();

        mStorageRef.child(mCurrentUserID).putBytes(photoByteArray, metadata).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                ProgressDialogUtil.hideProgress();

                if(!task.isSuccessful()){
                    //error saving photo
                    Toast.makeText(UpdateProfileActivity.this, "Error , Can't Upload ,Please Retry ", Toast.LENGTH_SHORT).show();
                }else{
                    //success saving photo
                    String userPhotoLink = task.getResult().getDownloadUrl().toString();
                    //now update the database with this user photo
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("image", userPhotoLink);
                    mUserDBRef.child(mCurrentUserID).updateChildren(childUpdates);

                    Toast.makeText(UpdateProfileActivity.this, "Updated Successfuly ... ", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }





}