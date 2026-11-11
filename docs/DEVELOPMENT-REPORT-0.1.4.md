# Historical Draft2Done 0.1.4 development report — before sign-in repair

> **HISTORICAL: pre-sign-in-repair artifact `579a7a2a…`.** This report records the earlier 844476-byte APK and its feature/durability investigation. Its references to “current,” “final” or “repaired” describe that historical handoff, not the newer sign-in repair. The preserved APK/source are under `test-results/checkpoints/v0.1.4-before-signin-repair-20261111`. For the current 852668-byte `33435ac8…` APK, successful normal Chrome sign-in on the Moto, exact current test scope and remaining verification, read [the sign-in repair report](SIGN-IN-REPAIR-0.1.4.md) and [current verification](VERIFICATION.md). The earlier hosted-search quota diagnostic does not establish the newly connected account's current usage; no inference or hosted search was rerun during sign-in repair.

Recorded 2026-11-11. The user's explicit **0.1.4** target supersedes the attachment's 0.1.3 references. Previous stable version: 0.1.2. See [the preserved release brief](RELEASE-BRIEF-0.1.4.md).

**Historical status for `579a7a2a…`: the repaired signed APK passed full host regression, actual native-store and force-stop tests, 71 standard device checks, 17 document checks, 55 genuine public-provider/device checks and a 16-check independent artifact audit. A separate exact-baseline preservation issue caused by appended fictional QA records was resolved and durably verified: all ten original projects and the original active selection/account matched the unchanged pre-upgrade baseline after a fresh harness launch. The installed APK and local candidate copies matched that identified signed APK at this handoff. Required live ChatGPT web-search verification was blocked; this was not a claim that every release acceptance condition passed.**

## Historical artifact and preservation

| Item | Recorded identity |
| --- | --- |
| Preserved pre-sign-in-repair APK | `D:\Draft2Done\test-results\checkpoints\v0.1.4-before-signin-repair-20261111\release\Draft2Done-0.1.4.apk` |
| Package | `io.draft2done.app` |
| Version / Android version code | **0.1.4 / 3** |
| Android compatibility | Android 8.0+; min API 26, target API 36; current Android System WebView required |
| Repaired APK size | **844476 bytes** |
| Repaired APK SHA-256 | `579a7a2abc63130afe5fe2b6bb3b92735f2e3cd36a34ef6b2f45d683f08a9f4c` |
| Preserved certificate SHA-256 | `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd` |
| Signing | Existing release identity retained; v2/v3 signature/alignment and independent final artifact audit passed |

The **pre-durability-fix candidate** was 844476 bytes, SHA-256 `1612569edfd75a7fbc3060b9d4a24fea2c9435d4f6b9af747ea43c391dadb9a8`. Its signature/alignment and broad device suites passed, but the later true process-stop test exposed pending-paste loss. That APK is historical evidence, not the final repaired release. Its passing reports must not be bound to a new APK hash without rerunning the affected checks.

The existing SDK build pipeline was retained: esbuild, AAPT2, javac, D8, ZIP alignment and APK signing. Clean rebuilding uses `scripts/build-android.ps1 -Clean`. There is no Gradle project/wrapper, and no Gradle execution is claimed. The package ID and signing identity are preserved. After the scoped fictional-QA cleanup described below, the complete original workspace and connected account again match the unchanged pre-upgrade fingerprint. Independent installed-byte comparison matches the APK SHA-256 above.

Before implementation, **90 source files and the exact 0.1.2 APK** were preserved at `D:\Draft2Done\test-results\checkpoints\v0.1.2-before-0.1.4-20261111`. Its manifest records source hashes, Git state and APK identity. The previous APK remains `dist/Draft2Done-0.1.2.apk`, SHA-256 `4b364483f50a94d2a4bf639e771484cf4c267c058abc0cec61ba6d71a1dbcff5`. Existing uncommitted work was retained; this checkpoint is not represented as a new Git commit. No staging, commit, push or public release was performed.

## Implemented changes

