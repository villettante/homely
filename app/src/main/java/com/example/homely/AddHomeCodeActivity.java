package com.example.homely;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class AddHomeCodeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText inputEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_home_code);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        inputEditText = findViewById(R.id.input_text_add_home_code);

        Button backArrow = findViewById(R.id.go_back_add_home_page);
        Button cancel = findViewById(R.id.cancel);
        Button finish = findViewById(R.id.finish);

        finish.setOnClickListener(v -> {
            String homeCode = Objects.requireNonNull(inputEditText.getText()).toString();
            addHome(homeCode);
        });

        cancel.setOnClickListener(v -> {
            navigateTo(MainActivity.class);
        });

        backArrow.setOnClickListener(v -> {
            navigateTo(AddHomeActivity.class);
        });
    }

    private void navigateTo(Class<?> goToClass) {
        Intent intent = new Intent(AddHomeCodeActivity.this, goToClass);
        startActivity(intent);
        finish();
    }

    private void addHome(String homeCode) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateTo(AuthActivity.class);
            return;
        }
        String userId = currentUser.getUid();

        mDatabase.child("homes").child(homeCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Home code exists
                    Log.d("AddHomeCodeActivity", homeCode + " exists in the database");
                    handleHomeCodeExistence(dataSnapshot, userId, Objects.requireNonNull(getIntent().getExtras()).getString("home_name"));
                } else {
                    // Home code does not exist
                    // Handle this case, e.g., show an error message
                    Toast.makeText(AddHomeCodeActivity.this, "The code is incorrect!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void handleHomeCodeExistence(DataSnapshot homeSnapshot, String userId, String homeName) {
        DatabaseReference homeRef = homeSnapshot.getRef();
        DatabaseReference usersRef = homeRef.child("users");

        homeRef.child("name").setValue(homeName);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                boolean ownerExists = false;

                for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                    String role = userSnapshot.getValue(String.class);
                    if ("owner".equals(role)) {
                        ownerExists = true;
                        break;
                    }
                }

                String userRole = ownerExists ? "manager" : "owner";
                usersRef.child(userId).setValue(userRole).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.e("AddHomeCodeActivity", userId + " was registered in the database with the role: " + userRole);
                        updateUserHomesList(userId, homeSnapshot.getKey());
                    } else {
                        Log.e("AddHomeCodeActivity", userId + " failed registering");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void updateUserHomesList(String userId, String homeId) {
        DatabaseReference userHomesRef = mDatabase.child("users").child(userId).child("homes").child(homeId);

        userHomesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // If the home exists for the user, check its value
                    Boolean homeStatus = dataSnapshot.getValue(Boolean.class);
                    if (homeStatus != null && !homeStatus) {
                        // If the home is marked as removed, set it back to true
                        userHomesRef.setValue(true);
                        Log.d("AddHomeCodeActivity","Activated back the home with id: " + homeId);
                    }
                } else {
                    // If the home does not exist for the user, add it
                    mDatabase.child("users").child(userId).child("homes").child(homeId).setValue(true);
                    Log.d("AddHomeCodeActivity","Added the home with id: " + homeId + " for user: " + userId);
                    navigateTo(MainActivity.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.flFragment, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
