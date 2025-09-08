
# Requirements Document

## A. Problem Background
1. Events (spiritual retreats, meditation sessions, etc.) are organized by Ramakrishna Math.  
2. Registration links are sent beforehand.  
3. On event day:  
   3.1. Not all preregistered people attend.  
   3.2. Many walk-ins arrive without preregistration.  
4. Attendance is taken for everyone.  
5. Spot registrations are recorded manually.  
6. Current process:  
   6.1. Print preregistration list.  
   6.2. Tick attendance manually on paper.  
   6.3. Add spot registrations manually.  
   6.4. Digitize everything later.  

## B. Issues
1. Manual, paper-based attendance and spot registration.  
2. Duplication of effort (manual first, then digitization).  
3. Prone to errors, inefficient.  

## C. Goal
1. Create an app that supports:  
   1.1. Registrations (pre-event). **(P1)**  
   1.2. Attendance (on event day). **(P1)**  
   1.3. Spot registrations (on event day). **(P1)**  
2. Eliminate paper-based duplication. **(P1)**  

## D. Functional Model

### D.1 Data Input & Master Database
1. Allow import of old attendance sheets and current event registration sheets via CSV. **(P1, optional)**  
2. Purposes of CSV import:  
   2.1. Initialize the Master DB. **(P1)**  
   2.2. Enrich the Master DB (add missing fields or new entries). **(P1)**  
   2.3. Supply attendance stats like recency/frequency (for future prioritization). **(P2)**  
3. Build a **master database (hashmap)** from CSV data (if any). **(P1)**  
4. Use **Name + Mobile number** as primary key. **(P1)**  
   4.1 Normalize mobile number - strip country code if present. **(P1)**  
5. Retain all fields from CSV (address, Aadhaar, sex, etc.) even if not in use. **(P2)**  
6. Capabilities:  
   6.1. Merge duplicate entries across CSVs. **(P1)**  
   6.2. If conflicts exist, retain duplicates and allow Admin to resolve later. **(P2)**  
   6.3. Maintain counters **cumulative attendance, last attendance date** per person/key. **(P2)**  
   6.4. Show **Master DB Health**:  
        - Number of records. **(P2)**  
        - Pending merge conflicts. **(P2)**  

### D.2 Modes of Operation

#### D.2.1 Admin Mode
1. Ingest CSV files and create/update master DB. **(P1)**  
2. Event management:  
   2.1. Create new event attendance. **(P1)**  
   2.2. Event name auto-suggested from last registration data but editable. **(P2)**  
   2.3. View list of ingested CSVs, past and present attendance event data, past and present on-spot registrations. **(P5)**  
   2.4. Delete specific event data. **(P2)**  
3. Data export:  
   3.1. Export master DB. **(P1)**  
   3.2. Export event attendance (for current event). **(P1)**  
4. Master data editing:  
   4.1. View, edit master records. **(P2)**  
   4.2. Conflict resolution interface. **(P2)**  

#### D.2.2 Operator Mode
1. Take attendance for **current event** (created in Admin mode). **(P1)**  
2. Add spot registrations if any with just **Name + Mobile required**, other fields optional. **(P1)**  
3. Spot registration automatically marks attendance as Present. **(P1)**  
4. Attendance flow:  
   4.1. Operator searches by name or mobile. **(P1)**  
   4.2. If entry found and pre-registered → mark attendance. **(P1)**  
   4.3. If entry found in Master DB but not pre-registered → create spot registration record (P5) + mark attendance. **(P1)**  
   4.4. If entry not found in Master DB → create new Master record (operator may fill details or skip) + create spot registration record (P5) + mark attendance. **(P1)**  

### D.3 Search & Prioritization
1. All searches use master DB. **(P1)**  
2. Search capabilities:  
   2.1. **Name-based search:**  
        - Match on first, middle, last names. **(P1)**  
        - Handle salutations (Sri, Mr, Dr, etc.). **(P2)**  
        - Optional **fuzzy matching** (configurable). **(P2)**  
        - Custom fuzzy rules (e.g., Amit ↔ Amith, Srinivas ↔ Shrinivas/Srinivaas). **(P2)**  
   2.2. **Mobile-based search:**  
        - Strip country codes before comparing. **(P1)**  
   2.3. **If multiple results found, display order:**  
        - (a) Entry most active recently. **(P2)**  
        - (b) Entry most active overall. **(P2)**  
        - (c) Other entries. **(P2)**  
   2.4. Checkbox to enable/disable fuzzy matching during search. **(P2)**  
3. Result handling:  
   3.1. In **P1**, simply list results in Master DB order.  
   3.2. In **P2**, apply prioritization rules.  
