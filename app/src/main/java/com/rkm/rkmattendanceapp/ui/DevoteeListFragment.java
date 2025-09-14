// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.opencsv.CSVReader;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DevoteeListFragment extends Fragment implements DevoteeListAdapter.OnDevoteeClickListener {

    private DevoteeListViewModel devoteeListViewModel;
    private DevoteeListAdapter adapter;
    private EditText searchEditText;

    private ActivityResultLauncher<Intent> addEditDevoteeLauncher;
    // NEW: Launcher for the file picker
    private ActivityResultLauncher<String> filePickerLauncher;
    // NEW: Launcher for the mapping activity
    private ActivityResultLauncher<Intent> mappingActivityLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This launcher handles the result from the Add/Edit screen
        addEditDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (searchEditText != null) {
                            searchEditText.setText("");
                        }
                        devoteeListViewModel.loadAllDevotees();
                    }
                });

        // NEW: This launcher handles the result from the file picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onFileSelected
        );

        // NEW: This launcher handles the result from the MappingActivity
        mappingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // The import was successful, reload the list to show new data
                        devoteeListViewModel.loadAllDevotees();
                    }
                }
        );
    }

    // NEW: This method is called when the user selects a file
    private void onFileSelected(Uri uri) {
        if (uri == null) {
            Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             InputStreamReader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headers = csvReader.readNext(); // Read just the header row
            if (headers == null || headers.length == 0) {
                Toast.makeText(getContext(), "Error: Could not read CSV headers or file is empty.", Toast.LENGTH_LONG).show();
                return;
            }

            // Launch the MappingActivity
            Intent intent = new Intent(getActivity(), MappingActivity.class);
            intent.putExtra(MappingActivity.EXTRA_FILE_URI, uri);
            intent.putStringArrayListExtra(MappingActivity.EXTRA_CSV_HEADERS, new ArrayList<>(Arrays.asList(headers)));
            mappingActivityLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devotee_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();
        devoteeListViewModel = new ViewModelProvider(this).get(DevoteeListViewModel.class);
        searchEditText = view.findViewById(R.id.edit_text_search);
        setupRecyclerView(view);
        observeViewModel();
        setupSearch();

        FloatingActionButton fab = view.findViewById(R.id.fab_add_devotee);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
            String prefillQuery = searchEditText.getText().toString().trim();
            if (!prefillQuery.isEmpty()) {
                intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery);
            }
            addEditDevoteeLauncher.launch(intent);
        });
        devoteeListViewModel.loadAllDevotees();
    }

    @Override
    public void onDevoteeClick(Devotee devotee) {
        Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
        intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
        addEditDevoteeLauncher.launch(intent);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_devotees);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new DevoteeListAdapter();
        adapter.setOnDevoteeClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        devoteeListViewModel.getDevoteeList().observe(getViewLifecycleOwner(), enrichedDevotees -> {
            if (enrichedDevotees != null) {
                List<Devotee> simpleDevotees = enrichedDevotees.stream()
                        .map(DevoteeDao.EnrichedDevotee::devotee)
                        .collect(Collectors.toList());
                adapter.setDevotees(simpleDevotees);
            }
        });
        devoteeListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                devoteeListViewModel.filterDevotees(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.devotee_list_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_import_devotees) {
                    // Launch the file picker
                    filePickerLauncher.launch("*/*");
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}
