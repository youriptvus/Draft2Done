# Draft2Done

**Your Draft. Your Voice. Done.**

Signed candidate: **Android 0.1.4, version code 3**. The published 0.1.2/code 2 release and its source checkpoint are preserved. Windows desktop work remains deferred.

**Normal Chrome sign-in now succeeds on the Moto G 2025 / Android 16.** The repaired APK is **852668 bytes**, SHA-256 `33435ac8a214729c7dabe7e6415528439e90509bfd6194cf8a4b418aea2ce6db`. Its temporary sign-in foreground service keeps the callback responsive while Chrome is open, then stops; the connected session survived the final Moto regressions and a real force-stop/restart. See [the sign-in repair report](docs/SIGN-IN-REPAIR-0.1.4.md) and [current verification](docs/VERIFICATION.md) for exact results and remaining checks. This repaired build has not been retested on the Samsung.

Draft2Done is a writing studio for papers you have already started. It protects the original, keeps generated continuations separate until approval, measures descriptive Writer DNA and stores projects locally. Editing, saving, counts, DNA and document tools work offline; no language model is bundled on the phone.

The primary AI route connects Android directly to an eligible ChatGPT account over HTTPS. Each actual Helper, Complete or regeneration request previews the entire exact payload and needs fresh approval. Account credentials remain native and encrypted. The phone does not depend on the user's firewalled PC, USB forwarding or a paid API-key fallback. Free scholarly APIs are a separate optional research route.

Version 0.1.4 adds a prominent **Paste Your Draft** entry, contextual information links that do not alter document text or exports, and the permanent Victoria Downs dedication in About. The unchanged web bundle passed the full host regression, including 107 JavaScript tests and the paste/link workflows; native protocol checks were rerun after the sign-in service patch. See [current verification](docs/VERIFICATION.md), [the historical pre-sign-in development report](docs/DEVELOPMENT-REPORT-0.1.4.md) and [the target-override brief](docs/RELEASE-BRIEF-0.1.4.md).

**0.1.4 is not yet fully verified.** An earlier Samsung web diagnostic returned `subscription_sharing_usage_limit_exceeded` (`invalid_request_error`) before completed output or search evidence. That preserved result does not establish the newly connected Moto account's current usage, reset time or hosted-search outcome. The sign-in repair verified authentication and eligible-model metadata without AI inference; writing and hosted web/citations were not rerun. Requested but unconfirmed web research is refused without saving a brief; no automatic account/model/billing fallback is used.

Version 0.1.2 previously passed actual ChatGPT Helper/Complete/regeneration/query-only research, refresh/revocation/reconnection, native document save/reimport and software regression checks. Those historical results do not establish 0.1.4 results. See [current verification](docs/VERIFICATION.md) and [preserved 0.1.2 verification](docs/VERIFICATION-0.1.2.md).

Development stays in `D:\Draft2Done`. Private signing materials remain in ignored `signing/`. The local GitHub checkout is `D:\Draft2Done\repo\Draft2Done`; candidate files are prepared in `D:\Draft2Done\current release`. Required live AI acceptance checks remain unverified. Preparation does not stage, commit, push or publish. The user handles publication manually. The repair remains code 3, so replacing an earlier 0.1.4/code 3 installation requires an explicit install; the updater does not automatically deliver a same-code replacement.

The project uses the existing Android SDK pipeline, not Gradle. Once source is frozen, perform a clean candidate build with JDK 17+ and Node installed:

```powershell
npm ci
powershell -ExecutionPolicy Bypass -File scripts\build-android.ps1 -Clean
powershell -ExecutionPolicy Bypass -File scripts\build-device-tests.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-all.ps1
```

`-Clean` rebuilds generated web/native output while preserving SDK downloads, signing identity and older `dist` artifacts. It cannot be combined with `-SkipWebBuild`. The resulting candidate is `dist/Draft2Done-0.1.4.apk`; release readiness still requires the checks in VERIFICATION.md.

Useful documentation: [user guide](docs/USER-GUIDE.md), [architecture](docs/ARCHITECTURE.md), [build/signing](docs/ANDROID-BUILD.md), [release procedure](docs/RELEASING.md), [requirements](docs/REQUIREMENTS.md), [decisions](docs/DECISIONS.md), [changes](docs/CHANGELOG.md), [providers](docs/PROVIDERS.md) and [model guidance](docs/MODELS.md).

Browser tests use the project-local Chromium under `tools/playwright`. Device tests are a separate signed instrumentation APK and are not bundled into the release.
