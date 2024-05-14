package com.example.homely;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HomePageFragment homePageFragment = new HomePageFragment();

        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, homePageFragment).commit();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_user);
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_automation);
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_devices);
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bottom_nav_home) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, homePageFragment).commit();
                return true;
            }
            return false;
        });
    }
}