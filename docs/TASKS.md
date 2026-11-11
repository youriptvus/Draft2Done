# Draft2Done 0.1.4 implementation tracker

Target 0.1.4/code 3 overrides the attachment's 0.1.3 references. Preserve working 0.1.2/code 2 and all prior artifacts. Current repair results are below; earlier feature-development and 0.1.2 checklists are historical and do not establish passes for the repaired bytes.

## Chrome sign-in repair on Moto G

The user requested that the repair remain 0.1.4 and be installed on the Moto before further testing. Current APK SHA-256: `33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db`, 852668 bytes. See [the repair report](SIGN-IN-REPAIR-0.1.4.md) and [current verification](VERIFICATION.md).

- [x] Preserve the prior 102-source 0.1.4 candidate and four release assets, plus the first listener-only repair, without changing older releases or the signing identity.
- [x] Reproduce the old idle-peer listener failure on the Moto with browser authorization intercepted; preserve the account, papers, preferences and staging files.
- [x] Isolate bounded callback peers and pass 126 genuine local TCP checks; malformed, idle and reset peers cannot end the authorization wait.
- [x] Investigate the remaining normal-Chrome failure: Android reported the app cached and frozen while the browser was open. Preserve that failed result rather than accepting the controlled listener test as a browser pass.
- [x] Add a temporary user-started foreground sign-in service, ready before Chrome, with Return/Cancel actions and bounded cleanup; install the repaired same-key 0.1.4 before broader testing.
- [x] Verify the normal app remains active and unfrozen after 20 seconds in Chrome, then complete real user authentication and verify connected/plan-enabled state, cleared error, service cleanup and the live model catalog.
- [x] Pass 11 native listener/service checks, seven same-key upgrade checks, 71 standard device checks, 34 native paste-store checks, 20 release-UI checks and real force-stop setup/verification checks (5/16).
- [x] Verify the authenticated connection survives all regression tests and a real cold restart; independently pull the installed APK and match the exact final hash and size.
- [x] Pass 24 independent artifact checks, including compiled service permissions, all five bundled web assets, signatures and historical preservation.
- [x] Refresh and verify 108 exact local source copies and all four release assets, LF/CRLF rules, private-file exclusions and unchanged Git HEAD/index/configuration. Source remains unstaged and unpublished; evidence: `test-results/line-endings-local-copy-signin-014.json`.

This repair verifies real Chrome sign-in on the Moto G 2025/Android 16. No new AI inference, hosted web-search result or repaired Samsung installation is claimed. The earlier hosted-search acceptance requirement remains unverified; its previous usage-limit failure is historical, not a measurement of the newly connected account's remaining usage.

## Earlier 0.1.4 feature-development requirements

The following completed checks describe the preserved pre-sign-in-repair 579-hash artifact. Current repaired-APK checks are listed above; see [the archived verification](VERIFICATION-0.1.4-PRE-SIGNIN-REPAIR.md) for the earlier device and copy results.

- [x] Read the new brief and existing architecture, requirements, decisions, guides, release and verification records before edits.
- [x] Preserve 0.1.2 source and four release assets in `test-results/checkpoints/v0.1.2-before-0.1.4-20261111`; retain old versioned `dist` artifacts.
- [x] Set package/lock/Android/test metadata to target 0.1.4/code 3 and derive test APK metadata at build time.
- [x] Add explicit clean existing-SDK rebuild and reject stale bundled web versions; do not rewrite the functioning project into Gradle.
- [x] Preserve 0.1.2 verification separately and add the new release brief with the explicit target override.
- [x] Complete and regress Paste Your Draft above import options, exact requested label/placeholder/button, long-text validation and interruption-safe persistence.
- [x] Verify paste creates a protected original without silently overwriting a saved project, then integrates with DNA, both writing modes, revisions and research.
- [x] Complete restrained contextual concept links with an explicit toggle, relevant retrieved source URLs, user-triggered opening and unchanged draft/export text; URL checks do not prove factual support.
- [x] Verify link behavior through edits, selection/copy/scroll, saved-project reopening, offline/error states and both Helper/Complete workflows.
- [x] Add/verify permanent exact Victoria Downs dedication and app version/description in About on phone-sized layouts.
- [x] Run genuine distinct-query public research with relevant returned source URLs and save/restore; verify source payload use in both workflows with clearly simulated inference.
- [x] Run controlled missing-network, timeout/slow/error/empty/invalid-response and cancellation cases; preserve drafts and provide understandable retry actions.
- [x] Rerun the full host unit/native boundary/browser/document/management/paste/information-link suites after integrated source freeze.
- [x] Clean build and verify the actual signed 0.1.4 APK, bundle identity, hash, certificate, alignment and previous-release preservation.
- [x] Install the same-key upgrade on the authorized Samsung and retest startup, version/About, keyboard/touch, rotation/lifecycle, persistence, imports/exports and new features; verify the installed APK matches the signed artifact exactly.
- [x] Repair pending-paste loss exposed by a real force-stop; pass 34 native-store checks and the 5/16 setup/restart checks on the repaired APK.
- [x] Strengthen test cleanup to restore complete state and native files, remove only two cryptographically verified added QA fixtures, then pass the 71-check device regression and the unchanged original seven-check upgrade baseline again.
- [ ] Complete all genuine integration tests A–G: browser/simulated A/B pass, but actual ChatGPT inference/search steps remain quota-BLOCKED. See the report for individual workflow results.
- [x] Prepare the local signed candidate, explicit incomplete-verification notices and current checkout; verify all 102 source files and four release assets, line endings, original checkpoint and unchanged Git HEAD/index/configuration. All changes remain unstaged and unpublished.

