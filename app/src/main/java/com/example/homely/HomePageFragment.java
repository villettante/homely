package com.example.homely;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class HomePageFragment extends Fragment implements HomePageBottomSheetDialogFragment.OnItemSelectedListener {
    public HomePageFragment() {}

    Activity referenceActivity;
    View parentHolder;
    LinearLayout firstRoomsColumn, secondRoomsColumn;
    ImageView backgroundImage;
    ShapeableImageView accountPhoto;
    TextView roomsTitle;
    TextView sensorDataText;
    TextView title;
    LinearLayout topLinearLayout;

    SensorData sensorData;
    FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private static final String ARG_USER = "user";
    private User user;
    private FirebaseAuth.AuthStateListener authStateListener;
    private boolean isListVisible = false;

    private final LinkedHashMap<String, ArrayList<String>> roomAppliances = new LinkedHashMap<>();
    DatabaseReference databaseReference;

    public static HomePageFragment newInstance(User user) {
        HomePageFragment fragment = new HomePageFragment();
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
        parentHolder = inflater.inflate(R.layout.fragment_home_page, container, false);

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

        if (getArguments() != null) {
            String uid = getArguments().getString("uid");
            String displayName = getArguments().getString("displayName");
            String email = getArguments().getString("email");
            String photoUrl = getArguments().getString("photoUrl");
            user = new User(uid, displayName, email, photoUrl);
            Log.e("HomePageFragment", "User received: " + user.getEmail());
        }

        accountPhoto = parentHolder.findViewById(R.id.accountPhoto);
        title = parentHolder.findViewById(R.id.title_home_page);
        topLinearLayout = parentHolder.findViewById(R.id.top_group_home_page);

        if (user != null) {
            Glide.with(this).load(user.getPhotoUrl()).into(accountPhoto);
        } else {
            Log.e("HomePageFragment", "currentUser is null in onCreateView");
        }

        topLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomePageBottomSheetDialogFragment bottomSheet = new HomePageBottomSheetDialogFragment();
                bottomSheet.setOnItemSelectedListener(HomePageFragment.this);
                bottomSheet.show(getChildFragmentManager(), "HomePageBottomSheetDialogFragment");
            }
        });

        accountPhoto.setOnClickListener(v -> signOutAndSignIn());

        //backgroundImage = referenceActivity.findViewById(R.id.background_image);
        //backgroundImage.setImageResource(R.drawable.background_image_2);
        //backgroundImage.setClipToOutline(true);
        //backgroundImage.setAdjustViewBounds(true);
        //backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        //int greyTint = Color.argb(30, 100, 76, 76);
        //backgroundImage.setColorFilter(greyTint, PorterDuff.Mode.SRC_ATOP);

        ArrayList<String> livingRoomAppliances = new ArrayList<>();
        livingRoomAppliances.add("Bulb");
        livingRoomAppliances.add("Humidity");
        livingRoomAppliances.add("Temperature");
        roomAppliances.put("Living room", livingRoomAppliances);
        roomAppliances.put("Kitchen", new ArrayList<>());
        roomAppliances.put("Hallway", new ArrayList<>());
        roomAppliances.put("Bathroom", new ArrayList<>());
        roomAppliances.put("Office", new ArrayList<>());
        ArrayList<String> bedroomAppliances = new ArrayList<>();
        bedroomAppliances.add("Humidity");
        bedroomAppliances.add("Temperature");
        bedroomAppliances.add("Air Quality");
        roomAppliances.put("Bedroom", bedroomAppliances);
        Objects.requireNonNull(roomAppliances.get("Kitchen")).add("Bulb");

        //sensorDataText = parentHolder.findViewById(R.id.sensor_data);
        sensorData = new SensorData();

        //View customButtonView = inflater.inflate(R.layout.custom_button, container, false);
        //TextView buttonTitle = customButtonView.findViewById(R.id.buttonTitle);
        //ImageView buttonArrow = customButtonView.findViewById(R.id.buttonArrow);

        //Removed the dropdown home selection mode
        //FrameLayout buttonPlaceholder = parentHolder.findViewById(R.id.buttonPlaceholder);
        //buttonPlaceholder.addView(customButtonView);

        //buttonTitle.setText(addresses[0]);

        final ListPopupWindow listPopupWindow = new ListPopupWindow(referenceActivity);

        listPopupWindow.setAdapter(new ArrayAdapter<>(referenceActivity, android.R.layout.simple_dropdown_item_1line, addresses));
        //listPopupWindow.setAnchorView(customButtonView);

        listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);

        firstRoomsColumn = parentHolder.findViewById(R.id.firstLinearLayout);
        secondRoomsColumn = parentHolder.findViewById(R.id.secondLinearLayout);

        //Removed the rooms TextView
        //roomsTitle = parentHolder.findViewById(R.id.rooms_title);
        //roomsTitle.setTextSize(18);

        int index = 0;
        for (Map.Entry<String, ArrayList<String>> room : roomAppliances.entrySet()) {

            View roomsColumnLayout = LayoutInflater.from(referenceActivity).inflate(R.layout.room_button_layout, firstRoomsColumn, false);
            ImageView roomButtonIcon = roomsColumnLayout.findViewById(R.id.buttonIcon);
            TextView roomButtonText = roomsColumnLayout.findViewById(R.id.buttonText);
            //Typeface typeface = getResources().getFont(R.font.roboto_black);
            //roomButtonText.setTypeface(typeface);
            roomButtonText.setText(room.getKey());

            switch (room.getKey()) {
                case "Living room":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.sofa));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_blue), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.getBackground().setAlpha(200);
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb( 139, 185, 209), R.drawable.button_rounded_rooms));
                    break;
                case "Kitchen":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.kitchen));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_green), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(158, 183, 64), R.drawable.button_rounded_rooms));
                    break;
                case "Hallway":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.door));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_yellow), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(254, 218, 41), R.drawable.button_rounded_rooms));
                    break;
                case "Bathroom":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity,R.drawable.sink));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_orange), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(224, 167, 40), R.drawable.button_rounded_rooms));
                    break;
                case "Office":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.desk));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_pink), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(254, 182, 184), R.drawable.button_rounded_rooms));
                    break;
                case "Bedroom":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.bed));
                    //roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.room_shade_purple), R.drawable.button_rounded_rooms));
                    //roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(218, 91, 97), R.drawable.button_rounded_rooms));
                    break;
            }

            GridLayout applianceIconGridLayout = roomsColumnLayout.findViewById(R.id.applianceIconGridLayout);
            LayoutInflater layoutInflater = LayoutInflater.from(referenceActivity);

            for (String appliance : room.getValue()) {
                roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.container), R.drawable.button_rounded_rooms_no_stroke));
                View applianceIconLayout = layoutInflater.inflate(R.layout.room_button_appliance_icon_layout, applianceIconGridLayout, false);
                ImageView applianceIcon = applianceIconLayout.findViewById(R.id.applianceIcon);
                if (getDrawableIdForAppliance(appliance) != -1) {
                    applianceIconLayout.setAlpha(0.5f);
                    if (room.getKey().equals("Living room") && appliance.equals("Bulb")) {
                        applianceIconLayout.setAlpha(1f);
                    }
                    applianceIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, getDrawableIdForAppliance(appliance)));
                    applianceIcon.setBackground(ContextCompat.getDrawable(referenceActivity, R.drawable.button_rounded_appliance_icon));
                    GridLayout.LayoutParams applianceParams = new GridLayout.LayoutParams();
                    applianceParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
                    applianceParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    applianceParams.setMargins(0, 0, 0, 20);
                    applianceIconGridLayout.addView(applianceIconLayout, applianceParams);

                    View child = applianceIconGridLayout.getChildAt(room.getValue().indexOf(appliance));
                    child.setOnLongClickListener(v -> {
                        SpannableString styledTooltipText = new SpannableString(appliance);
                        styledTooltipText.setSpan(new RelativeSizeSpan(0.8f), 0, styledTooltipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        TooltipCompat.setTooltipText(v, styledTooltipText);
                        return false;
                    });

                    databaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String roomName = room.getKey();
                            if (snapshot.hasChild(roomName)) {
                                DataSnapshot roomSnapshot = snapshot.child(roomName);
                                for (DataSnapshot timeSnapshot : roomSnapshot.getChildren()) {
                                    if (timeSnapshot.hasChild(appliance)) {
                                        applianceIconLayout.setAlpha(1);
                                        break;
                                    }
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

                    child.setOnClickListener(v -> {
                        Fragment fragment = new AppliancePageFragment(); //TODO to be replaced to the required custom fragment class
                        Bundle args = new Bundle();
                        args.putString("appliance_title", appliance);
                        args.putParcelable("sensor_data", sensorData);
                        fragment.setArguments(args);
                        replaceFragment(fragment, R.id.flFragment);
                    });
                }
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(15, 15, 15, 15);

            if (index % 2 == 0) {
                firstRoomsColumn.addView(roomsColumnLayout, params);
            } else {
                secondRoomsColumn.addView(roomsColumnLayout, params);
            }
            index++;
        }

        /*
        customButtonView.setOnClickListener(v -> {
            if (isListVisible) {
                buttonArrow.setImageResource(R.drawable.chevron_down);
                listPopupWindow.dismiss();
            } else {
                buttonArrow.setImageResource(R.drawable.chevron_up);
                listPopupWindow.show();
            }
            isListVisible = !isListVisible;
        });

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLanguage = addresses[position];
            buttonTitle.setText(selectedLanguage);
            listPopupWindow.dismiss();
            buttonArrow.setImageResource(R.drawable.chevron_down);
            isListVisible = false;
        });

        buttonTitle.setOnClickListener(v -> {
            if (isListVisible) {
                buttonArrow.setImageResource(R.drawable.chevron_down);
                listPopupWindow.dismiss();
            } else {
                buttonArrow.setImageResource(R.drawable.chevron_up);
                listPopupWindow.show();
            }
            isListVisible = !isListVisible;
        });
        */

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

    @Override
    public void onResume() {
        super.onResume();
        checkIfUserIsLoggedIn();
    }

    @Override
    public void onItemSelected(String text) {
        title.setText(text);
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

    private void signOutAndSignIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(referenceActivity, task -> {
            firebaseAuth.signOut();
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
                    Log.d("HomePageFragment", account.getEmail() + " Signed in successfully");
                    firebaseAuthWithGoogle(account);
                    Log.d("HomePageFragment", account.getEmail() + " Signed in successfully");
                } else {
                    Log.w("HomePageFragment", "Account is null");
                }
            } catch (ApiException e) {
                Log.w("HomePageFragment", "signInResult:failed code=" + e.getStatusCode());
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
                        Log.w("HomePageFragment", "signInWithCredential:failure", task.getException());
                        Toast.makeText(referenceActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        DatabaseReference userRef = databaseReference.child("users").child(firebaseUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user;
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
                    Log.e("HomePageFragment", "Could not update the new preferences");
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
                        Log.d("HomePageFragment", "User data saved successfully.");
                    } else {
                        Log.w("HomePageFragment", "Failed to save user data.", task.getException());
                    }
                });
    }

    private Drawable changeDrawableColor(int color, int resource) {
        Drawable drawable = Objects.requireNonNull(ContextCompat.getDrawable(referenceActivity, resource)).mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawable;
        gradientDrawable.setColor(color);

        return gradientDrawable;
    }

    private int getDrawableIdForAppliance(String applianceName) {
        switch (applianceName) {
            case "Bulb":
                return R.drawable.bulb;
            case "Humidity":
                return R.drawable.humidity;
            case "Temperature":
                return R.drawable.temperature_three_quarter;
            case "Air Quality":
                return R.drawable.air_filter;
            default:
                return -1;
        }
    }

    private void replaceFragment(Fragment fragment, int resource) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(resource, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private static final String[] addresses = new String[]{"Sommersby Street Home", "Spring Street Home", "Goethe Webber Apartment Complex"};

}