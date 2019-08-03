package com.halam.travelmantics.utils;

import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.halam.travelmantics.ListActivity;
import com.halam.travelmantics.data.TravelDeal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirebaseUtill {
    public static FirebaseDatabase mFirebaseDatabase;
    public static DatabaseReference mDatabaseReference;
    private static FirebaseUtill firebaseUtill;
    private static FirebaseAuth mFirebaseAuth;
    private static FirebaseStorage mFirebaseStorage;
    public static StorageReference mFirebaseStorageR;
    private static FirebaseAuth.AuthStateListener mAuthStateListener;
    public static ArrayList<TravelDeal> mDeals;
    private static ListActivity caller;
    public static boolean isAdmin;

    private static final int RC_SIGN_IN = 123;

    private FirebaseUtill() {
    }

    public static void openFbReference(String ref, final ListActivity callerActivity) {
        if (firebaseUtill == null) {
            firebaseUtill = new FirebaseUtill();
            FirebaseApp.initializeApp(callerActivity);
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mFirebaseAuth = FirebaseAuth.getInstance();
            caller = callerActivity;
            mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() == null) {
                        FirebaseUtill.signIn();
                    } else {
                        String userId = firebaseAuth.getUid();
                        checkAdmin(userId);
                    }
                    Toast.makeText(callerActivity.getBaseContext(), "Welcome back!", Toast.LENGTH_LONG).show();
                }
            };

        }
        mDeals = new ArrayList<TravelDeal>();
        mDatabaseReference = mFirebaseDatabase.getReference().child(ref);

    }

    private static void checkAdmin(String uid) {
        FirebaseUtill.isAdmin = false;
        DatabaseReference ref = mFirebaseDatabase.getReference().child("adminstrators")
                .child(uid);
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FirebaseUtill.isAdmin = true;
                Log.d("Admin", "You are an administrator");
                caller.showMenu();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        ref.addChildEventListener(listener);
    }

    private static void signIn() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        caller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    public static void attachListener() {
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    public static void detachListener() {
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    public static void connectStorage() {
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorageR = mFirebaseStorage.getReference().child("deals_pictures");
    }
}