## Required live web check — BLOCKED

The fresh real Samsung baseline diagnostic returned `subscription_sharing_usage_limit_exceeded` / `invalid_request_error` before any completed search/tool evidence. Evidence: `test-results/device-web-diagnostic-014-baseline.json`. This is the exact observed external blocker, not evidence of an unsupported model/tool or a successful parser repair.

- [x] Record genuine current service failure and preserve earlier failed assertions.
- [x] Keep strict refusal of requested but unconfirmed web research; no prose-URL inference or silent provider/account/model/billing fallback.
- [ ] Successful genuine ChatGPT hosted web search with completed tool/citation evidence. **BLOCKED**; no further inference retries to evade the usage limit.

A candidate build may be produced with this blocker clearly stated. The overall release must not be called fully verified while that required test is blocked. Free-provider success remains separate evidence.

---

## Preserved 0.1.2 implementation baseline

The current deliverable is a signed Android APK. Windows desktop work is deferred at the user's request. See `DECISIONS.md` for all user changes and `VERIFICATION.md` for executed checks and outstanding live tests.

## Foundation
- [x] Inspect empty destination and preserve project isolation.
- [x] Inventory Python, Java, hardware and existing model manifests without changing them.
- [x] Initialize dedicated local Git repository.
- [x] Select Android native shell plus bundled JavaScript application, without a remote UI dependency.
- [x] Obtain project-local Android build tools and produce a reproducible release build.

## Working application
- [x] Local projects, separate original and working drafts, autosave and recovery.
- [x] TXT/Markdown/DOCX/PDF import and TXT/Markdown/DOCX/PDF export.
- [x] Body word counts, optional title/reference exclusion, assignment constraints.
- [x] Writer DNA metrics, reusable editable profiles and original/polished voice.
- [x] Offline draft analysis and selectable Helper suggestions.
- [x] Ollama and local OpenAI-compatible discovery, generation, failures and cancellation.
- [x] Optional official-API research, source library, verification and citation metadata.
- [x] Complete Mode with editable proposals, individual approval/rejection and provenance.
- [x] Settings, themes, revision history, project backup/import and export preview.

## Verification and delivery
- [x] Unit tests for domain logic and service boundaries.
- [x] Browser integration tests using the packaged web interface.
- [x] Live scholarly API checks and real inference using an existing model, if it can run safely.
- [x] Signed APK build, manifest inspection and cryptographic signature verification.
- [x] Install and test the signed release on the authorized Samsung SM-S166V (Android 16), including real local AI, live research, persistence, keyboard visibility and native pickers/print.
- [x] Setup, installation, privacy, signing-key preservation and limitation documentation.

## Environment
Windows 10 Pro; AMD Ryzen 3 2200G (four cores); about 7 GB usable system RAM. NVIDIA RTX 5050 detected; WMI VRAM values are unreliable so do not infer usable VRAM from WMI. Python 3.12 virtual environment created inside the project. JDK 27 installed. Ollama installed; no service detected at discovery. Existing manifests: llama3:8b-instruct-q4_K_M and qwen2.5-coder:3b. No AI model has been downloaded or changed. No Android SDK was found initially.

## Added requirements
- [x] Optional personal ChatGPT research connection, encrypted native credentials, account/model picker, consent preview, and saved cited briefs.
- [x] Automatic update checks enabled by default for youriptvus/Draft2Done; signed-package verification before Android installation.
- [x] Live ChatGPT consent, signed identity, encrypted session persistence and account model catalog.
- [x] Successful live ChatGPT writing and query-only research after prior account/app usage-limit failures; no automatic retry or paid fallback. Web-search verification is tracked separately below.
- [x] Verify the user-published public v0.1.0 release, manifest and independently downloaded signed APK against the local build.
- [x] Fresh Samsung check of the public release: five live checks pass and the same-version app correctly reports up to date.
- [ ] Future real higher-version update installation after a higher versionCode is published.

## Current limitations
Windows deferred. No model bundled or on-phone inference engine. Text import does not retain document page layout, images or tables; scanned PDFs need OCR. Chicago is author-date, not footnotes. Citations and factual claims need review. OpenAlex and Semantic Scholar are documented future providers. See USER-GUIDE.md and PROVIDERS.md.

## Latest appearance and phone verification
- [x] Dark theme is the default; light remains optional. The elegant welcome screen opens with recent papers below.
- [x] Move the paper illustration above the green tagline to remove overlap.
- [x] APK asset path, system-bar insets/colors and keyboard focus issues found on Samsung were fixed.
- [x] 77 JavaScript tests, 38 OAuth/stream/citation, 7 lifecycle, 39 DNS/registration/research-status, 53 assist/session, 12 research-request and 52 update checks passed; browser and Samsung reports are recorded in VERIFICATION.md.

