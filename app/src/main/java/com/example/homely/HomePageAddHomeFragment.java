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

import java.util.ArrayList;
import java.util.List;

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
        args.putSerializable("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_home_page_add_home, container, false);
        user = (User) getArguments().getSerializable("user");
        Log.e("HomePageAddHomeFragment", "User received: " + user.getEmail());
        user.getHomes().forEach(home -> Log.e("HomePageAddHomeFragment/Homes", home.getName()));

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        accountPhoto = parentHolder.findViewById(R.id.account_photo_home_page_add_home);
        if (user != null) {
            Glide.with(this).load(user.getPhotoUrl()).into(accountPhoto);
        } else {
            Log.e("HomePageAddHomeFragment", "currentUser is null in onCreateView");
        }

        createHome = parentHolder.findViewById(R.id.create_home_button);
        createHome.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddHomeActivity.class);
            startActivity(intent);
        });

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

    public void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkIfUserIsLoggedIn();
    }

    private void checkIfUserIsLoggedIn() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(referenceActivity);

        if (firebaseUser == null && googleSignInAccount == null) {
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            startActivity(intent);
            referenceActivity.finish();
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
                        if (user != null) {
                            checkUserInDatabase(user);
                        }
                    } else {
                        Log.w("HomePageAddHomeFragment", "signInWithCredential:failure", task.getException());
                        Toast.makeText(referenceActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        DatabaseReference userRef = databaseReference.child("users").child(firebaseUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "", new ArrayList<>());
                    storeNewUserData(firebaseUser, user);
                } else {
                    String uid = firebaseUser.getUid();
                    String displayName = snapshot.child("displayName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                    user = new User(uid, displayName, email, photoUrl, new ArrayList<>());
                    fetchHomesForUser(user, firebaseUser.getUid());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomePageAddHomeFragment", "retrieveUserData onCancelled: " + error.getMessage());
            }
        });
    }

    private void fetchHomesForUser(User user, String uid) {
        DatabaseReference homesRef = databaseReference.child("users").child(uid).child("homes");
        homesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Home> homes = new ArrayList<>();
                for (DataSnapshot homeSnapshot : dataSnapshot.getChildren()) {
                    String homeId = homeSnapshot.getKey();
                    boolean isCurrentHome = Boolean.TRUE.equals(homeSnapshot.getValue(Boolean.class));
                    fetchHomeDetails(homeId, isCurrentHome, homes, user, uid);
                    updateUI(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomePageAddHomeFragment", "fetchHomesForUser onCancelled: " + error.getMessage());
            }
        });
    }

    private void fetchHomeDetails(String homeId, boolean isCurrentHome, List<Home> homes, User user, String uid) {
        DatabaseReference homeRef = databaseReference.child("homes").child(homeId);
        homeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String homeName = dataSnapshot.child("name").getValue(String.class);
                    Home home = new Home(homeId, homeName, isCurrentHome);
                    homes.add(home);
                    if (homes.size() == dataSnapshot.child("homes").getChildrenCount()) {
                        user.setHomes(homes);
                    }
                }
                else {
                    Intent intent = new Intent(referenceActivity, MainActivity.class);
                    intent.putExtra("user", user);
                    startActivity(intent);
                    referenceActivity.finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomePageAddHomeFragment", "fetchHomeDetails onCancelled: " + error.getMessage());
            }
        });
    }

    private void storeNewUserData(FirebaseUser firebaseUser, User user) {
        databaseReference.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("HomePageAddHomeFragment", "User data saved successfully.");
                        updateUI(user);
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
}
