<#
.SYNOPSIS
Copies reviewed source and release assets into the local GitHub checkout.
.DESCRIPTION
No git add, commit, push or publication is performed. Private signing files,
credentials, app data, tools and test results are never copied.
#>
[CmdletBinding()]
param([string]$RepositoryPath)
$ErrorActionPreference = 'Stop'
$sourceRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
if (!$RepositoryPath) { $RepositoryPath = Join-Path $sourceRoot 'repo\Draft2Done' }
$destination = [IO.Path]::GetFullPath($RepositoryPath)
if ($destination -eq $sourceRoot) { throw 'Choose the separate local GitHub checkout, not the build workspace itself.' }
if (!(Test-Path -LiteralPath (Join-Path $destination '.git'))) { throw "No Git checkout at $destination. Existing files were left unchanged." }
$origin = (& git -C $destination remote get-url origin | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $origin -notmatch '^(https://github\.com/youriptvus/Draft2Done(?:\.git)?/?|git@github\.com:youriptvus/Draft2Done(?:\.git)?)$') {
    throw 'The destination must be the local youriptvus/Draft2Done checkout.'
}
$copied = 0
foreach ($directory in @('android','android-tests','assets','docs','samples','scripts','src','tests','web')) {
    $base = Join-Path $sourceRoot $directory
    if (!(Test-Path -LiteralPath $base)) { continue }
    foreach ($file in Get-ChildItem -LiteralPath $base -File -Recurse) {
        if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Refusing to copy a linked source file: $($file.FullName)" }
        $relative = $file.FullName.Substring($sourceRoot.Length + 1)
        if ($relative -match '(^|[\\/])(\.git|__pycache__|node_modules)([\\/]|$)' -or $file.Extension -in @('.pyc','.p12','.jks','.apk','.idsig')) { continue }
        $target = Join-Path $destination $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $target -Force
        $copied++
    }
}
foreach ($name in @('.gitignore','package.json','package-lock.json','README.md')) {
    Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination (Join-Path $destination $name) -Force
    $copied++
}
$releaseRoot = Join-Path $sourceRoot 'dist'
$metadataPath = Join-Path $releaseRoot 'Draft2Done-0.1.0-build.json'
$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
$apk = Join-Path $releaseRoot $metadata.file
if ((Split-Path -Leaf $metadata.file) -ne $metadata.file -or $metadata.file -notmatch '^Draft2Done-[0-9.]+\.apk$') { throw 'Invalid release APK filename.' }
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hash -ne $metadata.sha256 -or (Get-Item -LiteralPath $apk).Length -ne $metadata.bytes) { throw 'Release APK does not match build metadata. Rebuild before preparing release assets.' }
$manifest = Get-Content -LiteralPath (Join-Path $releaseRoot 'latest.json') -Raw | ConvertFrom-Json
if ($manifest.sha256 -ne $hash) { throw 'Update manifest hash does not match the signed APK.' }
$releaseDestination = Join-Path $destination 'release-assets'
New-Item -ItemType Directory -Force -Path $releaseDestination | Out-Null
foreach ($name in @($metadata.file,'Draft2Done-0.1.0-build.json','Draft2Done-0.1.0-signature.txt','latest.json')) {
    Copy-Item -LiteralPath (Join-Path $releaseRoot $name) -Destination (Join-Path $releaseDestination $name) -Force
}
$instructions = @'
# Local release files

Source changes are ready for manual review and commit. No commit, push or release
was performed by the preparation script. Private signing materials were excluded.

Committing source or these JSON files alone does not activate Android updates.
To publish this prepared release, manually create a stable GitHub Release in
youriptvus/Draft2Done with the tag specified in latest.json (currently v0.1.0),
and upload both Draft2Done-0.1.0.apk and latest.json as release assets. The build
metadata and signature report are optional supporting assets. Verify that the
release's APK hash matches latest.json. APK binaries are intentionally gitignored;
upload the existing local file using GitHub's Release interface.

Future updates need a higher Android versionCode and the same private release key.
See docs/RELEASING.md. Keep the private signing directory backed up separately.
The current account access/release availability limits are in docs/VERIFICATION.md.
'@
[IO.File]::WriteAllText((Join-Path $releaseDestination 'README.md'),$instructions,(New-Object Text.UTF8Encoding($false)))
Write-Host "Prepared $copied source files and verified release assets in $destination"
Write-Host 'Files remain unstaged. No commit, push or GitHub publication was performed.'
& git -C $destination status --short
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the local checkout status.' }
