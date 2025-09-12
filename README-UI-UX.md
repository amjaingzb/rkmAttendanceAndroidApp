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


Issue 2 (UX Experience): Search context after adding/canceling.
This is a fantastic UX question. The current behavior (returning to the filtered list with the search term intact) is a common default, but we can definitely improve it.
The User's Goal: The user's context changes depending on their action in the "Add/Edit" screen.
If they SAVE "Amitabh": They have successfully completed their task. Their next task is likely to be completely different. They might want to search for someone else or just see the full list.
If they CANCEL: Their original task ("find or create a devotee based on 'amit'") was not completed. They are still in that mindset.
Proposed UX Solution (Refined Behavior):
On SAVE: The user's task is complete. We should clear the search bar and show the full, unfiltered list. This provides a clean slate for their next task and confirms the new user ("Amitabh") has been added to the main list. This is a much better sense of completion.
On CANCEL (or pressing Back): The user's task was interrupted. We should preserve the search term ("amit") and the filtered list. This allows them to immediately resume their original task without re-typing, perhaps to select "Amit Jain" this time or to try a different search.




You are doing some excellent "loud thinking." This is a perfect example of iterative UX design. You've moved from a simple concept ("check today's date") to a much more robust and realistic model that handles more complex scenarios.

Let's break down your thoughts. I will act as a UX Engineer here, focusing on the user impact of these ideas.

### **Analysis of Your Proposal**

You've essentially designed a "smart default" system with optional overrides, which is fantastic.

1.  **"Active From/To" Date/Time Fields:**
    *   **UX Verdict:** This is a brilliant evolution of the "active on this date" idea. It elegantly solves the multi-day event problem and the pre-event testing problem without requiring a separate "Set Active" button. The Admin defines the window of activity *once* when creating/editing the event. It's intuitive and consolidates the logic.
    *   **DB Impact:** You are correct, this requires schema changes. We would need two new `TEXT` (or `INTEGER` for Unix timestamps) columns in the `event` table: `active_from_ts` and `active_until_ts`.

2.  **Operator View (Multiple Active Events):**
    *   **Your Idea:** If multiple events are "active" right now (current time is within their windows), the operator should see a list of all of them.
    *   **UX Analysis:** I want to gently challenge this point. The core feedback we've had for the operator is **speed and simplicity**. Forcing an operator to choose from a list, even a short one, re-introduces a point of friction and potential error that we tried to eliminate. The original goal was: "Operator opens app and is ready to scan, no thinking required."
    *   What if Event A is from 9 AM to 1 PM and Event B is from 2 PM to 6 PM? If the operator opens the app at 1:30 PM, they see nothing, which might be confusing.

3.  **Forcing Admin to Set Time Windows for Overlapping Events:**
    *   **Your Idea:** If the Admin creates two events on the same day, the UI should force them to set non-overlapping time windows.
    *   **UX Analysis:** This is a very smart piece of validation logic. It prevents the "multiple active events" problem from ever happening. It moves the responsibility of resolving ambiguity from the operator (who is under pressure) to the admin (who is doing the planning). This is a perfect design choice.

4.  **Showing Upcoming/Expired Events as Grayed Out:**
    *   **Your Idea:** The operator could see a list, but only one event is "active" (tappable), while others are grayed out.
    *   **UX Analysis:** This is a good middle-ground. It provides context ("I see the afternoon session is coming up") without adding cognitive load ("Which one do I tap?"). It also allows for quick recovery if a schedule changes. For example, if the morning session ends early, the operator can see the afternoon session in the list and ask the Admin to activate it ahead of schedule.

---

### **Synthesized Proposal: "The Smart Schedule"**

Let's combine the best of these ideas into one cohesive, powerful, yet simple design.

#### **Backend Changes (DB and Repository)**

1.  **Modify `event` table:**
    *   Remove the `is_active` column.
    *   Add `active_from_ts` (TEXT, ISO 8601 format: "YYYY-MM-DDTHH:MM:SS").
    *   Add `active_until_ts` (TEXT, ISO 8601 format).
