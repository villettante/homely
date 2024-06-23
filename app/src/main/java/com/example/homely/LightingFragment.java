package com.example.homely;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
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

public class LightingFragment extends Fragment {
    Activity referenceActivity;
    View parentHolder;
    User user;
    private LinearLayout lightsContainer;
    private final Map<String, List<Device>> roomLightsMap = new HashMap<>();

    public static LightingFragment newInstance(User user) {
        LightingFragment fragment = new LightingFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        referenceActivity = getActivity();
        parentHolder = inflater.inflate(R.layout.fragment_lighting, container, false);
        if (getArguments() != null) {
            user = (User) getArguments().getSerializable("user");
        }
        
        lightsContainer = parentHolder.findViewById(R.id.lights_container);

        MaterialButtonToggleGroup toggleGroupAllLights = parentHolder.findViewById(R.id.toggle_group_all_lights);
        toggleGroupAllLights.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean turnOn = checkedId == R.id.button_all_lights_on;
                for (String roomId : roomLightsMap.keySet()) {
                    for (Device device : Objects.requireNonNull(roomLightsMap.get(roomId))) {
                        DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("devices").child(device.deviceId).child("settings");
                        if (device.intensity >= 0) {
                            deviceRef.child("intensity").setValue(turnOn ? 100 : 0);
                            device.intensity = turnOn ? 100 : 0;
                        } else {
                            deviceRef.child("status").setValue(turnOn ? "on" : "off");
                            device.isOn = turnOn;
                        }
                    }
                }
                updateLightingUI(roomLightsMap);
            }
        });

        for (Home home : user.getHomes()) {
            if (home.isCurrent()) {
                fetchLightsData();
            }
        }

        return parentHolder;
    }

    private void fetchLightsData() {
        DatabaseReference devicesRef = FirebaseDatabase.getInstance().getReference("devices");

        devicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomLightsMap.clear();
                for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                    String deviceId = deviceSnapshot.getKey();
                    String deviceType = deviceSnapshot.child("type").getValue(String.class);
                    String deviceName = deviceSnapshot.child("name").getValue(String.class);
                    Boolean intensityCapability = deviceSnapshot.child("capabilities").child("intensity").getValue(Boolean.class);
                    String roomId = deviceSnapshot.child("roomId").getValue(String.class);

                    if ("light".equals(deviceType) && roomId != null) {
                        boolean isOn;
                        int intensity = -1;

                        if (intensityCapability != null && intensityCapability) {
                            Integer intensityValue = deviceSnapshot.child("settings").child("intensity").getValue(Integer.class);
                            intensity = (intensityValue != null) ? intensityValue : 0;
                            isOn = intensity > 0;
                        } else {
                            String statusValue = deviceSnapshot.child("settings").child("status").getValue(String.class);
                            isOn = "on".equals(statusValue);
                        }

                        Device device = new Device(deviceId, deviceName, isOn, intensity);
                        if (!roomLightsMap.containsKey(roomId)) {
                            roomLightsMap.put(roomId, new ArrayList<>());
                        }
                        Objects.requireNonNull(roomLightsMap.get(roomId)).add(device);
                    }
                }
                updateLightingUI(roomLightsMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    static class Device {
        String deviceId;
        String name;
        boolean isOn;
        int intensity;

        Device(String deviceId, String name, boolean isOn, int intensity) {
            this.deviceId = deviceId;
            this.name = name;
            this.isOn = isOn;
            this.intensity = intensity;
        }
    }

    private void updateLightingUI(Map<String, List<Device>> roomLightsMap) {
        lightsContainer.removeAllViews();

        for (Map.Entry<String, List<Device>> entry : roomLightsMap.entrySet()) {
            String roomId = entry.getKey();
            List<Device> devices = entry.getValue();
            
            TextView roomHeader = new TextView(getContext());
            roomHeader.setText(roomId);
            roomHeader.setTextSize(18);
            roomHeader.setPadding(16, 16, 16, 16);
            lightsContainer.addView(roomHeader);

            for (Device device : devices) {
                LinearLayout deviceLayout = new LinearLayout(getContext());
                deviceLayout.setOrientation(LinearLayout.HORIZONTAL);
                deviceLayout.setPadding(16, 8, 16, 8);

                TextView deviceName = new TextView(getContext());
                deviceName.setText(device.name);
                deviceName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                deviceLayout.addView(deviceName);

                if (device.intensity >= 0) {
                    SeekBar deviceIntensity = new SeekBar(getContext());
                    deviceIntensity.setProgress(device.intensity);
                    deviceIntensity.setVisibility(View.VISIBLE);
                    deviceIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("devices").child(device.deviceId).child("settings");
                            deviceRef.child("intensity").setValue(progress);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {}
                    });

                    deviceLayout.addView(deviceIntensity);
                } else {
                    Switch deviceSwitch = new Switch(getContext());
                    deviceSwitch.setChecked(device.isOn);
                    deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("devices").child(device.deviceId).child("settings");
                        deviceRef.child("status").setValue(isChecked ? "on" : "off");
                    });

                    deviceLayout.addView(deviceSwitch);
                }

                lightsContainer.addView(deviceLayout);
            }
        }
    }
}
