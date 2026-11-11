# Current development and delivery state

Updated 2026-11-10 (America/New_York). The user-requested pause was saved at checkpoint `0d27d35`, then development resumed. The standalone Android changes are implemented, built, signed, installed and tested. The user handles Git commits and uploads manually.

## Standing requirements

- Signed Android APK first; Windows remains deferred.
- Automate tests, edits, builds and device checks. Involve the user only for direct authentication.
- The deployed phone cannot connect to the user's PC behind its firewall. Primary AI uses direct phone-to-OpenAI HTTPS; no PC model, USB reverse, port forwarding or VPN is required.
- Keep the elegant dark welcome screen, raised paper illustration, protected originals and reviewable AI additions.
- Recommend suitable strong models using the actual eligible account catalog. No model downloads or standard paid API-key fallback.
- Automatic update checks default on, targeting `youriptvus/Draft2Done`.
- Prepare changes locally in `D:\Draft2Done\repo\Draft2Done`; no automatic staging, commits, push or publication. The user will commit/upload manually.

## Final installed release

`dist/Draft2Done-0.1.0.apk`, 836284 bytes, SHA-256 `7574934df73594784417fb73c6bdce9ca3fb4fafb61204f5a70484389bc05580`.

Package `io.draft2done.app`, version 0.1.0/code 1, Android 8+ / target API 36. Non-debuggable, cloud backup disabled. Signature schemes v2/v3 verified with the persistent release certificate. Same-key installation preserved projects and the encrypted account session.

Private signing material stays in ignored `signing/` in the build workspace. Never print, commit or publish it. Future updates require that same key and an increased versionCode.

## Completed standalone AI changes

- Direct ChatGPT Helper, follow-up, Complete and section regeneration use the encrypted native account session.
- Every actual writing request needs fresh exact-payload preview and explicit approval. Cancellation sends nothing. Changed draft/requirements/profile/source/account/model invalidates consent; changes during a pending reply discard that reply.
- Reviewed system content becomes Responses instructions verbatim; user data becomes array input. Requests use store:false/stream:true, no tools or unsupported sampling/output parameters, and require terminal completion.
- Full reviewed payloads are retained under 200,000 message characters and 1 MiB serialized limits. Local server context settings do not restrict the direct route; the provider checks its actual model capacity. No draft truncation.
- Account model membership is checked natively with an account-specific short-lived encrypted catalog cache. Sign-out/switch invalidates it. Unusable refresh credentials clear only the matching old session.
- Model guidance uses only returned eligible slugs. Astra is available on the tested account and was selected through the recommendation button. Catalog availability is not successful inference or proof of writing quality.
- Offline editing, saving, counts, descriptive DNA, draft observations, saved research and document tools remain available. No on-phone language model is bundled. Advanced server support is optional for other environments with a reachable endpoint.

## Final verification

- Host: 76 JavaScript tests; 18 OAuth, 7 request lifecycle, 39 DNS/registration/quota, 53 assist/session and 52 updater checks; all browser writing/review suites and 11 document/add-on checks passed.
- Samsung SM-S166V / Android 16: 18 atomic recovery checks, 5 native file-byte checks, 24 update/provider checks and 19 standalone phone checks passed against the final signed APK.
- Standalone testing uses the real connected session and actual eligible model catalog, then verified simulated assist replies. It covers exact consent/cancel/Helper/Complete/regen/accept/quota/no PC fallback and cleans up its own new fixture paper. No provider inference was sent by that suite.
- The developer USB reverse mapping was removed before the final standalone run. No model server transport was used.
- Earlier actual source retrieval and PC model quality tests are preserved in VERIFICATION.md. They do not establish a remote connection from the production phone.
- Detailed results and screenshots remain in ignored `test-results/`; host/device reports have separate filenames.

## Real account and external limits

Personal browser consent, signed identity, encrypted restart persistence and model catalog succeeded on the Samsung. The observed catalog was: `gpt-6.1-sol`, `gpt-6-astra`, `gpt-6.1-terra`, `gpt-6-sol`, `gpt-6-luna`, `gpt-5.6-sol`, `gpt-5.6-terra`, `gpt-5.6-luna`.

Actual plan requests returned `subscription_sharing_usage_limit_exceeded`. The account remains connected; no incomplete brief or paid fallback was saved. Further automatic inference attempts are stopped. Successful live plan writing/research, web search, refresh rotation and remote revocation remain unverified until usage permits intentional testing. Do not infer a reset time or the whole plan's balance. An account or app-specific limit can apply.

Automatic approval review blocked opening ChatGPT Usage through ADB, citing policy; no alternate action bypassed it and no account limits were changed.

The public GitHub latest-release endpoint returned 404; authenticated GitHub CLI previously reported account suspension. No publication occurred. The user's new instruction is to prepare local files only. `scripts/prepare-repo.ps1` copies source and verified release files into the existing checkout without touching its `.git` metadata or staging/committing/pushing. `release-assets/README.md` explains manual GitHub Release uploads. A source commit alone does not activate APK updates.

## Workspace and remaining work

- Build workspace: `D:\Draft2Done`. GitHub Desktop checkout: `D:\Draft2Done\repo\Draft2Done` (origin `https://github.com/youriptvus/Draft2Done.git`).
- Leave the separate older user-created `draft2done/` checkout untouched. Neither nested checkout is added as a submodule to the build workspace.
- Samsung uses the existing ADB server on port 5038 with explicit serial `R5GL75PGMFP`; leave other devices and ADB servers unchanged.
- Remaining external checks: successful plan inference after account/app usage permits it, and a real higher-version GitHub update after the user publishes a release.
- Windows, OCR, exact imported page layout, exhaustive citation types and Chicago footnotes remain deferred/limited as documented.