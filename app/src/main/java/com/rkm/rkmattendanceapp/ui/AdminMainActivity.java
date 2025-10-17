// In: src/main/java/com/.../ui/AdminMainActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.settings.SettingsActivity;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
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

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(getRoleSubtitle(privilege));
                // Apply the bandaid fix after the subtitle is set.
                applySubtitleFix();
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

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        
        NavigationUI.setupWithNavController(navView, navController);
    }
    
    // === START OF BANDAID FIX ===
    private void applySubtitleFix() {
        // This is a last-resort bandaid. It attempts to find the subtitle TextView
        // and force its font size to a small, safe value.
        try {
            Toolbar toolbar = findToolbar(getWindow().getDecorView());
            if (toolbar != null && getSupportActionBar() != null) {
                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    View child = toolbar.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        CharSequence subtitle = getSupportActionBar().getSubtitle();
                        if (subtitle != null && subtitle.equals(tv.getText())) {
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Hardcoded small size
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to apply subtitle bandaid fix.", e);
        }
    }

    private Toolbar findToolbar(View view) {
        if (view instanceof Toolbar) {
            return (Toolbar) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                Toolbar found = findToolbar(viewGroup.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    // === END OF BANDAID FIX ===

    // ANNOTATION: onPrepareOptionsMenu is used to show/hide items dynamically
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        Privilege currentPrivilege = adminViewModel.currentPrivilege.getValue();
        if (settingsItem != null && currentPrivilege != null) {
            settingsItem.setVisible(currentPrivilege == Privilege.SUPER_ADMIN);
        }
        return super.onPrepareOptionsMenu(menu);
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
//            long sevenDaysAgo = Instant.now().getEpochSecond() - (7 * 24 * 60 * 60);
            long sevenDaysAgo = Instant.now().getEpochSecond() - 2 * 60;
            boolean isStale = lastBackupTimestamp < sevenDaysAgo;

            // In the next phase, we'll get this from BackupStateManager
            boolean isDirty = false; // Placeholder for now

            boolean needsBackup = isStale || isDirty;

            if (needsBackup) {
                backupStatusItem.setIcon(R.drawable.ic_cloud_warning);
            } else {
                backupStatusItem.setIcon(R.drawable.ic_cloud_done);
            }

            // Remove theme tint to show the icon's true color
            android.graphics.drawable.Drawable icon = backupStatusItem.getIcon();
            if (icon != null) {
                icon.mutate();
                DrawableCompat.setTintList(icon, null);
                backupStatusItem.setIcon(icon);
            }

        } else {
            backupItem.setVisible(false);
            backupStatusItem.setVisible(false);
        }
        return true;
    }

    private String getBackupStatusMessage(boolean needsBackup, boolean isStale, boolean isDirty) {
        if (!needsBackup) {
            return "Backup is recent and the database is clean.";
        } else {
            if (isStale && isDirty) {
                return "Backup is recommended: Data has changed and the last backup is over 1 month old.";
            } else if (isDirty) {
                return "Backup is recommended: Data has been changed since the last backup.";
            } else { // isStale must be true
                return "Backup is recommended: The last backup was more than 1 month old.";
            }
        }
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
        } else if (itemId == R.id.action_settings) { // ANNOTATION: Handle click on new item
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_backup_status) {
            long lastBackupTimestamp = BackupRestoreActivity.getLastBackupTimestamp(this);
//            long sevenDaysAgo = Instant.now().getEpochSecond() - (7 * 24 * 60 * 60);
            long sevenDaysAgo = Instant.now().getEpochSecond() - 2 * 60;
            boolean isStale = lastBackupTimestamp < sevenDaysAgo;
            boolean isDirty = false; // Placeholder, will be replaced by BackupStateManager
            boolean needsBackup = isStale || isDirty;

            String message = getBackupStatusMessage(needsBackup, isStale, isDirty);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
