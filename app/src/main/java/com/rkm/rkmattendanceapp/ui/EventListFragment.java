// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListFragment.java
package com.rkm.rkmattendanceapp.ui;

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

import com.rkm.rkmattendanceapp.R;

public class EventListFragment extends Fragment {

    private EventListViewModel eventListViewModel;
    private EventListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Get the ViewModel
        eventListViewModel = new ViewModelProvider(this).get(EventListViewModel.class);

        // 2. Set up the RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new EventListAdapter();
        recyclerView.setAdapter(adapter);

        // 3. Observe the LiveData from the ViewModel
        eventListViewModel.getEventList().observe(getViewLifecycleOwner(), events -> {
            // This code runs on the main thread whenever the event list is updated.
            adapter.setEvents(events);
        });

        eventListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // 4. Trigger the data load
        eventListViewModel.loadEvents();
    }
}