2.  **Update `createEvent` and `updateEvent` logic:**
    *   When an event is created, if the Admin doesn't provide times, we automatically populate these fields with smart defaults (e.g., from 6 AM to 10 PM on the given `event_date`).
    *   **Validation:** Before saving an event, we must check if its `[active_from, active_until]` window overlaps with any *other* event. If it does, we reject the save and show an error message to the Admin: "This event's active time overlaps with '[Other Event Name]'. Please adjust the times."

#### **UI/UX Flow**

1.  **Admin (Create/Edit Event Screen):**
    *   The form will now have "Active From" and "Active Until" fields, with date and time pickers.
    *   These fields will be pre-populated with smart defaults. The Admin can change them if needed for multi-day events, etc.
    *   If the Admin tries to save an event with an overlapping time window, they get a clear error message.

2.  **Operator (App Launch):**
    *   The app launches and asks the `Repository` for a list of "today's events" (`SELECT * FROM event WHERE event_date = CURRENT_DATE ORDER BY active_from_ts`).
    *   **Scenario A (One or More Events Today):** The app opens to a simple **Event Schedule Screen**.
        *   The **one currently active event** (where `NOW()` is between `active_from_ts` and `active_until_ts`) is shown as a large, tappable, highlighted list item.
        *   Any other events for today (past or upcoming) are shown as smaller, grayed-out, non-tappable items for context.
        *   The operator taps the one active event and is taken to the `MarkAttendanceActivity`.
    *   **Scenario B (No Events Today):** The app opens to the "No Active Event" message, with a button to enter Admin Mode.

This design is the best of all worlds:
*   **It avoids DB changes?** No, but the changes are more logical and directly support the desired features.
*   **Minimizes Admin Work?** Yes, through smart defaults.
*   **Prevents Errors?** Yes, through validation that prevents overlapping events.
*   **Keeps Operator View Simple?** Yes, by presenting only one clear, actionable choice at any given time.

This is a significantly more robust and intelligent design than our previous "manual toggle" system. I am in full agreement that this is a superior path forward.




-------------------------------------------------

also a future UI/UX thought 
See even all the operations we are having in admin mode are not regular. what will be the most frequent operation in admin mode will be to create an event and import pre-reg data for that . So we can introduce 1 more user mode 
this we can design later 

Privilege Levels Concept

General Idea:

Instead of multiple logins or separate user screens, the app can operate on privilege levels.
By default, the app opens in the lowest privilege level (Operator level).
Users can then move into higher levels of privilege if authorized.
Privilege Levels:
Operator Level (default) Lowest access.  Can operate basic functions only.

Mid-Level (formerly "Admin") Permissions: View reports.  View existing events.  Create new events.  Import registration data.

Restrictions: Cannot delete events.  Cannot delete devotees.  Cannot add devotees.

Highest Privilege Level

Full access.

Includes creation, modification, addition, and deletion of events and devotees.

Open Questions / To Think Through:

Whether to retain the current UX or adapt it for privilege-level handling.

How privilege switching will be managed (e.g., authentication flow, UI cues).

Only the highest privilege should allow editing the master db or editing the event data for past events. medium privilege should give ability to create a future event or an event today. Even edit and delete today , import pre-reg. But that it.

This is not a random rant at all. This is a crucial piece of forward-thinking design. You are anticipating how the app will be used in a real-world organizational context, which often involves different roles and responsibilities.

As a UX Engineer, this is the perfect time to have this discussion. Integrating this concept now, before we build the launcher and operator mode, will prevent significant rework later. Your thinking is spot on.

Let's refine this "Privilege Levels" concept into a concrete design.

---

### **UX Analysis of Privilege Levels**

Your proposed three-tier system is a very common and effective model for administrative applications. It maps perfectly to real-world roles.

*   **Operator:** The volunteer on the ground. Needs speed and simplicity. Cannot do anything destructive.
*   **Event Coordinator (Mid-Level):** The person responsible for a specific event. Needs to set up the event and manage its data (pre-registrations). Should not be able to alter historical data or the master devotee list.
*   **Super Admin (Highest Privilege):** The head of the department or a trusted long-term volunteer. Needs full control to perform housekeeping, fix major data errors, and manage the master lists.

