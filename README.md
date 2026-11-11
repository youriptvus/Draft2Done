# Draft2Done

**Your Draft. Your Voice. Done.**

Version 0.1.0. Android is the current priority; the Windows desktop app is deferred.

Draft2Done is an Android writing studio for papers you have already started. It protects the original, tracks approved additions, measures descriptive Writer DNA, and helps you develop the next part of your argument. Editing, saving, counts, DNA and local document tools work offline.

The primary AI route connects your phone directly to your own eligible ChatGPT account over HTTPS. Helper, Complete and section regeneration show the exact full request before each send: current draft, assignment requirements, style measurements and included evidence excerpts. Each bounded generation round needs fresh approval. Account eligibility, available models and usage limits apply; an error stops the request without switching providers or using a paid API key.

The signed release is `dist/Draft2Done-0.1.0.apk` (Android 8+). The default is the dark welcome screen; light mode is available in Settings. Automatic update checks target `youriptvus/Draft2Done` on GitHub and are enabled by default. No release has been published in this session.

The application includes Helper and Complete modes, original/working draft separation, Writer DNA, assignment goals, review of generated sections, revision history, scholarly source retrieval, APA/MLA/Chicago author-date metadata citations, TXT/MD/DOCX/text-PDF import, document exports and project backups. The separate ChatGPT research flow sends its approved query and selected excerpts; it does not automatically include the paper.

No language model is bundled on the phone, so offline descriptive tools continue to work while AI generation waits for an available provider. Advanced Ollama, LM Studio and llama.cpp support remains for environments with a reachable server. The user's remote phone cannot depend on the PC behind its firewall, USB reverse or port forwarding.

On the Samsung, real ChatGPT sign-in and the account model catalog succeeded. Actual plan requests returned `subscription_sharing_usage_limit_exceeded`; no successful ChatGPT research or writing inference is claimed. The connection remains available for an intentional retry when account/app usage permits it. See [verification](docs/VERIFICATION.md) for the observed limits and [model guidance](docs/MODELS.md) for tested and optional choices.

Development remains isolated to this directory. The native shell only serves bundled app assets; external sources are treated as untrusted text. Private signing materials stay in `signing/` and must be backed up separately, never published.

The local GitHub checkout is `D:\Draft2Done\repo\Draft2Done`. Add that folder in GitHub Desktop. `scripts\prepare-repo.ps1` copies source and verified `release-assets/` there without staging, committing or pushing; the user handles those actions manually. Upload the APK and `latest.json` as GitHub Release assets to enable update delivery. A source commit alone does not create a release. See [release instructions](docs/RELEASING.md).

Useful documentation:

- [User guide](docs/USER-GUIDE.md)
- [Build and signing](docs/ANDROID-BUILD.md)
- [Release/update procedure](docs/RELEASING.md)
- [Architecture](docs/ARCHITECTURE.md)
- [AI and research providers](docs/PROVIDERS.md) and [model guidance](docs/MODELS.md)
- [User decisions](docs/DECISIONS.md) and [task tracker](docs/TASKS.md)
- [Full brief and requirements mapping](docs/REQUIREMENTS.md)
- [Verification and outstanding checks](docs/VERIFICATION.md)

Build with a JDK and Node installed:

```powershell
npm ci
npm run build
powershell -ExecutionPolicy Bypass -File scripts\build-android.ps1 -SkipWebBuild
```

The build acquires its own Android SDK in `tools/` and verifies the APK's payload paths, alignment and signature. It uses a persistent release key. Existing user projects survive same-key APK updates.

```powershell
npm test
npm run test:ui
npm run test:documents
powershell -ExecutionPolicy Bypass -File scripts\test-oauth.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-updates.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-all.ps1
```

Browser tests need the project-local Chromium installed with `PLAYWRIGHT_BROWSERS_PATH=D:\Draft2Done\tools\playwright` and `npx playwright install chromium`. Device tests are a separate, same-key instrumentation APK under `build/device-tests`; they are not bundled into the released app.
