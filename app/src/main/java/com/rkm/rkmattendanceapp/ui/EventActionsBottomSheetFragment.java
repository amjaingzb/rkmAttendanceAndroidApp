// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventActionsBottomSheetFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rkm.rkmattendanceapp.R;

public class EventActionsBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "EventActionsBottomSheet";
    public static final String REQUEST_KEY = "event_action_request";
    public static final String KEY_ACTION = "selected_action";
    public static final String KEY_EVENT_ID = "event_id";
    
    public static EventActionsBottomSheetFragment newInstance(long eventId) {
        Bundle args = new Bundle();
        args.putLong(KEY_EVENT_ID, eventId);
        EventActionsBottomSheetFragment fragment = new EventActionsBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_event_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long eventId = getArguments().getLong(KEY_EVENT_ID);

        view.findViewById(R.id.action_set_active).setOnClickListener(v -> sendResult("SET_ACTIVE", eventId));
        view.findViewById(R.id.action_edit_event).setOnClickListener(v -> sendResult("EDIT", eventId));
        view.findViewById(R.id.action_delete_event).setOnClickListener(v -> sendResult("DELETE", eventId));
    }

    private void sendResult(String action, long eventId) {
        Bundle result = new Bundle();
        result.putString(KEY_ACTION, action);
        result.putLong(KEY_EVENT_ID, eventId);
        // Use Fragment Result API to send data back to the parent fragment
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}