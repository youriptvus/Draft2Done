<#
.SYNOPSIS
Copies reviewed source and release assets into the local GitHub checkout.
.DESCRIPTION
No git add, commit, push or publication is performed. Private signing files,
credentials, app data, tools and test results are never copied. The workspace's
current release folder receives only the current verified assets and instructions.
#>
[CmdletBinding()]
param([string]$RepositoryPath)
$ErrorActionPreference = 'Stop'
$sourceRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
. (Join-Path $PSScriptRoot 'release-info.ps1')
$releaseInfo = Get-Draft2DoneReleaseInfo $sourceRoot
if (!$RepositoryPath) { $RepositoryPath = Join-Path $sourceRoot 'repo\Draft2Done' }
$destination = [IO.Path]::GetFullPath($RepositoryPath)
if ($destination -eq $sourceRoot) { throw 'Choose the separate local GitHub checkout, not the build workspace itself.' }
if (!(Test-Path -LiteralPath (Join-Path $destination '.git'))) { throw "No Git checkout at $destination. Existing files were left unchanged." }
$origin = (& git -C $destination remote get-url origin | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $origin -notmatch '^(https://github\.com/youriptvus/Draft2Done(?:\.git)?/?|git@github\.com:youriptvus/Draft2Done(?:\.git)?)$') {
    throw 'The destination must be the local youriptvus/Draft2Done checkout.'
}
$releaseRoot = Join-Path $sourceRoot 'dist'
$releaseFiles = @($releaseInfo.ApkFileName,$releaseInfo.BuildFileName,$releaseInfo.SignatureFileName,'latest.json')
foreach ($name in $releaseFiles) {
    if (!(Test-Path -LiteralPath (Join-Path $releaseRoot $name) -PathType Leaf)) { throw "Missing current release file $name. Build version $($releaseInfo.VersionName) first." }
}
$metadata = Get-Content -LiteralPath (Join-Path $releaseRoot $releaseInfo.BuildFileName) -Raw | ConvertFrom-Json
if ($metadata.file -cne $releaseInfo.ApkFileName -or $metadata.version -cne $releaseInfo.VersionName -or
    $metadata.versionCode -ne $releaseInfo.VersionCode -or $metadata.package -cne $releaseInfo.PackageName) {
    throw 'Build metadata does not match the current Android/package.json release identity. Rebuild before preparing release assets.'
}
$apk = Join-Path $releaseRoot $releaseInfo.ApkFileName
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hash -ne $metadata.sha256 -or (Get-Item -LiteralPath $apk).Length -ne $metadata.bytes) { throw 'Release APK does not match build metadata. Rebuild before preparing release assets.' }
$manifest = Get-Content -LiteralPath (Join-Path $releaseRoot 'latest.json') -Raw | ConvertFrom-Json
if ($manifest.sha256 -ne $hash -or $manifest.version -cne $releaseInfo.VersionName -or
    $manifest.versionCode -ne $releaseInfo.VersionCode -or $manifest.package -cne $releaseInfo.PackageName -or
    $manifest.apkUrl -cne $releaseInfo.ApkUrl) {
    throw 'Update manifest does not match the current signed APK identity, release URL and hash.'
}
$currentReleaseRoot = [IO.Path]::GetFullPath((Join-Path $sourceRoot 'current release'))
if ([IO.Path]::GetDirectoryName($currentReleaseRoot) -ne $sourceRoot -or
    [IO.Path]::GetFileName($currentReleaseRoot) -cne 'current release') { throw 'Unsafe current release folder path.' }
