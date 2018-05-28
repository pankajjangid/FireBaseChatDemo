package com.pankaj.firebasechatdemo.acitivitys;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pankaj.firebasechatdemo.R;
import com.pankaj.firebasechatdemo.adapter.MessagesAdapter;
import com.pankaj.firebasechatdemo.model.ChatMessage;
import com.pankaj.firebasechatdemo.model.User;
import com.pankaj.firebasechatdemo.utils.AppPermissions;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mChatsRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private EditText mMessageEditText;
    private ImageButton mSendImageButton,pickMedia;
    private DatabaseReference mMessagesDBRef;
    private DatabaseReference mUsersRef;
    private List<ChatMessage> mMessagesList = new ArrayList<>();
    private MessagesAdapter adapter = null;

    private String mReceiverId;
    private String mReceiverName;
    String senderId;
    String senderName;
    private byte[] byteArray = null;
    Toolbar toolbar;


    //Permission
    private static final String[] CAMERA_ALL_PERMISSION = {
            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private AppPermissions mRuntimePermission;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_PICK = 2;
    private static final int ALL_PERMISSION_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRuntimePermission = new AppPermissions(this);

        //get Current User Info
        senderId     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();


        //initialize the views
        mChatsRecyclerView = (RecyclerView)findViewById(R.id.messagesRecyclerView);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendImageButton = (ImageButton)findViewById(R.id.sendMessageImagebutton);
        pickMedia = (ImageButton)findViewById(R.id.pickMedia);
        mChatsRecyclerView.setHasFixedSize(true);


        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        mChatsRecyclerView.setLayoutManager(mLayoutManager);

        //init Firebase
        mMessagesDBRef = FirebaseDatabase.getInstance().getReference().child("Messages");
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        //get receiverId from intent
        mReceiverId = getIntent().getStringExtra("USER_ID");




        /**listen to send message imagebutton click**/
        mSendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mMessageEditText.getText().toString();


                if(message.isEmpty()){
                    Toast.makeText(ChatActivity.this, "You must enter a message", Toast.LENGTH_SHORT).show();
                }else {
                    //message is entered, send
                    sendMessageToFirebase(message,false);
                }
            }
        });


        //pick the images
        pickMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mRuntimePermission.hasPermission(CAMERA_ALL_PERMISSION)) {
                    openCameraDialog();
                } else {
                    mRuntimePermission.requestPermission(ChatActivity.this, CAMERA_ALL_PERMISSION, ALL_PERMISSION_REQUEST_CODE);
                }

            }
        });
    }

    private void sendMessageToFirebase(String message,boolean isMedia){
        mMessagesList.clear();
        ChatMessage newMsg;
        if (isMedia)
         newMsg = new ChatMessage("",senderId,senderName, mReceiverId,mReceiverName,message,true);
        else
         newMsg = new ChatMessage(message,senderId,senderName, mReceiverId,mReceiverName,"",false);
        mMessagesDBRef.push().setValue(newMsg).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    //error
                    Toast.makeText(ChatActivity.this, "Error " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(ChatActivity.this, "Message sent successfully!", Toast.LENGTH_SHORT).show();
                    mMessageEditText.setText(null);
                    hideSoftKeyboard();
                }
            }
        });


    }
    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        /**Query and populate chat messages**/
        querymessagesBetweenThisUserAndClickedUser();


        /**sets title bar with recepient name**/
        try {
            queryRecipientName(mReceiverId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void querymessagesBetweenThisUserAndClickedUser(){

        mMessagesDBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mMessagesList.clear();
                for(DataSnapshot snap: dataSnapshot.getChildren()){
                    ChatMessage chatMessage = snap.getValue(ChatMessage.class);
                    if(chatMessage.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) && chatMessage.getReceiverId().equals(mReceiverId) || chatMessage.getSenderId().equals(mReceiverId) && chatMessage.getReceiverId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        mMessagesList.add(chatMessage);
                    }

                }

                /**populate messages**/
                populateMessagesRecyclerView();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void populateMessagesRecyclerView(){
        adapter = new MessagesAdapter(mMessagesList, this);
        mChatsRecyclerView.setAdapter(adapter);

    }

    private void queryRecipientName(final String receiverId){

        mUsersRef.child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User recepient = dataSnapshot.getValue(User.class);
                assert recepient != null;
                mReceiverName = recepient.getDisplayName();

                try {
                    getSupportActionBar().setTitle(mReceiverName);
                    getActionBar().setTitle(mReceiverName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void uploadChatImageImage() {

        if(byteArray != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            StorageReference  mStorageRef = FirebaseStorage.getInstance().getReference().child("Photos").child("Chat").child("images/"+senderId+" "+senderName+"->"+mReceiverName+"/"+getDateTimeStamp());

            mStorageRef.putBytes(byteArray)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(ChatActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();

                            sendMessageToFirebase(taskSnapshot.getDownloadUrl().toString(),true);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(ChatActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");


                        }
                    });
        }
    }


    //Camera Work
    private void openCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
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
           // mUserPhotoImageView.setImageBitmap(imageBitmap);

            /**convert bitmap to byte array to store in firebase storage**/
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            assert imageBitmap != null;
           imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();

            uploadChatImageImage();
        }else if(requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
           // mUserPhotoImageView.setImageBitmap(imageBitmap);

            /** convert bitmap to byte array to store in firebase storage**/
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            assert imageBitmap != null;
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byteArray = stream.toByteArray();

            uploadChatImageImage();
        }
    }

    public static String getDateTimeStamp() {
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
        return s.format(new Date());
    }


}
