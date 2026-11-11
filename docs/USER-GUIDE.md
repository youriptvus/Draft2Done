# Using Draft2Done on Android

## Start on your phone

The signed 0.1.4 repair is `current release/Draft2Done-0.1.4.apk` for Android 8 or later. Normal Chrome sign-in now passes on the Moto G 2025; writing inference and hosted ChatGPT search were not rerun for this repair. See [VERIFICATION.md](VERIFICATION.md) for current test coverage. Previous 0.1.2 remains preserved. The app runs independently of VS Code and the user's PC. No PC model server, USB reverse, port forwarding or VPN is required for primary AI. No on-phone language model is bundled.

1. **Paste Your Draft** appears above import options: paste writing you already started and choose **Use This Draft**. This saves a separate paper with a protected original; it does not replace an existing project. TXT, Markdown, DOCX and text-PDF import remains available.
2. Use **Set goal** for the word minimum, optional maximum, instructions and rubric. Reference sections and explicitly marked title blocks are excluded by default. End a title block with `---` or `<!-- title-end -->`; unmarked text is counted in full.
3. Edit **My draft**. Undo/Redo survives tab switches. The paragraph selector helps navigate longer drafts. The paper menu contains the original, checkpoints, history, project backup and export.
4. Open Settings and **Continue with ChatGPT** for direct phone-to-OpenAI assistance. Sign in in the system browser, approve plan usage and return to the app. The native model list contains only choices available to that account. No standard API key is required.
5. Helper offers observations and selectable advice. Complete generates separate editable proposals; accept or reject each section. Generated text is never silently merged into the protected original.

Editing, local saving, word counts, descriptive Writer DNA, previously saved research and document import/export work without ChatGPT. Free scholarly search requires internet but no ChatGPT subscription. AI drafting on the primary route requires internet and an eligible ChatGPT account with plan access and available account/app usage. A free website sign-in alone does not grant inference.

## Complete ChatGPT sign-in

Start **Continue with ChatGPT** while Draft2Done is open, then finish authentication and consent in the browser. Android keeps a temporary **Connecting to ChatGPT** status while you do this. Tap its notification to return to Draft2Done, or choose **Cancel** to stop that sign-in attempt. The browser's **Return to Draft2Done** button also brings you back. The temporary status stops when the attempt completes, is canceled or expires.

The browser may show a return address beginning with `127.0.0.1`. That address is the same phone, not your PC; it is the local return step for sign-in. You do not need to change the firewall, browser security or battery settings. If the attempt expires or an old browser tab shows a connection error, return to Draft2Done Settings and start a new sign-in instead of refreshing the old return page. Connected status confirms sign-in; a later writing or research request still needs its own approval and available plan usage.

## Paste and explore your draft

Pasted wording, punctuation and blank lines are retained. The scrollable input supports Android selection, copying and pasting. On Android, unsubmitted text is committed synchronously to a separate app-private file on input, navigation and pause, even when workspace Autosave is off. The browser preview uses localStorage. **Use This Draft** explicitly saves the new paper before clearing its staging copy. Storage errors leave the previous committed paste and existing projects intact; keep a copy of any newer unsaved text if a warning appears. Input is bounded to 10 MiB and the complete workspace to 4 MiB, so very large papers may need splitting or old backed-up projects removing.

My Draft, Helper and Complete include an **Information links** toggle, off initially. When enabled, a separate reading view underlines selected educational concepts without changing the editor, original, export or bibliography. Tap a concept to review related saved records and their provenance. **Open saved source** opens the browser only after your tap. **Review research query** takes an editable short query to Research; it does not turn on Online or send anything. Enable Online and choose Search separately when ready. Offline reading and previously saved metadata remain available; opening a web page requires a connection.

Concept detection uses conservative local English concepts and Unicode name/technical-term heuristics. It can miss or misunderstand a term. Analysis scans the first 500,000 characters, while the reader retains the entire text; links are limited and nearby repeats suppressed. Retrieved URL metadata and relevance checks do not guarantee page availability, credibility or claim support. Formal academic citations are still added through the source library.

Settings → **About Draft2Done** shows the current version, description and permanent dedication: “Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.”

## Approve each remote writing request

Helper, follow-up questions, Complete and section regeneration can send writing to ChatGPT. Before **every actual request**, the app shows the selected model and the exact data prepared for transmission: the draft, assignment details, style profile, relevant evidence excerpts and any previous proposed wording needed for that step.

Choose **Send to ChatGPT** to approve that request, or **Keep this on my phone** to cancel without sending. Expand the exact-payload view when you want to inspect every field. A changed draft, assignment, profile, source selection, account or model invalidates the pending approval. Multi-step completion asks again for each step. An earlier approval is not permission for a later changed request.

Writing requests use the phone's encrypted native account session and OpenAI's supported Responses endpoint with `store:false` and `stream:true`. The app saves a result only after terminal completion. It does not introduce an API-key billing fallback, change account limits or upload drafts automatically. OpenAI's own data-handling policies still apply to requests you approve.

Direct ChatGPT writing retains the entire reviewed request, up to 200,000 total message characters and 1 MiB serialized payload. Advanced server context settings apply only to server backends; OpenAI enforces the chosen account model's actual context. Oversized requests fail without truncation, and changed content needs approval again. Continuation steps remain bounded in the app; the phone sends no output-token override.