if (Test-Path -LiteralPath $currentReleaseRoot) {
    $currentDirectory = Get-Item -LiteralPath $currentReleaseRoot -Force
    if (!$currentDirectory.PSIsContainer -or $currentDirectory.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw 'The current release folder must be an ordinary workspace directory.'
    }
}
$currentFiles = @($releaseFiles) + @('README.md')
foreach ($name in $currentFiles) {
    $target = [IO.Path]::GetFullPath((Join-Path $currentReleaseRoot $name))
    if ([IO.Path]::GetDirectoryName($target) -ne $currentReleaseRoot) { throw 'Unsafe current release asset path.' }
    if (Test-Path -LiteralPath $target) {
        $existing = Get-Item -LiteralPath $target -Force
        if ($existing.PSIsContainer -or $existing.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "Refusing to replace a linked or non-file current release asset: $name"
        }
    }
}
$obsoleteCurrentAssets = @()
if (Test-Path -LiteralPath $currentReleaseRoot) {
    $knownArtifactPattern = '^Draft2Done-(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:\.apk|-build\.json|-signature\.txt)$'
    $obsoleteCurrentAssets = @(Get-ChildItem -LiteralPath $currentReleaseRoot -File -Force | Where-Object {
        $_.Name -cmatch $knownArtifactPattern -and $_.Name -cnotin $currentFiles
    })
    foreach ($file in $obsoleteCurrentAssets) {
        if ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($file.FullName)) -ne $currentReleaseRoot -or
            $file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Unsafe previous current release asset.' }
    }
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
foreach ($name in @('.gitattributes','.gitignore','package.json','package-lock.json','README.md')) {
    Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination (Join-Path $destination $name) -Force
    $copied++
}
$releaseDestination = Join-Path $destination 'release-assets'
New-Item -ItemType Directory -Force -Path $releaseDestination | Out-Null
foreach ($name in $releaseFiles) {
    Copy-Item -LiteralPath (Join-Path $releaseRoot $name) -Destination (Join-Path $releaseDestination $name) -Force
}
$instructions = @"
# Local release files

Source changes are ready for manual review and commit. No commit, push or release
was performed by the preparation script. Private signing materials were excluded.

Committing source or these JSON files alone does not activate Android updates.
To publish this prepared release, manually create a stable GitHub Release in
youriptvus/Draft2Done with the tag $($releaseInfo.Tag) (Android versionCode $($releaseInfo.VersionCode)),
and upload both $($releaseInfo.ApkFileName) and latest.json as release assets. The build
metadata and signature report are optional supporting assets. Verify that the
release's APK hash matches latest.json. APK binaries are intentionally gitignored;
upload the existing local file using GitHub's Release interface.

Future updates need a higher Android versionCode and the same private release key.
See docs/RELEASING.md. Keep the private signing directory backed up separately.
The current account access/release availability limits are in docs/VERIFICATION.md.
"@
Write-Draft2DoneReleaseText (Join-Path $releaseDestination 'README.md') $instructions
New-Item -ItemType Directory -Force -Path $currentReleaseRoot | Out-Null
foreach ($name in $releaseFiles) {
    Copy-Item -LiteralPath (Join-Path $releaseRoot $name) -Destination (Join-Path $currentReleaseRoot $name) -Force
}
Write-Draft2DoneReleaseText (Join-Path $currentReleaseRoot 'README.md') $instructions
# Only prior generated release files directly in this verified directory are
# removed. Unknown files and subdirectories remain untouched; dist is archival.
foreach ($file in $obsoleteCurrentAssets) {
    $checkedDirectory = Get-Item -LiteralPath $currentReleaseRoot -Force
    $stalePath = [IO.Path]::GetFullPath($file.FullName)
    $staleFile = Get-Item -LiteralPath $stalePath -Force
    if (!$checkedDirectory.PSIsContainer -or $checkedDirectory.Attributes -band [IO.FileAttributes]::ReparsePoint -or
        [IO.Path]::GetDirectoryName($stalePath) -ne $currentReleaseRoot -or $staleFile.PSIsContainer -or
        $staleFile.Attributes -band [IO.FileAttributes]::ReparsePoint -or $staleFile.Name -cnotmatch $knownArtifactPattern -or
        $staleFile.Name -cin $currentFiles) { throw 'Refusing unsafe previous release asset cleanup.' }
    Remove-Item -LiteralPath $stalePath -Force
}
Write-Host "Prepared $copied source files and verified release assets in $destination"
Write-Host "Newest release files: $currentReleaseRoot"
Write-Host 'Files remain unstaged. No commit, push or GitHub publication was performed.'
& git -C $destination status --short
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the local checkout status.' }
