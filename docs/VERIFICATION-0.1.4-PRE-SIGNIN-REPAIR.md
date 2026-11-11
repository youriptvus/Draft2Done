# Verification record — Android 0.1.4

Status: **signed candidate; required hosted web-search verification is BLOCKED, so not release-ready**. Updated 2026-11-11. The user changed the attachment's 0.1.3 target to 0.1.4. Current results below were actually executed; preserved 0.1.2 results remain historical.

## Release identity and preservation

Package `io.draft2done.app`, version 0.1.4/code 3, min API 26/target API 36. The existing certificate SHA-256 is `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`. Current candidate after the pasted-draft durability repair: `dist/Draft2Done-0.1.4.apk`, **844476 bytes**, SHA-256 **`579a7a2abc63130afe5fe2b6bb3b92735f2e3cd36a34ef6b2f45d683f08a9f4c`**. All **16 independent artifact checks passed**, including v2/v3 signing, alignment, bundle/source version agreement, five exact bundled assets, native durable-staging and web bridge markers, paste/link/dedication markers and previous stable preservation. Evidence: `test-results/final-artifact-audit-014.json`. The earlier `1612569e...` APK is a historical candidate before this repair.

The APK pulled from the Samsung matches the current dist APK byte-for-byte: SHA-256 `579a7a2abc63130afe5fe2b6bb3b92735f2e3cd36a34ef6b2f45d683f08a9f4c`, 844476 bytes. Evidence: `test-results/final-installed-binding-014.json`. Local candidate-copy validation passed: all 102 source files match the checkout, all four release assets match across dist, `current release` and checkout `release-assets`, text/command/binary attributes are correct, and HEAD/index/Git configuration are unchanged. Evidence: `test-results/line-endings-and-local-copy-014.json`. Nothing was staged, committed, pushed or published.

The prior stable APK remains `dist/Draft2Done-0.1.2.apk`, SHA-256 `4b364483f50a94d2a4bf639e771484cf4c267c058abc0cec61ba6d71a1dbcff5`. Its source/release checkpoint is `test-results/checkpoints/v0.1.2-before-0.1.4-20261111`. Historical executed results are preserved verbatim in [VERIFICATION-0.1.2.md](VERIFICATION-0.1.2.md), with earlier results in VERIFICATION-0.1.0.md. Neither history is a current pass report.

## Current genuine live-search blocker

The new real Samsung baseline diagnostic ended with error code **`subscription_sharing_usage_limit_exceeded`**, type **`invalid_request_error`**, before any search tool or terminal completion evidence. Report: `test-results/device-web-diagnostic-014-baseline.json`. This identifies the observed request blocker as a usage limit; it does not establish the account's overall balance, reset time, future allowance or an unsupported-tool policy.

No further ChatGPT inference, automatic model/account switch or billing fallback will be used to evade that limit. Successful live ChatGPT web search and citations remain **BLOCKED**, not resolved. Earlier fluent answers and prose URLs did not prove browsing; requested research without confirmed completed search evidence is refused before saving.

Free public scholarly/encyclopedic search was tested separately with genuine responses and several distinct queries. A passing free-provider check is not successful ChatGPT web-tool execution.

## Passed host and historical first-candidate phone checks

The full host runner passed again after the durability repair on frozen production source: **107 JavaScript tests**, **38 OAuth/stream**, **7 lifecycle**, **39 DNS/registration/research-status**, **53 assist/session**, **12 research-request** and **52 update-validation** checks. All writing, review and management suites passed, plus **14 document/add-on**, **11 paste/About** and **8 information-link** browser groups. Provider and AI fixtures in the browser suites are explicitly simulated. Evidence: `test-results/all-tests-host.json`, `paste-ui-report.json`, `information-links-ui-report.json`.

Paste checks include whitespace/Unicode/leading blank lines, long and oversized drafts, failed saves, separate new-project protection, autosave-off staging, navigation/reload/pause, and responsive layouts. Link checks cover bounded/repeated/Unicode concepts, relevant saved evidence, actual approved Helper/Complete payload contents with simulated inference, explicit-only opening/query transmission, editing/stale offsets, unchanged exports/citations, offline/errors/retry and save/reopen. Dialog headings display untrusted HTML literally; apostrophes and ampersands retain correct accessible names.

