package com.eemf.sirgoingfar.travelmantics.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.eemf.sirgoingfar.travelmantics.R;
import com.eemf.sirgoingfar.travelmantics.models.Item;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class AddItemActivity extends AppCompatActivity {

    private final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 0;
    private final int REQUEST_CODE_PICK_IMAGE_FROM_GALLERY = 1;


    private EditText etLabel;
    private EditText etDesc;
    private EditText etValue;
    private ImageView ivImage;
    private Button btnSelectImage;

    private Uri mImageUri;
    private Item mItem;

    private FirebaseStorage mStorage;
    private DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        etLabel = findViewById(R.id.et_label);
        etDesc = findViewById(R.id.et_desc);
        etValue = findViewById(R.id.et_value);
        ivImage = findViewById(R.id.iv_selected_image);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageFromGallery();
            }
        });

        mStorage = FirebaseStorage.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference(FirebaseAuth.getInstance().getUid());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveItem();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveItem() {
        String label = etLabel.getText().toString();
        String desc = etDesc.getText().toString();
        String value = etValue.getText().toString();

        if (TextUtils.isEmpty(label)) {
            etLabel.setError("Label cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(desc)) {
            etDesc.setError("Description cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(value)) {
            etValue.setError("Value cannot be empty");
            return;
        }

        if(mImageUri == null){
            toastMsg("Select an image");
            return;
        }

        mItem = new Item(label, desc, value);
        saveImageToCloudStorage(mImageUri, mItem);
    }

    private void saveImageToCloudStorage(Uri imageUri, final Item item) {
        final StorageReference ref = mStorage.getReference().child("deals_pictures").child(imageUri.getLastPathSegment());
        ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        item.setUrl(uri.toString());
                        String id = mDatabaseReference.push().getKey();
                        item.setId(id);
                        mDatabaseReference.child(id).setValue(item).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    toastMsg("Item added");
                                    finish();
                                } else {
                                    toastMsg("Process failed, please try again.");
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void toastMsg(String msg) {
        Toast.makeText(AddItemActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            openGallery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE_FROM_GALLERY && data != null) {
            handleSelectedImage(data.getData());
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        if (imageUri != null) {
            mImageUri = imageUri;
            ivImage.setImageURI(imageUri);
            btnSelectImage.setText(getString(R.string.text_pick_another_image));
        }
    }

    private void selectImageFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimetypes = {"image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE_FROM_GALLERY);
    }
}
