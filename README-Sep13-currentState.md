Of course. This is the perfect time to take a full, meticulous stock of the project. It will serve as an excellent "source of truth" and a testament to the robust, user-centric application we have built through our iterative process.

I will be as detailed as possible, checking our entire history to capture every requirement, decision, and learning.

---

### **Project Debrief & Status Report: RKM Attendance App**

This document provides a comprehensive overview of the project's requirements, the current implementation status, a list of pending features with their agreed-upon designs, and a summary of the key challenges and learnings from the development process.

### **Part I: The Project's Journey & Evolution**

#### **1. Genesis: The Original Goal**

The project began with a clear mission: to replace an inefficient, paper-based system for managing event attendance. The core problems to be solved were data entry errors, duplication of effort (manual ticking followed by digitization), and a lack of real-time data on event day. The initial concept was a desktop application with two primary user roles: an **Admin** for setup and a volunteer **Operator** for on-site execution.

#### **2. Key Design Evolutions & Critical Decisions**

As we ported the application to Android, our collaborative discussions led to several critical design improvements that have fundamentally shaped the final product. These were not just feature additions but significant enhancements to the core logic and user experience.

*   **From Manual Flag to "Smart Schedule":** We discarded the initial idea of an Admin manually flagging an event as "active." We replaced it with a much more robust and error-proof system using an "Active Window" (`active_from_ts` and `active_until_ts`). The app now *automatically* determines the active event based on the current time, and we added a data integrity rule to prevent admins from creating events with overlapping time windows.

*   **From Two Modes to a Three-Tiered Privilege System:** We evolved the simple "Admin/Operator" split into a more granular and secure three-role system, each with specific, logical permissions:
    1.  **Operator:** The default, no-login mode for on-site attendance marking.
    2.  **Event Coordinator:** A mid-level admin (PIN protected) who can manage upcoming events but cannot alter the master devotee list or delete historical data.
    3.  **Super Admin:** The highest level (separate PIN), with full CRUD access to all data, ensuring a clear separation of duties.

*   **From Ambiguous to Explicit Attendance Schema:** We identified that a simple counter (`cnt`) for attendance was insufficient. We added a `reg_type` column to the `attendance` table to explicitly distinguish between a "PRE_REG" devotee (who has not yet arrived) and a "SPOT_REG" devotee (a walk-in). This was critical for both accurate reporting and for the Operator's UI.

*   **From Hiding to Disabling (A Superior UX):** Based on your excellent feedback, we refined our privilege rules. Instead of hiding the "Edit" button for past events from a Coordinator (which is confusing), we now **disable and grey it out**. This transparently communicates the rule to the user without causing confusion.

*   **From Static to "Refresh-in-Place" (Operator Search):** We improved the operator's workflow. Instead of clearing the search results after marking someone present, the UI now refreshes in place. The devotee who was just marked present instantly changes their status to a grey "Present" badge and moves to the bottom of the list, providing immediate, clear feedback.

### **Part II: Current State of the Application (Implemented Features)**

The application is currently stable and the core features are robustly implemented.

*   **Architecture:** The app is built on a modern and resilient `Fragment -> ViewModel -> Repository -> DAO` architecture, with a clean separation between the UI and the core business logic. We have successfully refactored the admin section to use a **Shared ViewModel** for state management, making it immune to Android lifecycle issues.

*   **Core Backend:** The entire headless library (DAOs, Repository, Models, Importers) is fully functional on Android. Database operations for events, devotees, and attendance are working as designed, including fuzzy matching and data merging.

*   **Privilege System & Login Flow:** The entry point to the application is complete and working correctly.
    *   `LauncherActivity` correctly routes the user to Operator Mode if an event is active, or to the `RoleSelectionActivity` if not.
    *   The PIN entry screen (`PinEntryActivity`) functions correctly, using a native implementation that is stable and reliable.
    *   The `AdminMainActivity` correctly receives the privilege level and persists it across configuration changes using the `AdminViewModel`.
    *   A consistent "Switch Role / Logout" feature is implemented in all modes.

*   **Operator Mode (`MarkAttendanceActivity`):** This screen is feature-complete and polished.
    *   The "Smart Search Bar" provides clear, four-state feedback to the user (pristine, insufficient input, searching, results).
    *   Search results correctly display the three states: **Pre-Reg (Green)**, **Walk-in (Blue)**, and **Present (Gray)**, with "Present" items correctly disabled and sorted to the bottom.
    *   The "refresh-in-place" logic is working.
    *   On-spot registration and data merging work correctly.
    *   The stats panel is now visually corrected and readable.

