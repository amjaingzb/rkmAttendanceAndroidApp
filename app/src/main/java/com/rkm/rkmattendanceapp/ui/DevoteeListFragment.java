// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

// 1. Add the interface to the class declaration
public class DevoteeListFragment extends Fragment implements DevoteeListAdapter.OnDevoteeClickListener {

    private DevoteeListViewModel devoteeListViewModel;
    private DevoteeListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devotee_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        devoteeListViewModel = new ViewModelProvider(this).get(DevoteeListViewModel.class);

        setupRecyclerView(view);
        observeViewModel();

        FloatingActionButton fab = view.findViewById(R.id.fab_add_devotee);
        fab.setOnClickListener(v -> {
            // Launch the activity to add a new devotee
            Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
            startActivity(intent);
        });

        // Trigger the initial data load
        devoteeListViewModel.loadAllDevotees();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_devotees);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new DevoteeListAdapter();
        // 2. Set this fragment as the listener for item clicks
        adapter.setOnDevoteeClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        devoteeListViewModel.getDevoteeList().observe(getViewLifecycleOwner(), devotees -> {
            adapter.setDevotees(devotees);
        });

        devoteeListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // 3. Implement the required method from the interface
    @Override
    public void onDevoteeClick(Devotee devotee) {
        // This is called when a user taps an item in the list.
        // Launch the AddEditDevoteeActivity in "edit mode".
        Intent intent = new Intent(getActivity(), AddEditDevoteeActivity.class);
        // Pass the ID of the devotee to be edited
        intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
        startActivity(intent);
    }
}