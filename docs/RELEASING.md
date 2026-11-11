# Signed releases and automatic updates

Signed candidate: **0.1.4, Android version code 3**. Chrome sign-in is repaired and verified on the Moto G; [current verification](VERIFICATION.md) binds the executed tests to the installed APK. Required hosted-search verification remains incomplete, so the overall release is not fully verified. The previously inspected public release was [v0.1.2](https://github.com/youriptvus/Draft2Done/releases/tag/v0.1.2), package/version 0.1.2/code 2, despite the page's mismatched display title at that inspection. No new public-release inspection or publication is part of this local repair.

At the user's request, this repair retains 0.1.4/code 3. A phone already running earlier 0.1.4/code 3 bytes needs explicit manual installation of the repaired APK; the automatic updater requires a strictly higher versionCode. Do not describe this same-code repair as an automatic update. Future releases must increase versionCode.

Before edits, source and four release assets were preserved at `test-results/checkpoints/v0.1.2-before-0.1.4-20261111`; historical versioned `dist` files remain intact. Keep both 0.1.0 and 0.1.2. The new version must use the same signing key and package `io.draft2done.app` to preserve installed projects.

## Prepare a candidate locally

The project uses project-local Android SDK tools, not Gradle. After integrated source freeze, run `scripts\build-android.ps1 -Clean` for fresh generated web/native output, followed by the separate device-test build and regression checks. Do not claim a Gradle build. `-Clean` preserves tools, signing files and old `dist` assets and cannot be combined with `-SkipWebBuild`.

Current derived output names:

- `Draft2Done-0.1.4.apk`
- `Draft2Done-0.1.4-build.json` (hash, size, package, version and versionCode)
- `Draft2Done-0.1.4-signature.txt`
- `latest.json` (version 0.1.4/code 3 and exact v0.1.4 GitHub Release asset URL)

Verify the actual package/version/code, bundled web version, signatures, ZIP alignment and independently computed hash before preparation. Test the same-key upgrade without uninstalling or clearing app data.

For this blocked candidate, run `scripts\prepare-repo.ps1 -Candidate` to copy reviewed source and signed assets to `D:\Draft2Done\repo\Draft2Done` and refresh `D:\Draft2Done\current release`. The candidate flag places an incomplete-verification notice in both delivery folders. The script verifies the checkout origin, excludes private signing/data/tools, leaves `.git` intact and does not stage, commit, push or publish. Only old recognized generated files directly in `current release` are removed after copying the new files. Old `dist` files and unrelated checkout files remain untouched.

## Release gate and manual publication

Successful real ChatGPT web search remains unverified. The earlier pre-repair Samsung baseline returned `subscription_sharing_usage_limit_exceeded`; this sign-in repair sent no inference and does not establish the current account's usage allowance. Candidate packaging may proceed, but it must not be called fully verified or release-ready while the required genuine tool check is incomplete. Preserve failed/blocked evidence and report it in VERIFICATION.md.

Publication requires the owner's explicit authorization. If authorized later, manually create a stable GitHub Release tagged **v0.1.4**, with a matching title, and upload the exact current `Draft2Done-0.1.4.apk` and `latest.json` together. Committing source or JSON alone does not activate updates. APKs are intentionally gitignored. Draft/prerelease releases are ignored by the updater.

The updater reads `https://api.github.com/repos/youriptvus/Draft2Done/releases/latest`, restricts release URLs and redirects to approved GitHub HTTPS hosts, and checks APK hash, package, strictly increasing code and installed signing certificate. Android controls final installation approval; the updater never uninstalls the app. A local ADB upgrade test does not substitute for a real published-update installation test.

Never upload `signing/credentials.json`, the private PKCS12 key, personal projects, OAuth tokens or model files. Losing the signing key prevents compatible updates; key rotation is not implemented. Future releases increase Android versionCode and keep Android/package/lockfile versions consistent; the helpers derive names, tags and URLs from that metadata.
