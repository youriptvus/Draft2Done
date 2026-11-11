# Current development and delivery state

Updated 2026-11-10 (America/New_York). Android 0.1.2/code 2 is signed, installed, verified locally and prepared for manual release. The user handles Git commits and GitHub publication manually.

## Standing requirements

- Signed Android APK first; Windows remains deferred.
- Automate edits, builds and tests; involve the user only for authentication.
- The deployed phone cannot reach the user's firewalled PC. Primary AI uses direct phone-to-OpenAI HTTPS. No PC model, USB reverse, forwarding or VPN is required.
- Preserve the elegant dark welcome screen, raised paper illustration, protected originals and reviewable additions.
- Recommend models only from the actual eligible account catalog. No bundled language model, model downloads or paid API-key fallback.
- Every writing request requires a fresh exact-payload preview and approval; changed context invalidates consent and stale replies.
- Automatic update checks default on, targeting `youriptvus/Draft2Done`.
- Prepare source in `D:\Draft2Done\repo\Draft2Done` and newest release files in `D:\Draft2Done\current release`. No automatic staging, commit, push or publication. Leave the separate older `draft2done/` checkout untouched.
- Repository source uses LF, Windows `.bat`/`.cmd` scripts use CRLF, and binaries remain unchanged. Apply `.gitattributes`; do not change global Git settings.

## Release identity

Package `io.draft2done.app`, version 0.1.2/code 2, min API 26/target API 36. The non-debuggable APK disables Android cloud backup. Persistent certificate SHA-256: `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`. APK SHA-256 and final test results are maintained in VERIFICATION.md and generated release metadata.

Private signing material remains in ignored `signing/`; never print, commit or publish it. Future updates require that same key and a higher versionCode. Version names, artifact paths and visible versions now derive from validated package/Android metadata.

## Implemented changes and verification

- Direct native ChatGPT Helper, Complete, regeneration and research use the encrypted account session and actual eligible catalog; no credentials cross the web bridge.
- Reviewed requests preserve the full draft within documented limits, use `store:false`, and require a terminal completed response. Quota failure saves no incomplete result or billing fallback.
- Offline editing, saving, counts, descriptive DNA, deterministic observations, saved research and document conversion remain available.
- 0.1.2 fixes already-used references disappearing from exports after future AI use is disabled, and imports ignoring saved writing/count/export defaults. Portable project backups retain their own preferences.
- Host checks pass: 77 JavaScript tests; 38 OAuth/stream/citation, 7 lifecycle, 39 DNS/registration/quota, 53 assist/session, 12 research-request and 52 update checks; all browser writing/review/management suites and 14 document/add-on checks. Eleven isolated release-metadata checks also pass.
- Seven initial same-key 0.1.0/code 1 to 0.1.2/code 2 checks verified all project/profile/note/active-paper fingerprints, connected account and installed/displayed versions. Later web fixes retain the storage format.
- Seventeen real Samsung document-workflow checks pass on the final APK: TXT/DOCX/PDF saves and all three native-picker reimports, print cancellation/reopening, fictional-file cleanup and exact workspace/backup/account restoration. Real tests corrected duplicate print handoff, printing an error page, generic PDF filenames and spaces inside PDF glyph runs; the actual fictional PDF is a browser regression fixture. See VERIFICATION.md for the first pre-checkpoint crash caveat and private cleanup evidence.
- The final signed APK passed the Samsung suite: 18 storage, 5 document-byte, 24 update/provider and 19 standalone workflow checks. Standalone assistance is simulated after real account/catalog validation. An independently pulled installed APK matches the final release hash exactly. The dark welcome screen is restored; Settings reports connected with no current connection error.
- Nine real refresh checks passed. Actual Helper/Complete/regeneration/query-only research passed with exact consent and protected-original checks; fixture cleanup passed. The original combined run retains a failed web-search/citation assertion, which is not hidden as a fully passing suite.
- Nine real sign-out/revocation checks passed, including one rejected revoked-refresh probe. The user completed ordinary browser authentication and reconnection passed. No revoked credentials were restored.
- Selecting web search requires the tool and confirmed completed search evidence; unconfirmed research is refused before saving. Query-only/writing requests remain isolated. The focused web retest returned no explicit search/citation evidence; a subsequent single public-query diagnostic ended in terminal service error without completed output. No further inference retries are planned. Successful live web search/citations remain unverified, not a passing test.

## External services and delivery

Real browser consent, signed identity, encrypted restart persistence and eligible model loading passed. The observed catalog includes Astra, Sol, Terra and Luna variants; the test selected the recommendation before actual writing. Historical inference returned `subscription_sharing_usage_limit_exceeded`; current successful inference does not establish a reset time or whole-plan balance. Do not automatically retry a usage limit or claim catalog loading alone proves successful writing.

An earlier automatic approval review rejected opening ChatGPT Usage via ADB. That action was not bypassed, and no usage limits were changed.

The user manually published v0.1.0. Its public API, manifest and independently downloaded APK were verified against the signed local build; five live Samsung update checks passed. A real GitHub-delivered 0.1.2 installation remains pending the user's manual publication under `v0.1.2`. Local ADB upgrade testing is distinct from public update delivery.

## Device and scope boundaries

Samsung SM-S166V / Android 16 uses explicit serial `R5GL75PGMFP` on already-running ADB servers 5038 and 5037. Final checks used 5037. Leave other devices/servers alone. Wireless fallback is authorized; mDNS currently discovers no pairing service. The user reapproved USB debugging; same-key installations used `-r` without uninstalling. No production PC model route is configured.

Windows, OCR, exact imported page layout, exhaustive citation types and Chicago footnotes remain deferred/limited. Research uses saved-library caching and manual Verify; configurable cache/verification policies are not implemented. See REQUIREMENTS.md, TASKS.md and VERIFICATION.md for scope and evidence.
