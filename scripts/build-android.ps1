param([switch]$SkipWebBuild,[switch]$Clean)
$ErrorActionPreference = 'Stop'
if ($Clean -and $SkipWebBuild) { throw '-Clean requires rebuilding the web assets; omit -SkipWebBuild.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'release-info.ps1')
$releaseInfo = Get-Draft2DoneReleaseInfo $projectRoot
$sdkRoot = Join-Path $projectRoot 'tools\android-sdk'
$buildTools = Join-Path $sdkRoot 'build-tools\36.0.0'
$androidJar = Join-Path $sdkRoot 'platforms\android-36\android.jar'
$buildRoot = Join-Path $projectRoot 'build\android'
$webRoot = Join-Path $projectRoot 'web-dist'
$releaseRoot = Join-Path $projectRoot 'dist'
$signingRoot = Join-Path $projectRoot 'signing'
function Invoke-Checked([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed with exit code $LASTEXITCODE" }
}
if (!(Test-Path -LiteralPath $androidJar)) { & (Join-Path $PSScriptRoot 'setup-android.ps1') }
$javaCandidate = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\javac.exe' } else { $null }
if ($javaCandidate -and (Test-Path -LiteralPath $javaCandidate)) { $jdkRoot = Split-Path -Parent (Split-Path -Parent $javaCandidate) }
else {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacCommand -and (Test-Path -LiteralPath (Join-Path (Split-Path -Parent $javacCommand.Source) 'keytool.exe'))) { $jdkRoot = Split-Path -Parent (Split-Path -Parent $javacCommand.Source) }
    else {
        $jdkRoot = Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}
if (!$jdkRoot) { throw 'A JDK 17 or newer is required. Set JAVA_HOME to its installation directory.' }
$javaExe = Join-Path $jdkRoot 'bin\java.exe'
$javacExe = Join-Path $jdkRoot 'bin\javac.exe'
$keytoolExe = Join-Path $jdkRoot 'bin\keytool.exe'
if ($Clean -and (Test-Path -LiteralPath $webRoot)) {
    $resolvedWebRoot = [IO.Path]::GetFullPath($webRoot)
    if ([IO.Path]::GetDirectoryName($resolvedWebRoot) -ne [IO.Path]::GetFullPath($projectRoot) -or
        [IO.Path]::GetFileName($resolvedWebRoot) -cne 'web-dist' -or
        (Get-Item -LiteralPath $resolvedWebRoot -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Unsafe generated web output path.' }
    Remove-Item -LiteralPath $resolvedWebRoot -Recurse -Force
}
if (!$SkipWebBuild) {
    Push-Location $projectRoot
    try { Invoke-Checked 'npm.cmd' @('run','build:web') } finally { Pop-Location }
}
if (!(Test-Path -LiteralPath (Join-Path $webRoot 'index.html'))) { throw 'Web assets are missing. Run npm run build:web first.' }
$webVersionPath = Join-Path $webRoot 'version.json'
if (!(Test-Path -LiteralPath $webVersionPath -PathType Leaf)) { throw 'Bundled web version is missing. Rebuild the web assets.' }
$webVersion = Get-Content -LiteralPath $webVersionPath -Raw | ConvertFrom-Json
if ($webVersion.version -cne $releaseInfo.VersionName) { throw 'Bundled web version does not match the Android/package release. Rebuild the web assets before signing.' }
# Only generated Android output is replaced. Never clean signing/ or dist/.
$resolvedBuildRoot = [IO.Path]::GetFullPath($buildRoot)
if (!$resolvedBuildRoot.StartsWith([IO.Path]::GetFullPath($projectRoot) + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe Android build path.' }
if (Test-Path -LiteralPath $resolvedBuildRoot) { Remove-Item -LiteralPath $resolvedBuildRoot -Recurse -Force }
$classesRoot = Join-Path $buildRoot 'classes'
$generatedRoot = Join-Path $buildRoot 'generated'
$assetsRoot = Join-Path $buildRoot 'assets'
$dexRoot = Join-Path $buildRoot 'dex'
New-Item -ItemType Directory -Force -Path $classesRoot,$generatedRoot,$assetsRoot,$dexRoot,$releaseRoot,$signingRoot | Out-Null
Copy-Item -LiteralPath $webRoot -Destination (Join-Path $assetsRoot 'web') -Recurse
$compiled = Join-Path $buildRoot 'resources.zip'
$unsigned = Join-Path $buildRoot 'unsigned.apk'
$aligned = Join-Path $buildRoot 'aligned.apk'
$apk = Join-Path $releaseRoot $releaseInfo.ApkFileName
Invoke-Checked (Join-Path $buildTools 'aapt2.exe') @('compile','--dir',(Join-Path $projectRoot 'android\res'),'-o',$compiled)
Invoke-Checked (Join-Path $buildTools 'aapt2.exe') @('link','-o',$unsigned,'-I',$androidJar,'--manifest',$releaseInfo.ManifestPath,'--java',$generatedRoot,'--min-sdk-version',[string]$releaseInfo.MinSdkVersion,'--target-sdk-version',[string]$releaseInfo.TargetSdkVersion,'-A',$assetsRoot,$compiled)
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'android\src'),$generatedRoot -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
Invoke-Checked $javacExe (@('--release','8','-encoding','UTF-8','-classpath',$androidJar,'-d',$classesRoot) + $sources)
$classFiles = @(Get-ChildItem -LiteralPath $classesRoot -Recurse -Filter '*.class' | Select-Object -ExpandProperty FullName)
Invoke-Checked $javaExe (@('-cp',(Join-Path $buildTools 'lib\d8.jar'),'com.android.tools.r8.D8','--release','--min-api',[string]$releaseInfo.MinSdkVersion,'--lib',$androidJar,'--output',$dexRoot) + $classFiles)
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$zip = [IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
try {
    foreach ($dex in Get-ChildItem -LiteralPath $dexRoot -Filter '*.dex') { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$dex.FullName,$dex.Name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null }
    # AAPT2 on Windows 36 can emit nested asset paths with backslashes. Android
    # AssetManager requires slash-separated ZIP entry names on the device.
    foreach($entry in @($zip.Entries | Where-Object { $_.FullName.Contains('\') })) {
        $name=$entry.FullName.Replace('\','/')
        $memory=New-Object IO.MemoryStream
        $input=$entry.Open()
        try{$input.CopyTo($memory)}finally{$input.Dispose()}
        $bytes=$memory.ToArray();$memory.Dispose();$entry.Delete()
        $replacement=$zip.CreateEntry($name,[IO.Compression.CompressionLevel]::Optimal)
        $output=$replacement.Open()
        try{$output.Write($bytes,0,$bytes.Length)}finally{$output.Dispose()}
    }
}
finally { $zip.Dispose() }
$checkZip=[IO.Compression.ZipFile]::OpenRead($unsigned)
try {
    foreach($required in @('assets/web/index.html','assets/web/app.js','assets/web/styles.css','assets/web/pdf.worker.min.mjs','assets/web/version.json','classes.dex')) {
        if(!$checkZip.GetEntry($required)){throw "Missing required APK payload: $required"}
    }
    if(@($checkZip.Entries | Where-Object { $_.FullName.Contains('\') }).Count){throw 'Invalid backslash in an APK entry.'}
} finally {$checkZip.Dispose()}
Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-f','-P','16','4',$unsigned,$aligned)
$credentialsPath = Join-Path $signingRoot 'credentials.json'
$keystorePath = Join-Path $signingRoot 'draft2done-release.p12'
if ((Test-Path -LiteralPath $keystorePath) -ne (Test-Path -LiteralPath $credentialsPath)) { throw 'Signing key or credentials are missing. Restore the matching backup; existing release keys must never be replaced.' }
$previousStore = $env:DRAFT2DONE_KEYSTORE_PASSWORD
$previousKey = $env:DRAFT2DONE_KEY_PASSWORD
try {
    if (!(Test-Path -LiteralPath $keystorePath)) {
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $bytes = New-Object byte[] 36; $random.GetBytes($bytes); $password = [Convert]::ToBase64String($bytes) } finally { $random.Dispose() }
        $credentials = @{ alias='draft2done-release'; storePassword=$password; keyPassword=$password; createdUtc=[DateTime]::UtcNow.ToString('o') }
        $credentials | ConvertTo-Json | Set-Content -LiteralPath $credentialsPath -Encoding UTF8
        $env:DRAFT2DONE_KEYSTORE_PASSWORD = $password
        Invoke-Checked $keytoolExe @('-genkeypair','-keystore',$keystorePath,'-storetype','PKCS12','-alias',$credentials.alias,'-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Draft2Done Release, OU=Draft2Done, O=Draft2Done','-storepass:env','DRAFT2DONE_KEYSTORE_PASSWORD','-keypass:env','DRAFT2DONE_KEYSTORE_PASSWORD','-noprompt')
    }
    $credentials = Get-Content -LiteralPath $credentialsPath -Raw | ConvertFrom-Json
    $env:DRAFT2DONE_KEYSTORE_PASSWORD = $credentials.storePassword
    $env:DRAFT2DONE_KEY_PASSWORD = $credentials.keyPassword
    Invoke-Checked $javaExe @('-jar',(Join-Path $buildTools 'lib\apksigner.jar'),'sign','--ks',$keystorePath,'--ks-key-alias',$credentials.alias,'--ks-pass','env:DRAFT2DONE_KEYSTORE_PASSWORD','--key-pass','env:DRAFT2DONE_KEY_PASSWORD','--v1-signing-enabled','false','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',$apk,$aligned)
} finally {
    $env:DRAFT2DONE_KEYSTORE_PASSWORD = $previousStore
    $env:DRAFT2DONE_KEY_PASSWORD = $previousKey
}
$verification = & $javaExe '-jar' (Join-Path $buildTools 'lib\apksigner.jar') 'verify' '--verbose' '--print-certs' $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
Write-Draft2DoneReleaseText (Join-Path $releaseRoot $releaseInfo.SignatureFileName) ($verification -join "`n")
$verification | Write-Host
Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-c','-P','16','4',$apk)
$badging = & (Join-Path $buildTools 'aapt.exe') dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the signed APK package identity.' }
$expectedIdentity = "package: name='$($releaseInfo.PackageName)' versionCode='$($releaseInfo.VersionCode)' versionName='$($releaseInfo.VersionName)'"
if (!@($badging | Where-Object { $_.StartsWith($expectedIdentity + ' ') }).Count) { throw 'Signed APK package/version does not match current release metadata.' }
$apkHash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$buildJson = @{ file=$releaseInfo.ApkFileName; sha256=$apkHash; bytes=(Get-Item -LiteralPath $apk).Length; package=$releaseInfo.PackageName; version=$releaseInfo.VersionName; versionCode=$releaseInfo.VersionCode; minAndroid='8.0'; minSdk=$releaseInfo.MinSdkVersion; targetSdk=$releaseInfo.TargetSdkVersion } | ConvertTo-Json
Write-Draft2DoneReleaseText (Join-Path $releaseRoot $releaseInfo.BuildFileName) $buildJson
Write-Host "Signed APK: $apk"
$updateJson = @{ version=$releaseInfo.VersionName; versionCode=$releaseInfo.VersionCode; apkUrl=$releaseInfo.ApkUrl; sha256=$apkHash; package=$releaseInfo.PackageName } | ConvertTo-Json
Write-Draft2DoneReleaseText (Join-Path $releaseRoot 'latest.json') $updateJson
