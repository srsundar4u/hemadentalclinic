$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicDeployment = Join-Path $clinicRoot '.deployment'
$clinicClient = Join-Path $clinicRoot '.tools\cloudflare\cloudflared.exe'
if (!(Test-Path $clinicClient)) { throw 'Cloudflare tunnel client is missing.' }
$clinicPidFile = Join-Path $clinicDeployment 'tunnel.pid'
if (Test-Path $clinicPidFile) {
    $clinicExisting = Get-Process -Id ([int](Get-Content $clinicPidFile)) -ErrorAction SilentlyContinue
    if ($clinicExisting) { throw 'A tunnel process is already recorded. Stop it before starting another.' }
}
$clinicLogin = Invoke-WebRequest 'http://127.0.0.1:8080/login' -UseBasicParsing
if ([string]$clinicLogin.Headers['Set-Cookie'] -notmatch '(?i)\bSecure\b') {
    throw 'Restart the clinic with start-clinic.ps1 -UseIntelliJCredentials -PublicHttps first.'
}
$clinicTunnel = Start-Process -FilePath $clinicClient -ArgumentList @('tunnel', '--url', 'http://127.0.0.1:8080', '--no-autoupdate') -WorkingDirectory $clinicRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $clinicDeployment 'tunnel-output.log') -RedirectStandardError (Join-Path $clinicDeployment 'tunnel.log') -PassThru
$clinicTunnel.Id | Set-Content $clinicPidFile
for ($clinicAttempt = 0; $clinicAttempt -lt 45; $clinicAttempt++) {
    Start-Sleep -Seconds 1
    $clinicTunnel.Refresh()
    if ($clinicTunnel.HasExited) { throw 'Tunnel exited. Check .deployment\tunnel.log.' }
    $clinicLog = Get-Content (Join-Path $clinicDeployment 'tunnel.log') -Raw -ErrorAction SilentlyContinue
    $clinicUrl = [regex]::Match([string]$clinicLog, 'https://[a-z0-9-]+\.trycloudflare\.com').Value
    if ($clinicUrl) {
        $clinicUrl | Set-Content (Join-Path $clinicDeployment 'public-url.txt')
        Write-Host "Public clinic address: $clinicUrl"
        Write-Host 'Enter this address in the Android app Server field. The address changes when a new tunnel starts.'
        return
    }
}
throw 'Tunnel address was not received. Check .deployment\tunnel.log.'
