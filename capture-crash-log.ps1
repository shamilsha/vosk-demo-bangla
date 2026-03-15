# Capture logcat when the app crashes
# 1. Connect device via USB, enable USB debugging
# 2. Run this script: .\capture-crash-log.ps1
# 3. Reproduce the crash (open app, use English or Bengali mic for 1-2 sentences)
# 4. As soon as the app crashes, press Ctrl+C here
# 5. Open the generated .txt and search for "FATAL", "AndroidRuntime", "alphacephei", "releaseBuffer", "backtrace", "Abort message"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

$outFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Write-Host "Clearing logcat (so only this run is captured)..." -ForegroundColor Yellow
& $adb logcat -c
Write-Host "Recording to $outFile" -ForegroundColor Green
Write-Host "  -> Reproduce the crash NOW (open app, use mic), then press Ctrl+C" -ForegroundColor Cyan
# Full log so native crash backtraces and app logs are captured
& $adb logcat -v time 2>&1 | Tee-Object -FilePath $outFile
