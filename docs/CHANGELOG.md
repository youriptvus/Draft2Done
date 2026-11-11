# Release changes

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
