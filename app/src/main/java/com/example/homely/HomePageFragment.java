package com.example.homely;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class HomePageFragment extends Fragment {
    public HomePageFragment() {}

    Activity referenceActivity;
    View parentHolder;
    LinearLayout firstRoomsColumn, secondRoomsColumn;
    ImageView backgroundImage;
    TextView roomsTitle;
    TextView sensorDataText;

    SensorData sensorData;

    private boolean isListVisible = false;

    private final LinkedHashMap<String, ArrayList<String>> roomAppliances = new LinkedHashMap<>();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_home_page, container, false);

        backgroundImage = referenceActivity.findViewById(R.id.background_image);
        backgroundImage.setImageResource(R.drawable.background_image_2);
        backgroundImage.setClipToOutline(true);
        //backgroundImage.setAdjustViewBounds(true);
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int greyTint = Color.argb(30, 100, 76, 76);
        backgroundImage.setColorFilter(greyTint, PorterDuff.Mode.SRC_ATOP);

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

        View customButtonView = inflater.inflate(R.layout.custom_button, container, false);
        TextView buttonTitle = customButtonView.findViewById(R.id.buttonTitle);
        ImageView buttonArrow = customButtonView.findViewById(R.id.buttonArrow);
        
        FrameLayout buttonPlaceholder = parentHolder.findViewById(R.id.buttonPlaceholder);
        buttonPlaceholder.addView(customButtonView);

        buttonTitle.setText(addresses[0]);

        final ListPopupWindow listPopupWindow = new ListPopupWindow(referenceActivity);

        listPopupWindow.setAdapter(new ArrayAdapter<>(referenceActivity, android.R.layout.simple_dropdown_item_1line, addresses));
        listPopupWindow.setAnchorView(customButtonView);

        listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);

        firstRoomsColumn = parentHolder.findViewById(R.id.firstLinearLayout);
        secondRoomsColumn = parentHolder.findViewById(R.id.secondLinearLayout);

        roomsTitle = parentHolder.findViewById(R.id.rooms_title);
        roomsTitle.setTextSize(18);

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
                roomsColumnLayout.setBackground(changeDrawableColor(ContextCompat.getColor(referenceActivity, R.color.container_highlight), R.drawable.button_rounded_rooms_no_stroke));
                View applianceIconLayout = layoutInflater.inflate(R.layout.room_button_appliance_icon_layout, applianceIconGridLayout, false);
                ImageView applianceIcon = applianceIconLayout.findViewById(R.id.applianceIcon);
                if (getDrawableIdForAppliance(appliance) != -1) {
                    applianceIconLayout.setAlpha(0.5f);
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

        return parentHolder;
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