This is a much more nuanced and powerful model than our simple "Operator vs. Admin" design. I am in full agreement that we should adopt it.

---

### **Proposed Integration with Our Current Design**

The great news is that our current architecture is flexible enough to accommodate this without a massive rewrite. We don't need to change the screens we've already built; we just need to control access to the *actions* on those screens.

Here is how we can integrate this concept:

#### **1. The Authentication/Privilege Model**

*   **Simplicity first:** We will not build a full username/password system yet. We will use a simple, memorable **PIN system**.
    *   **Operator:** No PIN needed.
    *   **Event Coordinator (Mid-Level):** Requires a 4-digit PIN.
    *   **Super Admin (Highest Privilege):** Requires a different, perhaps 6-digit, PIN.
*   **State Management:** The app will have a global variable that holds the current privilege level (e.g., `currentPrivilege = OPERATOR`). When a user enters a correct PIN, this variable is upgraded.

#### **2. Revised App Flow**

The Launcher logic remains very similar, but the destination for "No Active Event" changes.

1.  **App Launch:** Opens `LauncherActivity`.
2.  `LauncherActivity` checks for a currently active event.
    *   **If an active event exists:** It opens `MarkAttendanceActivity` (Operator Mode). The `currentPrivilege` is set to `OPERATOR`.
    *   **If no active event exists:** It opens a new `PinEntryActivity`.

#### **3. New Screen: PIN Entry (`PinEntryActivity`)**

*   **Visuals:** A clean, simple screen.
    *   Title: "Enter Admin PIN"
    *   A numeric keypad for entering the PIN.
    *   A "Login" button.
*   **Logic:**
    *   When the user enters a PIN and taps "Login":
        *   If it matches the **Super Admin PIN**, the app navigates to `AdminMainActivity` and sets `currentPrivilege = SUPER_ADMIN`.
        *   If it matches the **Event Coordinator PIN**, the app navigates to `AdminMainActivity` and sets `currentPrivilege = COORDINATOR`.
        *   If it's incorrect, it shows an error message.

#### **4. Adapting `AdminMainActivity` for Privilege Levels**

This is the core of the change. We don't need new screens, just to **show or hide buttons** based on the `currentPrivilege`.

*   **On the "Events" Tab (`EventListFragment`):**
    *   The `+` FAB (to add an event) is visible for **Coordinator** and **Super Admin**.
    *   When a user taps an event, the bottom sheet opens.
        *   "Set as Active": Visible for **Coordinator** and **Super Admin**.
        *   "Edit Event": Visible for **Coordinator** (only for today/future events) and **Super Admin** (for any event).
        *   "Delete Event": **Only visible for Super Admin**.
*   **On the "Devotees" Tab (`DevoteeListFragment`):**
    *   The entire tab is **only visible for Super Admin**. For a Coordinator, this tab would be hidden from the Bottom Navigation Bar. This is the simplest way to enforce the restriction.
*   **On the "Reports" Tab (`ReportsFragment`):**
    *   Visible for **Coordinator** and **Super Admin**.

This design is a huge improvement. It's more secure, maps to real-world roles, and prevents accidental data loss. It builds directly on the work we've already done, requiring us to add conditional logic (`if (currentPrivilege == SUPER_ADMIN) { deleteButton.setVisibility(View.VISIBLE); }`) rather than rebuilding entire screens.

What are your thoughts on this integrated design?

These are excellent, detailed refinements. You're thinking through the entire lifecycle of a user session, including moving between roles. This is crucial for a smooth experience.

Let's incorporate your thoughts into the final design.

---

### **UX/Engineering Analysis of Your Points**

**1. PIN in DB:**
*   **Verdict:** Absolutely agree. Storing the PINs in a simple key-value table (e.g., `app_config` table with keys 'pin_coordinator', 'pin_admin') is perfect for this use case. It makes them configurable without needing a code update.