- Added the prominent **Paste Your Draft** input above the existing import controls, with the exact requested placeholder and **Use This Draft** button. Standard textarea selection, copying, pasting and scrolling remain available.
- Android staged paste now uses a separate private `draft-input.json` file committed synchronously through `AtomicFile`. The native load/save/history bridge is independent of workspace Autosave. WebView localStorage is a compatibility mirror rather than durability truth; browser-only previews retain their localStorage fallback. Actual force-stop/restart verification now passes with exact text, selection and prior user-state restoration. Storage failures are visible, and no paste action uploads writing.
- **Use This Draft** validates nonempty text, the existing 10 MiB document limit and the 4 MiB serialized workspace limit. It creates and durably saves a separate project before clearing staging. A rejected size or failed save preserves the current workspace and unaccepted paste. Accepted text is not trimmed or rewritten; Unicode, punctuation, spelling, line breaks and leading blank lines remain intact.
- Pasted papers use the existing defaults, protected original, working copy, Writer DNA, assignment goals, revisions, Helper and Complete workflows. Existing document imports remain available.
- Added an optional **Read with information links** view in My draft, Helper and Complete. Local bounded rules suggest concepts, academic terminology and names. Highlighting defaults off, is configurable, and updates after editing. The separate selectable reader leaves the normal editor, original, formal citations and exported text unchanged.
- Tapping a concept opens a review sheet. Relevant retrieved library records and confirmed completed web-brief citations can supply destinations. The app does not invent canonical URLs or open/search automatically. An editable short query can be handed to Research; the user still chooses whether to enable Online and start the search. Public URL syntax, relevance, repeated links and stale ranges are checked conservatively.
- Added the permanent About statement: **Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.** About retains the application name, dynamic version and description. Browser and Samsung checks cover the exact wording and responsive presentation.
- Escaped all dialog headings consistently. Information concepts containing apostrophes or ampersands retain correct visible and accessible names; malicious HTML-like suggested titles remain text.
- Added paste, contextual-link and provider-failure regression coverage, current-release device checks, durable QA restoration and richer sanitized web-diagnostic error evidence. Version metadata and release helpers now target 0.1.4/code 3.
- Added one-time legacy-paste migration, a committed empty tombstone that prevents consumed text from returning through a stale mirror, and early load-error protection. A corrupt/unreadable saved paste is reported and retained rather than silently overwritten during pause. The UI reports a saved paste only after a successful native write.

The established per-request ChatGPT consent, protected-original and proposal-approval behavior remains in place. Every approved writing request still requires its exact payload preview; there is no automatic provider, account, model or billing fallback.

## Cold-start durability defect and repair

Navigation, rotation and WebView-reload tests passed on the earlier candidate, but a separate actual Android force-stop/restart test showed that the **entire unsubmitted pending-paste record was absent**, not merely its cursor position. The independently saved workspace and connected account remained recoverable. The historical failed cold-paste restoration assertion remains visible in `test-results/device-paste-restart-verify-014-attempt1.json`; the `-final.json` filename now contains the successful repaired run.

The underlying storage choice was insufficient: WebView localStorage can acknowledge a JavaScript update before its backing data is durably committed for an immediate process stop. Re-render and same-process reload checks did not prove cold-process persistence.

The repair introduces `DraftInputStore.java`, with synchronous app-private `AtomicFile` commits, an explicit file-descriptor `sync()` before completion, and exact committed-record readback before acknowledging success. Filesystem guards reject non-file base/backup/new paths without destroying their contents; a failed commit cannot be reported as saved merely because `AtomicFile` logged rather than threw a rename failure. JSON/text/selection bounds and separate history are validated. MainActivity exposes `loadDraftInput`, `saveDraftInput` and `hasDraftInputHistory`. The platform adapter migrates an existing legacy record only before any native history exists, mirrors successful committed records exactly, and thereafter treats native data as authoritative. Clearing commits a literal JSON `null` tombstone instead of creating an ambiguous missing record; old browser data cannot revive a consumed draft.

The app preserves current text after a write failure, keeps the previous durable record, and does not display successful-save status on failure. A native read error protects both the native record and legacy mirror from automatic overwrite. The focused browser suite passes **11 workflow groups**, including explicitly simulated native migration, lost/stale mirrors, write-failure/retry, tombstone consumption and corrupt-load preservation. These simulations validate the adapter contract; separate actual native-store and force-stop tests now establish the device evidence below.

