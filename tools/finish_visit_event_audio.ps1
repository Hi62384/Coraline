# Sequential local synthesis/validation prevents simultaneous model memory pressure.
param([switch]$SkipOutingValidation)
$ErrorActionPreference = 'Stop'
$taskPython = Join-Path $PSScriptRoot '../.voice-tools/index-tts/.venv/Scripts/python.exe'
$taskRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$fatherReport = Join-Path $taskRoot 'voicelines/otherfather/events/family_outing/generation_report.json'
$taskWaitStarted = Get-Date
while (!(Test-Path -LiteralPath $fatherReport) -or @((Get-Content -LiteralPath $fatherReport -Raw | ConvertFrom-Json).lines.PSObject.Properties).Count -lt 15) {
    if (((Get-Date)-$taskWaitStarted).TotalMinutes -gt 60) { throw 'Outing Father synthesis did not finish; inspect its repair log.' }
    Start-Sleep -Seconds 15
}
Start-Sleep -Seconds 10
if (!$SkipOutingValidation) {
    $env:CORALINE_VOICE_MANIFEST = 'tools/outing_father_voice_manifest.json'
    & $taskPython tools/check_welcome_audio.py --transcribe --speaker-check *> outing-father-validation-final.log
    if ($LASTEXITCODE -ne 0) { throw 'Outing Father audio needs review.' }
}
foreach ($batch in @(
    @('game-night-mother','tools/game_night_mother_voice_manifest.json'),
    @('game-night-father','tools/game_night_father_voice_manifest.json'),
    @('delivery-mother','tools/delivery_mother_voice_manifest.json')
)) {
    $env:CORALINE_VOICE_MANIFEST = $batch[1]
    $generated = $false
    foreach ($seed in @(0,2000,4000,6000,8000)) {
        & $taskPython tools/generate_other_mother_welcome.py --seed-offset $seed *> ($batch[0]+'-generation-'+$seed+'.log')
        if ($LASTEXITCODE -eq 0) { $generated = $true; break }
    }
    if (!$generated) { throw ($batch[0]+' synthesis failed its quality gate.') }
    & $taskPython tools/check_welcome_audio.py --transcribe --speaker-check *> ($batch[0]+'-validation-final.log')
    if ($LASTEXITCODE -ne 0) { throw ($batch[0]+' audio needs review.') }
}
Write-Output 'All new event synthesis and objective validation batches finished. Export and independent review remain separate.'
