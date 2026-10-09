# Dental Clinic Management

A Java learning project for a dentist and receptionist. The first version will run on a laptop, with a web interface. An Android app will follow later and use the same backend.

## Milestone 1: start the application

The application now has staff login, a protected welcome page and PostgreSQL connection verification on startup. Patient screens are next. Complete the database and staff account setup below before running the current version.

1. Install Temurin JDK 21 for Windows from https://adoptium.net/temurin/releases/?version=21&os=windows. Enable the installer options for PATH and JAVA_HOME.
2. Install IntelliJ IDEA from https://www.jetbrains.com/idea/download/. Its free core Java features are sufficient for this milestone.
3. Open this folder in IntelliJ and load the Maven project from `pom.xml`.
4. Set the project SDK to JDK 21. Let IntelliJ download the Maven dependencies (internet is needed for the initial download).
5. Open `src/main/java/com/dentalclinic/ClinicApplication.java` and run its `main` method.
6. Open http://localhost:8080 in your browser. You should see the welcome page.
7. Stop the application using the IDE's Stop button when finished.

In a new PowerShell window, `java -version` and `javac -version` should both report version 21 after installation. A separate Maven installation is not required when using IntelliJ's bundled Maven.

## Milestone 2: connect PostgreSQL

Create the local `dental_clinic` database and a login named `clinic_app`, with that login as database owner. PostgreSQL should listen on port 5432.

In IntelliJ:

1. Reload the Maven project after changes to `pom.xml` so the JDBC starter and PostgreSQL driver are downloaded.
2. Open Run > Edit Configurations and select the configuration for `ClinicApplication`.
3. Find Environment variables (use Modify options if it is hidden). Add a variable named `CLINIC_DB_PASSWORD`, with the password you chose for `clinic_app` as its value. Use the variables table to avoid separator issues in passwords.
4. Apply the changes, stop the existing application, and run it again.
5. Look for `Database connection verified: dental_clinic` in the Run console. Then open http://localhost:8080.

The password stays in your local IntelliJ configuration, which is ignored by Git. Do not paste it into source files or chat. A `.env` file is not automatically loaded by this project.

`spring.datasource.url` identifies the database host, port, and database name. `spring.datasource.username` selects the login. `${CLINIC_DB_PASSWORD}` reads the password from the environment. The JDBC starter supplies `JdbcTemplate` and a connection pool; the PostgreSQL driver lets Java communicate with PostgreSQL.

`DatabaseConnectionCheck` is a Spring component. Spring passes a configured `JdbcTemplate` into its constructor (dependency injection). Its `CommandLineRunner.run` method runs during startup and executes a read-only SQL query. If the connection fails, startup fails rather than silently displaying an unconnected home page. This check creates no tables and changes no records.

Troubleshooting: a missing `CLINIC_DB_PASSWORD` means the variable was not set in the selected run configuration. Authentication failure usually means the role password is incorrect. Connection refused means PostgreSQL is stopped or uses a different port. A nonexistent database means the database name or server does not match your pgAdmin setup.

## Understand the first milestone

- `pom.xml` describes the project, Java version, libraries, and build plugin. Maven downloads those libraries.
- `ClinicApplication.java` contains the Java entry point. `@SpringBootApplication` enables Spring Boot configuration, and `SpringApplication.run` starts the application and embedded web server.
- `application.properties` configures the server. For now it accepts connections only from this laptop on port 8080.
- The original milestone used `static/index.html`. Login now uses `PageController` and templates: `templates/login.html` and `templates/home.html`. The static index was removed so there is one home page to maintain.

Exercise: change the welcome heading in `templates/home.html`, restart the application, and refresh the browser. Explain which file controls the page and which file starts the server.

## Milestone 3: staff login

Reload Maven in IntelliJ. In Run > Edit Configurations > ClinicApplication > Environment variables, keep `CLINIC_DB_PASSWORD` and add:

| Variable | Value |
| --- | --- |
| `CLINIC_DENTIST_PASSWORD` | Your chosen password for username `dentist` |
| `CLINIC_RECEPTIONIST_PASSWORD` | Your chosen password for username `receptionist` |

