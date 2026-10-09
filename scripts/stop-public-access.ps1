$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicDeployment = Join-Path $clinicRoot '.deployment'
$clinicPidFile = Join-Path $clinicDeployment 'tunnel.pid'
if (!(Test-Path $clinicPidFile)) { Write-Host 'No public tunnel is recorded.'; return }
$clinicProcessId = [int](Get-Content $clinicPidFile)
$clinicProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$clinicProcessId"
if ($clinicProcess) {
    $clinicClient = Join-Path $clinicRoot '.tools\cloudflare\cloudflared.exe'
    if ($clinicProcess.ExecutablePath -ne $clinicClient -or $clinicProcess.CommandLine -notmatch 'http://127\.0\.0\.1:8080') {
        throw 'The recorded PID belongs to another process. It has not been stopped.'
    }
    Stop-Process -Id $clinicProcessId
}
Remove-Item -LiteralPath $clinicPidFile
Remove-Item -LiteralPath (Join-Path $clinicDeployment 'public-url.txt') -ErrorAction SilentlyContinue
Write-Host 'Public access stopped. The local clinic application is still running.'
