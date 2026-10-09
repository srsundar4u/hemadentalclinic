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

Internet access is deferred at the user's request. No Cloudflare tunnel has been started. The optional scripts below are saved for a later setup and must only be run when public access is requested again. A phone's `localhost` means the phone itself; the Android app needs the laptop's reachable HTTPS URL.

For a future public-access setup, run:

```powershell
.\scripts\stop-clinic.ps1
.\scripts\start-clinic.ps1 -UseIntelliJCredentials -PublicHttps
.\scripts\start-public-access.ps1
```

The generated HTTPS URL appears in the terminal and `.deployment\public-url.txt`. Enter it in the Android app's **Server** field, sign in and book a synthetic appointment. Secure session cookies mean staff must use the HTTPS URL when the server runs in this mode, including on the laptop.

Stop external access with `.\scripts\stop-public-access.ps1`. To return to local browser use, stop the clinic and restart without `-PublicHttps`.

Quick Tunnels have temporary random addresses and are intended for testing, with no uptime guarantee. Restarting creates a new address to enter on the phones. A stable address for ongoing clinic use needs a separate domain/tunnel setup. See [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

The first Android version will embed these same staff screens in a Java WebView. Booking will use the existing Spring Boot validation and shared PostgreSQL database. It requires a connection to the laptop; it does not store an offline copy of patient records.

## Git

Code is committed locally. The personal GitHub repository is https://github.com/srsundar4u/hemadentalclinic. Uploading commits requires GitHub authentication on this laptop. Passwords, IDE configuration, packages, logs, database backups and downloaded build tools are ignored.
