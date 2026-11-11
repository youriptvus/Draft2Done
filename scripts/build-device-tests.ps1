$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build\device-tests'
$tools=Join-Path $projectRoot 'tools\android-sdk\build-tools\36.0.0'
$androidJar=Join-Path $projectRoot 'tools\android-sdk\platforms\android-36\android.jar'
$jdk='C:\Program Files\Java\jdk-27.0.1\bin'
New-Item -ItemType Directory -Force -Path $buildRoot,(Join-Path $buildRoot 'classes'),(Join-Path $buildRoot 'dex') | Out-Null
function Checked([string]$exe,[string[]]$arguments){& $exe @arguments;if($LASTEXITCODE -ne 0){throw "$exe failed."}}
$unsigned=Join-Path $buildRoot 'unsigned.apk'
Checked (Join-Path $tools 'aapt2.exe') @('link','-o',$unsigned,'-I',$androidJar,'--manifest',(Join-Path $projectRoot 'android-tests\AndroidManifest.xml'),'--min-sdk-version','26','--target-sdk-version','36')
Checked (Join-Path $jdk 'javac.exe') (@('--release','8','-encoding','UTF-8','-cp',$androidJar,'-d',(Join-Path $buildRoot 'classes'))+@(Get-ChildItem (Join-Path $projectRoot 'android-tests\src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName))
Checked (Join-Path $jdk 'java.exe') (@('-cp',(Join-Path $tools 'lib\d8.jar'),'com.android.tools.r8.D8','--release','--min-api','26','--lib',$androidJar,'--output',(Join-Path $buildRoot 'dex'))+@(Get-ChildItem (Join-Path $buildRoot 'classes') -Recurse -Filter '*.class' | Select-Object -ExpandProperty FullName))
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $buildRoot 'dex\classes.dex'),'classes.dex') | Out-Null}finally{$zip.Dispose()}
$aligned=Join-Path $buildRoot 'aligned.apk'
Checked (Join-Path $tools 'zipalign.exe') @('-f','4',$unsigned,$aligned)
$credentials=Get-Content -LiteralPath (Join-Path $projectRoot 'signing\credentials.json') -Raw | ConvertFrom-Json
$env:DRAFT2DONE_TEST_STORE_PASSWORD=$credentials.storePassword
try{Checked (Join-Path $jdk 'java.exe') @('-jar',(Join-Path $tools 'lib\apksigner.jar'),'sign','--ks',(Join-Path $projectRoot 'signing\draft2done-release.p12'),'--ks-pass','env:DRAFT2DONE_TEST_STORE_PASSWORD','--out',(Join-Path $buildRoot 'Draft2Done-device-tests.apk'),$aligned)}finally{Remove-Item Env:DRAFT2DONE_TEST_STORE_PASSWORD -ErrorAction SilentlyContinue}
Write-Host "Local instrumentation APK: $buildRoot\Draft2Done-device-tests.apk"
