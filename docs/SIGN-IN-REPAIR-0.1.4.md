# Draft2Done 0.1.4 sign-in listener repair

Recorded 2026-11-11. This document covers the new Chrome/loopback sign-in investigation and patch. Earlier 0.1.4 test results remain historical evidence for their identified APKs; they are not automatically passes for this rebuilt APK.

**Current status:** actual normal-Chrome sign-in now succeeds on the installed foreground-service repair, 0.1.4/code 3. During a 20-second browser wait the Moto app was not cached or frozen and its sign-in service was active. The user then completed authorization; the connected, plan-enabled session survived Activity restart, the final regressions and a real force-stop/restart. Its sign-in error is cleared and the temporary service has stopped. Controlled service/lifecycle checks pass 11, upgrade checks seven, final device regression 71, native paste storage 34, feature UI 20, cold-restart setup/verification 5/16 and independent artifact audit 24. The pulled installed APK exactly matches the final `33435ac8…` release. Local release-copy verification passed for all 108 source files and four release assets; Git HEAD/index/configuration remain unchanged. Model metadata loaded without inference; no AI generation or hosted-web success is claimed.

## Reported problem and preserved starting point

The user reported that Chrome timed out at `127.0.0.1` during ChatGPT sign-in on both Moto and Samsung, with 0.1.2 and 0.1.4 mentioned. The requested priority was to repair 0.1.4 and install it before continuing other project work.

Before changes, the prepared 0.1.4 source snapshot of **102 files** and its signed APK were preserved at:

```text
D:\Draft2Done\test-results\checkpoints\v0.1.4-before-signin-repair-20261111
```

That checkpoint's historical APK SHA-256 is `579a7a2abc63130afe5fe2b6bb3b92735f2e3cd36a34ef6b2f45d683f08a9f4c`. The signing identity, original projects, pending-paste storage and existing account data were retained.

The actual test phone for this repair is a **Moto G 2025 running Android 16**, connected through wireless ADB. It initially had Draft2Done **0.1.2/code 2** installed. The Samsung report is recorded as user evidence; no new controlled Samsung reproduction or repaired Samsung installation is claimed here.

## Confirmed defect and uncertainty

The old sign-in listener accepted one TCP client and attempted to read its HTTP request with a five-second socket timeout. That peer timeout escaped the entire authorization wait. The sign-in method then closed the listener in its `finally` block. An idle connection could therefore stop the whole local callback listener while the intended browser authorization was still pending.

This was reproduced against the actual old native sign-in implementation on the Moto. The test intercepted the browser launch before external navigation, opened a real idle loopback connection, waited beyond the old five-second read timeout and verified that the old listener closed. **Nine checks passed** in `test-results/moto-signin-listener-old.json`, including exact account/workspace restoration. No OAuth authorization request, token exchange or AI inference was sent by this controlled reproduction.

The listener shutdown is a confirmed product defect and can explain a later browser being unable to reach the local callback. It is **not proven that an idle Chrome preconnection was the exact initiating event in the user's original attempts**. The controlled test does not establish a Chrome defect. The first repaired build's actual browser attempt subsequently exposed a distinct, directly observed background-freezing failure described below.

## Actual Chrome failure after the first repair

On the first repaired APK (`c2692f60…`), the user continued the normal consent flow. Buttons became disabled while the flow waited, then Chrome displayed **`ERR_CONNECTION_TIMED_OUT`** at the local `/auth/callback`. This was a genuine normal-app browser attempt outside instrumentation, not a synthetic callback.

The sanitized capture `test-results/moto-signin-browser-first-repair-failed.json` records all of the following together:

- The app's PID 12128 was still alive.
- Android reported `cached:true` and `processFrozen:true` for that process.
- The browser callback host/path were `127.0.0.1` and `/auth/callback`; no query or credentials were saved.
- An independent local socket probe timed out.
- `oauthCompleted:false`.

This is direct evidence of a frozen callback-owning process during the failed real browser return. Merely observing a live PID did not prove that its threads could accept/respond to a callback. The idle-socket repair is therefore necessary for the separately reproduced defect but was **insufficient to complete actual browser authorization**.

Android's official documentation states that Android 14 and later freeze cached processes after ten seconds in that state; a frozen process remains in RAM but its threads cannot execute. It also describes elevating work to a noncached state, such as an appropriate foreground service, before that work is needed. This explains the significance of the captured frozen state; it does not imply permission to change system freezer or battery policies. [Android cached-apps freezer documentation](https://source.android.com/docs/core/perf/cached-apps-freezer).

### Second repair: implemented, installed and verified through actual Chrome

