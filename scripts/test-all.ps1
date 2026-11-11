<#
.SYNOPSIS
Runs Draft2Done host checks, with optional tests against an explicitly selected Android device.
.DESCRIPTION
The default run rebuilds web assets, then runs logic, OAuth/request lifecycle,
update validation, browser workflow and document checks. It does not sign APKs,
publish anything, open personal sign-in, or make an AI request.

-Device additionally uses already-installed release/test APKs for isolated native
storage, document-byte and updater checks, plus standalone phone UI consent checks.
The standalone suite uses the existing account's real model catalog but simulated
assist replies; it sends no inference and requires no PC model. Build/install first.
-InstallApks installs the existing local signed APKs without clearing app data.
-LiveAI explicitly enables the sample-writing smoke test, live source/update
requests, and a restart check. It creates a fictional QA paper and requires an
existing llama3:8b-instruct-q4_K_M server on this computer's loopback port 11435.
.EXAMPLE
.\scripts\test-all.ps1
.EXAMPLE
.\scripts\test-all.ps1 -Device -AdbServerPort 5038 -Serial R5GL75PGMFP
.EXAMPLE
.\scripts\test-all.ps1 -Device -InstallApks -LiveAI -AdbServerPort 5038 -Serial R5GL75PGMFP
#>
[CmdletBinding()]
param(
    [switch]$Device,
    [switch]$LiveAI,
    [switch]$InstallApks,
    [switch]$SkipHost,
    [ValidateRange(1,65535)][int]$AdbServerPort = 5038,
    [ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial = 'R5GL75PGMFP',
    [ValidateSet('samsung','motorola')][string]$ExpectedManufacturer = 'samsung'
)
$ErrorActionPreference = 'Stop'
if (($LiveAI -or $InstallApks) -and !$Device) { throw '-LiveAI and -InstallApks require -Device.' }
if ($SkipHost -and !$Device) { throw '-SkipHost requires -Device; the default run always checks the host.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'release-info.ps1')
$releaseInfo = Get-Draft2DoneReleaseInfo $projectRoot
$reportRoot = Join-Path $projectRoot 'test-results'
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null
$steps = New-Object 'System.Collections.Generic.List[object]'
$started = [DateTime]::UtcNow
$passed = $false
$reverseCreated = $false
$adb = Join-Path $projectRoot 'tools\android-sdk\platform-tools\adb.exe'
$adbArguments = @('-P',[string]$AdbServerPort,'-s',$Serial)

function Invoke-NativeChecked([string]$Executable,[string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Executable failed with exit code $LASTEXITCODE." }
}
function Invoke-TestStep([string]$Name,[scriptblock]$Action) {
    Write-Host "Running: $Name"
    $timer = [Diagnostics.Stopwatch]::StartNew()
    try {
        & $Action | Out-Host
        $steps.Add([pscustomobject]@{name=$Name;passed=$true;seconds=[Math]::Round($timer.Elapsed.TotalSeconds,2)})
    } catch {
        $steps.Add([pscustomobject]@{name=$Name;passed=$false;seconds=[Math]::Round($timer.Elapsed.TotalSeconds,2);error=$_.Exception.Message})
        throw
    }
}
function Invoke-DeviceTest([string]$Component,[string[]]$Options,[string]$ReportName) {
    # am instrument often exits adb with 0 even when the actual test failed.
    $output = @(& $adb @adbArguments shell am instrument -w -r @Options $Component 2>&1)
    $exitCode = $LASTEXITCODE
    $raw = $output -join "`n"
    $output | Write-Host
    $logPath = Join-Path $reportRoot ("device-" + $ReportName + '.txt')
    $raw | Set-Content -LiteralPath $logPath -Encoding UTF8
    if ($exitCode -ne 0 -or $raw -notmatch '(?m)^\s*INSTRUMENTATION_CODE:\s*-1\s*$' -or $raw -match 'INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|FAILURES!!!|Device test failed:|Update verification failed:') {
        throw "Device test $ReportName failed. See $logPath."
    }
    $destination = Join-Path $reportRoot ("device-" + $ReportName + '.json')
    Invoke-NativeChecked $adb ($adbArguments + @('pull',"/sdcard/Android/data/io.draft2done.app/files/qa/$ReportName.json",$destination))
    $report = Get-Content -LiteralPath $destination -Raw | ConvertFrom-Json
    if ($report.PSObject.Properties['passed'] -and $report.passed -ne $true) { throw "Device report $ReportName records a failure." }
    $checks = @($report.checks)
    if (!$checks.Count) { throw "Device report $ReportName contains no checks." }
    foreach ($check in $checks) {
        if ($check.PSObject.Properties['passed'] -and $check.passed -ne $true) { throw "Device report $ReportName has a failed check." }
    }
    Write-Host "Verified $($checks.Count) device checks in $ReportName."
}

Push-Location $projectRoot
try {
    if (!$SkipHost) {
        Invoke-TestStep 'JavaScript logic and service boundaries' { Invoke-NativeChecked 'npm.cmd' @('test') }
        Invoke-TestStep 'OAuth protocol and request lifecycle' { & (Join-Path $PSScriptRoot 'test-oauth.ps1') }
        Invoke-TestStep 'Update validation' { & (Join-Path $PSScriptRoot 'test-updates.ps1') }
        Invoke-TestStep 'Production web bundle' { Invoke-NativeChecked 'npm.cmd' @('run','build:web') }
        Invoke-TestStep 'Browser writing workflow and lifecycle' { Invoke-NativeChecked 'npm.cmd' @('run','test:ui') }
        Invoke-TestStep 'Document roundtrips and optional add-on UI' { Invoke-NativeChecked 'npm.cmd' @('run','test:documents') }
        Invoke-TestStep 'Proposal, profile, revision and research review workflows' { Invoke-NativeChecked 'npm.cmd' @('run','test:review') }
        Invoke-TestStep 'Assignment, saved voice and project management workflows' { Invoke-NativeChecked 'npm.cmd' @('run','test:management') }
        Invoke-TestStep 'Pasted draft safety and About dedication' { Invoke-NativeChecked 'npm.cmd' @('run','test:paste') }
        Invoke-TestStep 'Contextual information links and source consent (simulated responses)' { Invoke-NativeChecked 'npm.cmd' @('run','test:information') }
    }

    if ($Device) {
        Invoke-TestStep 'Authorized Android device and installed test packages' {
            if (!(Test-Path -LiteralPath $adb)) { throw 'Project-local ADB is missing. Run scripts/setup-android.ps1.' }
            $state = (& $adb @adbArguments get-state 2>&1 | Out-String).Trim()
            if ($LASTEXITCODE -ne 0 -or $state -ne 'device') { throw "Android device $Serial is not authorized on ADB port $AdbServerPort. Existing ADB servers were left running." }
            $manufacturer = (& $adb @adbArguments shell getprop ro.product.manufacturer | Out-String).Trim()
            if ($LASTEXITCODE -ne 0 -or $manufacturer -ne $ExpectedManufacturer) { throw "The explicit serial must identify the authorized $ExpectedManufacturer device." }
            if ($InstallApks) {
                $release = Join-Path (Join-Path $projectRoot 'dist') $releaseInfo.ApkFileName
                $harness = Join-Path $projectRoot 'build\device-tests\Draft2Done-device-tests.apk'
                if (!(Test-Path -LiteralPath $release) -or !(Test-Path -LiteralPath $harness)) { throw 'Build the signed release and test APKs first with scripts/build-android.ps1 and scripts/build-device-tests.ps1.' }
                Invoke-NativeChecked $adb ($adbArguments + @('install','-r',$release))
                Invoke-NativeChecked $adb ($adbArguments + @('install','-r',$harness))
            }
            foreach ($package in @('io.draft2done.app','io.draft2done.test')) {
                $path = (& $adb @adbArguments shell pm path $package | Out-String).Trim()
                if ($LASTEXITCODE -ne 0 -or !$path.StartsWith('package:')) { throw "Missing installed package $package. Build/install matching signed APKs before -Device." }
            }
        }
        Invoke-TestStep 'Native atomic storage recovery and failed writes' {
            Invoke-DeviceTest 'io.draft2done.test/.SmokeInstrumentation' @('-e','mode','recovery') 'recovery'
        }
        Invoke-TestStep 'Native import/export bytes and limits' {
            Invoke-DeviceTest 'io.draft2done.test/.SmokeInstrumentation' @('-e','mode','documents') 'documents'
        }
        Invoke-TestStep 'Installed APK update validation and provider boundaries' {
            Invoke-DeviceTest 'io.draft2done.test/.UpdateValidationInstrumentation' @() 'update-validation'
        }
        Invoke-TestStep 'Standalone phone route and exact sharing consent (simulated assist)' {
            Invoke-DeviceTest 'io.draft2done.test/.SmokeInstrumentation' @('-e','mode','standalone') 'standalone'
        }
        if ($LiveAI) {
            Invoke-TestStep 'Existing local AI and Samsung USB connection' {
                $catalog = Invoke-RestMethod -Uri 'http://127.0.0.1:11435/api/tags' -TimeoutSec 10
                if (!@($catalog.models | Where-Object { $_.name -eq 'llama3:8b-instruct-q4_K_M' }).Count) { throw 'Start the existing llama3:8b-instruct-q4_K_M server on port11435; this runner does not download models.' }
                $mappings = @(& $adb @adbArguments reverse --list)
                if ($LASTEXITCODE -ne 0) { throw 'Could not inspect Samsung reverse mappings.' }
                $mapping = @($mappings | Where-Object { $_ -match '\s+tcp:11435\s+' })
                if ($mapping.Count -and !($mapping -match '\s+tcp:11435\s+tcp:11435\s*$')) { throw 'Samsung port11435 already has a different reverse mapping; it was left unchanged.' }
                if (!$mapping.Count) {
                    Invoke-NativeChecked $adb ($adbArguments + @('reverse','tcp:11435','tcp:11435'))
                    $script:reverseCreated = $true
                }
            }
            $checkpoint = 'QA-' + [Guid]::NewGuid().ToString('N')
            Invoke-TestStep 'Real AI writing workflow on a fictional Samsung paper' {
                Invoke-DeviceTest 'io.draft2done.test/.SmokeInstrumentation' @('-e','checkpoint',$checkpoint) 'smoke'
            }
            Invoke-TestStep 'Accepted draft survives process restart' {
                Invoke-NativeChecked $adb ($adbArguments + @('shell','am','force-stop','io.draft2done.app'))
                Invoke-DeviceTest 'io.draft2done.test/.SmokeInstrumentation' @('-e','mode','verify-reopen','-e','checkpoint',$checkpoint) 'reopen'
            }
        }
    }
    $passed = $true
} finally {
    if ($reverseCreated) {
        & $adb @adbArguments reverse --remove tcp:11435 | Out-Host
        if ($LASTEXITCODE -ne 0) { Write-Warning 'The temporary Samsung reverse mapping could not be removed.' }
    }
    Pop-Location
    $reportName = if ($Device) { 'all-tests-device.json' } else { 'all-tests-host.json' }
    $reportPath = Join-Path $reportRoot $reportName
    [pscustomobject]@{
        startedUtc=$started.ToString('o');finishedUtc=[DateTime]::UtcNow.ToString('o');passed=$passed
        device=[bool]$Device;liveAI=[bool]$LiveAI;hostSkipped=[bool]$SkipHost
        version=$releaseInfo.VersionName;versionCode=$releaseInfo.VersionCode;package=$releaseInfo.PackageName;steps=@($steps.ToArray())
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "Regression report: $reportPath"
}