## Full-brief completion pass
- [x] Show draft outline, gaps, research questions and unfinished observations in Helper.
- [x] Make local-model advice selectable for follow-up.
- [x] Review research queries before online requests and inspect evidence before generation.
- [x] Offer an explicit continuation point for an unfinished final sentence; reject stale anchors.
- [x] Edit full source metadata, add manual sources, and insert grouped paragraph citations.
- [x] Preserve manual citation links in project backups and bibliography exports.
- [x] Add explicit Undo/Redo across tabs and paragraph navigation.
- [x] Apply default writing mode, voice, citation/count rules and export appearance to new papers.
- [x] Add measured repeated expressions, language cues, paragraph openings and spelling variants to Writer DNA.
- [x] Render headings/bold/italic in DOCX and PDF; deduplicate exported reference headings and entries.
- [x] Recover missing/corrupt primary saves without overwriting a valid fallback; flush permitted autosaves on backgrounding.
- [x] Harden queued cancellation, concurrent update downloads and immutable installer files.
- [x] Add reproducible whole-app host/device regression runner with truthful instrumentation failure detection.
- [x] Run the expanded regression against the final signed APK and record results.
- [x] Complete personal ChatGPT consent and validate a real connected account/model catalog.
- [x] Diagnose native DNS failure, preserve pending registrations, clear stale login errors and handle actual plan-quota failure without saving a partial brief.
- [x] Successful actual Helper/Complete/regeneration/query-only research and real token refresh/sign-out revocation. Web-search citation verification remains tracked below.
- [x] User-managed publication completed; public API and both release assets verified anonymously. Commits/uploads remain manual by the user's instruction.
- [x] Finish the Samsung public-release check after USB recovery, including automatic preference and actual version/hash/status.
- [ ] Verify a real upgrade when a higher version is published.

## Standalone Android deployment (latest constraint)
- [x] Record that the deployed phone cannot reach the user's PC behind its firewall; USB model tests are development-only.
- [x] Direct native ChatGPT writing requests using the encrypted account, Responses instructions/input, no unsupported fields and terminal stream confirmation.
- [x] First-class direct-account Helper/Complete/regeneration adapter with no PC or API-key fallback.
- [x] Fresh exact-payload preview and explicit consent before every remote writing request; cancel/stale-context refusal before transmission and stale-result refusal after waiting.
- [x] Primary phone-friendly connection/model setup; PC-server controls remain optional advanced settings.
- [x] Recommend strong research/writing models only when returned by the actual eligible account catalog.
- [x] Test privacy/transport/service boundaries, rebuild/sign/install, and verify the phone without PC model routing; actual direct-account writing and query-only research now succeed.
- [x] Update user guide, architecture, privacy and release records for this deployment.
- [x] Prepare the local GitHub checkout and signed release assets for manual user commits/uploads; no automatic staging, commit, push or publication.

## 0.1.2 release and remaining-test audit
- [x] Set app/package/test metadata to 0.1.2 and Android version code 2.
- [x] Derive visible/native/network version information and release artifact names from validated metadata.
- [x] Verify same-key 0.1.0→0.1.2 installation preserves projects, protected/working drafts, profiles, notes and the connected session; check native, welcome and About versions.
- [x] Create `D:\Draft2Done\current release` for the newest prepared GitHub upload files.
- [x] Apply repository-local LF source / CRLF Windows command / binary-preservation rules without changing global Git settings or staging files.
- [x] Correct and regress cited-source bibliography retention after disabling future AI use.
- [x] Correct and regress imported-document application of saved count/export/mode defaults.
- [x] Cover assignment/rubric file import, max validation, Polished Voice prompts, saved-voice editing/reopen, rejection, restore and deletion confirmations in the full host runner.
- [x] Complete 17 actual Android file-picker TXT/DOCX/PDF save/import and print cancel/reopen checks using fictional QA files, with exact workspace/backup/account restoration and cleanup.
- [x] Correct and regress duplicate print handoff, blocked/error-page PDF rendering, title-based default filenames and intact PDF glyph-run imports; retain a real fictional Samsung PDF fixture.
- [x] Make a fresh controlled ChatGPT attempt: actual Helper/Complete/regeneration/query-only research succeed and preserve the protected original; cleanup restores prior selection/preferences.
- [x] Verify production token refresh, remote sign-out revocation, rejected old refresh token and normal user-authenticated reconnection.
- [x] Require selected web search, reject unconfirmed/failed/progress-only search evidence before saving, and validate citation spans. Query-only research remains separate.
- [ ] Successful live ChatGPT web tool/citation output: controlled requests returned no confirmed evidence and the final structural diagnostic ended in a service error. Keep those failed assertions visible; no further automatic inference retries.
- [x] Run final checks against the rebuilt 0.1.2 APK, verify the installed APK hash and update the verification record, current release folder and local checkout.
- [ ] User-managed publication of v0.1.2, then a production GitHub-delivered upgrade check. No automated publication is authorized.
