Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
if($taskRoot -ne 'C:\Users\User1\Documents\Downloads\Coraline'){throw 'Unexpected project root'}
Set-Location -LiteralPath $taskRoot
$taskReport=[ordered]@{kind='read-only-cache-inspection-and-low-memory-audio-validation';cacheWriteProbe=$false;cache=@();validationExit=$null}
$taskParts=@('Users','User1','.gradle','wrapper','dists','gradle-8.8-bin','dl7vupf4psengwqhwktix4v1','gradle-8.8-bin.zip.lck')
$taskPath='C:\'
foreach($taskPart in $taskParts){
 $taskPath=Join-Path $taskPath $taskPart
 try{
  $taskItem=Get-Item -LiteralPath $taskPath -Force -ErrorAction Stop
  $taskReparse=($taskItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0
  $taskEntry=[ordered]@{path=$taskPath;exists=$true;reparsePoint=$taskReparse;type=if($taskItem.PSIsContainer){'directory'}else{'file'}}
  if($taskReparse){$taskReport.cache+=@($taskEntry);break}
  if($taskPart -eq 'dl7vupf4psengwqhwktix4v1' -or $taskPart -eq 'gradle-8.8-bin.zip.lck'){
   try{$taskAcl=Get-Acl -LiteralPath $taskPath;$taskEntry['aclReadable']=$true;$taskEntry['protected']=$taskAcl.AreAccessRulesProtected}catch{$taskEntry['aclReadable']=$false;$taskEntry['aclError']=$_.Exception.Message}
  }
  $taskReport.cache+=@($taskEntry)
 }catch{
  $taskReport.cache+=@([ordered]@{path=$taskPath;readError=$_.Exception.Message})
  break
 }
}
$taskOutput=Join-Path $taskRoot 'build-character-cast/mcp-cache-audio-inspection.json'
$taskReport | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $taskOutput -Encoding UTF8
Write-Output 'Cache paths were inspected read-only. No cache write test, ACL modification or Gradle launch occurred.'
$taskPython=Join-Path $taskRoot '.voice-tools\index-tts\.venv\Scripts\python.exe'
$taskLog=Join-Path $taskRoot 'build-character-cast/mcp-temporary-audio-validation.log'
$ErrorActionPreference='Continue'
& $taskPython 'tools/verify_temporary_event_audio.py' 2>&1 | Tee-Object -FilePath $taskLog
$taskCode=$LASTEXITCODE
$ErrorActionPreference='Stop'
$taskReport.validationExit=$taskCode
$taskReport | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $taskOutput -Encoding UTF8
Write-Output ('Read-only cache report: '+$taskOutput)
exit $taskCode
