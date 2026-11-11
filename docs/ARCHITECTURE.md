# Architecture

Android 0.1.0 is a native Java host for a bundled browser UI. It does not depend on a remote website. Windows desktop work is deferred; the Python venv is unused by the APK.

| Area | Code | Responsibility |
| --- | --- | --- |
| Mobile interface | `web/app.js`, `styles.css`, `chatgpt-ui.js` | Editor, research, review, themes and settings |
| Domain | `web/core.js` | Versioned projects, counts, Writer DNA, draft observations, citations, revisions and contribution ranges |
| AI and research | `web/services.js` | Direct-account and optional server adapters, data-separated prompts, request consent, context guards, proposals and official scholarly APIs |
| Documents | `web/documents.js` | Bounded imports, export and print preview |
| Native adapter | `web/platform.js` | Cancellable promise bridge, storage and file operations; browser fallback |
| Android shell | `MainActivity.java` | Pinned-origin WebView, atomic private state/recovery, background requests and SAF |
| Accounts | `ChatGPTConnection.java`, `OAuthProtocol.java` | PKCE/loopback OAuth, signed identities, native encrypted tokens, accounts, refresh/revocation and terminal SSE handling |
| Remote writing contract | `AssistRequest.java`, `ChatGPTSessionState.java` | Exact reviewed instructions/input, bounded full payloads, eligible per-account models, quota and unusable-session handling |
| Updates | `AppUpdates.java`, `UpdateProvider.java` | GitHub releases, hash/package/version/certificate checks and Android install handoff |
| Build | `scripts/` | Local SDK, esbuild, javac/D8/AAPT2, ZIP alignment, release signing and verification |

The deployed phone cannot reach the user's PC. Primary AI calls go directly from Android to OpenAI over HTTPS using the encrypted account session. Each draft-assistance request needs a fresh preview/approval of an immutable payload; changed context invalidates approval. Native writing requests translate trusted application instructions to Responses instructions and put draft data in user input, omit unsupported parameters/tools, and require terminal stream completion. Neither a PC endpoint nor USB reverse is involved.

Full papers are not sent to scholarly APIs or the separate ChatGPT research action, which retain the minimal query/excerpt contract. Approved writing requests can include the full draft and assignment context. Optional advanced server support remains available for other environments. Offline editing and deterministic analysis do not need an AI account; no on-phone language model is bundled. Imported text and sources cannot run commands, invoke bridge methods or choose file paths. HTML is escaped; the WebView blocks external resources/navigation, direct web requests, frames and inline scripts.

Projects use versioned `.d2d` JSON and atomic app-private storage. This keeps drafts, research and revisions together without a remote database. Tokens and signing keys are separate from project storage and exports. Portable domain/service modules can support a later Windows interface.
