# Signed releases and automatic updates

The current prepared release is **0.1.2, Android version code 2**. Its APK and GitHub upload files are in `D:\Draft2Done\current release`. The user previously published [v0.1.0](https://github.com/youriptvus/Draft2Done/releases/tag/v0.1.0); its public downloads were verified. All commits/uploads remain manual; the development agent performs no staging, commits, push or publication.

After the final build, run `scripts\prepare-repo.ps1` from the build workspace to copy source and verified release assets into `D:\Draft2Done\repo\Draft2Done` and refresh `D:\Draft2Done\current release`. The script checks that checkout's origin, leaves `.git` intact, excludes signing credentials/data/tools, and performs no staging, commit, push or publication. It does not delete extra checkout files. Only older recognized generated artifacts directly inside `current release` are removed after the new files are copied; historical `dist` outputs and unrelated files are retained. Review the resulting diff before your manual commit.

`release-assets/` contains the signed APK, `latest.json`, public build/signature records and upload instructions. APKs are intentionally ignored by Git; upload the existing local APK and `latest.json` together through GitHub Releases. A source commit alone is not a release and will not activate the updater.

Build outputs:

- `Draft2Done-0.1.2.apk`: signed Android application.
- `Draft2Done-0.1.2-build.json`: SHA-256, size and package/version details.
- `Draft2Done-0.1.2-signature.txt`: verification and public certificate fingerprints.
- `latest.json`: update manifest with version, versionCode, exact GitHub Release APK URL and SHA-256.

For this release, manually commit/push the reviewed source and create a stable GitHub Release tagged **`v0.1.2`**. Upload **`Draft2Done-0.1.2.apk`** and **`latest.json`** from `current release` together. Keep v0.1.0 intact. Version code 2 enables an upgrade from 0.1.0/code 1; users already running 0.1.2 are up to date. A future release must increase `android:versionCode` and update `android:versionName`, `package.json` and the lockfile's root package version. Build and preparation helpers derive filenames, tags and URLs and reject mismatched package/manifest versions. Rebuild with the **same** private signing key. Draft/prerelease releases are ignored by the updater.

Never upload `signing/credentials.json`, the PKCS12 key, local projects, models or OAuth credentials. Back up the signing directory privately. Losing that key prevents installing a compatible update over this signing identity. Key rotation is not implemented.

The app calls `GET https://api.github.com/repos/youriptvus/Draft2Done/releases/latest`, finds the `latest.json` asset and accepts repository-specific release URLs. Redirects are restricted to GitHub's official asset hosts over HTTPS. The hash, expected package/version and installed certificate are checked before exposing the APK through a private content provider to Android's installer. Android enforces package integrity/signature and asks for confirmation.

The v0.1.0 public APK's size, hash, package/version, target API and v2/v3 signing certificate were verified, and the Samsung successfully fetched that public manifest. The local 0.1.0→0.1.2 same-key upgrade preserved the workspace and encrypted account; welcome/About/native version displays agree. A production GitHub-delivered 0.1.2 installation can be verified after the user publishes that release. The updater never uninstalls the app. See VERIFICATION.md for executed checks and remaining limits.