Original Voice asks for compatible wording and rhythm; Polished Voice asks for clearer writing while retaining the argument. Writer DNA provides descriptive measurements, not authorship detection or a guarantee of identical style. Review all claims, personal details, quotations and reasoning before accepting text. Missing evidence can cause a refusal or shortfall; the app reports this instead of declaring an unsupported target complete.

## Choose a model

Load the actual account catalog in Settings. The recommendation button can select a recognized stronger model only if that model is in the returned catalog. Existing explicit selections remain under your control. Guidance is dated 2026-11-10 and may change as models and account policies change.

Astra is the documented highest-capability choice among the recognized research models; Sol balances capability and responsiveness; Terra is suited to natural writing; Luna emphasizes efficiency. Availability, tools and usage depend on the account. A recommendation does not prove that a generated claim is true. Sources: [OpenAI model selection](https://developers.openai.com/api/docs/guides/model-selection) and [plan-backed model catalog](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference).

A usage-limit message can apply to an account or an individual app. The account can remain connected while requests are limited. Check ChatGPT Settings → Usage; the app does not guess a reset time, bypass a limit or switch to another billing path.

An earlier Samsung test returned `subscription_sharing_usage_limit_exceeded` before completed hosted web-search evidence. That request was not repeated after the repaired Moto sign-in, so it does not establish the current account's allowance. Successful sign-in and model loading do not prove that hosted search or writing will succeed. The app does not silently retry with another account/model/provider. Offline work and separately selected public research remain available.

## Research without sending your paper

Research actions are separate from writing assistance. Enable **Online**, review the short query and select Crossref, PubMed or Wikipedia. Only that visible query goes to those APIs. Source metadata and available abstracts remain in the project. The optional **Ask ChatGPT** research brief sends the question and selected excerpts, never the full draft. Its optional web-search tool depends on model/account policy.

When **Use web search** is selected, the request requires a search. If the service does not confirm completed search use, the app shows an error and saves no brief. Unsupported tools or account policies also produce an error. With search off, the brief uses the question and chosen excerpts; outside claims remain unverified. Always inspect the returned search status and links.

**Record retrieved** verifies bibliographic existence, not claim truth. **Abstract retrieved** means an abstract or encyclopedia introduction was available, not the full publication. Open the source and check its methodology, scope, limitations and the claim you intend to make. Uncheck **Use** to exclude a source from generation. Writing requests use up to four relevant excerpts of 900 characters each; the library retains other saved sources.

Complete's research plan lets you review short queries before searching and inspect the evidence before generation. Never infer that the AI has searched merely because it produced fluent text.

Citation dialogs support basic APA, MLA and Chicago author-date metadata. Correct authors, title, DOI, publication, volume, issue, pages, URL and notes; add manual references and grouped paragraph citations. Edited/manual records remain unverified until checked. No page locators are invented. Exhaustive source types, capitalization and Chicago footnotes need manual finishing.

## Contextual information links and About

The 0.1.4 work adds an optional reading presentation that highlights selected research-worthy concepts. This view must leave editing, original text and exports unchanged. Opening a page requires a tap; source existence and relevance still need checking. Information links are for exploration, not automatic academic citations or bibliography entries. Link behavior and offline/error cases remain subject to current verification.

About is reachable from Settings. The permanent dedication is: **Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.** About shows the current application version and description; executed browser and device checks verify the wording and readable layout.

## Saving and exporting

Changes stay in app-private storage. Autosave is optional; manual Save writes immediately. Backgrounding flushes pending edits when autosave is enabled. Atomic writes and a previous valid save support recovery. Export a `.d2d` backup regularly: it contains original/working drafts, requirements, research, Writer DNA, proposals and revision history, without account tokens.

The workspace is limited to 4 MB and each imported/exported document to 10 MB. Back up completed work before removing it. Uninstalling or clearing app data removes the private workspace.

Export DOCX/PDF/TXT/MD or a project backup. DOCX and PDF render headings, bold and italic Markdown; font, 10–16 point size and line spacing are configurable. TXT/MD retain text. Pending proposals are not exported as accepted writing. Library references reuse an existing reference heading and avoid identical entries. Turning off library bibliography preserves references already written in the draft.

DOCX/PDF imports extract text without exact page layout, images or tables. Scanned PDFs need OCR elsewhere. PDF export opens Android Print, where Save as PDF can be selected.

## Optional server connection

Advanced server settings are for an environment that already has a reachable Ollama, LM Studio or llama.cpp endpoint. They are not a requirement for the deployed phone, and the user's firewalled PC is not a usable endpoint. `127.0.0.1` refers to the phone. Developer USB model tests do not establish remote availability. No model is downloaded or server changed by the app.

## App updates

Automatic checks are on by default, run at most once per day on app open, and target stable releases at `youriptvus/Draft2Done`. No draft or account token is sent to GitHub. An update must match its hash, package, increasing version code and signing certificate. Android asks for installation approval. Target 0.1.4 uses code 3 to upgrade published 0.1.2/code 2; it is not published automatically. A phone already running the latest or a newer local build should report up to date.

This sign-in repair keeps version 0.1.4/code 3 at the owner's request. If your phone already has an earlier 0.1.4/code 3 build, install the repaired signed APK explicitly: the automatic updater requires a higher version code and will not replace it with another build carrying the same code.
