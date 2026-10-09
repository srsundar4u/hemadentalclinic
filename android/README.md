# Dental Studio Android demo

This is a Java Android application that embeds the existing responsive Spring Boot screens in a WebView. It shares login, patients, appointment booking and validation with the website. No second database or offline booking queue is added.

## Use

1. Install `dental-studio-demo.apk` from the project's `.deployment` folder on an Android 8.0 or newer phone. This debug installer is for the private learning demo, not a Play Store release.
2. Enter the laptop's HTTPS server address when prompted. With private access, the phone must be signed in to the selected VPN too. Remote access must be configured first.
3. Sign in as `dentist` or `receptionist` using the existing password.
4. Tap **Book**, search and select a synthetic patient, choose a future date/time, and submit. The appointment should also appear in the laptop browser.
5. On a saved prescription page the native **Print** button opens Android's print dialog. Choose a printer or Save as PDF. The web page's browser print button alone is intended for desktop browsers.

The **Server** button changes the connection address and clears session cookies. The app only loads its configured HTTPS server internally. Other HTTPS links open in the browser; invalid certificates and unencrypted HTTP are not allowed. Clinic records are not backed up by Android app backup.

## Learn the code

`app/src/main/java/com/dentalclinic/mobile/ClinicActivity.java` creates the toolbar, configures the WebView, validates the HTTPS server address and opens Android printing. `AndroidManifest.xml` declares internet access and the launch activity. `app/build.gradle` selects Java 17 syntax and Android API 35; the build runs using the installed JDK 21.

The backend stays in the root Maven project. Android uses Gradle because it packages Java code and resources into an APK rather than a Spring Boot JAR.

## Build

On this laptop, approved Android command-line tools, SDK API 35, build tools 35.0.0 and Gradle 8.11.1 are stored in the ignored `.tools` folder. Run from the project root:

```powershell
.\scripts\build-android.ps1
```

On another laptop, install Android Studio, accept the SDK licence, install API 35 and open the `android` folder. Gradle 8.11.1 and Android Gradle Plugin 8.9.2 are required. Do not commit SDK paths, downloaded tools or APK files. A release signing key and a distribution plan are separate later steps.
