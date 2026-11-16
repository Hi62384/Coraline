# Prepared sequential synthesis/validation for numbered specs 1, 3, 4, 5, 6, 7.
# Run only after stage-0 event audio is validated and the user approves the command.
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$python = Join-Path $root '.voice-tools/index-tts/.venv/Scripts/python.exe'
$catalog = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'numbered_event_voice_catalog.json') -Raw | ConvertFrom-Json
$logs = Join-Path $root ('build-character-cast/numbered-voice-logs/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $logs | Out-Null
Set-Location -LiteralPath $root
function Assert-Capacity {
    $os = Get-CimInstance Win32_OperatingSystem
    if (($os.FreePhysicalMemory * 1024.0 / 1GB) -lt 4.0) { throw 'Less than 4 GiB free physical memory. No model process will start; leave applications untouched and resume later.' }
}
if (!(Test-Path -LiteralPath $python -PathType Leaf)) { throw 'The established IndexTTS environment is missing.' }
try {
    foreach ($batch in $catalog.manifests) {
        Assert-Capacity
        $manifest = Get-Content -LiteralPath (Join-Path $root $batch.path) -Raw | ConvertFrom-Json
        $reference = Join-Path $root $manifest.reference
        if ((Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest.reference_sha256) { throw ('Reference identity changed for ' + $batch.path) }
        $env:CORALINE_VOICE_MANIFEST = $batch.path
        $name = $batch.event + '-' + $batch.speaker
        $generated = $false
        foreach ($seed in @(0,2000,4000,6000,8000)) {
            Assert-Capacity
            & $python tools/generate_other_mother_welcome.py --seed-offset $seed *> (Join-Path $logs ($name+'-generation-'+$seed+'.log'))
            if ($LASTEXITCODE -eq 0) { $generated = $true; break }
        }
        if (!$generated) { throw ($name+' synthesis failed the existing quality gate. Review logs without lowering thresholds.') }
        Assert-Capacity
        & $python tools/check_welcome_audio.py --transcribe --speaker-check *> (Join-Path $logs ($name+'-validation.log'))
        if ($LASTEXITCODE -ne 0) { throw ($name+' needs audio review. Export has not run.') }
    }
} finally {
    Remove-Item Env:CORALINE_VOICE_MANIFEST -ErrorAction SilentlyContinue
}
Write-Output ('Numbered-event voice batches objectively validated; export, questionable-ASR review, native playback and release verification remain separate. Logs: '+$logs)