These are application login passwords, separate from PostgreSQL's `clinic_app` password. Do not send passwords in chat or store them in tracked files. Leave Store as project file unchecked. There are no default staff passwords; missing variables prevent startup.

Stop the application and start it again. If an old static welcome page persists, use Build > Rebuild Project to remove stale build output. Visit http://localhost:8080: you should be redirected to `/login`. Test each username with its configured password. A wrong password displays an error. Sign out returns to the login page; opening `/` again requires sign-in.

How it works:

1. `SecurityConfiguration` protects application URLs, permits the login page and CSS, and configures Spring Security form login and logout.
2. `PageController` chooses the login/home template. The login submission is handled by Spring Security, not a controller that manually compares passwords.
3. `InMemoryUserDetailsManager` provides two accounts with DENTIST and RECEPTIONIST roles. Passwords are encoded with BCrypt during startup. Accounts are recreated from environment variables after a restart; account storage in PostgreSQL and password management are deferred.
4. Successful login creates a session. Thymeleaf inserts a CSRF token into POST forms, including logout. CSRF protection remains enabled.

Both roles currently access the protected welcome page. Per-screen permissions will be added as the respective screens are built. Development is still restricted to the laptop.

Automated login tests use fictional test passwords and mock database access. They verify anonymous redirects, both staff logins, invalid credentials, logout and CSRF enforcement without needing your database password. Run the Maven `test` goal from IntelliJ to execute them.

## Dashboard milestone

After reloading Maven and restarting, both staff accounts see today's appointment count, arrival count and collections amount. Click a card for its read-only details and use Back to dashboard to return. The dashboard itself has no detail tables.

Flyway runs `db/migration/V1__create_clinic_tables.sql` once against the empty `dental_clinic` database, creating patients, appointments and payments tables plus its migration-history table. No records are seeded. `clinic_app` must own the database and be able to create tables in its public schema. Do not edit an already-applied migration; future schema changes will use a new numbered migration. Do not enable baseline or delete existing tables to work around a migration error; inspect the database first.

Initially the totals are 0, 0 and ₹0.00 because there are no appointments or payments yet. Registration, booking and payment entry forms will be developed in subsequent screens. The detail pages already read existing rows when records are available.

`PageController` selects today's date in Asia/Kolkata and asks `DashboardService` for totals. `DashboardService` runs parameterized SQL through `JdbcTemplate`; Thymeleaf displays the result. Appointments count all bookings today, including cancelled/no-show. Arrivals count today's bookings with a recorded check-in, even after consultation/completion. Collections sum payments received today regardless of appointment date. Date boundaries are inclusive at today's midnight and exclusive at tomorrow's midnight. Stored appointment/payment timestamps represent clinic local time.

Tests use a separate in-memory H2 database in PostgreSQL compatibility mode to verify empty totals, midnight boundaries, completed arrivals and payments for older visits. This does not replace verification on your PostgreSQL installation after restart.

## Patient screens

Reload Maven and restart using the same environment variables. From the dashboard choose Patients. Search by a partial name or phone, or an exact numeric patient ID. Create new opens registration; successful registration redirects to the patient's profile with its generated ID. Edit details allows corrections. Name and phone are required. Phone must contain exactly 10 digits and cannot start with zero; date of birth and address are optional. Shared family phone numbers are allowed. The list displays up to 100 matches, ordered by name; refine the search for larger lists.

Try a fictional patient named Synthetic Patient with phone 9876543210. Save, search by the generated ID, open the profile, edit the name and restart to confirm persistence. No real records are automatically seeded. Clinical history and payment sections will follow with their own screens.

`PatientController` handles URLs and form validation. `PatientForm` carries validated input. `PatientRepository` runs parameterized SQL against PostgreSQL. `Patient` represents a saved record, and the templates display it. POST forms retain CSRF protection. Successful saves redirect to prevent duplicate submission on browser refresh. Patients have no delete action.

Tests cover create/search/edit persistence in a separate H2 test database, literal search wildcards, required input, login protection, CSRF, form rendering and unknown patient IDs. Verify saving on local PostgreSQL before committing this milestone.

## Appointment screens