The scoped change is a temporary, user-initiated `SignInKeepAliveService`. `ChatGPTConnection` acquires its lease and waits for foreground readiness before opening the authorization browser, checks the pending request again, and closes the lease in `finally`. The non-exported service is non-sticky, limited to the current attempt and capped at ten minutes; cancellation, task removal, failure and normal completion release it. Identity-checked cleanup prevents an old attempt's teardown from stopping a newer one.

The manifest declares the `specialUse` service type and foreground-service permissions. The service creates an ongoing sign-in notification with immutable Return/Cancel actions. These actions carry no OAuth state, code, verifier, account identifier or authorization query; Cancel uses an opaque per-service generation value. The service is not a model/research transport and does not authorize any AI request.

The new signed build was installed before broader work, as requested. **Eleven actual controlled Moto checks pass**, including the exact attempt's active service and lease removal on cancellation, alongside the existing listener/account/workspace restoration checks. This controlled fixture intercepts the external authorization launch; actual human consent was verified separately.

In the normal app/Chrome flow outside instrumentation, the app remained `cached:false`, `isFrozen:false` with its foreground service active after a **20-second browser wait**: `test-results/moto-signin-foreground-lifetime.json`. That capture was taken while consent was still pending, so its `oauthCompleted:false` is not a failed completion result. The user subsequently finished consent and returned to the app. After the actual service stopped, a fresh Activity inspection confirmed a persisted connected session, enabled plan permission, a cleared sign-in error and a closed listener: `test-results/moto-chatgpt-connected-014.json`. This is the successful real-browser acceptance result for the second repair; the first repair's failure remains recorded above.

The live eligible-model catalog then loaded in **two passing checks** without an inference request: `test-results/moto-chatgpt-models-014.json`. Model availability establishes neither generation quality nor successful hosted browsing.

No battery-optimization exemption, system freezer change, browser-security change, public callback host or OAuth-validation bypass was used.

## Repair

`ChatGPTConnection` now delegates the callback wait to the new `LoopbackCallbackServer` helper:

- One idle, malformed, reset or timed-out peer is closed independently; it cannot terminate the pending login.
- Up to **12 concurrent peers** are accepted into bounded workers. Idle peers do not serialize the legitimate callback behind their read timeouts; excess peers are rejected without unbounded worker growth.
- Each peer has a bounded absolute deadline, and the whole authorization attempt retains its overall five-minute deadline. Cancellation is checked throughout and closes accepted sockets/workers promptly.
- The listener remains bound to device loopback. Request-line/header limits, the callback path, method, host, query shape and current state are validated. Duplicate/invalid parameters, unexpected bodies and stale states cannot complete a login.
- Callback responses are generic and discourage caching/referrer disclosure. They do not echo authorization codes or state. A verified callback is returned privately to the existing OAuth owner for the established code exchange and identity checks.

