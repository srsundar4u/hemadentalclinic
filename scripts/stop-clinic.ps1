$ErrorActionPreference = 'Stop'
$clinicRoot = Split-Path $PSScriptRoot -Parent
$clinicDeployment = Join-Path $clinicRoot '.deployment'
$clinicPidFile = Join-Path $clinicDeployment 'server.pid'
if (!(Test-Path $clinicPidFile)) { Write-Host 'No packaged clinic process is recorded.'; return }
$clinicProcessId = [int](Get-Content $clinicPidFile)
$clinicProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$clinicProcessId"
if ($clinicProcess) {
    $clinicJar = Join-Path $clinicDeployment 'clinic.jar'
    if ($clinicProcess.Name -ne 'java.exe' -or !$clinicProcess.CommandLine.Contains($clinicJar)) {
        throw 'The recorded PID belongs to another program. It has not been stopped.'
    }
    Stop-Process -Id $clinicProcessId
}
Remove-Item -LiteralPath $clinicPidFile
Write-Host 'Packaged clinic application stopped.'
