param(
    [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot',
    [string]$Maven = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
)
$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicOldJavaHome = $env:JAVA_HOME
Push-Location $clinicRoot
try {
    $env:JAVA_HOME = $JavaHome
    & $Maven --batch-mode --no-transfer-progress "-Dmaven.repo.local=$clinicRoot\.maven-cache" verify
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. The installed package has not been replaced.' }
    $clinicDeployment = Join-Path $clinicRoot '.deployment'
    New-Item -ItemType Directory -Force -Path $clinicDeployment | Out-Null
    Copy-Item -LiteralPath (Join-Path $clinicRoot 'target\clinic-management-0.0.1-SNAPSHOT.jar') -Destination (Join-Path $clinicDeployment 'clinic.jar')
    Write-Host 'Package ready in .deployment\clinic.jar'
} finally { $env:JAVA_HOME = $clinicOldJavaHome; Pop-Location }
