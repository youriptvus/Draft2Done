# Signed releases and automatic updates

No repository or release has been published in this session. The user selected `youriptvus/Draft2Done` as the update source; publication still requires explicit authorization.

The user now handles commits and uploads manually. After the final build, run `scripts\prepare-repo.ps1` from the build workspace to copy source and verified release assets into `D:\Draft2Done\repo\Draft2Done`. The script checks that checkout's origin, leaves `.git` intact, excludes signing credentials/data/tools, and performs no staging, commit, push or publication. It does not delete extra checkout files. Review the resulting diff before your manual commit.

`release-assets/` contains the signed APK, `latest.json`, public build/signature records and upload instructions. APKs are intentionally ignored by Git; upload the existing local APK and `latest.json` together through GitHub Releases. A source commit alone is not a release and will not activate the updater.

Build outputs:

- `Draft2Done-0.1.0.apk`: signed Android application.
- `Draft2Done-0.1.0-build.json`: SHA-256, size and package/version details.
- `Draft2Done-0.1.0-signature.txt`: verification and public certificate fingerprints.
- `latest.json`: update manifest with version, versionCode, exact GitHub Release APK URL and SHA-256.

For future updates, increase `android:versionCode` (strictly increasing integer) and `android:versionName`, update `package.json` and build-script version text/output filenames, and rebuild with the **same** private signing key. Publish the APK and `latest.json` together as stable GitHub Release assets under the tag matching the manifest URL, such as `v0.1.0`. Draft and prerelease releases are ignored.

Never upload `signing/credentials.json`, the PKCS12 key, local projects, models or OAuth credentials. Back up the signing directory privately. Losing that key prevents installing a compatible update over this signing identity. Key rotation is not implemented.

The app calls `GET https://api.github.com/repos/youriptvus/Draft2Done/releases/latest`, finds the `latest.json` asset and accepts repository-specific release URLs. Redirects are restricted to GitHub's official asset hosts over HTTPS. The hash, expected package/version and installed certificate are checked before exposing the APK through a private content provider to Android's installer. Android enforces package integrity/signature and asks for confirmation.

The live endpoint returned 404 during verification. Therefore no public release or live update installation is claimed. Android updates signed with the same key retain app-private projects; the updater never uninstalls the app.
