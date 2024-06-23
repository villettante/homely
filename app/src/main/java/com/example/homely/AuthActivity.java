package com.example.homely;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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

public class AuthActivity extends AppCompatActivity {
    static final int RC_SIGN_IN = 9001;
    FirebaseAuth mAuth;
    FirebaseUser firebaseUser;
    GoogleSignInClient mGoogleSignInClient;
    SignInButton googleSignInButton;
    DatabaseReference databaseReference;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        Log.d("AuthActivity", "onCreate: AuthActivity started");

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("768079826724-ddtmiivdits755tue8v1pmrltep7omv9.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> {
            Log.d("AuthActivity", "Google sign-in button clicked");
            signInWithGoogle();
        });

        // Check if user is already signed in
        firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            Log.d("AuthActivity", "User is already signed in: " + firebaseUser.getUid());
            checkUserInDatabaseAndProceed(firebaseUser);
        } else {
            Log.d("AuthActivity", "No user is signed in");
        }
    }

    private void signInWithGoogle() {
        Log.d("AuthActivity", "signInWithGoogle: Starting sign-in intent");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Log.d("AuthActivity", "onActivityResult: Received result for RC_SIGN_IN");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d("AuthActivity", "handleSignInResult: Google Sign-In successful");
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            Log.w("AuthActivity", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("AuthActivity", "firebaseAuthWithGoogle: Authenticating with Firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("AuthActivity", "firebaseAuthWithGoogle: Firebase sign-in successful");
                            checkUserInDatabaseAndProceed(user);
                        }
                    } else {
                        Log.w("AuthActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabaseAndProceed(FirebaseUser firebaseUser) {
        Log.d("AuthActivity", "checkUserInDatabaseAndProceed: Checking user in database: " + firebaseUser.getUid());
        DatabaseReference userRef = databaseReference.child("users").child(firebaseUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d("AuthActivity", "checkUserInDatabaseAndProceed: User does not exist in database");
                    user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "", new ArrayList<>());
                    storeNewUserData(firebaseUser, user);
                } else {
                    Log.d("AuthActivity", "checkUserInDatabaseAndProceed: User exists in database");
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
                Log.e("AuthActivity", "checkUserInDatabaseAndProceed: onCancelled: " + error.getMessage());
            }
        });
    }

    private void fetchHomesForUser(User user, String uid) {
        Log.d("AuthActivity", "fetchHomesForUser: Fetching homes for user: " + uid);
        DatabaseReference homesRef = databaseReference.child("users").child(uid).child("homes");
        homesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Home> homes = new ArrayList<>();
                long homeCount = dataSnapshot.getChildrenCount();
                Log.d("AuthActivity", "fetchHomesForUser: Total homes: " + homeCount);
                if (homeCount != 0) {
                    for (DataSnapshot homeSnapshot : dataSnapshot.getChildren()) {
                        String homeId = homeSnapshot.getKey();
                        boolean isCurrentHome = Boolean.TRUE.equals(homeSnapshot.getValue(Boolean.class));
                        fetchHomeDetails(homeId, isCurrentHome, homes, user, uid, homeCount);
                    }
                }
                else {
                    updateUIAndProceed(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AuthActivity", "fetchHomesForUser: onCancelled: " + error.getMessage());
            }
        });
    }

    private void fetchHomeDetails(String homeId, boolean isCurrentHome, List<Home> homes, User user, String uid, long expectedHomeCount) {
        Log.d("AuthActivity", "fetchHomeDetails: Fetching home details for Home ID: " + homeId);
        DatabaseReference homeRef = databaseReference.child("homes").child(homeId);
        homeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("AuthActivity", "fetchHomeDetails: onDataChange called for Home ID: " + homeId);
                if (dataSnapshot.exists()) {
                    String homeName = dataSnapshot.child("name").getValue(String.class);
                    Home home = new Home(homeId, homeName, isCurrentHome);
                    homes.add(home);
                    Log.d("AuthActivity", "fetchHomeDetails: Added home: " + homeName + ", total homes fetched: " + homes.size() + ", expected homes: " + expectedHomeCount);
                    if (homes.size() == expectedHomeCount) {
                        user.setHomes(homes);
                        Log.d("AuthActivity", "fetchHomeDetails: User and homes data fetched, updating UI and proceeding");
                        updateUIAndProceed(user);
                    }
                } else {
                    Log.d("AuthActivity", "fetchHomeDetails: Home snapshot does not exist for Home ID: " + homeId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AuthActivity", "fetchHomeDetails: onCancelled: " + error.getMessage());
            }
        });
    }

    private void storeNewUserData(FirebaseUser firebaseUser, User user) {
        Log.d("AuthActivity", "storeNewUserData: Storing new user data");
        databaseReference.child("publicProfiles").child(firebaseUser.getUid()).setValue(user.getPhotoUrl())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthActivity", "storeNewUserData: Public user data saved successfully");
                    } else {
                        Log.w("AuthActivity", "storeNewUserData: Failed to save public user data", task.getException());
                    }
                });
        databaseReference.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthActivity", "storeNewUserData: User data saved successfully");
                        updateUIAndProceed(user);
                    } else {
                        Log.w("AuthActivity", "storeNewUserData: Failed to save user data", task.getException());
                    }
                });
    }

    private void updateUIAndProceed(User user) {
        Log.d("AuthActivity", "updateUIAndProceed: Updating UI and proceeding");
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        Log.d("AuthActivity", "User passed to MainActivity: " + user.getEmail());
        startActivity(intent);
        finish();
    }
}