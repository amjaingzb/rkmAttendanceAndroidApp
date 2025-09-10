// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
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
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList; // Also add this for the transformation

public class DevoteeListFragment extends Fragment implements DevoteeListAdapter.OnDevoteeClickListener {

    private DevoteeListViewModel devoteeListViewModel;
    private DevoteeListAdapter adapter;
    private EditText searchEditText;

    // 1. Declare the ActivityResultLauncher
    private ActivityResultLauncher<Intent> addEditDevoteeLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 2. Register the launcher. This is where we define what happens when the activity returns.
        addEditDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if the result code is OK (meaning a save was successful)
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // A devotee was added or edited. Clear the search and reload the list.
                        if (searchEditText != null) {
                            searchEditText.setText("");
                        }
                        devoteeListViewModel.loadAllDevotees();
                    }
                    // If the result code is anything else (e.g., CANCELED), we do nothing,
                    // preserving the search term and the filtered list.
                });
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
            // 3. Use the launcher to start the activity
            Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
            String prefillQuery = searchEditText.getText().toString().trim();
            if (!prefillQuery.isEmpty()) {
                intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery);
            }
            addEditDevoteeLauncher.launch(intent);
        });

        // Load the initial data
        devoteeListViewModel.loadAllDevotees();
    }

    // 4. Update the onDevoteeClick to also use the launcher
    @Override
    public void onDevoteeClick(Devotee devotee) {
        Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
        intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
        addEditDevoteeLauncher.launch(intent);
    }

    // No changes needed for the methods below
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
                // Transform the List<EnrichedDevotee> into a List<Devotee>
                // before passing it to the adapter.
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
                    Toast.makeText(getContext(), "Import Devotees clicked (not implemented)", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}