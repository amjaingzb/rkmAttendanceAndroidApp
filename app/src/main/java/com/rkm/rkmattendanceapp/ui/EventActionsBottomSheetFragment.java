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
import com.rkm.rkmattendanceapp.util.AppLogger;

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
        View importAction = view.findViewById(R.id.action_import_attendance);

        deleteAction.setVisibility(privilege == Privilege.SUPER_ADMIN ? View.VISIBLE : View.GONE);
        
        boolean isPast = isDateInPast(eventDate);
        if (privilege == Privilege.EVENT_COORDINATOR && isPast) {
            editAction.setEnabled(false);
            editAction.setAlpha(0.5f);
            importAction.setEnabled(false);
            importAction.setAlpha(0.5f);
        } else {
            editAction.setEnabled(true);
            editAction.setAlpha(1.0f);
            importAction.setEnabled(true);
            importAction.setAlpha(1.0f);
        }

        importAction.setOnClickListener(v -> {
            if (v.isEnabled()) sendResult("IMPORT_ATTENDANCE", eventId);
        });
        editAction.setOnClickListener(v -> {
            if (v.isEnabled()) sendResult("EDIT", eventId);
        });
        deleteAction.setOnClickListener(v -> sendResult("DELETE", eventId));
    }

    private void sendResult(String action, long eventId) {
        Bundle result = new Bundle();
        result.putString(KEY_ACTION, action);
        result.putLong(KEY_EVENT_ID, eventId);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date eventDate = sdf.parse(dateStr);
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);
            return eventDate.before(todayCal.getTime());
        } catch (ParseException e) {
            AppLogger.e(TAG, "Could not parse event date string: " + dateStr, e);
            return false;
        }
    }
}
