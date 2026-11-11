# Candidate only â€” required verification remains incomplete

This signed build is for review and testing. It is not a fully verified release.
Read docs/VERIFICATION.md and docs/SIGN-IN-REPAIR-0.1.4.md
for the exact blockers and actual test results before considering publication.
No staging, commit, push or publication was performed.

# Local release files

Source changes are ready for manual review and commit. No commit, push or release
was performed by the preparation script. Private signing materials were excluded.

Committing source or these JSON files alone does not activate Android updates.
To publish this prepared release, manually create a stable GitHub Release in
youriptvus/Draft2Done with the tag v0.1.4 (Android versionCode 3),
and upload both Draft2Done-0.1.4.apk and latest.json as release assets. The build
metadata and signature report are optional supporting assets. Verify that the
release's APK hash matches latest.json. APK binaries are intentionally gitignored;
upload the existing local file using GitHub's Release interface.

Future updates need a higher Android versionCode and the same private release key.
See docs/RELEASING.md. Keep the private signing directory backed up separately.
The current account access/release availability limits are in docs/VERIFICATION.md.
