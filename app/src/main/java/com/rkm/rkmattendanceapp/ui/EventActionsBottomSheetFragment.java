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

// MODIFIED: Import the old, reliable date/time classes
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventActionsBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "EventActionsBottomSheet";
    public static final String REQUEST_KEY = "event_action_request";
    public static final String KEY_ACTION = "selected_action";

    public static final String KEY_EVENT_ID = "event_id";
    public static final String KEY_EVENT_DATE = "event_date";
    public static final String KEY_PRIVILEGE = "privilege";

    public static EventActionsBottomSheetFragment newInstance(long eventId, String eventDate, Privilege privilege) {
        Bundle args = new Bundle();
        args.putLong(KEY_EVENT_ID, eventId);
        args.putString(KEY_EVENT_DATE, eventDate);
        args.putSerializable(KEY_PRIVILEGE, privilege);
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
        String eventDate = getArguments().getString(KEY_EVENT_DATE);
        Privilege privilege = (Privilege) getArguments().getSerializable(KEY_PRIVILEGE);

        View deleteAction = view.findViewById(R.id.action_delete_event);
        View editAction = view.findViewById(R.id.action_edit_event);

        if (privilege != Privilege.SUPER_ADMIN) {
            deleteAction.setVisibility(View.GONE);
        }

        if (privilege == Privilege.EVENT_COORDINATOR && isDateInPast(eventDate)) {
            editAction.setVisibility(View.GONE);
        }

        view.findViewById(R.id.action_set_active).setOnClickListener(v -> sendResult("SET_ACTIVE", eventId));
        editAction.setOnClickListener(v -> sendResult("EDIT", eventId));
        deleteAction.setOnClickListener(v -> sendResult("DELETE", eventId));
    }

    private void sendResult(String action, long eventId) {
        Bundle result = new Bundle();
        result.putString(KEY_ACTION, action);
        result.putLong(KEY_EVENT_ID, eventId);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    // MODIFIED: This is the new, more compatible implementation using Calendar.
    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false; // Fail safe
        }
        try {
            // Define the format that matches our database string
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date eventDate = sdf.parse(dateStr);

            // Get today's date, but stripped of its time component
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);
            Date today = todayCal.getTime();

            // The isBefore() check is what we need.
            // If the event date is before today (stripped of time), it's in the past.
            return eventDate.before(today);

        } catch (ParseException e) {
            e.printStackTrace();
            return false; // Fail safe on parsing error
        }
    }
}