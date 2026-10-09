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
