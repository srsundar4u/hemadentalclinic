# Laptop deployment

The Spring Boot JAR includes Tomcat, the Java application and the web screens. PostgreSQL remains a separate Windows service. The laptop must be powered on, awake and connected for staff to access it.

## Package and run

From PowerShell in the project folder:

```powershell
.\scripts\package-clinic.ps1
.\scripts\start-clinic.ps1 -UseIntelliJCredentials
```

The second command explicitly reuses the three passwords already saved in the local IntelliJ `ClinicApplication` configuration. It passes them through environment variables to Java without printing them or writing a new password file. Without this switch, the script uses existing environment variables or asks for passwords with hidden input.

Open http://localhost:8080 and use the existing staff account. IntelliJ does not need to stay open. PostgreSQL must be running. Logs and the installed JAR are in the ignored `.deployment` folder. No automatic Windows startup is configured yet.

To stop, restart or update:

```powershell
.\scripts\stop-clinic.ps1
.\scripts\start-clinic.ps1 -UseIntelliJCredentials
# For a code update: stop, package, then start.
```

Do not run the IntelliJ application and the packaged application simultaneously on port 8080. The start script refuses to replace an existing server. The stop script only stops the recorded packaged clinic process.

## Internet access and Android

The server currently listens only on `127.0.0.1`. Remote access needs an HTTPS connection to this local service. Do not expose PostgreSQL or forward port 5432 to the internet.

A private VPN with HTTPS proxy is suitable for the two staff devices. A public HTTPS tunnel is another option for a synthetic-data demo. The choice and account setup are still pending. A phone's `localhost` means the phone itself; the Android app needs the laptop's reachable HTTPS URL.

The first Android version will embed these same staff screens in a Java WebView. Booking will use the existing Spring Boot validation and shared PostgreSQL database. It requires a connection to the laptop; it does not store an offline copy of patient records.

## Git

Code is committed locally. A GitHub repository URL and authentication are needed before pushing. Passwords, IDE configuration, packages, logs, database backups and downloaded build tools are ignored.
