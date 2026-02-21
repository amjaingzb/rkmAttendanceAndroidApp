### 1. `PROJECT_CONTEXT.md`
*Use this to explain **what** the app is and **how** it is built.*

--- START OF FILE PROJECT_CONTEXT.md ---
# Project Context: SevaConnect Halasuru (RKM Attendance App)

## 1. Project Overview
**Goal:** A native Android application to replace manual, paper-based systems for Event Attendance and Donation Collection at Ramakrishna Math, Halasuru. The app is offline-first, relying on a local SQLite database, with CSV-based import/export capabilities for data interchange.

## 2. Architecture & Tech Stack
*   **Language:** Java
*   **Architecture:** MVVM (Model-View-ViewModel).
*   **Data Layer:** Custom Repository Pattern (`AttendanceRepository`) wrapping raw SQLite DAOs.
*   **UI:** XML Layouts, Activity/Fragment based, ViewBinding/FindViewById.
*   **State Management:** `SharedViewModel` used for Admin UI state; `BackupStateManager` used for dirty-checking DB changes.
*   **Dependencies:** `OpenCSV` (Parsing), `Jackson` (JSON handling), `AndroidX` libraries.

## 3. Security & Roles (Privilege System)
The app operates on a 4-tier privilege system managed via PINs stored in the `app_config` table:
1.  **Operator (No PIN):** Can only mark attendance for the currently active event.
2.  **Donation Collector (PIN):** Can create donation batches, record donations, and close batches.
3.  **Event Coordinator (PIN):** Can manage future events and import attendance lists. Restricted from deleting history.
4.  **Super Admin (PIN):** Full CRUD access, DB backups, Settings, and Master Data management.

## 4. Core Features Implemented
*   **Smart Schedule:** Events have `active_from` and `active_until` timestamps. The app automatically launches Operator Mode if an event is currently active.
*   **Attendance:** Search-based check-in, "Walk-in" vs "Pre-reg" status, WhatsApp group gap analysis indicators.
*   **Donations:** "Batch" workflow. Volunteers start a batch, collect funds, and "Deposit/Close". Closing triggers an email intent with a CSV attachment of the session.
*   **Data Management:** 
    *   **Import:** Master Devotee List (CSV), Event Attendance (CSV), WhatsApp Group Map (CSV).
    *   **Export:** Full DB Backup (.zip), Reports (CSV).
    *   **Dirty Flag:** The UI (Cloud icon) visually indicates if the DB has changed since the last backup.

## 5. Database Schema (Version 5)
*   **`devotee`:** Master list (Name, Mobile, JSON extras).
*   **`event`:** Event metadata and active windows.
*   **`attendance`:** Links Devotee <-> Event.
*   **`donations`:** Financial records linked to Batches.
*   **`donation_batches`:** Session tracking for collectors.
*   **`whatsapp_group_map`:** Maps phone numbers to group IDs.
*   **`app_config`:** Key-value store for PINs and Templates.
--- END OF FILE PROJECT_CONTEXT.md ---

***

