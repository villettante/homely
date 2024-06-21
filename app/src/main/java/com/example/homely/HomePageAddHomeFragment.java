package com.example.homely;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.content.Context.MODE_PRIVATE;

public class HomePageAddHomeFragment extends Fragment {
    public HomePageAddHomeFragment() {}
    Activity referenceActivity;
    View parentHolder;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    GoogleSignInClient mGoogleSignInClient;
    ShapeableImageView accountPhoto;
    Button createHome;
    User user;
    private FirebaseAuth.AuthStateListener authStateListener;
    private static final int RC_SIGN_IN = 9001;

    public static HomePageAddHomeFragment newInstance(User user) {
        HomePageAddHomeFragment fragment = new HomePageAddHomeFragment();
        Bundle args = new Bundle();
        args.putString("uid", user.getUid());
        args.putString("displayName", user.getDisplayName());
        args.putString("email", user.getEmail());
        args.putString("photoUrl", user.getPhotoUrl());
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_home_page_add_home, container, false);

        if (getArguments() != null) {
            String uid = getArguments().getString("uid");
            String displayName = getArguments().getString("displayName");
            String email = getArguments().getString("email");
            String photoUrl = getArguments().getString("photoUrl");
            user = new User(uid, displayName, email, photoUrl);
            Log.e("HomePageAddHomeFragment", "User received: " + user.getEmail());
        }

        accountPhoto = parentHolder.findViewById(R.id.account_photo_home_page_add_home);
        if (user != null) {
            Glide.with(this).load(user.getPhotoUrl()).into(accountPhoto);
        } else {
            Log.e("HomePageAddHomeFragment", "currentUser is null in onCreateView");
        }

        createHome = parentHolder.findViewById(R.id.create_home_button);
        createHome.setOnClickListener(v -> {

        });

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        authStateListener = firebaseAuth1 -> {
            FirebaseUser firebaseUser = firebaseAuth1.getCurrentUser();
            if (firebaseUser != null) {
                checkUserInDatabase(firebaseUser);
            }
        };

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("768079826724-ddtmiivdits755tue8v1pmrltep7omv9.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(referenceActivity, gso);

        accountPhoto.setOnClickListener(v -> signOutAndSignIn());

        return parentHolder;
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void signOutAndSignIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(referenceActivity, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d("HomePageAddHomeFragment", account.getEmail() + " Signed in successfully");
                    firebaseAuthWithGoogle(account);
                } else {
                    Log.w("HomePageAddHomeFragment", "Account is null");
                }
            } catch (ApiException e) {
                Log.w("HomePageAddHomeFragment", "signInResult:failed code=" + e.getStatusCode());
                e.printStackTrace();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(referenceActivity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.d("HomePageAddHomeFragment", " Currently signed in: " + user.getEmail());
                        checkUserInDatabase(user);
                    } else {
                        Log.w("HomePageAddHomeFragment", "signInWithCredential:failure", task.getException());
                        Toast.makeText(referenceActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        DatabaseReference userRef = databaseReference.child("users").child(firebaseUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "");
                    storeNewUserData(firebaseUser);
                } else {
                    user = snapshot.getValue(User.class);
                }
                updateUI(user);
                if (user != null) {
                    saveUserToPreferences(user);
                    if (referenceActivity instanceof MainActivity) {
                        ((MainActivity) referenceActivity).reloadFragments();
                    }
                }
                else {
                    Log.e("HomePageAddHomeFragment", "Could not update the new preferences");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }

    private void storeNewUserData(FirebaseUser user) {
        String uid = user.getUid();
        String displayName = user.getDisplayName();
        String email = user.getEmail();
        String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";

        User newUser = new User(uid, displayName, email, photoUrl);

        databaseReference.child("users").child(user.getUid()).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("HomePageAddHomeFragment", "User data saved successfully.");
                    } else {
                        Log.w("HomePageAddHomeFragment", "Failed to save user data.", task.getException());
                    }
                });
    }

    private void updateUI(User user) {
        if (user != null) {
            if (!user.getPhotoUrl().isEmpty()) {
                Glide.with(this).load(user.getPhotoUrl()).into(accountPhoto);
            } else {
                accountPhoto.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }

    private void saveUserToPreferences(User user) {
        SharedPreferences sharedPreferences = referenceActivity.getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", user.getUid());
        editor.putString("displayName", user.getDisplayName());
        editor.putString("email", user.getEmail());
        editor.putString("photoUrl", user.getPhotoUrl());
        editor.apply();
    }
}