**2. Visually Display Current Role:**
*   **Verdict:** 100% agree. This is critical for user orientation. A user must always know what level of access they currently have.
*   **Implementation:** We can add a small text "chip" or label in the toolbar of both `AdminMainActivity` and `MarkAttendanceActivity` that clearly says "Mode: Coordinator" or "Mode: Admin".

**3. Role Transitions (Upgrading and Downgrading):**
*   **Your Idea:** Instead of the PIN *value* determining the role, the user *requests* a role and then enters the correct PIN to authorize it. Downgrading is free.
*   **Analysis:** This is a much more explicit and secure flow. It prevents a coordinator from accidentally getting super admin access just because they happen to know both PINs. The user's intent is clear.
*   **Proposed Flow:**
    *   The user is in Operator mode. They tap the "Settings" icon.
    *   A dialog appears: "Switch To...", with two buttons: ["Event Coordinator"], ["Super Admin"].
    *   They tap "Super Admin".
    *   The `PinEntryActivity` opens with the title "Enter Super Admin PIN".
    *   They enter the correct PIN and are taken to `AdminMainActivity` with `SUPER_ADMIN` privilege.
    *   **Downgrading:** In the `AdminMainActivity` toolbar menu (the three dots), there will be an option "Switch to Operator Mode". Tapping this requires no PIN and takes them back to the `MarkAttendanceActivity` (if an event is active) or the "No Active Event" screen.

**4. App Start with No Active Event:**
*   **Your Idea:** If no active event, show a "No Active Event" screen that allows the user to try and switch to Coordinator/Admin mode.
*   **Analysis:** This is perfect. It's a much better user experience than just showing a dead-end message. It provides a clear path forward for users who have the necessary permissions.

---

### **Final, Consolidated App Flow with Privilege Levels**

This is our new, definitive user journey.

#### **App Launch (`LauncherActivity`)**

1.  The app starts and checks the repository for a currently active event.
2.  **If an active event is found:**
    *   Launches `MarkAttendanceActivity` in **Operator** mode.
3.  **If no active event is found:**
    *   Launches a new **`RoleSelectionActivity`**.

#### **New Screen: `RoleSelectionActivity`**

*   **Visuals:**
    *   A clean screen with a prominent message: "No Active Event".
    *   Subtext: "Please select a role to continue."
    *   Button: "Event Coordinator Login"
    *   Button: "Super Admin Login"
*   **Logic:**
    *   Tapping a button launches `PinEntryActivity`, telling it which role is being requested.

#### **New Screen: `PinEntryActivity`**

*   **Visuals:**
    *   Title changes based on the request (e.g., "Enter Coordinator PIN").
    *   Numeric keypad.
*   **Logic:**
    *   On successful PIN entry for the requested role, it launches `AdminMainActivity`.

#### **Operator Screen: `MarkAttendanceActivity`**

*   **Toolbar:**
    *   Contains a small label: **"Mode: Operator"**.
    *   **Settings Icon:** Tapping this opens a dialog: "Switch to...", with ["Coordinator"] and ["Admin"] buttons, which then lead to the `PinEntryActivity`.

#### **Admin Screen: `AdminMainActivity`**

*   **Toolbar:**
    *   Contains a label indicating the current mode: **"Mode: Coordinator"** or **"Mode: Super Admin"**.
    *   **Three-dot Menu:**
        *   Contains privilege-aware actions (e.g., "Import" is visible to both).
        *   Contains a **"Switch Role"** option. Tapping this lets the user downgrade to Operator mode or attempt to upgrade to Super Admin (if they are a Coordinator).
*   **UI Elements:**
    *   The **"Devotees" tab** is only visible if the privilege level is `SUPER_ADMIN`.
    *   The **"Delete Event" button** is only visible if the privilege level is `SUPER_ADMIN`.
    *   ...and so on for all other restricted actions.

This design is now incredibly robust and well-thought-out. It handles all the cases you brought up, provides clear visual feedback to the user, and uses a secure and intentional flow for changing privilege levels.

