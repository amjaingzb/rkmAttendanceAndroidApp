### 3. `TODO_IMMEDIATE.md`
*Use this to tell the AI **what to do next**.*

--- START OF FILE TODO_IMMEDIATE.md ---
# Immediate Tasks & Roadmap

## 🔴 Priority 1: Critical Fixes
1.  **Fix Samsung File Picker Issue:**
    *   **Context:** `text/csv` filter fails on some Android skins.
    *   **Task:** Modify `EventListFragment.java`, `DevoteeListFragment.java`, and `BackupRestoreActivity.java`.
    *   **Change:** Change `launcher.launch("text/csv")` to `launcher.launch("*/*")`. Add logic in the `onActivityResult` to verify the file extension ends in `.csv` or `.zip` (for restore) to prevent invalid file processing.

## 🟠 Priority 2: UI/UX Polish
1.  **Donation Receipt (Enable Feature):**
    *   The "Send Receipt" button in `DonationActionsBottomSheetFragment` is currently disabled.
    *   *Task:* Implement the logic to generate a text receipt (using `app_config` templates) and launch a WhatsApp intent, then enable the button.
2.  **Role Selection UI:**
    *   Review `RoleSelectionActivity` buttons for better visual hierarchy (e.g., make "Operator/Donation" distinct from "Admin").

## 🔵 Priority 3: Maintenance
1.  **Testing:** Perform a full regression test on the "Dirty Flag" logic. Ensure `BackupStateManager.clearDbDirtyFlag` is only called on a *successful* export.
2.  **Hardcoded Strings:** Extract strings from `MarkAttendanceActivity` and `DonationActivity` into `strings.xml` for better maintainability.

## ⚪ Future (V2)
1.  **Cloud Sync:** Implement Google Drive API for automated backups.
2.  **Multiple Active Events:** Update `LauncherActivity` to show a "Today's Schedule" chooser if multiple events overlap.
--- END OF FILE TODO_IMMEDIATE.md ---


