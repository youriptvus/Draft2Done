# Using Draft2Done on Android

## Start on your phone

Install `current release/Draft2Done-0.1.2.apk` on Android 8 or later. It runs independently of VS Code and the user's PC. No PC model server, USB reverse, port forwarding or VPN is required for the primary AI route. The APK does not contain an on-phone language model.

1. Import a TXT, Markdown, DOCX or text PDF, or create a workspace and paste writing you already started. Imports preserve the original separately from the working copy.
2. Use **Set goal** for the word minimum, optional maximum, instructions and rubric. Reference sections and explicitly marked title blocks are excluded by default. End a title block with `---` or `<!-- title-end -->`; unmarked text is counted in full.
3. Edit **My draft**. Undo/Redo survives tab switches. The paragraph selector helps navigate longer drafts. The paper menu contains the original, checkpoints, history, project backup and export.
4. Open Settings and **Continue with ChatGPT** for direct phone-to-OpenAI assistance. Sign in in the system browser, approve plan usage and return to the app. The native model list contains only choices available to that account. No standard API key is required.
5. Helper offers observations and selectable advice. Complete generates separate editable proposals; accept or reject each section. Generated text is never silently merged into the protected original.

Editing, local saving, word counts, descriptive Writer DNA, previously saved research and document import/export work without ChatGPT. Free scholarly search requires internet but no ChatGPT subscription. AI drafting on the primary route requires internet and an eligible ChatGPT account with plan access and available account/app usage. A free website sign-in alone does not grant inference.

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

## Research without sending your paper

Research actions are separate from writing assistance. Enable **Online**, review the short query and select Crossref, PubMed or Wikipedia. Only that visible query goes to those APIs. Source metadata and available abstracts remain in the project. The optional **Ask ChatGPT** research brief sends the question and selected excerpts, never the full draft. Its optional web-search tool depends on model/account policy.

When **Use web search** is selected, the request requires a search. If the service does not confirm completed search use, the app shows an error and saves no brief. Unsupported tools or account policies also produce an error. With search off, the brief uses the question and chosen excerpts; outside claims remain unverified. Always inspect the returned search status and links.

**Record retrieved** verifies bibliographic existence, not claim truth. **Abstract retrieved** means an abstract or encyclopedia introduction was available, not the full publication. Open the source and check its methodology, scope, limitations and the claim you intend to make. Uncheck **Use** to exclude a source from generation. Writing requests use up to four relevant excerpts of 900 characters each; the library retains other saved sources.

Complete's research plan lets you review short queries before searching and inspect the evidence before generation. Never infer that the AI has searched merely because it produced fluent text.

Citation dialogs support basic APA, MLA and Chicago author-date metadata. Correct authors, title, DOI, publication, volume, issue, pages, URL and notes; add manual references and grouped paragraph citations. Edited/manual records remain unverified until checked. No page locators are invented. Exhaustive source types, capitalization and Chicago footnotes need manual finishing.

## Saving and exporting

Changes stay in app-private storage. Autosave is optional; manual Save writes immediately. Backgrounding flushes pending edits when autosave is enabled. Atomic writes and a previous valid save support recovery. Export a `.d2d` backup regularly: it contains original/working drafts, requirements, research, Writer DNA, proposals and revision history, without account tokens.

The workspace is limited to 4 MB and each imported/exported document to 10 MB. Back up completed work before removing it. Uninstalling or clearing app data removes the private workspace.

Export DOCX/PDF/TXT/MD or a project backup. DOCX and PDF render headings, bold and italic Markdown; font, 10–16 point size and line spacing are configurable. TXT/MD retain text. Pending proposals are not exported as accepted writing. Library references reuse an existing reference heading and avoid identical entries. Turning off library bibliography preserves references already written in the draft.

DOCX/PDF imports extract text without exact page layout, images or tables. Scanned PDFs need OCR elsewhere. PDF export opens Android Print, where Save as PDF can be selected.

## Optional server connection

Advanced server settings are for an environment that already has a reachable Ollama, LM Studio or llama.cpp endpoint. They are not a requirement for the deployed phone, and the user's firewalled PC is not a usable endpoint. `127.0.0.1` refers to the phone. Developer USB model tests do not establish remote availability. No model is downloaded or server changed by the app.

## App updates

Automatic checks are on by default, run at most once per day on app open, and target stable releases at `youriptvus/Draft2Done`. No draft or account token is sent to GitHub. A downloaded update must match its hash, package, increasing version code and signing certificate. Android asks for installation approval. Version 0.1.2 uses version code 2 and can update 0.1.0/code 1 after the user publishes its APK and manifest. A phone already running the latest or a newer local build should report that it is up to date.
