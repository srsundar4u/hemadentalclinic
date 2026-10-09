param([string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot')
$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicOldJava = $env:JAVA_HOME
$clinicOldSdk = $env:ANDROID_HOME
$clinicOldGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $JavaHome
    $env:ANDROID_HOME = Join-Path $clinicRoot '.tools\android-sdk'
    $env:GRADLE_USER_HOME = Join-Path $clinicRoot '.tools\gradle-cache'
    $clinicGradle = Join-Path $clinicRoot '.tools\gradle-8.11.1\bin\gradle.bat'
    if (!(Test-Path $clinicGradle)) { throw 'Gradle is missing. See android\README.md for setup.' }
    & $clinicGradle -p (Join-Path $clinicRoot 'android') --no-daemon --console=plain assembleDebug lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build or lint failed.' }
    $clinicOutput = Join-Path $clinicRoot '.deployment'
    New-Item -ItemType Directory -Force -Path $clinicOutput | Out-Null
    Copy-Item -LiteralPath (Join-Path $clinicRoot 'android\app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $clinicOutput 'dental-studio-demo.apk')
    Write-Host 'Android demo installer: .deployment\dental-studio-demo.apk'
} finally {
    $env:JAVA_HOME = $clinicOldJava
    $env:ANDROID_HOME = $clinicOldSdk
    $env:GRADLE_USER_HOME = $clinicOldGradle
}