4. If entries are found, operator can select candidate entry to mark attendance. **(P1)**  

### D.4 Terminology
1. **Master Record (Devotee Profile):** Persistent profile in Master DB with:  
   1.1. Mandatory fields: Name, Mobile.  
   1.2. Optional fields: Age, Address, Sex, Aadhaar, PAN, etc.  
   1.3. Meta data: cumulative attendance counter, lastEventAttendDate.  

2. **Event Registration Record:** Per-event entry:  
   2.1. Pre-registration record (from frozen CSV imported before event).  
   2.2. Spot registration record (created if person attends without pre-reg). **(P5)**  

3. **Attendance Record:** The check-in mark for an event. Must exist for all attendees, regardless of pre-reg or spot-reg.  

### D.5 Lifecycle
1. **Initialize Master DB**  
   1.1. Import historical attendance CSVs or other data CSVs.  
   1.2. Master DB is enriched with devotees, counters, metadata.  

2. **Before Each Event**  
   2.1. Admin creates new event in Admin Mode.  
   2.2. Pre-registrations are collected **outside the app** (e.g., Google Form).  
   2.3. Once submissions close, pre-reg list is frozen and exported as CSV.  
   2.4. Admin imports the pre-reg CSV into the app, fixing the preregistration list for that event.  

3. **On Event Day**  
   3.1. Operator searches by name or mobile.  
   3.2. If found in Master DB & pre-registered → mark attendance.  
   3.3. If found in Master DB but not pre-registered → create spot registration record (P5) + mark attendance.  
   3.4. If not found in Master DB → create new Master record (optionally fill details) + create spot registration record (P5) + mark attendance.  
   3.5. Operator may update missing details or update old mobile if provided.  

4. **After Event**  
   4.1. Master DB is updated with new attendance counters and lastEventAttendDate.  
   4.2. Admin can export Master DB, attendance for current event, and (P5) spot registration statistics.  

## E. Future Enhancements
1. <TBD>


## Update as on Sep 7, 2025

### 🔴 Missed Requirements

These are features specified in the requirements document that do not appear to have a corresponding implementation in the code.

#### Priority 1 (P1) Misses
These are the most critical features that were specified as core functionality but are missing.

*   **D.2.1.3.1 Export master DB (P1):** There is no functionality in the UI (`ImportFrame` or `EventFrame`) to export the entire master devotee database to a file (e.g., CSV). {ADDED}
*   **D.2.1.3.2 Export event attendance (P1):** Similarly, there is no option to export the attendance list for a specific event.{ADDED}

#### Priority 2 (P2) Misses
These are less critical or "future enhancement" features that were documented but not implemented.

*   **D.1.2.3 & D.1.6.3 Maintain Devotee Attendance Counters (P2):** The `devotee` table schema in `Database.java` is missing the specified `cumulative_attendance` and `last_attendance_date` fields. Consequently, there is no logic to update these statistics after an event. {ADDED}
*   **D.1.6.2 Conflict Resolution System (--Not Applicable Anymore--):** The system is designed to automatically merge or create new devotees based on fuzzy logic. It does not implement the specified feature of retaining duplicates with conflicting data for later manual resolution by an Admin. There is no "pending merge conflicts" state.
*   **D.1.6.4 Master DB Health View (P2):** There is no UI component that displays statistics like the total number of records {or the number of pending merge conflicts. -- N/A -- }
*   **D.2.1.2.2 Event Name Auto-Suggestion (P2):** The "Add Event" dialog is a simple form; it does not auto-suggest an event name based on previous data.
*   **D.2.1.4.2 Conflict Resolution Interface (P2):** Corresponding to the lack of a conflict state, there is no UI for an administrator to review and resolve data conflicts. {N/A} 
*   **D.3.2.1 Handle Salutations in Search (P2):** The `DevoteeDao.normalizeName` method standardizes spacing and case but does not include specific logic to strip common salutations (e.g., Sri, Mr, Dr). {Not really needed}
*   **D.3.2.1 & D.3.2.4 Optional/Configurable Fuzzy Search (P2):** The live search in `MarkAttendanceDialog` uses a standard SQL `LIKE` query. It does not implement the advanced, configurable fuzzy matching specified for operator searches. The fuzzy logic is only used during data *import and creation*, not during the operator's real-time search. { NOT-REALLY-NEEDED-ANYMORE}
*   **D.3.2.3 Prioritized Search Results (P2):** The search results in `MarkAttendanceDialog` are ordered alphabetically by name (`ORDER BY d.full_name`). They are not prioritized by recency or frequency of attendance, as the underlying data for this is also missing. { DONT-THINK-ITS-NEEDED}