The same-key first 0.1.4 installation passed **7 upgrade checks** preserving all project/profile/note/active-paper and connected-account fingerprints, installed identity and displayed versions. **19 phone UI checks** passed physical touch and keyboard, portrait/landscape rotation, staging navigation/WebView reload, new protected project creation, both modes, non-destructive concept reading, exact dedication and exact QA restoration. Reports: `device-upgrade-014-verify.json`, `device-release014.json`. These are historical first-candidate passes, preceding the durability repair; the current final upgrade pass is recorded separately below.

The first signed candidate passed **54 genuine public-research and UI checks** on the Samsung: four official API queries, returned metadata/excerpts, source relevance, explicit browser opening/return, actual Wi-Fi/mobile-data disabling, understandable offline failure, unchanged drafts/library, restored radios and successful explicit retry. It restored exact workspace/backup bytes, pending paste and account. Evidence: `test-results/device-research014-live.json`. No ChatGPT inference was sent by this suite.

| Genuine query | Provider | Representative returned record |
| --- | --- | --- |
| `photosynthesis` | Wikipedia | [Photosynthesis](https://en.wikipedia.org/wiki/Photosynthesis), introduction retrieved |
| `French Revolution` | Wikipedia | [French Revolution](https://en.wikipedia.org/wiki/French_Revolution), introduction retrieved |
| `urban green space wellbeing` | Crossref | [Urban green space, social equity and human wellbeing](https://doi.org/10.1016/b978-0-12-820730-7.00007-0), bibliographic metadata |
| `photosynthesis chlorophyll` | PubMed | [Evaluate the photosynthesis and chlorophyll fluorescence of Epimedium brevicornu Maxim.](https://pubmed.ncbi.nlm.nih.gov/36376438/), abstract retrieved |

These records overlap their query terms; they are examples for reviewing evidence, not proof of a paper's claims. Four earlier, separate public endpoint availability checks returned Wikipedia HTTP 200/200, DOI HTTP 302 to the publisher and PubMed HTTP 203. No paywall was bypassed or full-text access claimed. The current artifact audit retains these explicitly as historical endpoint checks; it did not repeat network requests.

## Current durable-staging and phone evidence

A real force-stop test exposed that WebView localStorage alone did not reliably retain an unsubmitted paste. The repaired candidate uses synchronous private native AtomicFile staging, validates the committed contents before acknowledging success and writes a cleared tombstone so stale browser data cannot migrate again. The original failed restart reports remain preserved: `device-paste-restart-start-014-attempt1.json` and `device-paste-restart-verify-014-attempt1.json`.

The rebuilt APK passed **34 isolated native staging checks**, **5 cold-process setup checks** and **16 verification checks after an actual force-stop**. These verify exact Unicode/leading blank lines, pending selection/scroll, the saved paper and protected original, new-instance reload, malformed/bounds refusal, backup recovery, failed writes, clear/history behavior and restoration of pre-test state. Evidence: `device-draft-input-store-014-final.json`, `device-paste-restart-start-014-final.json`, `device-paste-restart-verify-014-final.json`.

The current candidate also passed **17 actual document-workflow checks** (`device-document-workflow-014-final.json`) and **55 genuine public-source/UI/offline/retry checks** (`device-research014-live-014-final.json`), including restoration of native staging. No ChatGPT inference was sent by these workflows.

An earlier standard run passed 66 checks, but a subsequent original-baseline comparison failed. The failure report is retained as `device-upgrade-014-cleanup-failed.json`; an active-selection-only recovery attempt also failed and remains in `device-upgrade-selection-recover-014-final.json`. Read-only comparison then proved that the first 10 of 12 current projects, with the original active selection, matched the **unchanged pre-upgrade workspace hash exactly**. The two added tail records were recent, exact known fictional writing-test fixtures left by the older harness; the original project data had not drifted.

Guarded cleanup required that exact original hash and each removable fixture's known title, date and original text before any mutation, with a private pre-repair backup. The corrected native commit/readback and normal-reload workflow passed **10 preservation-repair checks** (`device-preservation-repair-014.json`). Its earlier memory-only attempt did not persist because a direct test state mutation did not mark the app dirty; that report remains `device-preservation-repair-014-memory-attempt.json` and is not the final repair evidence.

The corrected full-snapshot standalone harness then passed the full standard runner's **71 checks**: 18 native recovery, 5 document-byte, 24 updater and 24 standalone/consent checks (`test-results/all-tests-device.json`). A subsequent **7-check final upgrade verification passed against the original unchanged 0.1.2 baseline**, confirming the original projects, draft text, revisions, profiles, notes, active paper and connected-account fingerprint, plus all version displays (`device-upgrade-014-final.json`). These passes resolve the fixture-cleanup failure without discarding its evidence.

## 0.1.4 verification matrix

| Area | Current result |
| --- | --- |
| Preservation of 0.1.2 source/APK | Checkpoint created before edits; previous hash recorded above |
| Metadata and clean build | PASS: clean existing-SDK build and 16 independent final-artifact checks |
| JavaScript/domain/service suites | PASS: 107 current tests, including 21 contextual-link and 9 research-failure tests |
| Native OAuth/stream/research/update boundaries | PASS: current 38/7/39/53/12/52 host checks |
| Browser writing/review/management/documents | PASS: all current suites, including 14 document groups |
| Paste draft safety, long text, original protection and persistence | PASS: 11 browser groups, 34 native-store checks and 5 setup/16 verification checks across actual force-stop |
| About 0.1.4 and exact Victoria Downs dedication | PASS: current browser and first-candidate actual phone |
| Contextual links, toggle, relevance, editing, export exclusion and external opening | PASS: current unit/browser and first-candidate genuine-source phone workflow |
| Genuine multi-query public research and failures/cancellation | PASS: current candidate's 55 real public-provider/UI/offline/retry checks; cancellation/timeouts/HTTP/malformed/empty/slow cases simulated |
| Live ChatGPT hosted web search/citations | BLOCKED by observed subscription-sharing usage limit |
| Helper/Complete with approved source evidence | PASS with clearly simulated current browser/device inference; actual fresh plan inference BLOCKED by usage limit |
| Signed install/upgrade, launch, keyboard, rotation, lifecycle and saving/reopen | PASS: current standard 71 checks, real cold-process checks, 10 guarded preservation-repair checks and final 7-check comparison against the unchanged original upgrade baseline |
| Actual TXT/DOCX/PDF save and reimport | PASS: all 17 actual picker/print/save/reimport/cleanup/restoration checks on the final APK |
| Offline/disconnect/reconnect, malformed/empty/error/timeout responses | PASS: actual radio disable/reconnect/retry on first candidate plus simulated failure suite |
| Installed APK byte binding | PASS: extracted Samsung APK matches audited dist hash and size |
| Final release notes/upload files/local checkout | PASS: 102 exact source copies, four exact release assets in both delivery folders, correct line endings/binary attributes, unchanged Git HEAD/index/configuration; explicit candidate notices; no publication |

Two earlier document runs failed safely and retained their reports (`device-document-workflow-014-attempt1.json`, `device-document-workflow-014-attempt2.json`). Screenshot/accessibility inspection established a test selector defect: the picker search input contained the same UUID filename as the real file. The harness could click that editor and then cancel the picker with Back. The correction selects only a non-editable file label/card, waits for genuine picker completion and records bounded diagnostics on failure. No production import change was needed. The corrected full workflow passed; all failed runs also restored the exact workspace/backup/account and removed only their fictional files.

A candidate can be built and clearly labeled with remaining blockers. Do not call 0.1.4 fully tested while the required genuine hosted web check is blocked. Final reporting must distinguish passed, failed, blocked and unexecuted checks, with exact evidence and artifact identity.

## Build system

The working project uses AAPT2, javac, D8, ZIP alignment and APK signing through project-local Android SDK tools. There is no Gradle project or wrapper. `scripts/build-android.ps1 -Clean` performs a clean rebuild of generated web/native outputs without rewriting the build architecture; it is not described as a Gradle run. Private signing files and previous releases are preserved.
