# Android APK

The Android application packages the local web app in a native Java host. It needs Android 8.0 (API 26) or newer and an up-to-date Android System WebView. The package ID is `io.draft2done.app`; signed candidate 0.1.4/code 3 targets API 36. Clean build and artifact checks passed; required live ChatGPT hosted-search verification remains quota-blocked. Previous 0.1.2 artifacts remain preserved.

## Build on Windows

Install a JDK 17 or newer and Node.js. All downloaded Android packages and generated app files stay inside this project.

```powershell
npm ci
powershell -ExecutionPolicy Bypass -File scripts\build-android.ps1 -Clean
```

The first Android build calls `scripts/setup-android.ps1`, which downloads official, version-pinned Google SDK archives and verifies their published SHA-1 digests before extraction. No Android Studio, Gradle, emulator, global SDK installation, or system settings change is required. The archives are Android platform 36 revision 2, build-tools 36.0.0 and platform-tools 37.0.3. SDK license terms are included in the downloaded packages. A later rebuild can use the cached tools offline.

Expected candidate output: `dist/Draft2Done-0.1.4.apk`, its SHA-256/build metadata, and an `apksigner` verification report. Resource packaging uses AAPT2, Java compiles to Java 8 bytecode, D8 makes the DEX payload, and the APK is ZIP-aligned before signing with APK signature schemes v2 and v3. The script then verifies alignment and the signature.

Release identity comes from the Android manifest and matching package/lockfile metadata. The target is 0.1.4/code 3. The build checks the bundled web version and signed APK's actual package/version; filenames and update URLs are derived automatically. The test APK's generated manifest also derives its version from the release identity. Run `scripts\prepare-repo.ps1` only after validating the final candidate to refresh `D:\Draft2Done\current release` and the local GitHub checkout. No commit or publication is performed.

`-Clean` explicitly removes only the generated `web-dist` output before rebundling, while the Android build always recreates its generated `build/android` directory. Device-test builds recreate their own generated output. These operations preserve downloaded tools, signing credentials, personal app data, source checkpoints and older `dist` artifacts. `-Clean` rejects `-SkipWebBuild`; ordinary incremental builds can still skip the bundle only when its recorded version matches. This fulfills a clean rebuild through the working custom SDK pipeline. No Gradle project or wrapper exists, and no Gradle build is claimed.

## Keep the signing identity

The first build creates a persistent 3072-bit RSA release key in `signing/draft2done-release.p12`. Random credentials are kept in `signing/credentials.json`, excluded from Git. These are the private signing materials for this locally built app; they are not a Google Play signing certificate or a third-party publisher verification.

Back up the entire `signing/` directory in a secure private location. Later APK updates must use the same key. The build refuses to replace a key if either it or its credentials are missing. Never share the signing files with APK recipients, put them in a public repository, or delete them while you still intend to update the installed app.

## Install

Transfer the APK to an Android phone, open it, and allow installation from the particular Files/browser application when Android asks. Android may show its standard warning for an app installed outside a store. An APK is installable without publishing it to Google Play.

With USB debugging enabled and the phone authorized for this computer:

```powershell
.\tools\android-sdk\platform-tools\adb.exe devices
.\tools\android-sdk\platform-tools\adb.exe install -r .\dist\Draft2Done-0.1.4.apk
```

This workstation's authorized Samsung is currently managed by the existing ADB server on port 5038. Use `adb -P 5038 -s R5GL75PGMFP ...`, or `scripts\test-samsung.ps1 -AdbServerPort 5038 -Serial R5GL75PGMFP`. The helpers select that Samsung explicitly and leave the other device and ADB servers unchanged.

Run the full host regression with `scripts\test-all.ps1`. After building the release and separate harness, `scripts\test-all.ps1 -Device -SkipHost -InstallApks` runs isolated native fault, file-byte and update-validation checks, plus standalone phone writing/consent tests. The latter reuse the connected account's real model catalog and simulated assist replies; they make no inference request and need no PC model. `-LiveAI` additionally runs developer-only fictional writing and restart tests through the existing model server. Personal ChatGPT sign-in and publishing are excluded from this runner. Separate host/device reports are saved under `test-results/`.

`-r` updates an app while keeping its private storage. If Android reports a different signing certificate for an existing installation, preserve/export its data and restore the original signing key; do not uninstall it automatically.

## Storage, networking and printing

Projects live in atomic, application-private storage. Android cloud backup is disabled. Export work regularly; removing the app deletes its private workspace. Imports and exports use Android's document picker without broad storage permissions. Each imported/exported file is limited to 10 MB; the saved workspace and native network responses are limited to 4 MB. A failed state write returns an error and preserves the previous saved state.

Only the packaged `https://app.draft2done.local/` origin can load in the main WebView. File/content access, mixed content, external frames, inline scripts and direct web requests are blocked. Browser links open outside the application. Native bridge requests allow HTTPS endpoints and allow HTTP only to numeric loopback/private IPv4 addresses or localhost. Redirects are rejected, certificate verification uses Android's system trust, and requests have bounded response sizes and timeouts. Cleartext is enabled at Android's network layer to support local AI, with the bridge applying the URL restriction. Local AI traffic sent over HTTP can be read by others on the same network; use a trusted LAN or HTTPS.

The primary writing/research route connects Android directly to the user's eligible ChatGPT account over HTTPS; the deployed phone cannot reach the user's firewalled PC. Every writing request requires a fresh exact-data preview and approval. Advanced server controls are for other environments with an already reachable endpoint; `localhost` on a phone refers to the phone itself. No language model or desktop Ollama/LM Studio is bundled. PDF export opens Android Print, where Save as PDF can be selected. AI/research services require connectivity, while editing and workspace storage are local.

Official tool references: [AAPT2](https://developer.android.com/tools/aapt2), [D8](https://developer.android.com/tools/d8), [APK signing](https://developer.android.com/tools/apksigner), [ZIP alignment](https://developer.android.com/tools/zipalign).
