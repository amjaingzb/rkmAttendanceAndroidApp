// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListFragment.java
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.opencsv.CSVReader;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EventListFragment extends Fragment implements EventListAdapter.OnEventListener {

    private static final String TAG = "EventListFragment";

    private EventListViewModel eventListViewModel;
    private AdminViewModel adminViewModel;
    private EventListAdapter adapter;

    private ActivityResultLauncher<Intent> addEditEventLauncher;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<Intent> mappingActivityLauncher;

    private Long eventIdForAttendanceImport = null;
    private Privilege privilegeForAttendanceImport = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        addEditEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        eventListViewModel.loadEvents();
                    }
                });

        mappingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        AppLogger.d(TAG, "Returned from MappingActivity with RESULT_OK. Reloading events.");
                        eventListViewModel.loadEvents();
                    }
                });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onFileSelected
        );

        getParentFragmentManager().setFragmentResultListener(EventActionsBottomSheetFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            String action = bundle.getString(EventActionsBottomSheetFragment.KEY_ACTION);
            long eventId = bundle.getLong(EventActionsBottomSheetFragment.KEY_EVENT_ID);
            handleEventAction(action, eventId);
        });
    }
    
    private void onFileSelected(Uri uri) {
        if (uri == null) {
            Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
            eventIdForAttendanceImport = null;
            privilegeForAttendanceImport = null;
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
            
            if (eventIdForAttendanceImport != null && privilegeForAttendanceImport != null) {
                AppLogger.d(TAG, "Launching MappingActivity for ATTENDANCE import. EventID: " + eventIdForAttendanceImport + ", Privilege: " + privilegeForAttendanceImport);
                intent.putExtra(MappingActivity.EXTRA_EVENT_ID, eventIdForAttendanceImport);
                intent.putExtra(MappingActivity.EXTRA_PRIVILEGE, privilegeForAttendanceImport);
                intent.putExtra(MappingActivity.EXTRA_IMPORT_TYPE, ImportType.ATTENDANCE);
            }
            
            mappingActivityLauncher.launch(intent);

        } catch (Exception e) {
            AppLogger.e(TAG, "Error during file selection or reading headers.", e);
            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            eventIdForAttendanceImport = null;
            privilegeForAttendanceImport = null;
        }
    }

    private void handleEventAction(String action, long eventId) {
        if (action == null) return;
        switch (action) {
            case "DELETE":
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Event")
                        .setMessage("Are you sure you want to delete this event and all its attendance records?")
                        .setPositiveButton("Delete", (dialog, which) -> eventListViewModel.deleteEvent(eventId))
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
            case "EDIT":
                Intent intent = new Intent(getActivity(), AddEditEventActivity.class);
                intent.putExtra(AddEditEventActivity.EXTRA_EVENT_ID, eventId);
                intent.putExtra(AddEditEventActivity.EXTRA_PRIVILEGE, adminViewModel.currentPrivilege.getValue());
                addEditEventLauncher.launch(intent);
                break;
            case "IMPORT_ATTENDANCE":
                AppLogger.d(TAG, "IMPORT_ATTENDANCE action selected for eventId: " + eventId);
                this.eventIdForAttendanceImport = eventId;
                this.privilegeForAttendanceImport = adminViewModel.currentPrivilege.getValue();
                filePickerLauncher.launch("text/csv");
                break;
        }
    }

    // --- Other methods are unchanged ---
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) { return inflater.inflate(R.layout.fragment_event_list, container, false); }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { super.onViewCreated(view, savedInstanceState); eventListViewModel = new ViewModelProvider(this).get(EventListViewModel.class); setupRecyclerView(view); observeViewModel(); FloatingActionButton fab = view.findViewById(R.id.fab_add_event); fab.setOnClickListener(v -> { Intent intent = new Intent(getActivity(), AddEditEventActivity.class); intent.putExtra(AddEditEventActivity.EXTRA_PRIVILEGE, adminViewModel.currentPrivilege.getValue()); addEditEventLauncher.launch(intent); }); eventListViewModel.loadEvents(); }
    private void setupRecyclerView(View view) { RecyclerView recyclerView = view.findViewById(R.id.recycler_view_events); recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); recyclerView.setHasFixedSize(true); adapter = new EventListAdapter(); adapter.setOnEventListener(this); recyclerView.setAdapter(adapter); }
    private void observeViewModel() { eventListViewModel.getEventList().observe(getViewLifecycleOwner(), events -> { if (events != null) adapter.setEvents(events); }); eventListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> { if (message != null && !message.isEmpty()) { Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show(); } }); }
    @Override
    public void onEventClick(Event event) { Privilege currentPrivilege = adminViewModel.currentPrivilege.getValue(); EventActionsBottomSheetFragment.newInstance(event.getEventId(), event.getEventDate(), currentPrivilege) .show(getParentFragmentManager(), EventActionsBottomSheetFragment.TAG); }
}
