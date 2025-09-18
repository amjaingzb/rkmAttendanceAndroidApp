// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
    private AdminViewModel adminViewModel;
    private DevoteeListAdapter adapter;
    private EditText searchEditText;

    private ActivityResultLauncher<Intent> addEditDevoteeLauncher;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<Intent> mappingActivityLauncher;
    
    // Use the new, public ImportType enum
    private ImportType currentImportType = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        addEditDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (searchEditText != null) searchEditText.setText("");
                        devoteeListViewModel.loadAllDevotees();
                    }
                });

        mappingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        devoteeListViewModel.loadAllDevotees();
                    }
                });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onFileSelected
        );
    }
    
    private void onFileSelected(Uri uri) {
        if (uri == null) {
            Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
            currentImportType = null;
            return;
        }

        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                throw new Exception("Could not read CSV headers or file is empty.");
            }

            Intent intent = new Intent(getActivity(), MappingActivity.class);
            intent.putExtra(MappingActivity.EXTRA_FILE_URI, uri);
            intent.putStringArrayListExtra(MappingActivity.EXTRA_CSV_HEADERS, new ArrayList<>(Arrays.asList(headers)));
            intent.putExtra(MappingActivity.EXTRA_IMPORT_TYPE, currentImportType);
            
            mappingActivityLauncher.launch(intent);

        } catch (Exception e) {
            Log.e("DevoteeListFragment", "File selection error", e);
            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            currentImportType = null;
        }
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.devotee_list_menu, menu);
                MenuItem whatsappItem = menu.findItem(R.id.action_import_whatsapp);
                if (adminViewModel.currentPrivilege.getValue() == Privilege.SUPER_ADMIN) {
                    whatsappItem.setVisible(true);
                } else {
                    whatsappItem.setVisible(false);
                }
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_import_devotees) {
                    currentImportType = ImportType.DEVOTEE;
                    filePickerLauncher.launch("text/csv");
                    return true;
                } else if (itemId == R.id.action_import_whatsapp) {
                    currentImportType = ImportType.WHATSAPP;
                    filePickerLauncher.launch("text/csv");
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // --- No changes to the methods below ---
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) { return inflater.inflate(R.layout.fragment_devotee_list, container, false); }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { super.onViewCreated(view, savedInstanceState); setupMenu(); devoteeListViewModel = new ViewModelProvider(this).get(DevoteeListViewModel.class); searchEditText = view.findViewById(R.id.edit_text_search); setupRecyclerView(view); observeViewModel(); setupSearch(); FloatingActionButton fab = view.findViewById(R.id.fab_add_devotee); fab.setOnClickListener(v -> { Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class); String prefillQuery = searchEditText.getText().toString().trim(); if (!prefillQuery.isEmpty()) { intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery); } addEditDevoteeLauncher.launch(intent); }); devoteeListViewModel.loadAllDevotees(); }
    @Override
    public void onDevoteeClick(Devotee devotee) { Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class); intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId()); addEditDevoteeLauncher.launch(intent); }
    private void setupRecyclerView(View view) { RecyclerView recyclerView = view.findViewById(R.id.recycler_view_devotees); recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); recyclerView.setHasFixedSize(true); adapter = new DevoteeListAdapter(); adapter.setOnDevoteeClickListener(this); recyclerView.setAdapter(adapter); }
    private void observeViewModel() { devoteeListViewModel.getDevoteeList().observe(getViewLifecycleOwner(), enrichedDevotees -> { if (enrichedDevotees != null) { List<Devotee> simpleDevotees = enrichedDevotees.stream().map(DevoteeDao.EnrichedDevotee::devotee).collect(Collectors.toList()); adapter.setDevotees(simpleDevotees); } }); devoteeListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> { if (message != null && !message.isEmpty()) { Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show(); } }); }
    private void setupSearch() { searchEditText.addTextChangedListener(new TextWatcher() { @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {} @Override public void onTextChanged(CharSequence s, int start, int before, int count) { devoteeListViewModel.filterDevotees(s.toString()); } @Override public void afterTextChanged(Editable s) {} }); }
}
