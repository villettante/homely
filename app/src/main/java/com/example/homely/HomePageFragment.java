package com.example.homely;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.fonts.FontStyle;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class HomePageFragment extends Fragment {
    public HomePageFragment() {}

    Activity referenceActivity;
    View parentHolder;
    LinearLayout firstRoomsColumn, secondRoomsColumn;
    TextView roomsTitle;
    private boolean isListVisible = false;

    private LinkedHashMap<String, ArrayList<String>> roomAppliances = new LinkedHashMap<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_home_page, container, false);

        View customButtonView = inflater.inflate(R.layout.custom_button, null);
        TextView buttonTitle = customButtonView.findViewById(R.id.buttonTitle);
        ImageView buttonArrow = customButtonView.findViewById(R.id.buttonArrow);

        ArrayList<String> livingRoomAppliances = new ArrayList<>();
        livingRoomAppliances.add("Bulb");
        livingRoomAppliances.add("Humidity");
        livingRoomAppliances.add("Temperature");
        roomAppliances.put("Living Room", livingRoomAppliances);
        roomAppliances.put("Kitchen", new ArrayList<>());
        roomAppliances.put("Hallway", new ArrayList<>());
        roomAppliances.put("Bathroom", new ArrayList<>());
        roomAppliances.put("Office", new ArrayList<>());
        ArrayList<String> bedroomAppliances = new ArrayList<>();
        bedroomAppliances.add("Humidity");
        bedroomAppliances.add("Temperature");
        bedroomAppliances.add("Air Quality");
        roomAppliances.put("Bedroom", bedroomAppliances);

        roomAppliances.get("Kitchen").add("Bulb");
        
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

            System.out.println(room.getKey());

            View roomsColumnLayout = LayoutInflater.from(referenceActivity).inflate(R.layout.room_button_layout, firstRoomsColumn, false);
            ImageView roomButtonIcon = roomsColumnLayout.findViewById(R.id.buttonIcon);
            TextView roomButtonText = roomsColumnLayout.findViewById(R.id.buttonText);
            Typeface typeface = getResources().getFont(R.font.roboto_black);
            roomButtonText.setTypeface(typeface);
            roomButtonText.setText(room.getKey());

            switch (room.getKey()) {
                case "Living Room":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.sofa));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb( 139, 185, 209), R.drawable.button_rounded_rooms));
                    break;
                case "Kitchen":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.kitchen));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(158, 183, 64), R.drawable.button_rounded_rooms));
                    break;
                case "Hallway":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.door));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(254, 218, 41), R.drawable.button_rounded_rooms));
                    break;
                case "Bathroom":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity,R.drawable.sink));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(224, 167, 40), R.drawable.button_rounded_rooms));
                    break;
                case "Office":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.desk));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(254, 182, 184), R.drawable.button_rounded_rooms));
                    break;
                case "Bedroom":
                    roomButtonIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, R.drawable.bed));
                    roomsColumnLayout.setBackground(changeDrawableColor(Color.rgb(218, 91, 97), R.drawable.button_rounded_rooms));
                    break;
            }

            GridLayout applianceIconGridLayout = roomsColumnLayout.findViewById(R.id.applianceIconGridLayout);
            LayoutInflater layoutInflater = LayoutInflater.from(referenceActivity);

            for (String appliance : room.getValue()) {
                View applianceIconLayout = layoutInflater.inflate(R.layout.room_button_appliance_icon_layout, applianceIconGridLayout, false);
                ImageView applianceIcon = applianceIconLayout.findViewById(R.id.applianceIcon);
                if (getDrawableIdForAppliance(appliance) != -1) {
                    applianceIcon.setImageDrawable(ContextCompat.getDrawable(referenceActivity, getDrawableIdForAppliance(appliance)));
                    applianceIcon.setBackground(changeDrawableColor(Color.WHITE, R.drawable.button_rounded_appliance_icon));
                    GridLayout.LayoutParams applianceParams = new GridLayout.LayoutParams();
                    applianceParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
                    applianceParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    applianceParams.setMargins(0, 0, 0, 20);
                    applianceIconGridLayout.addView(applianceIconLayout, applianceParams);

                    View child = applianceIconGridLayout.getChildAt(room.getValue().indexOf(appliance));
                    child.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            SpannableString styledTooltipText = new SpannableString(appliance);
                            styledTooltipText.setSpan(new RelativeSizeSpan(0.8f), 0, styledTooltipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            TooltipCompat.setTooltipText(v, (CharSequence) styledTooltipText);
                            return false;
                        }
                    });
                }
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(20, 20, 20, 20);

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
        Drawable drawable = ContextCompat.getDrawable(referenceActivity, resource).mutate();
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

    private static final String[] addresses = new String[]{"Sommersby Street Home", "Spring Street Home", "Goethe Webber Apartment Complex"};
    private static final String[] rooms = new String[]{"Living Room", "Kitchen", "Hallway", "Bathroom", "Office", "Bedroom"};
    private static final String[] appliances = new String[]{"Bulb", "Sensors", "Appliances", "Security"};
}