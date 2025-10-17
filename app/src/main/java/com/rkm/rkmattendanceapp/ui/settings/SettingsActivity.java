// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/settings/SettingsActivity.java
package com.rkm.rkmattendanceapp.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.model.ConfigItem;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private SettingsViewModel viewModel;
    private SettingsAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("App Settings");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progress_bar);
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        observeViewModel();
        
        viewModel.loadSettings();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SettingsAdapter();
        adapter.setOnSettingClickListener(this::showEditDialog);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getConfigItems().observe(this, items -> {
            progressBar.setVisibility(View.GONE);
            if (items != null) {
                adapter.setConfigItems(items);
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Setting saved.", Toast.LENGTH_SHORT).show();
                viewModel.loadSettings(); // Reload to show the new value
                viewModel.onSaveHandled();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            progressBar.setVisibility(View.GONE);
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditDialog(ConfigItem item) {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_setting, null);
        final TextInputLayout textInputLayout = dialogView.findViewById(R.id.text_input_layout);
        final EditText editText = dialogView.findViewById(R.id.edit_text_value);
        
        textInputLayout.setHint(item.displayName);

        if (item.isProtected) {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER );
        } else {
            editText.setText(item.value);
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Setting")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newValue = editText.getText().toString();
                    if (!newValue.isEmpty()) {
                        viewModel.saveSetting(item.key, newValue);
                    } else if (item.isProtected) {
                        // Do nothing if the PIN field is left blank
                        Toast.makeText(this, "PIN cannot be empty. No changes made.", Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.saveSetting(item.key, newValue);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