*   **Admin Mode (`AdminMainActivity`):** The privilege-aware UI is now fully implemented.
    *   The toolbar subtitle correctly displays the current role.
    *   The "Devotees" tab is correctly hidden for Event Coordinators.
    *   The "Delete Event" option is correctly hidden for Event Coordinators.
    *   The "Edit Event" option is correctly disabled for Event Coordinators when viewing a past event.
    *   The event list is correctly sorted by Future, Present, and Past.
    *   The check preventing the creation of overlapping events is implemented and provides user feedback.

### **Part III: Pending Requirements & `TODO` Items**

This is the complete list of remaining work based on our discussions.

1.  **PIN Management (Next Major Feature):**
    *   **Requirement:** The Super Admin needs the ability to change the PIN for both the Super Admin and Event Coordinator roles.
    *   **Conclusion Reached:** This will require a new screen or dialog, accessible only to the Super Admin (likely from the "Reports" or main menu). It will need fields for "Old PIN" (for verification), "New PIN", and "Confirm New PIN". The `ConfigDao` will need new methods to update the PINs in the `app_config` table.

2.  **CSV Imports:**
    *   **Requirement:** Implement the UI for importing the Master Devotee List and the per-event Attendance Lists.
    *   **Conclusion Reached:** This will require a new `MappingActivity` where the user can map CSV columns to database fields, just like the desktop app. The menu items ("Import Master Devotee List" in the Devotee screen and "Import Attendance" in the Event Actions sheet) are already in place as entry points.

3.  **Reports Tab Expansion:**
    *   **Requirement:** The Reports tab currently only shows basic stats. It needs to be built out to include the other designed reports.
    *   **Conclusion Reached:** We need to implement the UI and logic for the remaining reports: "Full Devotee List," "Active Devotees," and "Attendance by Event," including the necessary export-to-CSV functionality.

4.  **Deferred UI/UX Polish:**
    *   **Requirement:** Improve the visual hierarchy of the `RoleSelectionActivity` buttons (the one you provided a screenshot for).
    *   **Requirement:** Improve the readability of the "Checked-in Attendees" title in `MarkAttendanceActivity` (e.g., increase font size, add a background).
    *   **Requirement:** If the launcher finds multiple active events for today, it should show a "Today's Schedule" screen. This was deferred in favor of the core logic.
    *   **Conclusion Reached:** These are all valid polish items that we have explicitly decided to defer until all core logic is complete.

### **Part IV: Key Learnings & Challenges ("Running in Circles")**

This project has been a fantastic case study in robust software development. Our iterative process of building, testing, and refining has been crucial. The periods where we seemed to "run in circles" were invaluable learning experiences that led to a much stronger final product.

1.  **Learning 1: The Danger of Unstable Third-Party Dependencies.**
    *   **Challenge:** Our biggest time sink was the repeated failure of external PIN entry libraries hosted on JitPack. We wasted significant time on build errors and Gradle configuration issues.
    *   **Lesson:** For simple, critical UI components, building them natively (`EditText`s) can be far more reliable than fighting with an unstable dependency. The principle of **preferring Maven Central over JitPack** is a key takeaway for future projects.

2.  **Learning 2: The "Porting Blind Spot" - Logic in the UI Layer.**
    *   **Challenge:** We encountered a critical bug where new devotee data was being discarded because the complex "merge" logic was part of the old desktop app's UI code and was not ported.
    *   **Lesson:** When porting an application, one must be vigilant for business logic that is "hiding" in the UI controller layer. The best practice, which we have now adopted, is to **extract this complex orchestration logic into the Repository layer**, making it a reusable and robust part of the core library.

3.  **Learning 3: The Primacy of Robust State Management.**
    *   **Challenge:** The "disappearing delete button" was a classic Android bug caused by the Activity Lifecycle destroying and recreating our UI, wiping out our fragile instance variables.
    *   **Lesson:** This forced us to refactor to a **Shared ViewModel**, which is the modern, correct architectural pattern for managing state that needs to be shared between an Activity and its Fragments. This has made the entire admin section fundamentally more stable and resilient.

This meticulous process has resulted in a well-architected and robust application. We are now in an excellent position to complete the remaining features.
