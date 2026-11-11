# Verification record — Android 0.1.2

Checked on 2026-11-10 (America/New_York). The historical 0.1.0 results and public-release validation are preserved in [VERIFICATION-0.1.0.md](VERIFICATION-0.1.0.md). The current release is prepared locally; commits and publication remain manual.

## Signed release identity

- Package `io.draft2done.app`, version **0.1.2**, Android version code **2**; Android 8+ / min API 26 / target API 36.
- Signed APK: `dist/Draft2Done-0.1.2.apk`, 836284 bytes, SHA-256 `4b364483f50a94d2a4bf639e771484cf4c267c058abc0cec61ba6d71a1dbcff5`.
- Signature schemes v2/v3 and ZIP alignment passed. Ten independent final-artifact checks passed, including bundle/version agreement and historical 0.1.0 preservation. Persistent certificate SHA-256: `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd`.
- Native app information and network identification read the installed package version; welcome/About use the package version bundled at build time. Signed APK badging, package metadata and web metadata agree.
- Upload files are prepared in `D:\Draft2Done\current release` and the local checkout's `release-assets`. Private keys, credentials and app data are excluded. The release is non-debuggable and Android cloud backup is disabled.

## Passed final host checks

- **77 JavaScript tests** covering domain logic, protected originals, counts, revisions, citations, source trust, Writer DNA, export presentation, full-draft guards, direct-account consent, cancellation, quota handling and no provider fallback.
- **38 OAuth/stream/citation**, **7 request lifecycle**, **39 DNS/registration/quota**, **53 assist/session**, **12 research-request**, and **52 update-validation** checks passed. Research checks exercise the exact native search configuration, reject missing search evidence before saving, and isolate query-only research and writing. Stream tests reject failed/progress-only tools and malformed citation ranges while preserving valid multipart offsets.
- **11 isolated release-metadata checks** passed: current/future names and URLs, package/manifest mismatch, invalid versions/codes/package/SDKs. PowerShell scripts parse, and package/lock versions agree.
- Browser writing, review and project-management suites passed. These exercise editing/save/reload/background behavior, responsive themes, undo/redo, assignment file import/rubric/max validation, Polished Voice reaching the prompt, voice editing/applying/reopen, rejection, revision restore, deletion cancel/confirm, exact remote previews, delayed/stale results and every generation round. Inference in these browser suites is simulated.
- **14 document/add-on browser checks** passed: TXT/MD/DOCX/text-PDF conversion, independent DOCX readback, formatting/references, backups, PDF handoff and account UI. An actual fictional Samsung-exported PDF fixture verifies intact fi/fl words and Unicode on reimport. Imports apply saved global defaults for all four text formats; `.d2d` retains its own project-owned preferences.

Evidence: `test-results/all-tests-host.json`, `documents-report.json`, `ui-report.json`, `release-script-checks.json`. The host report records version 0.1.2/code 2. Reproduce with `scripts/test-all.ps1`.

The repository-local `.gitattributes` overrides automatic Windows CRLF conversion for source text, keeps Windows command scripts CRLF and marks binaries nontext. The prepared checkout's inspected source text uses LF, effective attributes are correct, and `git diff --check` emits no whitespace or conversion warnings. All three copies of the signed APK match the final SHA-256; files remain unstaged and the checkout's existing commit is unchanged. Global Git configuration was not modified. Evidence: `test-results/line-endings-and-local-copy.json`.

## Audit defects corrected in 0.1.2

Already-cited or accepted-source bibliography entries now remain exported when their future AI-use switch is disabled. Unused excluded records remain absent; existing author-written references and deduplication are preserved.

New workspaces and imported TXT/MD/DOCX/PDF papers share saved voice, citation, counting, formatting and opening-mode defaults. Portable project imports retain their own settings. Both defects have regression coverage.

Actual Samsung PDF tests uncovered three export/import defects. Printing now starts once, retains the rendering WebView until Android finishes, and serves only its own immutable HTML at an exact synthetic URL; it blocks other resources and rejects load errors instead of printing a WebView error page. The paper title supplies the default PDF filename. PDF text extraction preserves adjacent glyph runs instead of inserting spaces inside fi/fl words. The saved physical PDF and a permanent fictional fixture verify meaningful text, Unicode and reimport.

## Samsung checks

The authorized Samsung SM-S166V / Android 16 is tested using existing ADB servers on ports 5038 and 5037; the final checks used 5037. Other devices and servers are left unchanged; wireless ADB is authorized as a fallback when USB is unavailable.

The same-key installation upgraded 0.1.0/code 1 to the first 0.1.2 candidate/code 2. **Seven upgrade/version checks passed**: exact project/profile/note/active-paper fingerprints, connected-account fingerprint, installed package code/name, native app information, welcome footer and About dialog. The later bibliography/import corrections do not change the storage format. No uninstall or downgrade was performed. Evidence: `test-results/device-upgrade-verify.json`.

The final signed APK passed **18 storage-recovery checks**, **5 native document-byte checks**, **24 update/provider checks** and **19 standalone phone workflow checks**. The standalone suite uses the actual connected-account catalog followed by explicitly simulated assist replies; it verifies consent/cancel/Helper/Complete/regeneration/acceptance/quota/no PC transport and cleans up its own fixture. Evidence: `test-results/all-tests-device.json`, version 0.1.2/code 2. The installed APK was independently pulled and its SHA-256 matches the final release exactly: `test-results/final-installed-apk-check.json`. Final status shows the account connected with no current connection error, and the dark welcome screen is restored.