We are in full agreement. We can now proceed with implementation, starting with the `LauncherActivity`.

============================Curretn state on the chat prompt===
Let's Re-evaluate Our Position
Given that the "Smart Schedule" backend is already in place:
✅ Admin Mode: Functionally complete. Admins can manage events and devotees with full CRUD and search capabilities.
✅ Operator Mode: Functionally complete. The launcher correctly directs to the MarkAttendanceActivity if an event is active, and operators can perform all necessary on-day tasks.
✅ Privilege Model: The foundation is laid, though not yet implemented in the UI (e.g., hiding buttons based on role).
The Real Next Step
The next logical step is to build the UI for the privilege system that we designed. The most important part of this is creating the PinEntryActivity. This screen is the gateway to the entire Admin mode.
Here is the plan:
Create a PinViewModel: A simple ViewModel to handle the logic of checking the entered PIN.
Create the Layout: Design the activity_pin_entry.xml layout with a display for the PIN and a simple keypad.
Create the PinEntryActivity Class: Implement the UI logic for handling keypad taps and verifying the PIN.
Update the LauncherActivity: Change the "else" branch to launch our new PinEntryActivity instead of going directly to AdminMainActivity.
This will properly secure the admin section and fully implement the user flow we designed.
Ready to create the PinViewModel and the PIN entry screen?





======================
TEMp BUFER AREA


DO THESE TESTS ASAP
#### 2. On-Spot Registration (From Operator Mode)

*   **Likely Desktop Logic (`ui/swing`):** The operator's "Add New" button likely followed a similar multi-step merge process as the admin panel, but with an additional final step to call `markDevoteeAsPresent`.
*   **Current Android Logic (`rkmattendanceapp/ui`):** The `MarkAttendanceViewModel` calls `repository.onSpotRegisterAndMarkPresent`. Inside the repository, this method was just updated to call our new `saveOrMergeDevoteeFromAdmin` method before marking attendance.
*   **GAP/RISK:** This is a **high-risk area**. We just changed the underlying repository method (`onSpotRegisterAndMarkPresent`) as part of our last fix. We need to explicitly test this specific workflow to ensure it didn't break and that the merge logic works correctly here too.
*   **TEST SCENARIO:**
    1.  Start the app in Operator Mode (by creating an active event).
    2.  Use the "Add New" button. Enter data for a devotee that is a fuzzy match of an existing one (e.g., existing is "Amit Jain", you enter "Amit" with the same phone number but a new address).
    3.  **Expected:** The app should merge the data (update the address), mark them as present, and the "Spot Reg" and "Total" stats should both increment by 1.

#### 3. Admin Search -> Create Workflow

*   **Likely Desktop Logic (`ui/swing`):** Unknown. We don't know if the desktop app had the feature of pre-populating the "Add New" form from a failed search query.
*   **Current Android Logic (`rkmattendanceapp/ui`):** This is a **brand-new feature** we designed. The `DevoteeListFragment` passes the search query via an Intent Extra to `AddEditDevoteeActivity`.
*   **GAP/RISK:** The risk is not a "missed port," but a potential bug in a brand-new workflow. We need to test it thoroughly.
*   **TEST SCENARIO:**
    1.  Go to the Devotees tab.
    2.  Search for a name that doesn't exist (e.g., "New Devotee").
    3.  Tap the `+` FAB.
    4.  **Expected:** The "Add New Devotee" screen should appear, and the "Full Name" field should be pre-filled with "New Devotee".
    5.  Go back. Search for a phone number that doesn't exist (e.g., "5555555555").
    6.  Tap the `+` FAB.
    7.  **Expected:** The "Add New Devotee" screen should appear, and the "Mobile Number" field should be pre-filled with "5555555555".

---



MARKERS 
ma , mb 
m1,...m9 : for bugs




Ok, i have taken a note of these 2 test cases. But before that I need to finish a small feature which is marring my ability to easily test anything

recall we had a requirement/discussion around a toolbar/icon/some visual indicating current role and ability to switch the role . 

