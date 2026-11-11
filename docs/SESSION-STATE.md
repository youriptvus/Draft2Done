# Current development and delivery state

Updated 2026-11-11. **Android 0.1.4/code 3** now includes the browser callback and foreground-service repairs. Real Chrome sign-in on the Moto G 2025/Android 16 completed, survives all QA and an actual force-stop with plan permission intact, and loads the actual model catalog. The temporary sign-in service stops on completion. Broader Moto regression and installed-byte binding passed. AI inference and hosted web search were not rerun. Local source/release-copy validation passed: 108 source files and all four release assets match, with correct line endings and unchanged Git HEAD/index/configuration. No public release has been performed.

## Current artifact

`dist/Draft2Done-0.1.4.apk`: **852668 bytes**, SHA-256 **`33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db`**, package `io.draft2done.app`, min API 26/target API 36. Existing certificate SHA-256 remains `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`.

The clean project-local Android SDK build and independent **24/24 artifact audit passed** (`test-results/signin-foreground-artifact-audit-014.json`). All five bundled web files exactly match current web-dist and frozen Android assets. The compiled non-exported specialUse foreground service, required permissions/subtype and production helper classes are present; test classes are absent. No Gradle project was introduced.

The user requested version 0.1.4/code 3 remain unchanged. Another 0.1.4/code 3 build requires explicit installation of the repaired bytes; do not claim automatic same-code updater delivery. The tested candidate has been copied and validated in both local delivery folders.

## Confirmed sign-in repairs and current evidence

The old loopback listener let an idle peer's five-second read timeout terminate the whole login. The new bounded callback server isolates peer failures, validates requests/state and preserves cancellation and the overall deadline. Actual old 0.1.2 native reproduction passed nine checks; the first repaired native listener also passed nine controlled checks.

That first repaired APK (`c2692f60...`) nevertheless failed real normal Chrome authorization: Android showed its alive callback-owning process as cached/frozen, and the callback/local probe timed out. This failure is preserved as `moto-signin-browser-first-repair-failed.json`. A passing socket fixture was insufficient to prove browser sign-in.

The current temporary `SignInKeepAliveService` starts before the authorization browser and stops on success, cancellation, task removal, failure or a bounded timeout. It is non-exported, user initiated and scoped to the exact attempt. No system freezer/battery setting or OAuth validation was bypassed.

Current Moto evidence under `test-results/`:

- **11 controlled native/service checks PASS**, `moto-signin-listener-foreground-fixed.json`; browser authorization was intercepted, with account/workspace restoration.
- **7 same-key upgrade checks PASS**, `moto-upgrade-014-foreground-fixed.json`.
- **20-second normal Chrome background lifetime PASS**, `moto-signin-foreground-lifetime.json`: cached=false, frozen=false, foreground service active.
- **Human Chrome authorization PASS**, `moto-chatgpt-connected-014.json`: connected after fresh Activity, plan permission enabled, sign-in error cleared, listener closed and service stopped; no inference sent.
- **2 actual eligible model-catalog checks PASS**, `moto-chatgpt-models-014.json`; no inference sent.

After foreground-service compilation, host native checks passed **126 loopback TCP + 38 OAuth/stream + 7 lifecycle + 39 support + 53 assist/session + 12 research-request** checks. The earlier full host pass covered **107 JavaScript + 52 updater**, all writing/review/management and **14 document / 11 paste / 8 information-link** browser groups. Production web files were unchanged by the service patch; those earlier executions are not mislabeled as a fresh whole-host run after it. Browser AI/provider fixtures remain simulated.

The current Moto APK passed **71 standard device checks**, **34 native paste-store checks**, **20 release-UI checks** and **5 setup/16 verification checks across a real force-stop**. Evidence: `all-tests-device.json`, `moto-draft-input-store-014.json`, `moto-release014-ui.json`, `moto-paste-restart-start-014.json` and `moto-paste-restart-verify-014.json`, all under `test-results/`. After every workflow and cold restart, a fresh Activity still reports connected, plan enabled, error cleared, no pending listener and service stopped. The pulled installed APK exactly matches the current dist hash and **852668 bytes**. Summary: `test-results/moto-final-signin-verification-014.json`.

Refreshed copies in `current release` and `repo/Draft2Done` passed exact-byte, line-ending, privacy-exclusion and Git-preservation checks: `test-results/line-endings-local-copy-signin-014.json`. No new controlled Samsung test or repaired Samsung installation is claimed. See [VERIFICATION.md](VERIFICATION.md) for exact current boundaries and [SIGN-IN-REPAIR-0.1.4.md](SIGN-IN-REPAIR-0.1.4.md) for detailed repair history.

## Preserve historical evidence

Published 0.1.2/code 2 and its 90-source checkpoint remain intact. The earlier 579-hash 0.1.4 source/release snapshot preserves 102 source files and four release files under `test-results/checkpoints/v0.1.4-before-signin-repair-20261111`. Its verification record is copied byte-for-byte to [VERIFICATION-0.1.4-PRE-SIGNIN-REPAIR.md](VERIFICATION-0.1.4-PRE-SIGNIN-REPAIR.md). Its Samsung 71 standard, 34 staging, 5/16 restart, 17 document and 55 genuine public-source checks are historical passes for those older bytes.

The first c269 listener-only repair remains under `test-results/checkpoints/v0.1.4-signin-first-repair-c269/release`. Preserve all failed restart, picker, fixture-cleanup and real Chrome reports. Private signing credentials, callback query strings and tokens must never enter source, reports or the local checkout.

The historical Samsung hosted-search error `subscription_sharing_usage_limit_exceeded` / `invalid_request_error` was not rerun in this repair. It does not establish the current Moto account's allowance, and new sign-in/catalog success does not prove writing inference or hosted browsing/citations. Do not retry or switch accounts/models automatically to bypass a limit.

## Standing product and delivery boundaries

Signed Android first; Windows deferred. Primary remote AI uses the phone's native ChatGPT connection; the firewalled PC is not required. Offline editing, protected originals, Writer DNA and documents work without a bundled language model. No model downloads or paid API-key fallback.

Paste Your Draft, synchronous native staging/tombstones, exact Victoria Downs dedication and optional separate contextual information reading remain implemented. Every remote writing request requires fresh exact-payload approval. Research sends only its reviewed query and selected excerpts; external content cannot control settings, file paths or native actions.

Local source/release copies are prepared and verified. No staging, commits, push or publication. Keep unrelated files, old releases, other devices and shared ADB servers unchanged. Repository text uses LF, Windows command scripts CRLF and binary bytes remain intact. User involvement is reserved for required authentication.
