package com.pankaj.firebasechatdemo.acitivitys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.pankaj.firebasechatdemo.image_pic_utils.DefaultCallback;
import com.pankaj.firebasechatdemo.image_pic_utils.EasyImage;
import com.pankaj.firebasechatdemo.model.User;
import com.pankaj.firebasechatdemo.utils.AppPermissions;
import com.pankaj.firebasechatdemo.utils.ProgressDialogUtil;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

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

        EasyImage.configuration(this)
                .setImagesFolderName("EasyImage sample")
                .setCopyTakenPhotosToPublicGalleryAppFolder(true)
                .setCopyPickedImagesToPublicGalleryAppFolder(true)
                .setAllowMultiplePickInGallery(false);

        /**listen to imageview click**/
        mUserPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mRuntimePermission.hasPermission(CAMERA_ALL_PERMISSION)) {
                   //openCameraDialog();
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
                    if (byteArray!=null)
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

        if (EasyImage.canDeviceHandleGallery(this)) {
            //Device has no app that handles gallery intent
            builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    /** Some devices such as Samsungs which have their own gallery app require write permission. Testing is advised! */
                    EasyImage.openGallery(UpdateProfileActivity.this, 0);
                }
            });

        }
        builder.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EasyImage.openCamera(UpdateProfileActivity.this, 0);

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                //Some error handling
                e.printStackTrace();
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
               // onPhotosReturned(imageFiles);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = null;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                switch (source){

                    case CAMERA:


                        //**convert bitmap to byte array to store in firebase storage**//*

                        try {
                            compreesImageByWidth(imageFiles.get(0));

                            bmOptions = new BitmapFactory.Options();
                            bitmap = BitmapFactory.decodeFile(imageFiles.get(0).getAbsolutePath(), bmOptions);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                            Picasso.get()
                                    .load(imageFiles.get(0))
                                    .fit()
                                    .centerCrop()
                                    .into(mUserPhotoImageView);
                             byteArray = stream.toByteArray();
                            break;


                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    case DOCUMENTS:

                        break;

                    case GALLERY:
                        try {
                            compreesImageByWidth(imageFiles.get(0));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        bmOptions = new BitmapFactory.Options();
                        bitmap = BitmapFactory.decodeFile(imageFiles.get(0).getAbsolutePath(), bmOptions);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                        Picasso.get()
                                .load(imageFiles.get(0))
                                .fit()
                                .centerCrop()
                                .into(mUserPhotoImageView);


                        byteArray = stream.toByteArray();

                        break;
                }
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                //Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(UpdateProfileActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }
        });

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



    public static void compreesImageByWidth(File imgFileOrig) throws IOException {
        // we'll start with the original picture already open to a file
        Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());
// original measurements
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();

        final int destWidth = 1000;//or the width you need

        if (origWidth > destWidth) {
            // picture is wider than we want it, we calculate its target height
            int destHeight = origHeight / (origWidth / destWidth);
            // we create an scaled bitmap so it reduces the image, not just trim it
            Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b2.compress(Bitmap.CompressFormat.JPEG, 70, outStream);
            // we save the file, at least until we have made use of it
            File f = new File(imgFileOrig.getAbsolutePath());
            f.createNewFile();
            //write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(outStream.toByteArray());
            // remember close de FileOutput
            fo.close();
        }
    }

    private void onPhotosReturned(List<File> returnedPhotos) {
       // photos.addAll(returnedPhotos);
       // imagesAdapter.notifyDataSetChanged();
       // recyclerView.scrollToPosition(photos.size() - 1);
    }

    @Override
    protected void onDestroy() {
        // Clear any configuration that was done!
        EasyImage.clearConfiguration(this);
        super.onDestroy();
    }
}