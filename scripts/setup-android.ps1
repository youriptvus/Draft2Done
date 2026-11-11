param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sdkRoot = Join-Path $projectRoot 'tools\android-sdk'
$downloadRoot = Join-Path $projectRoot 'tools\downloads'
New-Item -ItemType Directory -Force -Path $sdkRoot,$downloadRoot | Out-Null
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$packages = @(
    @{ File='platform-36_r02.zip'; Sha1='2c1a80dd4d9f7d0e6dd336ec603d9b5c55a6f576'; Target='platforms\android-36'; Marker='android.jar' },
    @{ File='build-tools_r36_windows.zip'; Sha1='f16ccffd34de8790dede813a6c7d8e2c11a27b50'; Target='build-tools\36.0.0'; Marker='aapt2.exe' },
    @{ File='platform-tools_r37.0.3-win.zip'; Sha1='b3fde5239ad89229d5a9178375b81c9887a24172'; Target='platform-tools'; Marker='adb.exe' }
)
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($package in $packages) {
    $destination = Join-Path $sdkRoot $package.Target
    if (Test-Path -LiteralPath (Join-Path $destination $package.Marker)) { continue }
    $archive = Join-Path $downloadRoot $package.File
    if (!(Test-Path -LiteralPath $archive) -or (Get-FileHash -LiteralPath $archive -Algorithm SHA1).Hash.ToLowerInvariant() -ne $package.Sha1) {
        Write-Host "Downloading official Android SDK package $($package.File)..."
        Invoke-WebRequest -UseBasicParsing -Uri "https://dl.google.com/android/repository/$($package.File)" -OutFile $archive
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA1).Hash.ToLowerInvariant() -ne $package.Sha1) { throw "Checksum mismatch: $($package.File)" }
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    # Official archives each contain one top-level folder; strip it without staging a copy.
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        foreach ($entry in $zip.Entries) {
            $relative = $entry.FullName.Replace('\','/') -replace '^[^/]+/',''
            if (!$relative -or $entry.FullName.EndsWith('/')) { continue }
            $target = [IO.Path]::GetFullPath((Join-Path $destination $relative))
            if (!$target.StartsWith([IO.Path]::GetFullPath($destination) + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe SDK archive path.' }
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$true)
        }
    } finally { $zip.Dispose() }
}
Write-Host "Android SDK ready: $sdkRoot"