Restart the application. Choose Appointments from the dashboard, or Book appointment on a patient's profile. Pick a future local date/time and a duration from 5 to 240 minutes. The appointment list supports date, status and checked-in filters. Click a time to open appointment details. Scheduled bookings can be rescheduled or cancelled. Arrival is allowed only on the booked date; no-show is allowed only after the start time. Consultation and completion actions come with the next screen.

The dashboard appointment/arrival cards now open the appointment list with the appropriate filter. Patient profiles show their appointment history. The booking patient field searches the database as you type a name, phone or exact patient ID and shows up to 20 suggestions per query. Narrow the search and select a suggestion: typing a name alone does not select a patient. This searches all patients, including those beyond the first 100 in the patient list. JavaScript provides the suggestions; the server validates the selected patient ID again. Rescheduling keeps the original patient.

`AppointmentController` handles pages and validated input. `AppointmentService` checks existing patients, future times, overlapping slots and allowed status changes. `AppointmentRepository` reads/writes PostgreSQL. Its schedule lock runs inside a Spring transaction so simultaneous booking requests cannot reserve overlapping slots. The initial schedule is for one dentist. Adjacent slots are allowed; cancelled/no-show bookings free their slots. Arrived bookings cannot be rescheduled or cancelled in this first flow.

Use a synthetic patient: book a time later today, try an overlapping booking (expect an error), reschedule, mark arrived and check the dashboard arrival total. Test cancellation on another scheduled booking. The tests cover overlap boundaries, rescheduling, state changes, persistence, form rendering and CSRF; the PostgreSQL table lock needs verification on the local PostgreSQL installation.

## Consultation / Treatment screen

Restart the application. Flyway applies V2 to create the consultations table without changing existing patients or appointments. Sign in as dentist, open an arrived appointment and select Start consultation. This changes its status to IN_CONSULTATION. Record complaint, relevant medical history, allergies, examination, treatment and follow-up recommendation/date. Save draft allows incomplete notes. Complete visit requires complaint and treatment, saves everything and changes the appointment to COMPLETED; completed records are read-only.

Follow-up dates cannot be in the past. Recommendations do not automatically create appointments. Use the patient profile's Book appointment action to schedule a follow-up. Clinical visit history links appear only for the dentist. Receptionist requests to consultation URLs are denied by Spring Security, and clinical notes are not loaded into the receptionist's profile view.

`ConsultationController` selects the page and validates form fields. `ConsultationService` enforces the visit lifecycle in a transaction. `ConsultationRepository` saves notes in PostgreSQL. A version number prevents stale browser tabs from overwriting more recent notes; the page asks you to copy notes and reload if another save changed the visit. Start is a POST action, so merely opening a page does not change an appointment. State changes preserve the original arrival timestamp.

Manual check: use a synthetic arrived appointment, save a draft, reopen it, complete with complaint/treatment and confirm read-only notes and COMPLETED status. Sign in as receptionist to verify the clinical links are absent and consultation URLs are forbidden. Account credentials stay the same. Billing follows later.

### Prescription tab and printing

Restart to apply V3, which adds a prescription field to existing consultations with an empty default. The consultation page has Consultation / Treatment and Prescription tabs. Enter clinician-authored medicine details and instructions in the Prescription tab. Save draft and Complete visit save both tabs together. Save and print prescription saves the draft and redirects to a saved-text print preview. Empty prescriptions cannot use Save and print. JavaScript enhances tabs; without it both sections remain visible.

Use Print / Save as PDF in the preview to open the browser's print dialog. Print styling targets A4 and hides controls. The printout includes only the clinic name, patient name/ID, visit date, prescription and signature space; it does not include medical history, allergies or examination notes. Drafts have a visible DRAFT label. Completed visits are read-only and can reprint their saved prescription. Enter the prescription before completing the visit. The software does not generate medication advice or a doctor's signature.

This is a basic demo prescription layout. Clinic address and professional registration details have not been supplied; they are not invented. Verify long prescriptions in browser print preview for pagination. Disabling browser headers/footers removes the URL/date added by the browser. Prescription URLs remain dentist-only.

## Billing and Payments

