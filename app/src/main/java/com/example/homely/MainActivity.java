package com.example.homely;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    BottomNavigationView bottomNavigationView;
    private boolean signInFromSettings = false;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "onCreate: MainActivity started");

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        authStateListener = firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                Log.d("MainActivity", "User is signed in: " + firebaseUser.getUid());
                fetchUserAndHomes(firebaseUser.getUid());
            } else {
                Log.d("MainActivity", "User is not signed in");
                redirectToAuth();
            }
        };

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bottom_nav_home) {
                Log.d("MainActivity", "Home nav item selected");
                if (user.getHomes().isEmpty()) {
                    loadAppropriateFragment(user, HomePageAddHomeFragment.class);
                    return true;
                }
                loadAppropriateFragment(user, HomePageFragment.class);
                return true;
            }
            if (item.getItemId() == R.id.bottom_nav_settings) {
                Log.d("MainActivity", "Settings nav item selected");
                loadAppropriateFragment(user, SettingsFragment.class);
                return true;
            }
            return false;
        });

        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void fetchUserAndHomes(String uid) {
        Log.d("MainActivity", "fetchUserAndHomes: Fetching user and homes for UID: " + uid);
        databaseReference.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("MainActivity", "fetchUserAndHomes: onDataChange called");
                        if (snapshot.exists()) {
                            String displayName = snapshot.child("displayName").getValue(String.class);
                            String email = snapshot.child("email").getValue(String.class);
                            String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                            List<Home> homes = new ArrayList<>();
                            long homeCount = snapshot.child("homes").getChildrenCount(); // Store the count of homes
                            if (homeCount != 0) {
                                for (DataSnapshot homeSnapshot : snapshot.child("homes").getChildren()) {
                                    String homeId = homeSnapshot.getKey();
                                    boolean isCurrentHome = Boolean.TRUE.equals(homeSnapshot.getValue(Boolean.class));
                                    fetchHomeDetails(uid, displayName, email, photoUrl, homes, homeId, isCurrentHome, homeCount);
                                }
                            }
                            else {
                                user = new User(uid, displayName, email, photoUrl, homes);
                                Log.d("MainActivity", "fetchHomeDetails: User and no homes data fetched, loading fragment");
                                if (signInFromSettings) {
                                    loadFragment(SettingsFragment.newInstance(user));
                                    bottomNavigationView.setSelectedItemId(R.id.bottom_nav_settings);
                                    signInFromSettings = false;
                                } else {
                                    loadAppropriateFragment(user, HomePageAddHomeFragment.class);
                                }
                            }
                        } else {
                            Log.d("MainActivity", "fetchUserAndHomes: User snapshot does not exist");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MainActivity", "fetchUserAndHomes: onCancelled: " + error.getMessage());
                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchHomeDetails(String uid, String displayName, String email, String photoUrl, List<Home> homes, String homeId, boolean isCurrentHome, long expectedHomeCount) {
        Log.d("MainActivity", "fetchHomeDetails: Fetching home details for Home ID: " + homeId);
        databaseReference.child("homes").child(homeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("MainActivity", "fetchHomeDetails: onDataChange called for Home ID: " + homeId);
                        if (snapshot.exists()) {
                            String homeName = snapshot.child("name").getValue(String.class);
                            Home home = new Home(homeId, homeName, isCurrentHome);
                            homes.add(home);
                            Log.d("MainActivity", "fetchHomeDetails: Added home: " + homeName + ", total homes fetched: " + homes.size() + ", expected homes: " + expectedHomeCount);
                            if (homes.size() == expectedHomeCount) {
                                user = new User(uid, displayName, email, photoUrl, homes);
                                Log.d("MainActivity", "fetchHomeDetails: User and homes data fetched, loading fragment");
                                if (signInFromSettings) {
                                    loadFragment(SettingsFragment.newInstance(user));
                                    bottomNavigationView.setSelectedItemId(R.id.bottom_nav_settings);
                                    signInFromSettings = false;
                                } else {
                                    loadAppropriateFragment(user, HomePageFragment.class);
                                }
                            }
                        } else {
                            Log.d("MainActivity", "fetchHomeDetails: Home snapshot does not exist for Home ID: " + homeId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MainActivity", "fetchHomeDetails: onCancelled: " + error.getMessage());
                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAppropriateFragment(User user, Class<? extends Fragment> fragmentClass) {
        Log.d("MainActivity", "loadAppropriateFragment: Loading fragment for user");
        try {
            Fragment fragment = fragmentClass.newInstance();
            Bundle bundle = new Bundle();
            bundle.putSerializable("user", user);
            fragment.setArguments(bundle);
            loadFragment(fragment);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment) {
        Log.d("MainActivity", "loadFragment: Loading fragment");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void redirectToAuth() {
        Log.d("MainActivity", "redirectToAuth: Redirecting to AuthActivity");
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    public void setSignInFromSettings(boolean signInFromSettings) {
        this.signInFromSettings = signInFromSettings;
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