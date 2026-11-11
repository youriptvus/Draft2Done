param([string]$Serial='',[switch]$Install,[switch]$UsbAI,[ValidateRange(1,65535)][int]$AdbServerPort=5038)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$adb=Join-Path $projectRoot 'tools\android-sdk\platform-tools\adb.exe'
$rows=& $adb -P $AdbServerPort devices -l
$rows | Write-Host
$candidates=@($rows | Where-Object { $_ -match '^\S+\s+device\b' })
if(!$candidates.Count){throw 'No authorized device. Unlock the Samsung, enable USB debugging, reconnect USB and approve its authorization prompt.'}
if(!$Serial){
 $samsung=@()
 foreach($row in $candidates){$candidate=($row -split '\s+')[0];$maker=(& $adb -P $AdbServerPort -s $candidate shell getprop ro.product.manufacturer).Trim();if($maker -eq 'samsung'){$samsung+=$candidate}}
 if($samsung.Count -ne 1){throw 'Specify -Serial for the authorized Samsung device.'}
 $Serial=$samsung[0]
}
$manufacturer=(& $adb -P $AdbServerPort -s $Serial shell getprop ro.product.manufacturer).Trim()
if($manufacturer -ne 'samsung'){throw 'Selected device is not a Samsung; no installation performed.'}
$testRoot=Join-Path $projectRoot 'test-results\samsung'
New-Item -ItemType Directory -Force -Path $testRoot | Out-Null
$info=@{manufacturer=$manufacturer;model=((& $adb -P $AdbServerPort -s $Serial shell getprop ro.product.model).Trim());android=((& $adb -P $AdbServerPort -s $Serial shell getprop ro.build.version.release).Trim());adbServerPort=$AdbServerPort;time=[DateTime]::UtcNow.ToString('o')}
$info | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $testRoot 'device.json') -Encoding UTF8
if($Install){& $adb -P $AdbServerPort -s $Serial install -r (Join-Path $projectRoot 'dist\Draft2Done-0.1.0.apk');if($LASTEXITCODE -ne 0){throw 'APK install failed; existing app was not uninstalled.'}}
if($UsbAI){& $adb -P $AdbServerPort -s $Serial reverse tcp:11435 tcp:11435;if($LASTEXITCODE -ne 0){throw 'USB reverse failed.'}}
& $adb -P $AdbServerPort -s $Serial shell am start -W -n io.draft2done.app/.MainActivity
if($LASTEXITCODE -ne 0){throw 'App launch failed.'}
Write-Host 'App launched. Review the screen on the phone, then capture specific test screens with adb shell screencap and adb pull. Avoid collecting unrelated app logs or personal screen content.'
