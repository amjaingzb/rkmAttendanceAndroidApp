// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rkm.rkmattendanceapp.R;

import java.util.HashSet;
import java.util.Set;

public class AdminMainActivity extends AppCompatActivity {

    // NEW: Define the key for receiving the user's privilege level from another activity.
    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);

        // Define the top-level destinations. The Up button will not be shown for these.
        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_events);
        topLevelDestinations.add(R.id.nav_devotees);
        topLevelDestinations.add(R.id.nav_reports);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        // Find the NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Set up the ActionBar to work with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Set up the BottomNavigationView to work with the NavController
        NavigationUI.setupWithNavController(navView, navController);
    }
}