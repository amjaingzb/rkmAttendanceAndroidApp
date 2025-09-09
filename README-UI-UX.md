You have correctly identified the need for UI/UX design. Since you're not a UI/UX expert, we will use a simple, practical, and text-based approach that focuses on functionality and standard Android patterns. We don't need fancy design software.

Of course. Consolidating the design into a single, clear document is the perfect way to finalize the UX phase. This will serve as our blueprint for development.

Here is the complete UI/UX design, incorporating all of our refinements.

---

### **Consolidated App UI/UX Design**

#### **Core Principle: App Modes**

The app operates in two distinct modes, determined by user action.

1.  **Operator Mode (Default):** The app launches directly into a simplified, task-focused interface for marking attendance for a single, pre-defined "active" event. The focus is on speed and simplicity.
2.  **Admin Mode:** Accessed via a settings icon from the Operator view. This mode provides full access to all application features (event management, devotee management, imports, reports) through a tabbed interface.

---

### **I. Operator Flow & Screens**

#### **User Flow (Operator)**

1.  User launches the app.
2.  The app checks for an event marked as "Active".
    *   **If found:** Opens directly to the `MarkAttendanceActivity` for that event.
    *   **If not found:** Displays a simple screen with the message: "No Active Event. Please ask an Admin to set one."
3.  On the `MarkAttendanceActivity`, the operator searches for attendees, marks them present, or performs on-the-spot registrations.

#### **Screen A: Mark Attendance Screen (`MarkAttendanceActivity`)**

*   **Toolbar (Top):**
    *   **Title:** The name of the active event (e.g., "Spiritual Retreat").
    *   **Subtitle:** The date of the active event.
    *   **Settings Icon (Gear):** Tapping this opens a dialog to switch to Admin Mode (can be password-protected later).
*   **Stats Header (Below Toolbar):**
    *   A non-scrolling header area displaying real-time counts for the current event:
        *   `Pre-Registered: [Count]`
        *   `Attended: [Count]`
        *   `Spot Reg: [Count]`
        *   `Total: [Count]`
*   **Search Area:**
    *   A prominent **Search Bar** with hint text: "Search by name or mobile..."
    *   An **"Add New" Button** located next to the search bar. Tapping this opens the `AddEditDevoteeActivity` for on-the-spot registration.
*   **Search Results Area (Dynamic):**
    *   Appears below the search bar *only when the user is typing*.
    *   Displays a list of matching devotees from the master database.
    *   Each list item will show:
        *   Devotee Name
        *   Devotee Mobile Number
        *   A visual indicator: **"Pre-registered"** (if they are on the initial list) or **"Walk-in"** (if they are not).
    *   Tapping a result marks the person as present, updates the stats, adds them to the "Checked-In" list, and clears the search bar.
*   **Main Content (Below Search):**
    *   A section title: "Checked-In Attendees"
    *   A scrolling list (`RecyclerView`) of all devotees who have been marked as present for this event.

---

### **II. Admin Flow & Screens**

#### **User Flow (Admin)**

1.  From the `MarkAttendanceActivity`, the Admin taps the Settings Icon to enter Admin Mode.
2.  The Admin is taken to the `AdminMainActivity`, which is the main hub with a bottom navigation bar.
3.  The Admin can switch between three main sections: Events, Devotees, and Reports.

#### **Screen B: Admin Main Screen (`AdminMainActivity`)**

*   **Structure:** A modern, tabbed interface.
*   **Toolbar (Top):** The title dynamically changes to "Events", "Devotees", or "Reports" based on the active tab.
*   **Bottom Navigation Bar:** Contains three icons and labels for the main app sections:
    1.  **Events (Default Tab)**
    2.  **Devotees**
    3.  **Reports**

#### **Screen B.1: Events Tab (Fragment)**

*   **Function:** Manage all events and set the active event for operators.
*   **Toolbar:** Title is "Events". A menu (3 dots) might contain "Import Attendance CSV".
*   **Main Content:** A scrolling list of all past, present, and future events.
    *   Each item shows Event Name and Event Date.
    *   Tapping an event could navigate to its details or show management options (Edit, Delete, **Set as Active Event**).
