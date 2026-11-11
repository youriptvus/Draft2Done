# Release changes

## 0.1.4 — Android version code 3 (signed candidate; Moto sign-in repaired, live AI unverified)

The user overrode the attachment's 0.1.3 target to 0.1.4. Implemented changes:

- Add prominent, durable Paste Your Draft entry while preserving import options, exact text, protected originals and existing projects.
- Add contextual educational information links as an optional non-destructive reading layer, separate from submitted writing and formal citations.
- Permanently dedicate About to Victoria Downs with the exact requested sentence, application description and current version.
- Investigate genuine live search using real service evidence; keep unconfirmed web results unsaved and preserve all network failure/cancellation protections.
- Add clean existing-SDK rebuild support, stale-web-version refusal and derived test APK version metadata; preserve the same package/signing identity and previous stable assets.
- Extend full regression coverage for paste, links, About, existing writing/document/project workflows and actual Android lifecycle behavior.
- Preserve leading blank lines after textarea rendering and escape all dialog headings, including retrieved concept titles, as plain text.
- Fix pending-paste loss found by an immediate Android force-stop test: commit staging synchronously through a private native AtomicFile, migrate legacy browser data once, preserve corrupt records on read errors, and keep a durable empty tombstone after consumption.
- Extend the test-only live diagnostic with bounded provider error codes/parameters and fixed message-term flags; never record raw streams, credentials or arbitrary error prose.
- Keep the pending OAuth callback listener alive when an individual peer is idle, malformed, reset or timed out. Use bounded concurrent peers and per-peer deadlines while preserving loopback, state, PKCE, HTTP/query validation and cancellation limits.
- Start a temporary foreground service from the visible, user-initiated sign-in flow before opening Chrome, preventing the callback-owning process from being frozen while awaiting consent. Show immutable Return/Cancel notification actions, stop on completion/cancellation and enforce a ten-minute cap without changing battery/freezer settings or bypassing authorization.

Actual normal Chrome consent and a persisted plan-enabled connection now pass on the Moto G 2025 / Android 16, including a real force-stop/restart. The installed repaired APK is 852668 bytes, SHA-256 `33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db`; the repaired Samsung flow has not been retested. See [the repair report](SIGN-IN-REPAIR-0.1.4.md) and [current verification](VERIFICATION.md).

The earlier Samsung hosted-search diagnostic returned `subscription_sharing_usage_limit_exceeded` / `invalid_request_error`. It remains historical evidence, not a measurement of this newly connected account's current usage. No AI writing or hosted web/citation request was rerun during the sign-in repair. The whole app is not claimed fully verified. Version code 3 and manual candidate publication are retained; same-code replacement requires explicit installation. No staging, commit, push or publication is authorized. See [historical 0.1.2 verification](VERIFICATION-0.1.2.md) for that release's results.

## 0.1.2 — Android version code 2

- Prepare the next release version requested by the user, including the verified changes made after publication of 0.1.0.
- Preserve bibliography entries for already-cited sources when their future AI-use switch is disabled; unused excluded sources remain excluded.
- Apply saved count, export, voice, citation and opening-mode defaults to imported TXT/MD/DOCX/PDF papers while preserving settings embedded in project backups.
- Require the hosted search tool when web research is selected, so a completed memory-only answer cannot silently substitute for the selected search. Query-only research and writing retain their separate request rules.
- Refuse to save requested web research without confirmed completed search evidence. Validate citation ranges and ignore malformed spans; prose links and failed/in-progress tools do not establish browsing.
- Start one Android print job per rendered document, reject overlapping jobs and keep its WebView alive until the print adapter finishes; cover cancellation and reopening during real-device export checks.
- Render PDF exports from pinned, immutable document HTML instead of printing a blocked WebView error page; use the paper title as the default filename and reject render errors.
- Preserve adjacent PDF glyph runs on import so fi/fl words remain intact; cover text and Unicode using a real fictional Samsung-exported PDF fixture.
- Derive the welcome/About version from package metadata at build time; native app information and network identification use the installed Android package version.
- Validate Android/package version agreement and derive artifact filenames, release tag, download URL and install paths from that identity.
- Add real published-release checking and same-key upgrade tests, including exact workspace/account preservation and agreement between installed and displayed versions.
- Prepare the newest signed APK, update manifest and public build/signature records in the stable `current release` folder, alongside the local checkout's `release-assets`.
- Refresh verification, task and manual release documentation. Commits and GitHub publication remain controlled by the user.
- Add repository-local LF source line-ending rules, CRLF rules for Windows command scripts and binary exclusions, avoiding global Git setting changes.

Core writing, research and privacy behavior is retained from 0.1.0. Live ChatGPT availability still depends on eligible account/app usage. Test results and remaining external checks are recorded in VERIFICATION.md.

## 0.1.0 — Android version code 1

Initial Android release: local projects and protected originals, Writer DNA, reviewed Helper/Complete proposals, scholarly research/citations, document conversion/backup, optional direct ChatGPT consent, dark UI and signed GitHub update checks. The user published this release manually; public downloads and the Samsung's release check were verified. See VERIFICATION-0.1.0.md for the historical record.
