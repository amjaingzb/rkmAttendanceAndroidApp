// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationActionsBottomSheetFragment.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rkm.rkmattendanceapp.R;

public class DonationActionsBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "DonationActionsBottomSheet";
    public static final String REQUEST_KEY = "donation_action_request";
    public static final String KEY_ACTION = "selected_action";
    public static final String KEY_DONATION_ID = "donation_id";

    public static DonationActionsBottomSheetFragment newInstance(long donationId) {
        Bundle args = new Bundle();
        args.putLong(KEY_DONATION_ID, donationId);
        DonationActionsBottomSheetFragment fragment = new DonationActionsBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_donation_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long donationId = getArguments().getLong(KEY_DONATION_ID);

        view.findViewById(R.id.action_edit_donation).setOnClickListener(v -> sendResult("EDIT", donationId));
        view.findViewById(R.id.action_delete_donation).setOnClickListener(v -> sendResult("DELETE", donationId));

        // Disable "Send Receipt" for now
        View receiptAction = view.findViewById(R.id.action_send_receipt);
        receiptAction.setEnabled(false);
        receiptAction.setAlpha(0.5f);
    }

    private void sendResult(String action, long donationId) {
        Bundle result = new Bundle();
        result.putString(KEY_ACTION, action);
        result.putLong(KEY_DONATION_ID, donationId);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}
