param([ValidateSet('compile','native','render','package','verify')][string]$Stage='compile')
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
if ($taskRoot -ne 'C:\Users\User1\Documents\Downloads\Coraline') { throw 'Unexpected project root' }
Set-Location -LiteralPath $taskRoot
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class CoralineBuildMemory {
 [StructLayout(LayoutKind.Sequential)] public struct Status {
 public uint Length; public uint Load; public ulong Total; public ulong Available;
 public ulong PageTotal; public ulong PageAvailable; public ulong VirtualTotal;
 public ulong VirtualAvailable; public ulong Extended;
 }
 [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GlobalMemoryStatusEx(ref Status value);
 public static double FreeGiB() { var v=new Status();v.Length=(uint)Marshal.SizeOf(v);if(!GlobalMemoryStatusEx(ref v))throw new Exception("Memory query failed");return v.Available/1073741824.0; }
}
'@
$taskFree=[CoralineBuildMemory]::FreeGiB()
$taskMinimum=if($Stage -in @('native','render')){3.0}else{2.0}
Write-Output ("Stage="+$Stage+" freeGiB="+$taskFree+" minimumGiB="+$taskMinimum)
if($taskFree -lt $taskMinimum){throw 'Insufficient physical memory; no build/native child started'}
$taskPython=Join-Path $taskRoot '.voice-tools\index-tts\.venv\Scripts\python.exe'
if((Get-FileHash -LiteralPath 'C:\ffmpeg\ffmpeg.exe' -Algorithm SHA256).Hash.ToLowerInvariant() -ne '0d47da15b8729af52277273ac44d138d5b5fe7b2fe4e09c1f1fa847713509e72'){throw 'FFmpeg pin changed'}
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:PATH=(Split-Path $taskPython)+';C:\ffmpeg;'+$env:JAVA_HOME+'\bin;'+$env:PATH
# Process-local limits apply to the launched Java tools only. Project defaults stay intact.
$env:JAVA_TOOL_OPTIONS='-Xmx1G -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=128m'
$taskArgs=@('--offline','-Dorg.gradle.jvmargs=-Xmx1G -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=128m','--project-cache-dir','.gradle-character-cast','-PisolatedActivityBuild','-PcharacterCastBuild','-PtemporaryEventAudio','--max-workers=1')
switch($Stage){
 'compile' {$taskArgs+=@('-PgameTests','-PwybieVisitTests','compileJava','testRuntimeAssetOptimization')}
 'native' {$taskArgs+=@('-PgameTests','-PwybieVisitTests','-PfreshGameTests','runGameTestServer')}
 'render' {$taskArgs+=@('-PrenderChecks','-PvisitEventsVisualCheck','runClient')}
 'package' {$taskArgs+=@('check','jar')}
 'verify' {
   $taskJar=Join-Path $taskRoot 'build-character-cast\libs\coraline-1.15.62-placeholder-audio.jar'
   & $taskPython 'tools/verify_temporary_event_audio.py' '--jar' $taskJar
   if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
   & $taskPython 'tools/verify_secrets_revision_release.py' $taskJar
   if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
   Get-FileHash -LiteralPath $taskJar -Algorithm SHA256
   exit 0
 }
}
$taskLog=Join-Path $taskRoot ("build-character-cast/mcp-"+$Stage+".log")
$taskOldPreference=$ErrorActionPreference
$ErrorActionPreference='Continue'
& (Join-Path $taskRoot 'gradlew.bat') @taskArgs 2>&1 | Tee-Object -FilePath $taskLog
$taskCode=$LASTEXITCODE
$ErrorActionPreference=$taskOldPreference
Write-Output ("Stage exit code: "+$taskCode+"; log: "+$taskLog)
exit $taskCode
