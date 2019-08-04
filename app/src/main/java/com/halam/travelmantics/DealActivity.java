package com.halam.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.halam.travelmantics.data.TravelDeal;
import com.halam.travelmantics.utils.FirebaseUtill;
import com.halam.travelmantics.utils.NetworkUtils;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private EditText mTxtTitle;
    private EditText mTxtPrice;
    private EditText mTxtDescription;
    TravelDeal deal;
    private int PICTURE_RESULT = 55;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        FirebaseUtill.connectStorage();
        // Creates an instance of the database
        mFirebaseDatabase = FirebaseUtill.mFirebaseDatabase;

        // Creates a reference of the database and assigns the targeted path
        mDatabaseReference = FirebaseUtill.mDatabaseReference;

        mTxtTitle = findViewById(R.id.txtTitle);
        mTxtDescription = findViewById(R.id.txtDescription);
        mTxtPrice = findViewById(R.id.txtPrice);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
//        if (deal == null) {
//            deal = new TravelDeal();
//        }
        this.deal = deal;
        imageView = findViewById(R.id.uploaded_img);

        if (deal != null) {
            mTxtTitle.setText(deal.getTitle());
            mTxtDescription.setText(deal.getDescription());
            mTxtPrice.setText(deal.getPrice());
            showImage(deal.getImageUrl());
        }
        findViewById(R.id.btn_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/jpeg");
                i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(i, getString(R.string.choose_images)), PICTURE_RESULT);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInternetAvailable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtill.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                if (savedDeal()) {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show();
                    clean();
                    backToList();
                }
                return true;
            case R.id.delete_menu:
                if (deleteDeal()) {
                    Toast.makeText(this, R.string.deleted, Toast.LENGTH_LONG).show();
                    backToList();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean savedDeal() {
        if (mTxtTitle.getText().length() == 0 || mTxtPrice.getText().length() == 0) {
            Toast.makeText(this, R.string.complete_data, Toast.LENGTH_LONG).show();
            return false;
        }
        if (deal == null)
            deal = new TravelDeal();
        deal.setTitle(mTxtTitle.getText().toString());
        deal.setDescription(mTxtDescription.getText().toString());
        deal.setPrice(mTxtPrice.getText().toString());
        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            if (deal == null)
                deal = new TravelDeal();
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtill.mFirebaseStorageR.child(imageUri.getLastPathSegment());

            ref.getDownloadUrl().addOnSuccessListener(this, new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    deal.setImageUrl(url);
//                    deal.setImageName(pictureName);
//                    Log.d("Url: ", url);
//                    Log.d("Name", pictureName);
                    showImage(url);
                }
            });
            ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    deal.setImageName(taskSnapshot.getStorage().getPath());

                }
            });
        }
    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
//                    .resize(width, width * 2 / 3)
//                    .centerCrop()
                    .into(imageView);
        }
    }

    private boolean deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, R.string.no_deal, Toast.LENGTH_SHORT).show();
            return false;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if (deal.getImageName() != null && deal.getImageName().isEmpty() == false) {

            StorageReference picRef = FirebaseUtill.mFirebaseStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // File deleted successfully
                    Toast.makeText(DealActivity.this, R.string.image_deleted_succeful, Toast.LENGTH_LONG).show();
                    Log.d("Delete image", "Delete Successful");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Uh-oh, an error occurred!
                    Log.d("Delete Image", exception.getMessage());
                }
            });
        }
        return true;
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void clean() {
        mTxtTitle.setText("");
        mTxtPrice.setText("");
        mTxtDescription.setText("");
        mTxtTitle.requestFocus();
        if (deal != null) {
            deal.setImageName(null);
            deal.setImageUrl(null);
        }
    }

    private void enableEditTexts(boolean isEnabled) {
        mTxtTitle.setEnabled(isEnabled);
        mTxtDescription.setEnabled(isEnabled);
        mTxtPrice.setEnabled(isEnabled);
    }

    public boolean isInternetAvailable() {
        boolean isConnected = NetworkUtils.isConnected(this);
        Toast.makeText(this, R.string.nointernetconnection, Toast.LENGTH_LONG).show();
        return isConnected;

    }
}
