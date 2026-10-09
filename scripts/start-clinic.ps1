param(
    [switch]$UseIntelliJCredentials,
    [switch]$PublicHttps,
    [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
)
$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicDeployment = Join-Path $clinicRoot '.deployment'
$clinicJar = Join-Path $clinicDeployment 'clinic.jar'
$clinicJava = Join-Path $JavaHome 'bin\java.exe'
if (!(Test-Path $clinicJava)) { throw 'Java 21 was not found. Supply -JavaHome with your JDK folder.' }
if (!(Test-Path $clinicJar)) { throw 'Package not found. Run scripts\package-clinic.ps1 first.' }
$clinicSocket = [System.Net.Sockets.TcpClient]::new()
try {
    $clinicSocket.Connect('127.0.0.1', 8080)
    throw 'Port 8080 is already in use. Stop the existing clinic application before starting this package.'
} catch [System.Net.Sockets.SocketException] {
    # No server is listening on the clinic port.
} finally { $clinicSocket.Dispose() }
$clinicSecrets = @('CLINIC_DB_PASSWORD', 'CLINIC_DENTIST_PASSWORD', 'CLINIC_RECEPTIONIST_PASSWORD')
$clinicPrevious = @{}
try {
    foreach ($clinicName in $clinicSecrets) { $clinicPrevious[$clinicName] = [Environment]::GetEnvironmentVariable($clinicName, 'Process') }
    if ($UseIntelliJCredentials) {
        [xml]$clinicIde = Get-Content (Join-Path $clinicRoot '.idea\workspace.xml') -Raw
        $clinicConfiguration = $clinicIde.SelectSingleNode('//configuration[@name="ClinicApplication"]')
        if (!$clinicConfiguration) { throw 'ClinicApplication run configuration was not found.' }
        foreach ($clinicName in $clinicSecrets) {
            $clinicEntry = $clinicConfiguration.SelectSingleNode(".//env[@name='$clinicName']")
            if ($clinicEntry) { [Environment]::SetEnvironmentVariable($clinicName, $clinicEntry.GetAttribute('value'), 'Process') }
        }
    }
    foreach ($clinicName in $clinicSecrets) {
        if (![Environment]::GetEnvironmentVariable($clinicName, 'Process')) {
            $clinicSecure = Read-Host "Enter $clinicName" -AsSecureString
            $clinicPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($clinicSecure)
            try { [Environment]::SetEnvironmentVariable($clinicName, [Runtime.InteropServices.Marshal]::PtrToStringBSTR($clinicPointer), 'Process') }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($clinicPointer) }
        }
    }
    $clinicArguments = @('-jar', ('"' + $clinicJar + '"'))
    if ($PublicHttps) {
        $clinicArguments += @('--server.forward-headers-strategy=native', '--server.servlet.session.cookie.secure=true')
    }
    $clinicProcess = Start-Process -FilePath $clinicJava -ArgumentList $clinicArguments -WorkingDirectory $clinicRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $clinicDeployment 'server.log') -RedirectStandardError (Join-Path $clinicDeployment 'server-error.log') -PassThru
    $clinicProcess.Id | Set-Content (Join-Path $clinicDeployment 'server.pid')
    for ($clinicAttempt = 0; $clinicAttempt -lt 60; $clinicAttempt++) {
        Start-Sleep -Seconds 1
        $clinicProcess.Refresh()
        if ($clinicProcess.HasExited) { throw 'Clinic failed to start. Check .deployment\server.log and server-error.log.' }
        try {
            $clinicResponse = Invoke-WebRequest 'http://127.0.0.1:8080/login' -UseBasicParsing -TimeoutSec 2
            if ($clinicResponse.StatusCode -eq 200) { Write-Host 'Clinic is running: http://localhost:8080'; return }
        } catch { }
    }
    throw 'Startup did not finish within 60 seconds. Check .deployment\server.log.'
} finally {
    foreach ($clinicName in $clinicSecrets) { [Environment]::SetEnvironmentVariable($clinicName, $clinicPrevious[$clinicName], 'Process') }
}
