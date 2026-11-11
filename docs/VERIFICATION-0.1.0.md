# Verification record — Android 0.1.0

Verified on 2026-11-10 (America/New_York). Results below distinguish software checks, real service requests, model-quality observations, and external limits.

## Passed host regression

- **76 JavaScript tests**: counts, original protection, revisions/provenance, reviewed inline continuation and stale anchors, requirements, source metadata/grouped citations, profile roundtrips, descriptive DNA, bibliography deduplication, Markdown export runs, local endpoints, whole-draft context guards, JSON schemas, output limits, cancellation, research failures and honest refusals. Direct ChatGPT checks cover immutable full-payload consent, each generation round, regeneration, quota/no fallback, cancellation, changed content, native size limits and long papers independent of advanced server context settings.
- **18 OAuth protocol checks**, **7 request-lifecycle checks**, and **39 DNS/registration/research-status checks**: PKCE, issued clients/stable hosts, signed identity validation, stream completion/errors, early cancellation, DNS-only retry, safe pending registration recovery, and account-specific quota notices.
- **53 native assist/session checks**: strict reviewed-system/user conversion into Responses instructions/input, no hidden instruction prefix, no unsupported fields, full-payload limits, account/catalog identity, unusable refresh credentials and preservation of newer/cancelled/unrelated account state.
- **52 update-validation checks**: trusted repository/redirects, manifest types, hashes, package/version/certificate identity and unsafe metadata.
- **Browser workflow and review suites**: editing, immediate manual save, autosave/background preference, default mode/count/export appearance, persistent-tab Undo/Redo with contribution ranges, paragraph navigation, profiles, themes, goals, failure/retry, source consent/evidence review, approval and responsive layouts. Phone setup/migration, every direct writing consent, exact previews, delayed replies after project replacement/editing, account/model changes and quota/no fallback are covered. Their model responses are explicitly simulated.
- **11 document/add-on browser checks**: real TXT/MD/DOCX/text-PDF import, independent DOCX text/heading/bold/italic readback, bibliography preview toggling, project backup/reimport, PDF handoff, simulated account/model/brief and quota-status UI.
- Production dependency audit reported zero known vulnerabilities when run.

Reports: `test-results/all-tests-host.json`, `ui-report.json`, `documents-report.json`. Reproduce with `scripts/test-all.ps1`.

## Actual scholarly APIs and local AI

Crossref, PubMed and Wikipedia each returned four records; official record re-fetches succeeded. The final live integration used an urban-green-space draft and actual available PubMed excerpts for Helper/Complete. Existing Llama 8B returned inference, Helper and a separate continuation; original text stayed unchanged and cancellation returned AbortError. Bibliographic existence and abstract access do not prove claim correctness. Evidence: `test-results/live-report.json`.

Five fictional writing styles were tested against existing Qwen 3B and Llama 8B models. The initial small coding-model run had three truncated responses and weak content. Those findings led to schema-constrained output, explicit output-limit handling, stronger missing-detail/hypothetical instructions, long-sentence reuse warnings, and removal of nonexistent citation examples from source-free prompts.

The final guarded Llama run produced five reviewable outputs without parser/runtime errors or unknown citations. Originals and working text remained unchanged; a genuine 14-word draft was rejected before transport. Cold review still found unconfirmed details, reasoning overstatement and approximate voice matching. These are not tests proving factual correctness or universal voice fidelity. Raw proposals and qualitative reviews are retained in `test-results/style-live-final.json`; prior comparison runs are preserved separately. No model was downloaded or changed.

## Samsung release checks

The signed, non-debuggable APK runs on Samsung SM-S166V / Android 16 through the existing authorized ADB server on port 5038. The other connected device was left untouched.