Restart to apply V4, creating charges and adding duplicate-submission tokens to payments. Both staff accounts can open Billing and payments from the dashboard, completed appointment or completed consultation. Search the billing list by patient name or ID; it shows up to 100 completed visits. Open a visit to add a charge description and amount. Add multiple charges if needed, then record partial or full payments using Cash, UPI or Card.

Amounts must be positive with at most two decimal places. Payments cannot exceed the remaining balance. Transactions lock the appointment while checking the balance and recording a payment, preventing simultaneous payments from exceeding it. Each submitted form has a unique request token to avoid duplicate entries from double submission; a successful save redirects to a fresh form. Records have no edit/delete action in this milestone; corrections/refunds are a future feature. No actual payment processing occurs.

The patient profile shows total charges, paid amount and remaining balance across visits. The dashboard's collections uses payment receipt date, including payments for older completed visits. Receipt timestamps are generated in Asia/Kolkata when the payment is saved; backdated entry is not included yet. Patient clinical notes and prescriptions do not appear on billing pages. Clinic app permissions restrict clinical access as before.

Manual check with synthetic data: complete a visit, charge ₹1000, record ₹400, verify ₹600 outstanding, record ₹600 and verify Paid in full. Check the patient's account totals and dashboard collections. Trying a payment above the balance should show an error. Forms, arithmetic, payment validation, duplicate submissions and persistence are covered by automated tests; verify locally on PostgreSQL before committing.

## Daily checks

Restart to apply V5 and open Daily checks from the dashboard. The same page records two daily confirmations for a selected date: Sterilization (Pending, Completed, Not required) and Required lab work sent (Pending/not sent, Sent, No lab work to send). Remarks are optional. Both staff roles can save. The page shows the latest saving account and timestamp. Open a past date to review or correct its checklist; future dates cannot be saved.

One checklist is stored per date. A new date starts Pending for both items, with no saved record until submitted. Pending work from previous dates remains in that date's record; it is not automatically carried forward. These are staff-entered daily confirmations, not individual sterilization cycles or patient-specific lab orders. More checklist items and detailed tracking can be added later.

The version field rejects stale updates from another browser tab or staff member. A conflict asks the user to copy remarks and reload. The repository uses an atomic insert or version-checked update. `DailyCheckController` validates input and uses the signed-in account for the saving username. Tests cover persistence by date, concurrent initial saves, stale edits, both staff roles, future-date rejection and CSRF.

## Enhanced patient details

Restart to apply V6, adding nullable sex, weight_kg and height_cm columns while preserving existing records. Registration and Edit details support sex (Male, Female, Other, or Not provided), optional weight in kilograms and optional height in centimetres. Entered measurements must be positive and use at most two decimal places. Clearing a measurement stores no value rather than zero.

The profile calculates age in completed years from date of birth using today's date in Asia/Kolkata. Age is not stored, so it stays current as birthdays pass. Missing birth date displays Not provided. Existing patients can be updated through Edit details. These measurements are current profile values, not a visit-by-visit measurement history.

## Planned increments

1. Start the server and display the home page.
2. Connect local PostgreSQL; register and search synthetic patients.
3. Book, reschedule, cancel, and check in appointments.
4. Record consultation notes and treatments.
5. Record charges and payments; implement dentist/receptionist logins and permissions.
6. Verify the full workflow, configure backups, and enable clinic Wi-Fi access.
7. Build an Android Java client using the same backend.

The daily workflow is registration/booking, arrival, consultation/treatment, payment, and follow-up booking. Real clinic use requires completed access controls, backup and restore verification, and workflow validation. This starter is not ready for real patient data.

## Data and source control

PostgreSQL will store records locally on the laptop. GitHub will store source code only. Keep patient exports, database backups, and passwords outside the repository. Git is available through the Codex runtime on this machine; verify it in your own terminal before relying on it. A personal GitHub repository has not yet been created or connected.

## Laptop as the clinic server

After login and permissions are implemented, we will enable access from other clinic devices and configure a Windows firewall rule scoped to the clinic network. Devices will use the laptop's local network address rather than `localhost`. The laptop must remain powered on, awake, and reachable, with the application and PostgreSQL running. Remote internet access and paid hosting are outside the initial setup.