#### Priority 5 (P5) Misses
This priority level appears in a few places and seems to indicate lower-priority features.

*   **D.2.1.2.3 View List of Ingested CSVs (P5):** The application does not keep or display a log of which CSV files have been imported.

---

### 🟡 Altered or Partially Implemented Requirements

These are features where the implementation exists but differs from the specification in a notable way.

*   **D.1.3 Master Database as "hashmap" (P1):**
    *   **Specification:** The document conceptually describes the master database as a "hashmap".
    *   **Implementation:** The code uses a persistent **SQLite database**. This is a significant improvement over an in-memory hashmap, providing data persistence, but it is an architectural alteration.
*   **D.3.3.1 Search Result Order (P1):**
    *   **Specification:** "In P1, simply list results in Master DB order."
    *   **Implementation:** The results are explicitly sorted alphabetically by `full_name`. This is a minor and reasonable alteration that provides a more user-friendly default order than a potentially arbitrary database order.
*   **Lifecycle - Pre-registration Import (P1):**
    *   **Specification:** The lifecycle section (D.5) implies a clear distinction between importing to the Master DB and importing a specific pre-registration list for an event.
    *   **Implementation:** The code provides two importers. `CsvImporter` is for the master list (in `ImportFrame`). `AttendanceImporter` is for an event's attendance list (in `EventFrame`). While both exist, the UI flow for an Admin to "import the pre-reg CSV into the app, fixing the preregistration list for that event" is handled by the "Import Attendance CSV" button, which directly marks people as present rather than just loading a list of pre-registrants. This slightly combines the "pre-registration list" and "attendance marking" steps.

---

### ✅ Successfully Implemented Requirements

It is also important to note that the vast majority of the core **P1 (Priority 1)** requirements that define the application's main purpose have been successfully and robustly implemented.

*   **Core App Goal (C.1):** The app successfully supports pre-event registration (via import), on-day attendance marking, and on-day spot registrations.
*   **Master DB Creation (D.1.1):** The `ImportFrame` and `CsvImporter` effectively build and enrich the master database from CSV files.
*   **Primary Key and Normalization (D.1.4):** The `mobile_e164` and `name_norm` unique index enforces the "Name + Mobile" primary key, and the normalization functions are used consistently.
*   **Duplicate Merging (D.1.6.1):** The sophisticated, multi-stage fuzzy matching logic in `DevoteeDao.resolveOrCreateDevotee` is a cornerstone of the implementation and directly addresses this requirement.
*   **Admin Mode - Event Management (D.2.1.2.1):** Admins can create, view, edit, and delete events through the `EventFrame`.
*   **Operator Mode - Full Workflow (D.2.2):** The entire on-day attendance flow is implemented as specified, from searching by name/mobile to marking attendance and handling all cases for spot registrations.


That's an excellent and perfectly accurate summary of our progress. Your assessment is spot on.

Let's formalize this into a clear status update.

### Confirmation of Completed / De-scoped Items

You are correct that the following requirements are now either **complete** or have been **intentionally de-scoped**:

*   ✅ **Export Event Attendance (P1):** **Completed.** The "Attendance by Event" tab in the Reports section directly fulfills this.
*   ✅ **Export Master DB (P1):** **Partially Completed.** You can now export *active* devotees. The spirit of the requirement (getting devotee data out) is met. A report for *all* devotees is a minor addition.
*   ✅ **Maintain Devotee Attendance Counters (P2):** **Completed.** This is now handled by on-demand queries in the enriched devotee views and reports, which is a superior design.
*   ✅ **Master DB Health View (P2):** **Completed.** The "Counters" tab in the reports provides the total number of devotee records.
*   ❌ **Conflict Resolution System / Interface (P2):** **De-scoped.** As you noted, the decision to use a fully automatic fuzzy-matching and creation logic makes a manual conflict resolution system unnecessary.
*   ❌ **Salutations / Fuzzy Search / Prioritized Search (P2):** **De-scoped.** You've determined that the current `LIKE` search with alphabetical sorting is sufficient for the application's needs, making these "nice-to-have" features unnecessary for now.

---

### Remaining Gaps

Based on your summary, we have just **two outstanding requirements** from the original document:

1.  **Export *All* Devotees (P1 Refinement):** While the "Active Devotees" report is great, a report/export for the *entire* master devotee list would fully satisfy the original P1 requirement `D.2.1.3.1`. This is a very small addition. {DONE}
2.  **Event Name Auto-Suggestion (P2):** The requirement `D.2.1.2.2` to auto-suggest an event name based on previous data is the only remaining P2 item that hasn't been implemented or de-scoped.

