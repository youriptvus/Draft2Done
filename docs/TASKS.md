# Draft2Done 0.1.0 implementation tracker

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
- [ ] Successful live ChatGPT research request: actual requests are blocked by subscription_sharing_usage_limit_exceeded (account or app usage controls). No automatic retry or paid fallback.
- [ ] Live GitHub update delivery (no public release available; no publication authorized).

## Current limitations
Windows deferred. No model bundled or on-phone inference engine. Text import does not retain document page layout, images or tables; scanned PDFs need OCR. Chicago is author-date, not footnotes. Citations and factual claims need review. OpenAlex and Semantic Scholar are documented future providers. See USER-GUIDE.md and PROVIDERS.md.

## Latest appearance and phone verification
- [x] Dark theme is the default; light remains optional. The elegant welcome screen opens with recent papers below.
- [x] Move the paper illustration above the green tagline to remove overlap.
- [x] APK asset path, system-bar insets/colors and keyboard focus issues found on Samsung were fixed.
- [x] 76 JavaScript tests, 18 OAuth protocol, 7 lifecycle, 39 DNS/registration/research-status, 53 assist/session and 52 update checks passed; browser and Samsung reports are recorded in VERIFICATION.md.

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
- [ ] Successful plan inference after the relevant account/app limit permits requests; web-search policy, live refresh/revocation remain unverified.
- [ ] User-managed publication and live GitHub delivery test after account access permits it. The authenticated GitHub API reported account suspension; commits/uploads are now manual by the user's instruction.

## Standalone Android deployment (latest constraint)
- [x] Record that the deployed phone cannot reach the user's PC behind its firewall; USB model tests are development-only.
- [x] Direct native ChatGPT writing requests using the encrypted account, Responses instructions/input, no unsupported fields and terminal stream confirmation.
- [x] First-class direct-account Helper/Complete/regeneration adapter with no PC or API-key fallback.
- [x] Fresh exact-payload preview and explicit consent before every remote writing request; cancel/stale-context refusal before transmission and stale-result refusal after waiting.
- [x] Primary phone-friendly connection/model setup; PC-server controls remain optional advanced settings.
- [x] Recommend strong research/writing models only when returned by the actual eligible account catalog.
- [x] Test privacy/transport/service boundaries, rebuild/sign/install, and verify the phone without PC model routing. Successful live plan inference remains externally blocked as recorded above.
- [x] Update user guide, architecture, privacy and release records for this deployment.
- [x] Prepare the local GitHub checkout and signed release assets for manual user commits/uploads; no automatic staging, commit, push or publication.
