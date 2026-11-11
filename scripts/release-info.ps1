# Shared release identity and canonical text output for build/preparation scripts.
function Write-Draft2DoneReleaseText([string]$Path, [AllowEmptyString()][string]$Text) {
    $canonical = $Text -replace '\r\n?', "`n"
    if (!$canonical.EndsWith("`n")) { $canonical += "`n" }
    [IO.File]::WriteAllText($Path, $canonical, (New-Object Text.UTF8Encoding($false)))
}

function Get-Draft2DoneReleaseInfo([string]$ProjectRoot) {
    $manifestPath = Join-Path $ProjectRoot 'android\AndroidManifest.xml'
    $manifest = [xml](Get-Content -LiteralPath $manifestPath -Raw)
    $androidNamespace = 'http://schemas.android.com/apk/res/android'
    $versionName = $manifest.manifest.GetAttribute('versionName', $androidNamespace)
    if ($versionName -notmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$') {
        throw 'Android versionName must be a stable semantic version such as 0.1.4.'
    }
    $versionCode = 0
    if (![int]::TryParse($manifest.manifest.GetAttribute('versionCode', $androidNamespace), [ref]$versionCode) -or $versionCode -lt 1) {
        throw 'Android versionCode must be a positive 32-bit integer.'
    }
    $packageInfo = Get-Content -LiteralPath (Join-Path $ProjectRoot 'package.json') -Raw | ConvertFrom-Json
    if ($packageInfo.version -cne $versionName) {
        throw "Version mismatch: package.json is $($packageInfo.version), Android manifest is $versionName. Update both before building or testing."
    }
    # Windows PowerShell 5 cannot deserialize npm's packages[""] property.
    # Node reads the lockfile and emits only two nonempty-key identity fields.
    $lockIdentityJson = @'
const fs = require("node:fs");
const lock = JSON.parse(fs.readFileSync(process.argv[2], "utf8").replace(/^\uFEFF/, ""));
process.stdout.write(JSON.stringify({ version: lock.version, rootVersion: lock.packages && lock.packages[""] && lock.packages[""].version }));
'@ | & node - (Join-Path $ProjectRoot 'package-lock.json')
    if ($LASTEXITCODE -ne 0) { throw 'Could not read the package lockfile release identity with Node.' }
    $lockInfo = $lockIdentityJson | ConvertFrom-Json
    if ($lockInfo.version -cne $versionName -or $lockInfo.rootVersion -cne $versionName) {
        throw 'The package lockfile root version must match package.json and the Android manifest.'
    }
    $packageName = $manifest.manifest.GetAttribute('package')
    if ($packageName -cne 'io.draft2done.app') { throw 'Unexpected Android package identity.' }
    $minSdk = 0
    $targetSdk = 0
    if (![int]::TryParse($manifest.manifest.'uses-sdk'.GetAttribute('minSdkVersion', $androidNamespace), [ref]$minSdk) -or
        ![int]::TryParse($manifest.manifest.'uses-sdk'.GetAttribute('targetSdkVersion', $androidNamespace), [ref]$targetSdk) -or
        $minSdk -lt 26 -or $targetSdk -lt $minSdk) { throw 'Invalid Android SDK version declarations.' }
    $baseName = "Draft2Done-$versionName"
    [pscustomobject]@{
        VersionName = $versionName
        VersionCode = $versionCode
        PackageName = $packageName
        ArtifactBaseName = $baseName
        ApkFileName = "$baseName.apk"
        BuildFileName = "$baseName-build.json"
        SignatureFileName = "$baseName-signature.txt"
        Tag = "v$versionName"
        ApkUrl = "https://github.com/youriptvus/Draft2Done/releases/download/v$versionName/$baseName.apk"
        ManifestPath = $manifestPath
        MinSdkVersion = $minSdk
        TargetSdkVersion = $targetSdk
    }
}
