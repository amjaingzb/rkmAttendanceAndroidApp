// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EventListFragment extends Fragment implements EventListAdapter.OnEventListener {

    private EventListViewModel eventListViewModel;
    private EventListAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up the Fragment Result Listener to get data back from the bottom sheet
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
        fab.setOnClickListener(v -> showAddEventDialog());

        eventListViewModel.loadEvents();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new EventListAdapter();
        // Set this fragment as the listener for item clicks
        adapter.setOnEventListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        eventListViewModel.getEventList().observe(getViewLifecycleOwner(), events -> {
            adapter.setEvents(events);
        });

        eventListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddEventDialog() {
        // Inflate a custom layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        final EditText eventNameInput = dialogView.findViewById(R.id.edit_text_event_name);
        final EditText eventDateInput = dialogView.findViewById(R.id.edit_text_event_date);
        final EditText eventRemarkInput = dialogView.findViewById(R.id.edit_text_event_remark);

        // Pre-fill today's date
        eventDateInput.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        new AlertDialog.Builder(requireContext())
                .setTitle("Create New Event")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = eventNameInput.getText().toString().trim();
                    String date = eventDateInput.getText().toString().trim();
                    String remark = eventRemarkInput.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(date)) {
                        Toast.makeText(getContext(), "Event Name and Date are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    eventListViewModel.createEvent(name, date, remark);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEventClick(Event event) {
        // When an event is clicked, show the bottom sheet with actions
        EventActionsBottomSheetFragment bottomSheet = EventActionsBottomSheetFragment.newInstance(event.getEventId());
        bottomSheet.show(getParentFragmentManager(), EventActionsBottomSheetFragment.TAG);
    }

    private void handleEventAction(String action, long eventId) {
        if (action == null) return;

        switch (action) {
            case "DELETE":
                // Show a confirmation dialog before deleting
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
                // TODO: Implement the Edit Event dialog/screen
                Toast.makeText(getContext(), "Edit feature not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
            case "SET_ACTIVE":
                eventListViewModel.setActiveEvent(eventId);
                break;
        }
    }
}