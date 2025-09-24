// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private AdminViewModel adminViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        if (savedInstanceState == null) {
            Privilege initialPrivilege = (Privilege) getIntent().getSerializableExtra(EXTRA_PRIVILEGE);
            if (initialPrivilege == null) {
                initialPrivilege = Privilege.EVENT_COORDINATOR; // Failsafe
            }
            adminViewModel.setPrivilege(initialPrivilege);
        }

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);
        
        adminViewModel.currentPrivilege.observe(this, privilege -> {
            if (privilege == null) return;

            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(getRoleSubtitle(privilege));
            }

            navView.getMenu().findItem(R.id.nav_devotees).setVisible(privilege == Privilege.SUPER_ADMIN);
        });

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_events);
        topLevelDestinations.add(R.id.nav_devotees);
        topLevelDestinations.add(R.id.nav_reports);
        
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_switch_role) {
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
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
}