**17 actual Android document-workflow checks passed** on the final APK: file-picker TXT/DOCX saves, loaded/selected PDF preview, cancellation without creating a PDF, reopening and physical Save as PDF, and native-picker reimport of all three saved formats into protected originals. TXT bytes and DOCX contents were verified; PDF text includes intact words and Unicode. Only the uniquely named fictional files were removed. Exact original primary/backup bytes, full workspace/active-paper state and connected account were restored. Evidence: `test-results/device-document-workflow.json`.

The expanded PDF check exposed a real repeated-page-finish print handoff crash. The native fix starts exactly one job per rendering WebView, rejects a concurrent job and retains that WebView until Android finishes the adapter. Cancel/reopen passes. A separate automation race clicked Save before the preview had selected pages; observed final ranges were empty. The fixture now waits for actual selected preview pages rather than only an enabled button. It does not change empty selections into all pages in production.

The first pre-fix crash preceded the durable test checkpoint. Targeted recovery removed only its uniquely named fictional project/files and preserved every other project/profile/note/setting, with private pre-recovery copies retained. Its old active-paper selection and exact earlier backup bytes could not be reconstructed; they are not claimed as restored. Subsequent fixtures use a durable checkpoint; four interrupted-checkpoint recovery checks passed and later runs verify exact primary/backup/state/account restoration.

Eight error-page PDFs created by the earlier failed print tests were independently read and identified by exact filenames, QA timestamps, matching copied hashes and their known WebView error text. They were backed up privately before removal; no paper-content PDFs were removed. Evidence: `test-results/fictional-print-cleanup.json`.

## ChatGPT and remaining external checks

Real personal sign-in, signed identity, encrypted persistence and eligible model loading passed. **Nine real refresh checks** (including startup) passed: controlled in-memory expiry exercised the production refresh grant, obtained a future expiry and a nonempty eligible catalog, preserved the account and cleaned request state. Credentials were neither logged nor restored by the test. Evidence: `test-results/device-chatgpt-refresh-live.json`.

The fresh real 0.1.2 test completed Helper, continuation, regeneration and query-only research. Each writing request presented the exact payload; explicit acceptance preserved the original. Cleanup removed only the newly identified fictional QA paper and restored prior selection/preferences. The combined run retained a failed web-search/citation assertion: its requested search completed without the required tool/link evidence. It is not reported as a wholly passed suite. Evidence: `test-results/device-chatgpt-writing-live.json`.

That failure prompted a native correction: selecting web search now requires the hosted tool instead of allowing default optional selection. A focused one-request retest still returned completed prose but no explicit search/citation metadata; its two failed assertion entries remain in `test-results/device-chatgpt-web-live.json`. A separate single public-query structural diagnostic then observed `response.created`, `response.in_progress`, `response.metadata` and terminal `error`, with no completed output, tool or annotation evidence. The parser correctly rejected it. No raw stream, response prose or credentials were saved by the diagnostic, and no further inference was retried. Evidence: `test-results/device-web-diagnostic.json`.

The final guard now refuses to save requested web research unless the response explicitly confirms a completed search. Failed/in-progress/missing-status tools and prose URLs cannot establish search use, and malformed citation spans are ignored. Query-only and writing requests retain their separate rules. **Successful live ChatGPT web search and cited output remain unverified**; the observed service error does not establish a particular account policy, usage balance or reset time. Native/host tests of safe refusal are distinct from a successful provider tool run.

**Nine real sign-out checks** passed (including startup): normal sign-out cleared local credentials/catalog, acknowledged remote revocation, and the server rejected one old-refresh probe with `invalid_grant`. The user then completed ordinary browser authentication; reconnection passed. No revoked credentials were restored. Evidence: `test-results/device-chatgpt-signout-live.json`, `device-chatgpt-reconnect.json`.

Historical requests returned `subscription_sharing_usage_limit_exceeded`; those failures remain in the 0.1.0 record. Current successful requests do not establish future usage allowance, a reset time or whole-plan balance.

The primary deployed route is phone-to-OpenAI HTTPS. The user's remote phone cannot reach the firewalled PC; earlier actual Llama/Qwen server evaluations are developer-only evidence, not a production dependency. Offline editing, deterministic observations, DNA and document tools remain available without a bundled language model. Software tests do not prove generated claims, personal details or exact voice fidelity.

The user-published v0.1.0 release was independently downloaded, hash/signature/package checked and read by the Samsung's actual updater. A fresh **five-check public update test** on installed 0.1.2 retrieved the known public 0.1.0 manifest and correctly offered no downgrade. Evidence: `test-results/device-updates-live-012.json`. A production GitHub-delivered 0.1.2 upgrade can be tested after the user manually publishes its APK and `latest.json` under `v0.1.2`. Local ADB upgrade testing does not substitute for that public installer path.

Windows remains deferred. OCR, exact imported page layout, exhaustive citation types and Chicago footnotes are outside this Android release. Research uses a saved library and manual Verify; configurable cache/verification policies are not implemented. See REQUIREMENTS.md and TASKS.md for the audited scope and remaining checks.
