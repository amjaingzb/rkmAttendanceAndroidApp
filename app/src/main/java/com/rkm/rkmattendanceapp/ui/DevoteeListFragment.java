// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListFragment.java
package com.rkm.rkmattendanceapp.ui;

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
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

public class DevoteeListFragment extends Fragment implements DevoteeListAdapter.OnDevoteeClickListener {

    private DevoteeListViewModel devoteeListViewModel;
    private DevoteeListAdapter adapter;
    private EditText searchEditText;

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
            startActivity(intent);
        });

        devoteeListViewModel.loadAllDevotees();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This will be called when returning from the AddEditDevoteeActivity,
        // ensuring the list is always fresh.
        devoteeListViewModel.loadAllDevotees();
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
        devoteeListViewModel.getDevoteeList().observe(getViewLifecycleOwner(), devotees -> {
            if (devotees != null) {
                adapter.setDevotees(devotees);
            }
        });

        devoteeListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDevoteeClick(Devotee devotee) {
        Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
        intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
        startActivity(intent);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Using a Handler to debounce the search input
                // This is optional but good practice to prevent filtering on every single keystroke
                // For now, we'll keep it simple and filter directly.
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