On the repaired APK, **34 actual native-store checks passed** in `test-results/device-draft-input-store-014-final.json`, including exact immediate disk bytes/new-instance reload, Unicode/selection bounds, invalid-input rejection, UTF-8 limits, interrupted writes, backup recovery, blocked filesystem writes, committed tombstones and preservation of the real workspace. The genuine force-stop workflow passed **5 setup checks and 16 cold-start/cleanup checks**, recorded in `test-results/device-paste-restart-start-014-final.json` and `test-results/device-paste-restart-verify-014-final.json`. The restarted process restored the exact native raw record, reconstructed its browser mirror, displayed leading blank lines/Unicode, reopened the saved paper with its original and revisions intact, and preserved existing projects/voices/notes/account. Cleanup restored the user's exact prior workspace/backup, native and mirrored pending input, selection/preferences and connected account. No AI inference was sent.

Test-only restart/store checks were added. Earlier harness issues, including a missing JavaScript return, were corrected separately and are not described as product defects. The failed cold attempt's subsequent cleanup/recovery verified the user's exact workspace/previous-save backup, projects/preferences, original pending-paste state and connected account; no fictional QA draft was left as user work. See `test-results/device-paste-restart-recover-014.json` and the cleanup checks in the failed verify report.

## Live web-search investigation: observed cause and boundary

The unresolved check concerns **ChatGPT's hosted web-search tool**, which is separate from Crossref, PubMed and Wikipedia source retrieval.

The new actual Samsung baseline made one approved fixed public-query diagnostic. It used an eligible model returned for the selected account, required the web-search tool, and included no paper or source excerpts. The sanitized stream contained `response.created`, `response.in_progress`, `response.metadata`, then `error`. The provider reported:

```text
code: subscription_sharing_usage_limit_exceeded
type: invalid_request_error
```

There was no completed response, output item, completed search call or citation annotation. This directly establishes a usage-limit blocker for that request. It does not establish a reset time, an overall account balance or a separate tool-availability diagnosis. Evidence: `test-results/device-web-diagnostic-014-baseline.json`.

Earlier memory-only prose and Markdown URLs were not accepted as proof of browsing. Inspection found a conditional stream-parser limitation if a service ever emitted tool/citation evidence only in earlier events and omitted it from terminal output. The actual diagnostic supplied no such evidence, so no speculative stream merge was added.

The existing production safeguards are retained: selected web research requires the hosted tool; only an explicitly completed `web_search_call` confirms search use; absent, failed or in-progress evidence cannot satisfy the request; invalid citation spans are discarded; unconfirmed requested web research is refused before a brief is saved. These safeguards were already present in the preserved 0.1.2 source and are not described as a newly successful hosted-search repair.

**No repair of the hosted-search service failure or quota reset is claimed. Successful live ChatGPT hosted search and citations remain BLOCKED.** No further inference retries, account/model switches or paid API fallback were used. Free-provider searches below were tested under their own names, not substituted for this failed acceptance check.

## Genuine public-provider search evidence

The repaired signed 0.1.4 APK passed **55 actual Samsung checks** in `test-results/device-research014-live-014-final.json`. Requests reached the official public provider APIs; these were not mock responses or cached substitutes. Each query returned records and absolute URLs, and the harness checked overlap between the query and retrieved titles/excerpts. This new run supersedes the historical 54-check pre-repair provider run for current-APK verification.

Representative results returned in that final 55-check run are listed below. They match the representative titles/URLs previously observed; the complete current per-query lists are retained in the final report. No abstract passages are reproduced here.

