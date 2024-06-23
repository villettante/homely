package com.example.homely;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends Fragment {
    public SettingsFragment() {}
    Activity referenceActivity;
    View parentHolder;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    ShapeableImageView accountPhoto;
    ListView listView;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private User user;
    private TextView homeNameTitle;
    private Button addNewAccountButton;
    private LinearLayout userContainer;
    private FirebaseAuth.AuthStateListener authStateListener;
    private List<String> currentHomeUsersPhotoUrl = new ArrayList<>();

    public static SettingsFragment newInstance(User user) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    private void getCurrentHomeName(User user) {
        for (Home home : user.getHomes()) {
            if (home.isCurrent()) {
                homeNameTitle.setText(home.getName());
                break;
            }
        }
    }

    private void getCurrentHomeUsersPhotoUrl(User user) {
        for (Home home : user.getHomes()) {
            if (home.isCurrent()) {
                DatabaseReference homeRef = databaseReference.child("homes").child(home.getId()).child("users");
                homeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, String> currentHomeUsersStatus = (HashMap) dataSnapshot.getValue();
                        DatabaseReference usersRef = databaseReference.child("users");
                        for (Map.Entry<String, String> uniqueId : currentHomeUsersStatus.entrySet()) {
                            usersRef.child(uniqueId.getKey()).child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    loadUser(snapshot.getValue().toString());
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("SettingsFragment", "Error fetching data for user " + uniqueId + ": " + error.getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SettingsFragment", "fetchHomesForUser onCancelled: " + error.getMessage());
                    }
                });
                break;
            }
        }
    }

    private void loadUser(String imageUrl) {
        ShapeableImageView userImage = new ShapeableImageView(new ContextThemeWrapper(parentHolder.getContext(), R.style.roundedImageViewRounded));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                80, 80);
        params.setMargins(8, 0, 8, 0);
        userImage.setLayoutParams(params);
        userImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .into(userImage);

        userContainer.addView(userImage);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_settings_page, container, false);
        user = (User) getArguments().getSerializable("user");
        Log.e("SettingsFragment", "User received: " + user.getEmail());

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

        homeNameTitle = parentHolder.findViewById(R.id.settings_home_name);
        getCurrentHomeName(user);

        userContainer = parentHolder.findViewById(R.id.user_container);

        getCurrentHomeUsersPhotoUrl(user);

        accountPhoto = parentHolder.findViewById(R.id.account_photo_settings_page);
        if (user.getPhotoUrl() != null) {
            Glide.with(this).load(user.getPhotoUrl()).into(accountPhoto);
        } else {
            accountPhoto.setImageResource(R.drawable.ic_launcher_background);
        }

        accountPhoto.setOnClickListener(v -> signOutAndSignIn());

        listView = parentHolder.findViewById(R.id.settings_list_view);

        List<SettingsFragmentListItem> items = new ArrayList<>();
        items.add(new SettingsFragmentListItem("Room", R.drawable.meeting_room_list_view));
        items.add(new SettingsFragmentListItem("Device", R.drawable.add_circle_list_view));
        items.add(new SettingsFragmentListItem("Home member", R.drawable.person_add_list_view));
        items.add(new SettingsFragmentListItem("Home", R.drawable.home_list_view));

        ListAdapter adapter = new SettingsFragmentListAdapter(requireContext(), items);
        listView.setAdapter(adapter);

        addNewAccountButton = parentHolder.findViewById(R.id.add_button);

        return parentHolder;
    }

    @Override
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
            if (referenceActivity instanceof MainActivity) {
                ((MainActivity) referenceActivity).setSignInFromSettings(true);
            }
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
                    Log.d("SettingsFragment", account.getEmail() + " Signed in successfully");
                    firebaseAuthWithGoogle(account);
                    Log.d("SettingsFragment", account.getEmail() + " Signed in successfully");
                } else {
                    Log.w("SettingsFragment", "Account is null");
                }
            } catch (ApiException e) {
                Log.w("SettingsFragment", "signInResult:failed code=" + e.getStatusCode());
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
                        Log.w("SettingsFragment", "signInWithCredential:failure", task.getException());
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
                Log.e("SettingsFragment", "retrieveUserData onCancelled: " + error.getMessage());
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
                Log.e("SettingsFragment", "fetchHomesForUser onCancelled: " + error.getMessage());
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
                    Intent intent = new Intent(referenceActivity, SettingsFragment.class);
                    intent.putExtra("user", user);
                    startActivity(intent);
                    referenceActivity.finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SettingsFragment", "fetchHomeDetails onCancelled: " + error.getMessage());
            }
        });
    }

    private void storeNewUserData(FirebaseUser firebaseUser, User user) {
        databaseReference.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("SettingsFragment", "User data saved successfully.");
                        updateUI(user);
                    } else {
                        Log.w("SettingsFragment", "Failed to save user data.", task.getException());
                    }
                });
    }

    private void updateUI(User user) {
        if (user != null) {
            if (!user.getPhotoUrl().isEmpty()) {
                Glide.with(referenceActivity).load(user.getPhotoUrl()).into(accountPhoto);
            } else {
                accountPhoto.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
}
