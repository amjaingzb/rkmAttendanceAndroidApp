// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.R;

public class EventListFragment extends Fragment implements EventListAdapter.OnEventListener {

    private EventListViewModel eventListViewModel;
    private AdminViewModel adminViewModel; // NEW: Reference to the Shared ViewModel
    private EventListAdapter adapter;
    private ActivityResultLauncher<Intent> addEditEventLauncher;
    // REMOVED: The fragile instance variable is gone.

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NEW: Get the ViewModel scoped to the parent Activity.
        // This ensures we get the same instance as the Activity.
        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        addEditEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        eventListViewModel.loadEvents();
                    }
                });

        getParentFragmentManager().setFragmentResultListener(EventActionsBottomSheetFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            String action = bundle.getString(EventActionsBottomSheetFragment.KEY_ACTION);
            long eventId = bundle.getLong(EventActionsBottomSheetFragment.KEY_EVENT_ID);
            handleEventAction(action, eventId);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventListViewModel = new ViewModelProvider(this).get(EventListViewModel.class);
        setupRecyclerView(view);
        observeViewModel();
        FloatingActionButton fab = view.findViewById(R.id.fab_add_event);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditEventActivity.class);
            // NEW: Get the privilege directly from the ViewModel's stable LiveData.
            intent.putExtra(AddEditEventActivity.EXTRA_PRIVILEGE, adminViewModel.currentPrivilege.getValue());
            addEditEventLauncher.launch(intent);
        });

        eventListViewModel.loadEvents();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new EventListAdapter();
        adapter.setOnEventListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        eventListViewModel.getEventList().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                adapter.setEvents(events);
            }
        });

        eventListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        // NEW: Get the privilege directly from the ViewModel's stable LiveData.
        Privilege currentPrivilege = adminViewModel.currentPrivilege.getValue();
        
        EventActionsBottomSheetFragment bottomSheet = EventActionsBottomSheetFragment.newInstance(
                event.getEventId(),
                event.getEventDate(),
                currentPrivilege
        );
        bottomSheet.show(getParentFragmentManager(), EventActionsBottomSheetFragment.TAG);
    }

    private void handleEventAction(String action, long eventId) {
        if (action == null) return;

        switch (action) {
            case "DELETE":
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Event")
                        .setMessage("Are you sure you want to delete this event and all its attendance records?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            eventListViewModel.deleteEvent(eventId);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;

            case "EDIT":
                Intent intent = new Intent(getActivity(), AddEditEventActivity.class);
                intent.putExtra(AddEditEventActivity.EXTRA_EVENT_ID, eventId);
                // NEW: Get the privilege directly from the ViewModel's stable LiveData.
                intent.putExtra(AddEditEventActivity.EXTRA_PRIVILEGE, adminViewModel.currentPrivilege.getValue());
                addEditEventLauncher.launch(intent);
                break;

            case "IMPORT_ATTENDANCE":
                Toast.makeText(getContext(), "Import Attendance (not implemented yet)", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}