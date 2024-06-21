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

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("768079826724-ddtmiivdits755tue8v1pmrltep7omv9.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (firebaseUser != null) {
            checkUserInDatabaseAndProceed(firebaseUser);
        } else {
            Log.d("AuthActivity", "No user is signed in");
        }
        
        googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            Log.w("AuthActivity", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserInDatabaseAndProceed(user);
                        }
                    } else {
                        Log.w("AuthActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabaseAndProceed(FirebaseUser firebaseUser) {
        DatabaseReference userRef = databaseReference.child("users").child(firebaseUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "");
                    storeNewUserData(firebaseUser, user);
                } else {
                    user = snapshot.getValue(User.class);
                    updateUIAndProceed(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AuthActivity", "retrieveUserData onCancelled: " + error.getMessage());
            }
        });
    }

    private void storeNewUserData(FirebaseUser firebaseUser, User user) {
        databaseReference.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthActivity", "User data saved successfully.");
                        updateUIAndProceed(user);
                    } else {
                        Log.w("AuthActivity", "Failed to save user data.", task.getException());
                    }
                });
    }

    private void updateUIAndProceed(User user) {
        saveUserToPreferences(user);
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        Log.d("AuthActivity", "User passed to MainActivity: " + user.getEmail());
        startActivity(intent);
        finish();
    }

    private void saveUserToPreferences(User user) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", user.getUid());
        editor.putString("displayName", user.getDisplayName());
        editor.putString("email", user.getEmail());
        editor.putString("photoUrl", user.getPhotoUrl());
        editor.apply();
    }
}