*   **Floating Action Button (FAB):** A `+` button to open a screen/dialog to create a new event.

#### **Screen B.2: Devotees Tab (Fragment)**

*   **Function:** Manage the master list of all devotees.
*   **Toolbar:** Title is "Devotees". A **Search Icon** is present to filter the list. A menu (3 dots) will contain "Import Master Devotee List".
*   **Main Content:** A scrolling list of all devotees in the database.
    *   Each item shows Devotee Name and Mobile Number.
    *   Tapping a devotee opens the `AddEditDevoteeActivity` in "Edit Mode".
*   **Floating Action Button (FAB):** A `+` button opens the `AddEditDevoteeActivity` in "New Devotee" mode.

### **Revised and Agreed-Upon Design for Devotee Search**

Based on your excellent feedback, here is our new, superior design for the Devotees tab.

#### **Screen B.2: Devotees Tab (Fragment) - Final Design**

*   **Toolbar (Top):**
    *   Title: "Devotees".
    *   (No search icon. The search bar is now part of the main content).
*   **Content Area (Below Toolbar):**
    *   A permanent, non-scrolling **`EditText` (Search Bar)**.
        *   When empty, it displays hint text: "Search by name or mobile...".
        *   As the user types, the list below is filtered in real-time.
*   **Main Content:**
    *   A scrolling list (`RecyclerView`) of devotees. The content of this list is controlled by the text in the search bar above.
*   **Floating Action Button (FAB) (Bottom Right):**
    *   A `+` button.
    *   When tapped:
        *   It reads the current text from the search bar.
        *   It launches the `AddEditDevoteeActivity`.
        *   It passes the search text as an "extra" in the `Intent`, so the new activity can use it to pre-populate the name or mobile field.


## **Final Agreed-Upon Design for Imports**

*   **On the "Devotees" Tab:**
    *   The toolbar will have a **three-dot menu icon**.
    *   Tapping it will reveal one option: **"Import Master Devotee List"**.
*   **On the "Events" Tab:**
    *   The toolbar will have a **three-dot menu icon**.
    *   Tapping it will reveal one option: **"Import Attendance CSV"**.



#### **Screen B.3: Reports Tab (Fragment/Activity)**

*   **Function:** View and export application data.
*   **Toolbar:** Title is "Reports".
*   **Main Content:** A simple, static list of available reports.
    *   "Overall Statistics"
    *   "Full Devotee List (Exportable)"
    *   "Active Devotees (Exportable)"
    *   "Attendance by Event" (requires selecting an event first)

---

### **III. Shared/Utility Screens**

#### **Screen C: Add / Edit Devotee Screen (`AddEditDevoteeActivity`)**

*   **Function:** A single screen used for both adding new devotees (by Operator or Admin) and editing existing ones (Admin only).
*   **Toolbar:**
    *   Back Arrow.
    *   **Title:** Dynamically set to "New Devotee" or "Edit Devotee".
    *   **Save Button** (or Checkmark Icon).
*   **Main Content:** A scrolling form with the following fields:
    *   Text Input: "Full Name*" (Required)
    *   Text Input: "Mobile Number*" (Required)
    *   Text Input: "Email"
    *   Text Input: "Address"
    *   Text Input: "Age"
    *   Dropdown/Spinner: "Gender"

#### **Screen D: CSV Mapping Screen (`MappingActivity`)**

*   **Function:** A utility screen shown after an Admin selects a CSV file for import.
*   **Toolbar:** Back Arrow, Title "Map CSV Columns", "Import" Button.
*   **Main Content:** A two-column list.
    *   **Column 1 (Read-only):** CSV Header Name (e.g., "Participant Name").
    *   **Column 2 (Editable):** A dropdown (Spinner) to select the target database field (e.g., "full_name", "mobile", "DROP").

This document now serves as our comprehensive guide. We can proceed to build the UI, screen by screen, with a clear and shared understanding of the final product.
