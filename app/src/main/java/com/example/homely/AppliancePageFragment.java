package com.example.homely;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AppliancePageFragment extends Fragment {
    Activity referenceActivity;
    View parentHolder;
    ImageView backgroundImage;

    TextView applianceTitle;

    Slider slider;

    SensorData sensorData;

    TextView sensorDataText;
    private RunPythonCode runPythonCodeTask;

    DatabaseReference database = FirebaseDatabase.getInstance().getReference("Living room");

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

        slider = parentHolder.findViewById(R.id.appliance_slider);
        slider.setVisibility(View.INVISIBLE);

        sensorDataText = parentHolder.findViewById(R.id.sensor_data);
        sensorData = new SensorData();

        Bundle args = getArguments();
        if (args != null) {
            String appliance = args.getString("appliance_title");
            //SensorData sensorData = args.getParcelable("sensor_data");
            applianceTitle.setText(appliance);
            if (appliance.equals("Bulb")) {
                slider.setVisibility(View.VISIBLE);
                slider.addOnChangeListener(new Slider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                        int intensity = (int) value;
                        if (fromUser) {
                            if (runPythonCodeTask != null) {
                                runPythonCodeTask.cancel(true);
                            }
                            runPythonCodeTask = new RunPythonCode();
                            runPythonCodeTask.execute("/home/pi/Documents/ba_thesis_project/TEST.py", Integer.toString(intensity));
                        }
                    }
                });
            }
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //StringBuilder sb = new StringBuilder();
                    String dateTime = null;
                    double humidity = 0, temperature = 0;

                    for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                        dateTime = dataSnapshot.getKey();
                        SensorData sensorData = dataSnapshot.getValue(SensorData.class);
                        humidity = sensorData.getHumidity();
                        temperature = sensorData.getTemperature();

                    }
                    sensorData.setHumidity(humidity);
                    sensorData.setTemperature(temperature);
                    if (appliance.equals("Temperature")) {
                        sensorDataText.setText(String.valueOf(sensorData.getTemperature()) + " C");
                    }
                    if (appliance.equals("Humidity")) {
                        sensorDataText.setText(String.valueOf(sensorData.getHumidity()) + " %");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            if (appliance.equals("Temperature")) {

            }
            //sensorDataText.setText(String.format("Humidity: %.2f\nTemperature: %.2f", sensorData.getHumidity(), sensorData.getTemperature()));
        }

        return parentHolder;
    }
}
