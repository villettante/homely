package com.example.homely;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        user = getUserFromPreferences();
        checkUserSignIn();

        authStateListener = firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                currentUser = firebaseUser;
                user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
                saveUserToPreferences(user);
                reloadFragments();
            } else {
                redirectToAuth();
            }
        };

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bottom_nav_home) {
                initializeUI(user);
                return true;
            }
            if (item.getItemId() == R.id.bottom_nav_settings) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, SettingsFragment.newInstance(user))
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private User getUserFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", null);
        String displayName = sharedPreferences.getString("displayName", null);
        String email = sharedPreferences.getString("email", null);
        String photoUrl = sharedPreferences.getString("photoUrl", null);

        if (uid != null) {
            return new User(uid, displayName, email, photoUrl);
        }
        return null;
    }

    private void checkUserSignIn() {
        if (currentUser == null) {
            Log.d("MainActivity", "No user is signed in");
            redirectToAuth();
        }
        else {
            User user = new User(currentUser.getUid(), currentUser.getDisplayName(), currentUser.getEmail(), String.valueOf(currentUser.getPhotoUrl()));
            initializeUI(user);
        }
    }

    private void initializeUI(User user) {
        Log.e("MainActivity", "User user uid: " + user.getUid() + ", email: " + user.getEmail());
        Log.e("MainActivity", "FirebaseUser user uid: " + currentUser.getUid() + ", email: " + currentUser.getEmail());

        databaseReference.child("users").child(currentUser.getUid()).child("homes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        saveUserToPreferences(user);
                        if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                            getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, HomePageAddHomeFragment.newInstance(user)).commit();
                        } else {
                            getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, HomePageFragment.newInstance(user)).commit();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void redirectToAuth() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    public void reloadFragments() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bottom_nav_home) {
                initializeUI(user);
                return true;
            }
            if (item.getItemId() == R.id.bottom_nav_settings) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, SettingsFragment.newInstance(user))
                        .commit();
                return true;
            }
            return false;
        });
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