Of course. This is an excellent idea and the perfect time to do it. Consolidating the project's entire history into a single, clean document will be invaluable for maintaining context and ensuring we're aligned.

Here is a detailed summary of the project's journey, from the original requirements to the final, agreed-upon design we are currently implementing.

---

### **Project Genesis & Evolution: RKM Attendance App**

This document outlines the requirements, design decisions, and current implementation status of the RKM Attendance application.

### **I. Original Requirements (Summary of `Requirements_attendance.md`)**

The initial goal was to create a desktop application to replace a manual, paper-based system for event registration and attendance tracking.

*   **Core Problem:** The existing process of printing lists, manually ticking attendance, and later digitizing the data was inefficient, error-prone, and involved duplicate effort.
*   **P1 (High Priority) Features:**
    *   An application to handle pre-event registrations (via CSV import), on-day attendance marking, and on-day spot registrations.
    *   A master devotee database using "Name + Mobile" as the primary key.
    *   Functionality for an **Admin** to manage events and import/export data.
    *   Functionality for an **Operator** to take attendance and perform spot registrations on event day.
*   **P2 (Lower Priority) Features:**
    *   Fuzzy matching for names to handle typos (e.g., Amit ↔ Amith).
    *   Tracking cumulative attendance stats per devotee.
    *   A "Master DB Health" view.
    *   Prioritized search results based on attendance frequency.

### **II. Key Design Evolutions & Decisions (Our Discussions)**

During the porting process to Android, we had several crucial UX discussions that significantly evolved and improved the initial design.

**1. The "Set Active Event" Logic: From Manual to Automated**

*   **Initial Idea:** An Admin would have to manually press a "Set as Active" button to flag an event for the Operator.
*   **Discussion:** We identified that this was a potential point of human error. The most common use case is that an event is active on the day it is scheduled.
*   **Final Agreed-Upon Design ("Smart Schedule"):**
    *   We **removed the manual `is_active` flag**.
    *   We added two new fields to the `event` table: `active_from_ts` and `active_until_ts`.
    *   When an Admin creates an event, these fields are given smart defaults (e.g., 6 AM to 10 PM on the event date). The Admin can override these for multi-day events or special cases.
    *   The app now **automatically determines the active event** by checking if the current time falls within an event's active window.
    *   To prevent ambiguity, the Admin is **prevented from creating events with overlapping time windows**.

**2. The App Structure: From Simple to Role-Based**

*   **Initial Idea:** A simple two-mode system: Operator and Admin.
*   **Discussion:** We recognized that the "Admin" role was overloaded. A person setting up an event (an Event Coordinator) has different needs and should have fewer permissions than a Super Admin responsible for data integrity.
*   **Final Agreed-Upon Design ("Privilege Levels"):**
    *   We defined three distinct roles:
        1.  **Operator (Default):** No login required. Can only mark attendance for the active event.
        2.  **Event Coordinator (Mid-Level):** Requires a PIN. Can manage future events (create, edit, import pre-registrations) and view reports. Cannot delete data or manage the master devotee list.
        3.  **Super Admin (Highest Privilege):** Requires a different PIN. Has full CRUD access to all data, including the master devotee list and historical events.
    *   The app flow was redesigned around this: the app starts in Operator mode if possible; otherwise, it presents a login screen to enter Coordinator or Super Admin mode.

**3. The `attendance` Table Schema**

*   **Initial Idea:** The `cnt` column would track attendance (0 for not present, >0 for present).
*   **Discussion:** We realized this created ambiguity. We could not distinguish between a person who was pre-registered but hadn't arrived (`cnt=0`) and a person who was a last-minute spot registrant. This was critical for the Operator's UI and for accurate reporting.
*   **Final Agreed-Upon Design:**
    *   We kept the powerful `cnt` column to support historical, aggregated data imports.
    *   We added a new `reg_type` (TEXT) column.
    *   The logic is now clear:
        *   Pre-registrations are inserted with `reg_type="PRE_REG"` and `cnt=0`.
        *   Spot registrations are inserted with `reg_type="SPOT_REG"` and `cnt=1`.
        *   Marking a pre-registrant present updates their `cnt` to `1`.

**4. Admin Devotee Search UX**

*   **Initial Idea:** A toolbar icon would reveal a search bar.
*   **Discussion:** We determined that the primary task on the Devotees screen is searching, so the search bar should always be visible to reduce taps. We also identified a workflow improvement.
*   **Final Agreed-Upon Design:**
    *   The Devotees tab has a **permanent, always-visible search bar** at the top.
    *   If a search yields no results and the Admin taps the `+` FAB, the search query is **automatically pre-populated** into the "Add New Devotee" form.

### **III. Current Application State (As Implemented in Android)**

*   **Architecture:** The app is successfully built on a `Fragment -> ViewModel -> Repository -> DAO` architecture, with a clean separation between the UI and the headless core library.
*   **Backend:**
    *   The entire core library is ported and functional.
    *   All DAOs have been rewritten to use the native Android SQLite API.
    *   The database schema reflects all our final design decisions (the "Smart Schedule" fields in `event` and the `reg_type` field in `attendance`).
*   **Admin Mode UI (`AdminMainActivity`):**
    *   A fully functional Bottom Navigation Bar switches between three fragments.
    *   **Events Tab:** Users can view a case-insensitively sorted list of events. They can launch the `AddEditEventActivity` to create and edit events, including the new "Active From/Until" fields. They can delete events.
    *   **Devotees Tab:** Users can view a case-insensitively sorted list of all devotees. A permanent search bar filters the list in real-time. Users can launch `AddEditDevoteeActivity` to create a new devotee (with pre-population from search) or edit an existing one.
    *   **Reports Tab:** Displays live "Overall Statistics" from the database.
*   **Operator Mode UI (`MarkAttendanceActivity`):**
    *   This screen is built and functional. It correctly displays stats, a search bar, and the checked-in list.
    *   The search function correctly identifies pre-registered vs. walk-in devotees.
    *   The "Add New" button correctly launches the form and performs a full "on-spot registration" (adds to master list AND marks as present).

### **IV. Immediate Next Steps (Where We Are Now)**

Our very next task is to implement the **Privilege System's entry point**.
1.  **Create the `PinEntryActivity`:** Build the UI and logic for the PIN screen that will act as the gateway to the Coordinator and Super Admin modes.
2.  **Update `LauncherActivity`:** Modify the launcher so that if no active event is found, it directs the user to our new PIN screen instead of directly to the `AdminMainActivity`.
3.  **Implement Privilege-Aware UI:** Begin hiding/showing UI elements (like the "Delete" button or the "Devotees" tab) based on the current privilege level.
