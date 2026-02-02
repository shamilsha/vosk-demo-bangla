# Capture logcat when the app crashes
# 1. Connect device via USB, enable USB debugging
# 2. Run this script: .\capture-crash-log.ps1
# 3. Open the app, tap "Start Microphone" or "Start File"
# 4. When the app closes, press Ctrl+C here
# 5. Open logcat-crash.txt and search for "FATAL", "signal", "alphacephei", "backtrace"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

$outFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Write-Host "Clearing logcat..." -ForegroundColor Yellow
& $adb logcat -c
Write-Host "Recording to $outFile - Reproduce the crash (tap Start Mic or Start File), then press Ctrl+C" -ForegroundColor Cyan
# Option A: Errors + app tag (faster, smaller file)
# & $adb logcat -v time *:E AndroidRuntime:E "com.alphacephei.vosk:V" 2>&1 | Tee-Object -FilePath $outFile

# Option B: FULL log (captures native crash backtraces - use when app closes on Start Mic/Start File)
& $adb logcat -v time 2>&1 | Tee-Object -FilePath $outFile
