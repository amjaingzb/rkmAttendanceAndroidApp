// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rkm.rkmattendanceapp.R;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class AdminMainActivity extends AppCompatActivity {

    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private AdminViewModel adminViewModel;
    private ActivityResultLauncher<Intent> backupRestoreLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        backupRestoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        invalidateOptionsMenu();
                    }
                }
        );

        if (savedInstanceState == null) {
            Privilege initialPrivilege = (Privilege) getIntent().getSerializableExtra(EXTRA_PRIVILEGE);
            if (initialPrivilege == null) {
                initialPrivilege = Privilege.EVENT_COORDINATOR;
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
            invalidateOptionsMenu();
        });

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_events);
        topLevelDestinations.add(R.id.nav_devotees);
        topLevelDestinations.add(R.id.nav_reports);
        
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // === START OF COMPILATION FIX ===
        // The extra 'this' argument has been removed.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        // === END OF COMPILATION FIX ===
        
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);

        MenuItem backupItem = menu.findItem(R.id.action_backup_restore);
        MenuItem backupStatusItem = menu.findItem(R.id.action_backup_status);
        Privilege currentPrivilege = adminViewModel.currentPrivilege.getValue();

        if (currentPrivilege == Privilege.SUPER_ADMIN) {
            backupItem.setVisible(true);
            backupStatusItem.setVisible(true);

            long lastBackupTimestamp = BackupRestoreActivity.getLastBackupTimestamp(this);
            long sevenDaysAgo = Instant.now().getEpochSecond() - (7 * 24 * 60 * 60);

            if (lastBackupTimestamp > sevenDaysAgo) {
                backupStatusItem.setIcon(R.drawable.ic_cloud_done);
            } else {
                backupStatusItem.setIcon(R.drawable.ic_cloud_warning);
            }
        } else {
            backupItem.setVisible(false);
            backupStatusItem.setVisible(false);
        }
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
        } else if (itemId == R.id.action_backup_restore) {
            Intent intent = new Intent(this, BackupRestoreActivity.class);
            backupRestoreLauncher.launch(intent);
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
