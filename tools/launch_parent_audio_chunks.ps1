param(
    [ValidateSet(640,1024,1536,2048,2560,3072,4096)][int]$StartFreeMiB = 2048,
    [ValidateSet(1,5,10)][int]$BatchNewTakes = 10,
    [string]$ResumeChain,
    [string[]]$ReferenceCacheReports,
    [string[]]$AdditionalAttemptReports,
    [switch]$InferencePageRelease,
    [string]$RecoverExhaustedChain,
    [string]$SuccessfulRecoveryProbe,
    [string]$FlowStagingReport
)
$ErrorActionPreference = 'Stop'
$audioProjectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$audioLaunchRoot = Join-Path $audioProjectRoot 'build-character-cast\parent-audio-launches'
$audioLaunchDirectory = Join-Path $audioLaunchRoot ([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
if (-not $audioLaunchDirectory.StartsWith($audioProjectRoot + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Audio launch escaped project' }
New-Item -ItemType Directory -Path $audioLaunchDirectory | Out-Null
$audioPython = (Get-Command python -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
$audioStdout = Join-Path $audioLaunchDirectory 'stdout.log'
$audioStderr = Join-Path $audioLaunchDirectory 'stderr.log'
$audioArguments = @('tools/run_parent_audio_chunks.py', '--low-memory-attempt', '--start-free-mib', "$StartFreeMiB", '--batch-new-takes', "$BatchNewTakes")
if ($InferencePageRelease) { $audioArguments += '--inference-page-release' }
if ($FlowStagingReport) {
    if ($FlowStagingReport -match '\s|"') { throw 'Use the project-relative flow component receipt without whitespace' }
    $audioArguments += @('--flow-staging-report', $FlowStagingReport)
}
if ($RecoverExhaustedChain -or $SuccessfulRecoveryProbe) {
    if (-not $RecoverExhaustedChain -or -not $SuccessfulRecoveryProbe -or $ResumeChain -or $AdditionalAttemptReports) { throw 'Recovery needs its exact successful probe and no other continuation' }
    if ($RecoverExhaustedChain -match '\s|"' -or $SuccessfulRecoveryProbe -match '\s|"') { throw 'Use project-relative recovery paths without whitespace' }
    $audioArguments += @('--recover-exhausted-chain', $RecoverExhaustedChain, '--successful-recovery-probe', $SuccessfulRecoveryProbe)
}
if ($ResumeChain) {
    if ($ResumeChain -match '\s|"') { throw 'Use the project-relative receipt path without whitespace' }
    $audioArguments += @('--resume-chain', $ResumeChain)
}
if ($ReferenceCacheReports) {
    if ($ReferenceCacheReports.Count -ne 2) { throw 'Both parent reference preparation receipts are required' }
    if (@($ReferenceCacheReports | Where-Object { $_ -match '\s|"' }).Count -ne 0) { throw 'Use project-relative receipt paths without whitespace' }
    $audioArguments += @('--reference-cache-reports') + $ReferenceCacheReports
}
if ($AdditionalAttemptReports) {
    if (@($AdditionalAttemptReports | Where-Object { $_ -match '\s|"' }).Count -ne 0) { throw 'Use project-relative probe receipt paths without whitespace' }
    $audioArguments += @('--additional-attempt-reports') + $AdditionalAttemptReports
}
$audioController = Start-Process -FilePath $audioPython -ArgumentList $audioArguments -WorkingDirectory $audioProjectRoot -WindowStyle Hidden -RedirectStandardOutput $audioStdout -RedirectStandardError $audioStderr -PassThru
[pscustomobject]@{ controllerPid = $audioController.Id; startedUtc = $audioController.StartTime.ToUniversalTime().ToString('o'); stdout = $audioStdout; stderr = $audioStderr } | ConvertTo-Json -Compress
