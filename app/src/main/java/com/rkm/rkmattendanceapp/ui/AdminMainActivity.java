// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // NEW: Import
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rkm.rkmattendanceapp.R;

import java.util.HashSet;
import java.util.Set;

public class AdminMainActivity extends AppCompatActivity {

    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";

    // REMOVED: The fragile instance variables for state are gone.
    // private Privilege currentPrivilege;

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private AdminViewModel adminViewModel; // NEW: Hold a reference to the Shared ViewModel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // NEW: Get the Activity-scoped ViewModel.
        // This same instance will be available to all child fragments.
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // NEW: Initialize the ViewModel's state ONLY on the first creation.
        // If the activity is recreated, the ViewModel already has the correct state.
        if (savedInstanceState == null) {
            Privilege initialPrivilege = (Privilege) getIntent().getSerializableExtra(EXTRA_PRIVILEGE);
            if (initialPrivilege == null) {
                initialPrivilege = Privilege.EVENT_COORDINATOR; // Failsafe
            }
            adminViewModel.setPrivilege(initialPrivilege);
        }

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);
        
        // NEW: Observe the LiveData from the ViewModel to drive the UI.
        adminViewModel.currentPrivilege.observe(this, privilege -> {
            if (privilege == null) return;

            // Update the subtitle whenever the privilege changes (it won't, but this is good practice)
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(getRoleSubtitle(privilege));
            }

            // Update the visibility of the bottom navigation items
            navView.getMenu().findItem(R.id.nav_devotees).setVisible(privilege == Privilege.SUPER_ADMIN);
        });

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_events);
        topLevelDestinations.add(R.id.nav_devotees);
        topLevelDestinations.add(R.id.nav_reports);
        
        // This logic is now handled by the observer above.
        // if (currentPrivilege == Privilege.EVENT_COORDINATOR) { ... }

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    // REMOVED: onSaveInstanceState is no longer needed. The ViewModel handles this automatically.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_switch_role) {
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
    
    private String getRoleSubtitle(Privilege privilege) {
        switch (privilege) {
            case SUPER_ADMIN:
                return "Super Admin Mode";
            case EVENT_COORDINATOR:
                return "Coordinator Mode";
            default:
                return "Admin Panel";
        }
    }

    // REMOVED: The public getter is no longer needed. Fragments will get the ViewModel directly.
}