- **18 recovery checks** including startup: Unicode saves, valid previous saves, missing/corrupt primary, malformed UTF-8, failed writes after recovery, byte limits, unfinished atomic writes and legacy backups. Faults use disposable app cache fixtures, never the personal workspace.
- **5 native document checks** including startup: exact Unicode import/export byte callbacks, oversized input rejection and invalid export encoding. Android chooser/print opening was separately tested; no personal file was selected.
- **24 native update checks**: real installed APK hash/signing data, tampered signed bytes, wrong package/certificate/hash/version, cancelled queued work, immutable provider paths, exact read-only bytes and traversal/write denial.
- **19 standalone phone checks**: real existing session/model catalog, eligible Astra selection, verified assist interception, full-data preview before transmission, cancellation, exact approved payload, Helper, Complete, regeneration, acceptance/original protection, quota/no retry/fallback, unchanged previous notes and cleanup scoped to this run's fictional paper. Assist replies are simulated at the native bridge; this does not prove successful provider inference. No PC/HTTP model transport was used, and the developer USB reverse mapping was removed before the final run.
- Earlier developer-only Samsung QA exercised actual local AI, section acceptance, original preservation, saved restart state, live research, offline default and update checks. These PC/USB tests are not evidence that the production phone can reach the user's firewalled PC. The current signed APK's direct route is covered by the separate standalone suite.
- Dark welcome/tagline spacing, portrait/landscape, system bars and Samsung keyboard/editor visibility were inspected in screenshots. All native harness code remains in a separate signed test APK.

Reports: `test-results/device-*.json`, `test-results/samsung/qa/`. `scripts/test-all.ps1 -Device -SkipHost -InstallApks` checks instrumentation result codes and JSON checks, rather than trusting ADB exit status alone. `-LiveAI` additionally enables the optional developer-server tests; it is not needed for the standalone route.

## Personal ChatGPT connection

Real browser OAuth consent, signed identity validation, encrypted session persistence, granted plan scopes and the account-specific model catalog succeeded on the Samsung. A transient native DNS failure was reproduced and fixed with bounded DNS-only retries; earlier sign-in errors are cleared durably after success. Pending registrations survive transport failures without making an unverified account active.

Actual Responses requests returned `subscription_sharing_usage_limit_exceeded`. The connected account was retained, no incomplete brief was saved, and no fallback API key or billing path was used. This code can reflect an account or app-specific limit; no reset time or whole-plan balance is inferred. Further automated plan requests were paused. The user can inspect ChatGPT Settings → Usage.

Successful plan inference, web-search tool availability, refresh rotation and live remote revocation remain unverified. Protocol tests and successful sign-in do not substitute for those live checks. The known quota result can be recorded locally by the QA harness without sending another request; that is a migration of the observed failure, not a new successful inference.

Automatic approval review blocked the ADB action to open ChatGPT's Usage page, citing policy. No alternate action was used to bypass that rejection, and account limits were not changed.

## Release and external boundaries

The final APK compiles, required bundled paths verify, ZIP alignment verifies, and signature schemes v2/v3 verify. Package `io.draft2done.app`, Android 8+ / target API 36. Size 836284 bytes; SHA-256 `7574934df73594784417fb73c6bdce9ca3fb4fafb61204f5a70484389bc05580`. Public certificate SHA-256 `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`. The same signing key preserves installed projects. Current metadata is also in `dist/Draft2Done-0.1.0-build.json`.

The user manually published [v0.1.0](https://github.com/youriptvus/Draft2Done/releases/tag/v0.1.0). The anonymous latest-release API now returns the stable release and both required assets. Independently downloaded APK and `latest.json` are byte-identical to the tested local release; the APK is 836284 bytes with the SHA-256 above. Both APKs independently pass v2/v3 signature verification with the same certificate; AAPT confirms package `io.draft2done.app`, version 0.1.0/code 1, min API 26 and target API 36. The earlier unavailable-release/account-access results are historical, not the current public endpoint status. Evidence is retained under ignored `test-results/release-verification/`.

The Samsung's initially offline USB connection recovered after a targeted reconnect. No other device or ADB server was changed. **Five live update checks passed** against the installed signed release: startup, automatic-check preference, actual public manifest matching the independently verified version/hash, comparison with the real installed version, and the displayed “Draft2Done is up to date.” status. This sends no draft or ChatGPT inference request. Report: `test-results/device-updates-live.json`; screenshot: `test-results/samsung/qa/public-release-status.png`. The new `updates-live` instrumentation mode accepts independently verified `expectedVersion`, `expectedCode` and `expectedSha` arguments and never opens installation.

A real release-to-release upgrade installation still needs a future higher versionCode; the installed app and current public release are both code 1. Local documentation and test-harness changes remain unstaged in `D:\Draft2Done\repo\Draft2Done` for manual user commits. The release APK itself is unchanged. No automatic commit, push or publication was performed.

Windows remains deferred. OCR, exact imported page layout, exhaustive citation types and Chicago footnotes are not included in this Android release. See `REQUIREMENTS.md` for the complete brief mapping and `TASKS.md` for remaining items.
