# Verification record - Android 0.1.4 sign-in repair

Updated 2026-11-11. **Real Chrome sign-in on the Moto G 2025/Android 16 now completes successfully on the foreground-service repair.** The eligible account model catalog, broader Moto regression, real cold restart and installed-byte binding also passed. The connected session still works after all tests and force-stop; its temporary sign-in service has stopped. Local source/release-copy validation passed: 108 source files and all four release assets match, line endings are correct, and Git HEAD/index/configuration are unchanged. Successful sign-in does not establish successful AI inference or hosted web research; neither was rerun for this repair.

## Current artifact and preserved history

Current signed APK: `dist/Draft2Done-0.1.4.apk`, package `io.draft2done.app`, **0.1.4/code 3**, min API 26/target API 36, **852668 bytes**, SHA-256 **`33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db`**. Existing certificate SHA-256: `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`.

The independent foreground-repair artifact audit passed **24/24 checks**: signing/certificate, v2/v3 and one signer, alignment, metadata/update manifest, exact current web bundle and frozen build assets, compiled non-exported specialUse service/permissions/subtype, production class markers, absence of instrumentation classes and preserved historical files. Evidence: `test-results/signin-foreground-artifact-audit-014.json`.

The APK pulled from the Moto is **852668 bytes and matches the current dist SHA-256 exactly**. The combined final record is `test-results/moto-final-signin-verification-014.json`. This is current Moto evidence; no new controlled Samsung test or repaired Samsung installation is claimed.

The user requested that this repair remain 0.1.4/code 3. Another 0.1.4/code 3 installation needs an explicit install of these repaired bytes; the updater's increasing-version-code rule does not automatically deliver this same-code replacement. No publication, staging, commit or push has been performed for this repair.

Earlier artifacts and their executed results remain separate:

| Historical artifact | Preservation and evidence |
| --- | --- |
| Published 0.1.2/code 2, SHA `4b364483f50a94d2a4bf639e771484cf4c267c058abc0cec61ba6d71a1dbcff5` | `dist/Draft2Done-0.1.2.apk`, 90-source checkpoint `test-results/checkpoints/v0.1.2-before-0.1.4-20261111`, [VERIFICATION-0.1.2.md](VERIFICATION-0.1.2.md) |
| Pre-sign-in-repair 0.1.4, SHA `579a7a2abc63130afe5fe2b6bb3b92735f2e3cd36a34ef6b2f45d683f08a9f4c`, 844476 bytes | 102-source/four-release-file checkpoint `test-results/checkpoints/v0.1.4-before-signin-repair-20261111`; exact checkpoint verification copied to [VERIFICATION-0.1.4-PRE-SIGNIN-REPAIR.md](VERIFICATION-0.1.4-PRE-SIGNIN-REPAIR.md) |
| First listener-only repair, SHA `c2692f60ee352766fb11a1ff78e7153472ebdd05d1aed4275cdafa18b89ba99e`, 848572 bytes | Four release files under `test-results/checkpoints/v0.1.4-signin-first-repair-c269/release`; historical `signin-repair-artifact-audit-014.json` |

The prior 579 artifact's Samsung **71 standard**, **34 native staging**, **5 setup/16 cold-process**, **17 actual document** and **55 genuine public-research/UI** checks are historical passes for those bytes. They are not current repaired-APK device passes. Its fixture-cleanup investigation, exact original-baseline recovery and failures remain in the archived record. The previous artifact reports were not overwritten.

## Actual sign-in evidence

The detailed repair history is in [SIGN-IN-REPAIR-0.1.4.md](SIGN-IN-REPAIR-0.1.4.md). The outcomes below distinguish the two observed defects and the completed normal browser flow.

| Executed check | Result and scope |
| --- | --- |
| Actual old 0.1.2 native idle-client reproduction | **9 PASS**, `moto-signin-listener-old.json`. An idle loopback socket times out the old whole listener. Browser authorization was intercepted; no OAuth request was sent. |
| First listener-only repair native regression | **9 PASS**, `moto-signin-listener-fixed.json`. Idle peers no longer terminate the pending listener. This controlled pass did not prove normal browser completion. |
| Actual normal Chrome on the first repair | **FAILED**, `moto-signin-browser-first-repair-failed.json`. Chrome timed out at the loopback callback while the app PID was alive but Android reported it cached/frozen; an independent local probe timed out. This failed build remains historical. |
| Foreground-service repair controlled listener/lifecycle | **11 PASS**, `moto-signin-listener-foreground-fixed.json`. The exact attempt has an actual active foreground service, survives the idle-client wait and releases the service on cancellation. Browser authorization was intercepted; prior account/workspace state was restored. |
| Current Moto same-key upgrade | **7 PASS**, `moto-upgrade-014-foreground-fixed.json`. Workspace/account fingerprints and installed/native/welcome/About version agreement are preserved. |
| Normal Chrome foreground wait outside instrumentation | **PASS**, `moto-signin-foreground-lifetime.json`. After 20 seconds with Chrome foregrounded, the app was noncached, unfrozen and running its sign-in foreground service. This observation preceded consent completion. |
| Human Chrome consent, callback and validated connection | **PASS**, `moto-chatgpt-connected-014.json`. A fresh Activity reports connected with plan permission, cleared sign-in error and no pending listener; the temporary foreground service has stopped. No provider inference was sent. |
| Actual eligible model catalog API | **2 PASS**, `moto-chatgpt-models-014.json`. The connected account's catalog loads using real provider traffic; no inference request was made. |