This changes the local callback transport, not the identity provider or authorization contract. The app still starts the listener before opening the system browser, uses `http://127.0.0.1:<selected-port>/auth/callback`, and keeps the exact callback URI for the authorization and token exchange. Fresh state, nonce and PKCE, issued-client handling, ID-token verification and granted-plan-permission checks remain required. These follow the [official OpenAI registration and sign-in guide](https://developers.openai.com/siwc/token-sharing-open-source/sign-in).

`127.0.0.1` here means the Android phone itself. No PC model server, USB reverse, port forwarding, VPN or public callback host is introduced. The repair does not skip consent, accept arbitrary callbacks, disable identity verification, reuse another account's session or add an API-key billing fallback.

## First repaired artifact and immediate installation: now historical

| Item | First listener-only repair; actual Chrome authorization failed |
| --- | --- |
| Preserved APK | `D:\Draft2Done\test-results\checkpoints\v0.1.4-signin-first-repair-c269\release\Draft2Done-0.1.4.apk` |
| Package | `io.draft2done.app` |
| Version / version code | **0.1.4 / 3** |
| Size | **848572 bytes** |
| SHA-256 | `c2692f60ee352766fb11a1ff78e7153472ebdd05d1aed4275cdafa18b89ba99e` |
| Certificate SHA-256 | `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd` |
| Signing | Existing release key retained; v2/v3 verification passed |
| Android | Min API 26, target API 36 |

The existing project-local Android SDK clean-build pipeline was used. No Gradle project was introduced. The new APK was installed on the Moto **before proceeding to the broader regression**, honoring the requested order. The same-key upgrade from 0.1.2/code 2 passed **seven actual checks**, including workspace/account fingerprints and installed/native/welcome/About version agreement: `test-results/moto-upgrade-014-signin-fixed.json`.

An independently pulled installed APK matches the rebuilt local APK's `c2692f60…` hash. This confirms which bytes were installed; it does not by itself establish a successful human OAuth exchange.

The first repaired APK and its build/signature/manifest sidecars are archived under `test-results/checkpoints/v0.1.4-signin-first-repair-c269/release`. The installed foreground-service build's identity is listed below. The table above identifies only the historical listener-only repair.

## Current foreground-service candidate and installation

| Item | Installed second repair |
| --- | --- |
| Local APK | `D:\Draft2Done\dist\Draft2Done-0.1.4.apk` |
| Package / version / version code | `io.draft2done.app` / **0.1.4 / 3** |
| Size | **852668 bytes** |
| SHA-256 | `33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db` |
| Certificate SHA-256 | `6c9baa34437afbd7a3607e5e0d5bff5cb76cc75f75b220dee9f883394b27f7fd` |
| Installation | Installed on the Moto before broader work; seven same-key upgrade/preservation/version checks passed |
| Installed-byte comparison | **EXACT MATCH**: pulled installed APK is 852668 bytes with the `33435ac8…` SHA-256 |
| Independent final artifact audit | **24/24 PASS**, `test-results/signin-foreground-artifact-audit-014.json` |

The earlier prepared `579a7a2a…` APK and listener-only `c2692f60…` APK remain historical snapshots. Neither hash identifies the currently installed foreground-service candidate. The independent audit confirms the new artifact's identity, signatures/alignment, all five packaged web assets matching current `web-dist` and frozen Android assets, compiled non-exported `specialUse` service/permissions, absence of test instrumentation, retained features and unchanged source/release checkpoints. This packaging result is separate from the actual Chrome/session evidence.

The final installed-byte and session summary is `test-results/moto-final-signin-verification-014.json`. Following all device suites and an actual force-stop/restart, a fresh Activity still reported a connected account, enabled plan permission, no sign-in error and no sign-in in progress; the temporary service was stopped. The Moto was left on Settings showing the connected account. The preservation tests restored the user's original papers, preferences, account and pending input. All actual repaired-device results in this report are from this Moto; the Samsung has not been retested with the repair.

The patch retains version code 3. A phone already on another 0.1.4/code 3 build needs an explicit installation of these repaired bytes; the updater's increasing-version-code rule must not be represented as automatically delivering this same-code replacement. Refreshed release/candidate copies and any public publication remain separate steps.

## Executed verification

| Check | Result | What it proves |
| --- | --- | --- |
| Host real loopback TCP suite | **126 PASS** | Actual TCP connections with synthetic callback data: idle/concurrent peers, invalid HTTP/query/state, bounds, deadline/cancellation and cleanup. No identity-provider traffic or human OAuth completion |
| Existing OAuth/protocol checks | **38 PASS** | Existing signed identity and stream/citation boundaries remain covered |
| Request lifecycle checks | **7 PASS** | Existing controlled request lifecycle checks |
| DNS/registration/research-status checks | **39 PASS** | Existing controlled native support boundaries |
| Assist/session checks | **53 PASS** | Existing reviewed payload/session boundaries; not live AI inference |
| Research-request checks | **12 PASS** | Existing tool/evidence request boundaries; not successful hosted web search |
| Actual old native listener reproduction | **9 PASS** | `test-results/moto-signin-listener-old.json`: the idle peer stops the old listener; external authorization browser launch was intercepted |
| Actual repaired native listener regression | **9 PASS** | `test-results/moto-signin-listener-fixed.json`: the listener stays active and sign-in remains pending after that same idle-peer timeout; external authorization browser launch was intercepted |
| Actual Moto signed upgrade | **7 PASS** | `test-results/moto-upgrade-014-signin-fixed.json`: same-key data/session preservation and current version agreement |
| First-repair installed-byte identity | **MATCH** | Pulled Moto APK and first-repair local APK share the `c2692f60…` SHA-256 |
| Actual normal Chrome OAuth on first repair | **FAILED** | `test-results/moto-signin-browser-first-repair-failed.json`: Chrome callback timeout, app alive but cached/frozen, local probe timeout and no OAuth completion |
| Foreground-service repair implementation/build/install | **COMPLETED** | Current `33435ac8…` candidate installed before broader work; service implementation and manifest compiled |
| Foreground candidate controlled native sign-in/lifecycle | **11 PASS** | `test-results/moto-signin-listener-foreground-fixed.json`: exact pending service active, cancellation removes its lease, listener/registry cleaned and prior account/workspace restored; authorization browser intercepted |
| Foreground candidate same-key upgrade | **7 PASS** | `test-results/moto-upgrade-014-foreground-fixed.json`: workspace/account fingerprints and actual/displayed version agreement |
| Targeted host checks after service compilation | **PASS: 126 + 38/7/39/53/12** | Real loopback TCP plus existing OAuth/lifecycle/support/assist/research-request checks rerun; no personal OAuth or inference |
| Actual normal Chrome background lifecycle on current candidate | **PASS** | `test-results/moto-signin-foreground-lifetime.json`: after 20 seconds outside instrumentation, app not cached/frozen and sign-in service active; consent still pending at this capture |
| Actual normal Chrome OAuth after service repair | **PASS** | `test-results/moto-chatgpt-connected-014.json`: user completed real Chrome consent; session connected and plan permission enabled after fresh Activity start, sign-in error cleared, service stopped and listener closed |
| Live eligible account model catalog | **2 PASS, no inference** | `test-results/moto-chatgpt-models-014.json`: live metadata request succeeded; no AI request or hosted-web result asserted |
| Independent final foreground artifact audit | **24/24 PASS** | `test-results/signin-foreground-artifact-audit-014.json`: current `33435ac8…` identity, signatures, all five assets, compiled service and historical checkpoint preservation |
| Complete first-repair full-host runner | **PASS, first-repair source snapshot** | `test-results/all-tests-host.json`: 107 JavaScript checks, existing browser suites, 11 paste groups, eight information-link groups and 52 update checks. Production web files were unchanged by the service patch; this historical host result is not successful current human sign-in |
| Final standard repaired-APK Moto regression | **71 PASS** | `test-results/all-tests-device.json`: native recovery, import/export bytes, update/provider boundaries and standalone sharing consent with simulated assist; no live inference |
| Final native draft-input storage regression | **34 PASS** | `test-results/moto-draft-input-store-014.json`: exact synchronous UTF-8 persistence, bounds, atomic recovery, failure preservation and consumed-input tombstone; real user workspace/backup unchanged |
| Final 0.1.4 feature UI regression | **20 PASS** | `test-results/moto-release014-ui.json`: paste, keyboard/navigation/rotation/reload, Helper/Complete, contextual reader, exact dedication, save/reopen and restoration. Executed on the Moto; inherited Samsung wording in two fixture labels does not indicate a Samsung run |
| Final actual force-stop setup / verification | **5 PASS / 16 PASS** | `test-results/moto-paste-restart-start-014.json` and `test-results/moto-paste-restart-verify-014.json`: exact native pending input, saved paper/revisions and connected account survive real process termination; original user workspace/backup/staging restored |
| Final installed-byte binding | **EXACT MATCH** | `test-results/moto-final-signin-verification-014.json`: independently pulled installed APK is 852668 bytes with the current `33435ac8…` SHA-256 |
| Final persisted sign-in after all tests and force-stop | **PASS** | Same final summary: genuine Chrome OAuth completed, session connected after real cold restart, error cleared and temporary service stopped; no inference sent |
| Final release-copy verification | **PASS** | `test-results/line-endings-local-copy-signin-014.json`: 108 exact source copies and all four exact release assets, correct LF/CRLF/binary rules, historical releases intact, no private directories, unchanged Git HEAD/index/configuration; unstaged and unpublished |

The repaired actual native test exercised production code, while deliberately preventing browser authorization from starting. It also verified that the isolated test restored encrypted account-file bytes/sidecars, the in-memory vault and prior error, account-status preferences, workspace and previous-save backup. A passing controlled test is not relabeled as a completed personal sign-in.

The earlier point-in-time observation that PID 12128 stayed alive after Chrome opened was superseded by the definitive capture showing that the same process was cached/frozen during the first repair's timeout. That failed attempt is not explained away by the controlled listener tests or relabeled as the later success. No credentials, live callback query strings, authorization codes or tokens are reproduced in this report.

The first broader-device runner invocation stopped at its default Samsung manufacturer guard because the explicitly selected authorized phone is a Motorola. Its configuration-only failure is preserved in `test-results/moto-device-runner-manufacturer-guard.json`; it did not exercise a product test. The runner now accepts the explicit expected manufacturer `motorola` for this authorized Moto run. No product defect is inferred from the guard rejecting a mismatched test-device configuration.

## Delivery and remaining verification

Local delivery is complete: `current release` contains the tested signed APK and its three supporting files; `repo/Draft2Done` contains the matching 108-source checkout and release assets. Exact copies, line endings, binary bytes, private-file exclusions, previous releases and unchanged Git HEAD/index/configuration passed the final audit. The first copy comparison caught a documentation edit still in flight; it was preserved as `local-copy-signin-014-first-attempt.json`, then the frozen files were recopied and the full audit passed. The installed-byte comparison, actual normal-browser sign-in, final device suites and cold-restart checks are complete. Host coverage consists of the unchanged-web full run and the native checks rerun after service compilation. No staging, commit, push or public release was performed.

The historical ChatGPT hosted-web-search usage-limit blocker is separate from sign-in transport. No inference or hosted-web request was retried during this sign-in repair, so the account's current inference quota and hosted-web outcome remain untested. Successful authentication does not reset quota or establish successful hosted browsing, AI generation or cited research. The current artifact's feature, storage and preservation passes are recorded above; successful live AI writing or cited web research is outside this repair's verification scope.