| Actual query | Provider | Returned title and URL | Relevance and evidence boundary |
| --- | --- | --- | --- |
| `photosynthesis` | Wikipedia | [Photosynthesis](https://en.wikipedia.org/wiki/Photosynthesis); [Artificial photosynthesis](https://en.wikipedia.org/wiki/Artificial_photosynthesis) | Direct background concept and a related technical topic; retrieved encyclopedia extracts, not primary experimental evidence |
| `French Revolution` | Wikipedia | [French Revolution](https://en.wikipedia.org/wiki/French_Revolution); [Reflections on the Revolution in France](https://en.wikipedia.org/wiki/Reflections_on_the_Revolution_in_France) | The historical event and a related work; other returned records covered distinct revolutions/influences and require selection for the intended question |
| `urban green space wellbeing` | Crossref | [Urban green space, social equity and human wellbeing](https://doi.org/10.1016/b978-0-12-820730-7.00007-0); [Urban nature and emotional wellbeing: The role of green and blue space in shaping affective responses](https://doi.org/10.1016/j.wss.2026.100489) | Relevant bibliographic titles/DOIs; these responses supplied metadata without an accessible abstract, so they do not establish research findings |
| `photosynthesis chlorophyll` | PubMed | [Regulatory and retrograde signaling networks in the chlorophyll biosynthetic pathway.](https://pubmed.ncbi.nlm.nih.gov/39853950/); [Modeling chlorophyll a fluorescence transient: relation to photosynthesis.](https://pubmed.ncbi.nlm.nih.gov/24910205/) | Relevant scientific concepts with retrieved abstracts; full publications and methodological quality were not independently reviewed |

The same final device test reused an actually retrieved Wikipedia record in the contextual concept sheet, explicitly opened the external browser, returned to the paper and checked original preservation. It physically disabled Wi-Fi/mobile data, observed a clear search error without losing text or saved sources, restored the original radio settings and successfully performed an explicit Wikipedia retry. Exact pre-test workspace/backup bytes, all projects/settings, the native durable pending-paste record or tombstone, its browser mirror and connected account were restored.

URL validation here means returned absolute public web addresses and a real browser-opening workflow. It is not a claim that every DOI/publisher page was independently fetched, accessible in full, or academically reliable. Broad, disambiguation and correction records still need user review.

## Test results and limits of the evidence

The full host suite was rerun after the durability repair and passed. The host/protocol counts below describe that current-source run; historical APK device results are labeled separately.

| Test area | Result at report writing | Evidence / qualification |
| --- | --- | --- |
| Full automated host runner before durability repair | PASS, historical source snapshot | `test-results/all-tests-host.json`; all recorded phases passed before the new repair |
| Full automated host runner after durability repair | PASS | `test-results/all-tests-host.json`; all current-source host phases passed without device/inference requests |
| JavaScript domain/service logic | 107 PASS | Includes contextual concepts/URL guards and controlled slow, empty, invalid-schema, HTTP, timeout, failure, cancellation and explicit-retry cases |
| OAuth/stream/citations | 38 PASS | Terminal completion, completed-tool distinction, citation ranges and existing identity checks |
| Request lifecycle | 7 PASS | Controlled request lifecycle/cancellation checks |
| DNS/registration/research status | 39 PASS | Controlled native support checks |
| Assist/session | 53 PASS | Exact writing payload/session boundaries; not new live inference |
| Native research-request boundaries | 12 PASS | Tool requirement, no-search isolation and refusal without confirmed search |
| Update validation | 52 PASS | Controlled package/version/hash/certificate/provider boundaries |
| Existing browser writing/review/management | PASS | Editing, original protection, goals/rubric, profiles, voice prompts, proposals, acceptance/rejection, Undo/Redo, revisions, delete/restore confirmations and persistence; AI replies simulated |
| Document/add-on browser suite | 14 PASS | Actual document parsing/export roundtrips and simulated add-on behavior |
| Paste/About suite after durability repair | 11 workflow groups PASS | `test-results/paste-ui.json`; previous eight groups plus simulated native migration/authoritative reload, failure/retry/tombstone behavior and corrupt-load preservation; no actual disk durability inferred |
| Contextual-link browser suite | 8 workflow groups PASS | `test-results/information-links-ui-report.json`; source and AI responses explicitly simulated, exact approved evidence verified, no implicit external traffic |
| First signed 0.1.4 upgrade | 7 PASS | `test-results/device-upgrade-014-verify.json`; same-key upgrade preserves project/profile/note/active-paper state and encrypted session, with native/footer/About version agreement |
| First signed candidate's device UI | 19 PASS | `test-results/device-release014.json`; real touchscreen/keyboard, rotation, navigation/reload, modes, reader, dedication, save/reopen and exact QA restoration |
| First signed candidate's public-source workflows | 54 PASS | `test-results/device-research014-live.json`; real requests, source opening and physical network-off/retry as described above; no ChatGPT inference |
| Historical candidate `1612569e…`: native regression | 66 PASS | Passed before the durability repair; not current-source verification |
| Historical candidate `1612569e…`: real document workflows | 17 PASS | Actual Android TXT/DOCX/PDF save/reimport and cleanup before the repair |
| Historical candidate `1612569e…`: public-source/UI workflows | 54 PASS | Actual provider/device checks before the repair |
| Historical candidate: true force-stop pending-paste restoration | FAILED | Entire pending record absent; user workspace/account preserved and cleanup/recovery verified |
| Repaired APK: isolated native atomic-store checks | 34 PASS | `test-results/device-draft-input-store-014-final.json`; actual Android filesystem/AtomicFile behavior with user workspace unchanged |
| Repaired APK: true force-stop/restart | 5 setup + 16 verification/cleanup PASS | `test-results/device-paste-restart-start-014-final.json`, `test-results/device-paste-restart-verify-014-final.json`; actual process termination, exact pending paste, saved work and account restoration |
| Repaired APK: corrected-harness native regression | 71 PASS | `test-results/all-tests-device.json`; 18 storage/recovery, 5 document-byte, 24 update/provider and 24 standalone checks. Assistance replies are simulated; added full-state cleanup checks pass without new inference |
| Repaired APK: real document save/import/PDF | 17 PASS | `test-results/device-document-workflow-014-final.json`; actual Android file-picker saves, TXT/DOCX/PDF reimports, print cancellation/reopen and exact cleanup |
| Repaired APK: public-source/UI workflows | 55 PASS | `test-results/device-research014-live-014-final.json`; actual four-query provider retrieval, real source browser opening, physical network-off/retry, paste/reader/dedication and exact workspace/native-paste/account restoration |
| Repaired APK: independent artifact audit | 16 PASS | `test-results/final-artifact-audit-014.json`; package/version/hash/certificate/alignment/bundle/durability bridge and historical preservation checks for `579a7a2a…` |
| Earlier original-workspace fingerprint recheck | FAILED historically; subsequently resolved | Two appended exact fictional QA records caused the mismatch; a selection-only substitution was insufficient. Failure is retained as investigation history rather than counted as an original pass |
| Guarded durable fictional-tail cleanup | 10 PASS | `test-results/device-preservation-repair-014.json`; original ten-project subset plus original active selection matches the entire baseline hash before mutation; only the two recent exact QA fixtures are removed, then native save/readback and normal reload confirm the original fingerprint |
| Original unchanged baseline after corrected harness | 7 PASS | `test-results/device-upgrade-014-final.json`; fresh harness launch confirms all original/working drafts, projects, profiles, notes, active paper and connected account against the unchanged original baseline, plus version displays |
| Installed production APK byte binding | PASS | `test-results/final-installed-binding-014.json`; independently pulled installed APK matches local `579a7a2a…`, 844476 bytes |
| Local candidate upload-copy verification | PASS | `test-results/line-endings-and-local-copy-014.json`: all 102 source files match the checkout; four release assets match across dist, current release and checkout; LF text/CRLF command/binary rules checked; original releases/checkpoint preserved; HEAD/index/config unchanged; candidate notices present; no staging or publication |
| Current live ChatGPT hosted web/citations | BLOCKED | Actual provider usage-limit response; not passed or resolved |
| Current real AI integration workflows A/B | BLOCKED | Fresh full workflows involving actual Helper/Complete inference and hosted web search cannot complete under the observed limit; simulated integration passes are separate |
| Other Android versions/devices | NOT EXECUTED | Actual device is the authorized Samsung SM-S166V on Android 16; browser viewport checks are not other-device certification |
| Public 0.1.4 publication / GitHub-delivered upgrade | NOT EXECUTED | User-managed publication remains required; local signed upgrade testing is distinct from public OTA installation |

The first candidate's UI/source tests preceded a two-line dialog-title correction. The later historical `1612569e…` candidate included that correction and passed the broad 66/17/54 device suites, then failed the separate cold-start durability check. The repaired `579a7a2a…` artifact has its own completed native-store, actual cold-start, corrected 71-check standard, 17-check document, 55-check genuine-provider and 16-check artifact runs. Its installed bytes and copies in both delivery folders match the local signed file.

### Original-baseline preservation investigation and final proof

A later comparison against the unchanged original pre-upgrade fingerprint failed even though individual QA tests had restored their own immediate checkpoints. Changing only the active paper could not explain that older baseline mismatch. Read-only bounded analysis then found an **exact complete original fingerprint for the first ten of twelve projects, with the original active selection**. The two additional tail records were recent, known fictional standalone-writing fixtures; every original project remained in the matching subset. The connected-account check also matched. This establishes the actual difference without inferring lost user papers from a hash mismatch.

Cleanup required both the whole baseline hash match and strict checks on each removable record's tail position, recent creation, known QA title and exact fictional original text. A private before-repair backup was retained. Only those two appended fixtures were removed, and the baseline active selection was restored. An initial in-memory cleanup did not durably update storage because direct `getState()` mutation does not mark application state dirty; its disk diagnostic remained unmatched. That insufficient attempt is preserved in `test-results/device-preservation-repair-014-memory-attempt.json`, rather than presented as a completed durable repair.

The corrected cleanup explicitly saved through native workspace storage, verified readback and performed a normal app reload. **All ten repair checks passed** in `test-results/device-preservation-repair-014.json`. The former standalone test had deleted its fixture from memory without durably restoring the whole prior workspace, allowing test records to reappear on a later reload; this is the probable source of the extra fixtures. The test-only repair now snapshots/restores the complete state and preferences in `finally`, commits it and verifies it. The corrected **71-check standard suite passed**, then a fresh harness launch passed **all seven original-baseline upgrade checks** without replacing or weakening that baseline. This is independent final proof that original projects, draft contents, revisions, selection and account are preserved. The production APK stayed at `579a7a2a…`; these cleanup corrections required no production rebuild.

Browser workflows A–F cover pasted/imported drafts, simulated explicit research, retrieved-evidence consent in Helper/Complete, proposal acceptance, updated links after editing, failure/retry, saves and reopening. Actual free-provider/device portions of A–F are demonstrated separately above. They do not establish successful current ChatGPT generation or hosted web-tool execution. Historical successful 0.1.2 writing/account lifecycle tests remain historical evidence only.

No production failure was disabled to obtain a pass. A temporary browser locator ambiguity was corrected, and long-text fixtures use an input-event assignment to avoid Playwright's very large `fill` payload timeout. Android document QA initially matched an editable search field rather than the exact saved file; the test selector was corrected to exclude editable controls, after which the real 17-check document suite passed. A missing-return issue in the restart harness was also corrected separately. Leading-newline preservation and dedication styling defects were fixed and regressed. The genuine pending-paste product defect is recorded above; its repaired actual native-store/cold-start checks now pass. The provider usage-limit failure remains visible.

## Modified files relative to the 0.1.2 checkpoint

This list was computed by comparing the checkpoint's 90 source-file hashes with current files, plus new files in the existing source/document/test directories. Generated APKs, bundles, ignored reports, QA data, dependencies and private signing material are excluded. Documentation owned by the release coordinator may continue to receive final result updates.

**Application:** `web/app.js`, `web/platform.js`, `web/styles.css`; new `web/information-links.js`. Domain and provider implementations were preserved rather than rewritten.

**Android identity:** `android/AndroidManifest.xml`, `android-tests/AndroidManifest.xml`.

**Native durability repair:** modified `android/src/io/draft2done/app/MainActivity.java`; new `android/src/io/draft2done/app/DraftInputStore.java`.

**Device test harness:** `android-tests/src/io/draft2done/test/ChatGPTWebDiagnosticChecks.java`, `android-tests/src/io/draft2done/test/NativeDocumentWorkflowChecks.java`, `android-tests/src/io/draft2done/test/SmokeInstrumentation.java`, `android-tests/src/io/draft2done/test/StandaloneChecks.java`; new `android-tests/src/io/draft2done/test/Release014Checks.java`, `android-tests/src/io/draft2done/test/Release014RestartChecks.java`, `android-tests/src/io/draft2done/test/DraftInputStoreChecks.java`.

**Browser/unit tests added:** `tests/information-links.test.mjs`, `tests/information-links-ui.mjs`, `tests/paste-ui.mjs`, `tests/research-failures.test.mjs`.

**Build/release/testing:** `package.json`, `package-lock.json`, `scripts/build-android.ps1`, `scripts/build-device-tests.ps1`, `scripts/release-info.ps1`, `scripts/test-all.ps1`, `scripts/prepare-repo.ps1`. The preparation script's explicit candidate option labels incomplete verification rather than advertising a fully verified release. Dependency versions were retained.

**Documentation modified:** `README.md`, `docs/ANDROID-BUILD.md`, `docs/ARCHITECTURE.md`, `docs/CHANGELOG.md`, `docs/DECISIONS.md`, `docs/RELEASING.md`, `docs/REQUIREMENTS.md`, `docs/SESSION-STATE.md`, `docs/TASKS.md`, `docs/USER-GUIDE.md`, `docs/VERIFICATION.md`.

**Documentation added:** `docs/RELEASE-BRIEF-0.1.4.md`, `docs/VERIFICATION-0.1.2.md`, this `docs/DEVELOPMENT-REPORT-0.1.4.md`.

## Remaining limitations and handoff

- The required successful live ChatGPT hosted web/citation check remains blocked. The release must not be labeled fully verified while this acceptance condition is unmet. A later authorized test needs genuinely completed tool/citation evidence after account/app usage permits it; no reset time is inferred.
- No on-phone language model is bundled. Offline editing, local storage, counts, descriptive Writer DNA, deterministic observations, saved research and document tools work locally. Primary AI drafting needs internet and an eligible account with available usage. Optional PC/server adapters require a reachable endpoint; the user's firewalled PC is not a production-phone dependency.
- Information highlighting is bounded, conservative local matching, not comprehensive named-entity recognition or a factual assessment. The first 500000 characters are scanned for suggestions while the full accepted text remains readable. URL syntax and title/excerpt relevance do not prove liveness, credibility or claim support. Information links are not automatically inserted as academic citations.
- Source records and abstracts are not full-text verification. Metadata-only records cannot support findings by themselves. Model factual accuracy, personal details, citations, reasoning and exact voice fidelity still require review.
- Windows remains deferred. Imports extract text without original page layout, images or tables; scanned PDFs require external OCR. Citation support is basic APA/MLA/Chicago author-date, without exhaustive source types or Chicago footnotes. OpenAlex and Semantic Scholar remain future integrations. Saved-library caching/manual Verify use fixed policies rather than configurable policy controls.
- The workspace limit remains 4 MiB and document input/output limits 10 MiB. The native pending-paste store validates up to 10 MiB UTF-8 text within a 24 MiB JSON-record limit. Device storage/quota failures can prevent staging or saving; the UI reports them. Actual native-store and cold-start durability checks pass on the tested Samsung; this is not certification of every device or storage-failure scenario. Export `.d2d` backups regularly, especially before uninstalling or clearing app data.
- Automatic GitHub update checks remain on by default for `youriptvus/Draft2Done`, with package/version/hash/signing checks and Android installation approval. Source preparation is not publication. The user must manually commit/upload the reviewed source and matching APK/manifest; a real GitHub-delivered 0.1.4 upgrade remains a later check.

At this historical handoff, the requested UI features and native durability repair were integrated and the earlier stable release was preserved. Full host, native-store, force-stop, corrected standard-device, document, genuine-provider, artifact, installed-byte and local-copy checks passed for `579a7a2a…`. The original-baseline preservation issue was resolved with guarded durable cleanup and fresh unchanged-baseline verification; its earlier failures remain documented. That candidate was copied to `D:\Draft2Done\current release\Draft2Done-0.1.4.apk` and `D:\Draft2Done\repo\Draft2Done\release-assets`, with source unstaged and unpublished. Those paths are also used for later candidates; use [current verification](VERIFICATION.md) for their current identity and copy-validation status. The historical hosted-search blocker was not a pass, and successful new sign-in does not establish current AI or hosted-web acceptance.