All reports above are under `test-results/`. No callback query, authorization code, state, verifier, token or email is reproduced in these records. No battery/freezer exemption, browser-security bypass, public callback, PC forwarding or API-key billing fallback was introduced.

## Current host and device regression

After service compilation, the targeted native runner passed **126 real loopback TCP checks**, **38 OAuth/stream**, **7 lifecycle**, **39 DNS/registration/research-status**, **53 assist/session** and **12 research-request** checks. Synthetic socket/code fixtures exercise transport and validation without personal authorization or inference.

The complete host runner before the foreground-service patch passed **107 JavaScript** and **52 updater** checks, all writing/review/management browser suites, **14 document/add-on**, **11 paste/About** and **8 information-link** groups. The production web files are unchanged by the service patch and match the current APK exactly. Those browser/provider/AI fixtures remain explicitly simulated; this earlier whole-host pass is not represented as a new post-service full-run execution. Evidence includes `all-tests-host.json`, `paste-ui-report.json` and `information-links-ui-report.json`.

The current Moto APK passed **71 standard checks** (`all-tests-device.json`), **34 native paste-store checks** (`moto-draft-input-store-014.json`), **20 actual release-UI checks** (`moto-release014-ui.json`) and **5 setup/16 verification checks across an actual force-stop** (`moto-paste-restart-start-014.json`, `moto-paste-restart-verify-014.json`). These verify the current device's storage, protected original, staging/selection/Unicode, navigation, keyboard/rotation, contextual reader and dedication without provider inference. After those workflows and the real cold restart, a fresh Activity still reports connected with plan permission, no sign-in error or pending listener, and the foreground service stopped. Summary: `moto-final-signin-verification-014.json`. Report paths in this paragraph are under `test-results/`.

| Current candidate area | Current result |
| --- | --- |
| Clean existing-SDK build and independent artifact audit | PASS; 24 artifact checks, hash above |
| Post-service native protocol/TCP regression | PASS: 126 + 38/7/39/53/12 |
| Real Moto browser sign-in, background lifetime and eligible catalog | PASS as detailed above |
| Broader Moto standard/device regression | PASS: 71 checks on the current repaired APK |
| Current Moto native paste store, UI and real cold-process staging | PASS: 34 native-store, 20 UI and 5 setup/16 post-force-stop verification checks |
| Current repaired-APK TXT/DOCX/PDF and live public-source rerun | No current repaired-APK pass claimed; prior 17/55-check Samsung workflows are archived |
| Extracted installed APK versus current dist bytes | PASS: exact 33435ac8... hash and 852668-byte match |
| Refreshed local checkout and `current release` copies | PASS: 108 exact source copies and all four exact release assets; LF/CRLF and binary checks pass, private directories excluded, Git HEAD/index/configuration unchanged. Evidence: `test-results/line-endings-local-copy-signin-014.json`; no public release performed |
| AI writing and hosted ChatGPT web/citations | NOT RERUN for this sign-in repair |

## Historical hosted-search limit

The earlier Samsung diagnostic returned `subscription_sharing_usage_limit_exceeded` / `invalid_request_error` before completed search evidence (`device-web-diagnostic-014-baseline.json`). That historical failure remains preserved and was not bypassed or relabeled as success. It does not establish this Moto account's current balance, reset time or eligibility. The successful new sign-in and model catalog do not resolve or retest hosted web search, citations or writing inference. No automatic model/account switch or billing fallback was used to evade a limit.

## Build and delivery boundaries

The project uses AAPT2, javac, D8, ZIP alignment and APK signing through the project-local Android SDK. There is no Gradle project or wrapper; the clean build is not described as Gradle execution. Private signing files, historical source snapshots, old releases and failed reports remain preserved. Current claims must stay tied to the exact artifact and executed report; pending checks become passes only when their results are available.
