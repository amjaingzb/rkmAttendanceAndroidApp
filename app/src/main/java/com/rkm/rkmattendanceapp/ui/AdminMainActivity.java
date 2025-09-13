// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
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

    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";

    private Privilege currentPrivilege;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        currentPrivilege = (Privilege) getIntent().getSerializableExtra(EXTRA_PRIVILEGE);
        if (currentPrivilege == null) {
            currentPrivilege = Privilege.EVENT_COORDINATOR;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(getRoleSubtitle(currentPrivilege));
        }

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_events);
        topLevelDestinations.add(R.id.nav_devotees);
        topLevelDestinations.add(R.id.nav_reports);

        if (currentPrivilege == Privilege.EVENT_COORDINATOR) {
            navView.getMenu().findItem(R.id.nav_devotees).setVisible(false);
            topLevelDestinations.remove(R.id.nav_devotees);
        }

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_switch_role) {
            // MODIFIED: This now performs a clean logout by restarting the app's flow
            // via the LauncherActivity.
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
    // NEW: Add a public getter so child fragments can access the current privilege level.
    public Privilege getCurrentPrivilege() {
        return currentPrivilege;
    }
}