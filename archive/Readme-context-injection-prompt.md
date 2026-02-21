
###TAG-CONVERSATION-2026-02-21 12:15:01
Of course. Here is a comprehensive context injection prompt designed to get a new AI assistant fully up to speed on this project.

You can copy and paste this directly into a new chat, attach the `combined-code.txt` file, and the new assistant will have all the necessary information to continue our collaboration seamlessly.

---

### **Definitive Prompt for Full AI Context Restoration**

**Project Title:** SevaConnect Halasuru (RKM Attendance App) - Context & Status Debrief

**Your Role:** You are an expert senior Android developer and UI/UX designer. Your task is to fully absorb the following project context, review the accompanying complete codebase, and then seamlessly continue our collaboration.

### **Part I: The Project's Journey & Evolution**

**1. Genesis: The Original Goal**
The project's mission was to replace an inefficient, paper-based system for managing event attendance and donations at Ramakrishna Math, Halasuru. The initial concept has since evolved to include robust data management and donation collection functionalities.

**2. Core Features (Completed):**
*   **V1 Attendance System:** A stable V1 is complete, featuring a three-tiered privilege system (Operator, Event Coordinator, Super Admin), smart event scheduling, and a polished UI for on-site attendance marking.
*   **Modern Architecture:** The app follows a modern `Fragment -> ViewModel -> Repository -> DAO` pattern with a shared `AdminViewModel` for state management.
*   **Robust Data Management:** Includes CSV imports, a reports dashboard, and a production-ready local backup/restore feature. All known device-specific UI bugs have been resolved.

**3. The Current Feature Under Development: "Collection Batches" for Donations**
We have fully implemented a new, sophisticated feature for managing donations based on a "Collection Batch" workflow. This system is designed to mirror the real-world operational process of volunteers collecting donations in shifts.

*   **New "Donation Collector" Role:** A fourth, PIN-protected privilege level was created specifically for this task.
*   **Batch Lifecycle:**
    *   A "batch" (collection session) starts automatically when a Donation Collector logs in.
    *   All donations recorded are tied to this active batch.
    *   The UI displays a live summary card for the current batch (total cash, UPI, etc.).
    *   When the volunteer's shift is over, they press a **"Deposit & Close Batch"** button.
*   **Automated Reporting:** Closing a batch triggers the creation of a summary email addressed to the office. A detailed **CSV file of all transactions in that batch is automatically generated and attached to this email.**
*   **Data Integrity:** The donation form now has a validation system. It requires mandatory fields (`Address`, `PAN`/`Aadhaar`) to be present on a devotee's record before a donation can be saved, guiding the user to update the information via an "Edit" button if necessary.
*   **Admin Reporting:** A simplified, powerful report for the Super Admin is complete. It shows a list of total collections grouped by day. From this list, the admin can export a full day's transactions as a single CSV or email a daily summary with the CSV attached.

### **Part II: Current Status & Immediate Task**

The "Collection Batches" and Admin Reporting features are functionally complete. However, we are in the final stages of debugging a minor, device-specific issue.

*   **The Problem:** On certain devices (specifically identified on a Samsung phone), the Android File Picker fails to select a `.csv` file when importing attendance data. The file is visible but cannot be tapped. There are no crashes or error messages.
*   **Our Hypothesis:** The issue is caused by the phone's specific File Manager app having a poor implementation for handling the `text/csv` MIME type.
*   **Immediate Next Step (Brainstorming):** The user has just proposed a simple, code-free test to confirm this hypothesis:
    1.  Rename the `.csv` file in Google Drive to `.txt`.
    2.  Attempt to select the renamed `.txt` file from within the app on the problematic Samsung phone.
    3.  If the `.txt` file is selectable, it proves our hypothesis is correct.
*   **The Proposed Code Fix (If Hypothesis is Confirmed):** The permanent solution will be to change the file picker launch code from `filePickerLauncher.launch("text/csv")` to the more generic `filePickerLauncher.launch("*/*")` to bypass the faulty MIME type filtering.

### **Your Task**

1.  Review this document and the complete codebase to gain full context of our journey and the current state of the Donations feature.
2.  Acknowledge that you are synchronized and understand the immediate task: we are waiting for the results of the `.csv` vs `.txt` file renaming test.
3.  Be prepared to provide the code modification for the proposed fix (`filePickerLauncher.launch("*/*")`) once the user confirms the test results.
