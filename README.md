# Dental Clinic Management

A Java learning project for a dentist and receptionist. The first version will run on a laptop, with a web interface. An Android app will follow later and use the same backend.

## Milestone 1: start the application

This starter serves a welcome page. It does not yet have a database, login, or patient records.

1. Install Temurin JDK 21 for Windows from https://adoptium.net/temurin/releases/?version=21&os=windows. Enable the installer options for PATH and JAVA_HOME.
2. Install IntelliJ IDEA from https://www.jetbrains.com/idea/download/. Its free core Java features are sufficient for this milestone.
3. Open this folder in IntelliJ and load the Maven project from `pom.xml`.
4. Set the project SDK to JDK 21. Let IntelliJ download the Maven dependencies (internet is needed for the initial download).
5. Open `src/main/java/com/dentalclinic/ClinicApplication.java` and run its `main` method.
6. Open http://localhost:8080 in your browser. You should see the welcome page.
7. Stop the application using the IDE's Stop button when finished.

In a new PowerShell window, `java -version` and `javac -version` should both report version 21 after installation. A separate Maven installation is not required when using IntelliJ's bundled Maven.

## Understand the first milestone

- `pom.xml` describes the project, Java version, libraries, and build plugin. Maven downloads those libraries.
- `ClinicApplication.java` contains the Java entry point. `@SpringBootApplication` enables Spring Boot configuration, and `SpringApplication.run` starts the application and embedded web server.
- `application.properties` configures the server. For now it accepts connections only from this laptop on port 8080.
- `static/index.html` is the home page automatically served by Spring Boot. It uses HTML and CSS; there is no Java controller yet.

Exercise: change the welcome heading, restart the application, and refresh the browser. Explain which file controls the page and which file starts the server.

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
