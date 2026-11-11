$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'release-info.ps1')
$releaseInfo=Get-Draft2DoneReleaseInfo $projectRoot
$buildRoot=Join-Path $projectRoot 'build\device-tests'
$tools=Join-Path $projectRoot 'tools\android-sdk\build-tools\36.0.0'
$androidJar=Join-Path $projectRoot 'tools\android-sdk\platforms\android-36\android.jar'
$jdk='C:\Program Files\Java\jdk-27.0.1\bin'
$resolvedBuildRoot=[IO.Path]::GetFullPath($buildRoot)
if(!$resolvedBuildRoot.StartsWith([IO.Path]::GetFullPath($projectRoot)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe device-test build output path.'}
if(Test-Path -LiteralPath $resolvedBuildRoot){
 if((Get-Item -LiteralPath $resolvedBuildRoot -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Refusing a linked device-test build directory.'}
 Remove-Item -LiteralPath $resolvedBuildRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $buildRoot,(Join-Path $buildRoot 'classes'),(Join-Path $buildRoot 'dex') | Out-Null
function Checked([string]$exe,[string[]]$arguments){& $exe @arguments;if($LASTEXITCODE -ne 0){throw "$exe failed."}}
$unsigned=Join-Path $buildRoot 'unsigned.apk'
$testManifest=[xml](Get-Content -LiteralPath (Join-Path $projectRoot 'android-tests\AndroidManifest.xml') -Raw)
$androidNamespace='http://schemas.android.com/apk/res/android'
$testManifest.manifest.SetAttribute('versionName',$androidNamespace,$releaseInfo.VersionName)
$testManifest.manifest.SetAttribute('versionCode',$androidNamespace,[string]$releaseInfo.VersionCode)
$generatedManifest=Join-Path $buildRoot 'AndroidManifest.xml'
Write-Draft2DoneReleaseText $generatedManifest $testManifest.OuterXml
Checked (Join-Path $tools 'aapt2.exe') @('link','-o',$unsigned,'-I',$androidJar,'--manifest',$generatedManifest,'--min-sdk-version',[string]$releaseInfo.MinSdkVersion,'--target-sdk-version',[string]$releaseInfo.TargetSdkVersion)
Checked (Join-Path $jdk 'javac.exe') (@('--release','8','-encoding','UTF-8','-cp',$androidJar,'-d',(Join-Path $buildRoot 'classes'))+@(Get-ChildItem (Join-Path $projectRoot 'android-tests\src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName))
Checked (Join-Path $jdk 'java.exe') (@('-cp',(Join-Path $tools 'lib\d8.jar'),'com.android.tools.r8.D8','--release','--min-api',[string]$releaseInfo.MinSdkVersion,'--lib',$androidJar,'--output',(Join-Path $buildRoot 'dex'))+@(Get-ChildItem (Join-Path $buildRoot 'classes') -Recurse -Filter '*.class' | Select-Object -ExpandProperty FullName))
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
