package com.example.homely;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class AppliancePageFragment extends Fragment {
    Activity referenceActivity;
    View parentHolder;
    ImageView backgroundImage;

    TextView applianceTitle;

    public AppliancePageFragment() {}
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_appliance_page, container, false);

        backgroundImage = referenceActivity.findViewById(R.id.background_image);
        backgroundImage.setImageResource(0);
        backgroundImage.setBackgroundColor(ContextCompat.getColor(referenceActivity, R.color.background));

        applianceTitle = parentHolder.findViewById(R.id.appliance_title);
        //sensorDataText = parentHolder.findViewById(R.id.fragment_appliance_page_sensor_data);
        applianceTitle.setTextSize(18);

        Bundle args = getArguments();
        if (args != null) {
            String appliance = args.getString("appliance_title");
            //SensorData sensorData = args.getParcelable("sensor_data");
            applianceTitle.setText(appliance);
            //sensorDataText.setText(String.format("Humidity: %.2f\nTemperature: %.2f", sensorData.getHumidity(), sensorData.getTemperature()));
        }

        return parentHolder;
    }
}
