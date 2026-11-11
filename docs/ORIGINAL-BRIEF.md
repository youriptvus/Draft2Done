# NEW SOFTWARE PROJECT: Draft2Done
## Local AI Research, Academic Writing Assistance & Paper Completion Studio

**Application Name:** Draft2Done  
**Tagline:** Your Draft. Your Voice. Done.  
**Project Location:** `D:\Draft2Done`  
**Platform:** Windows Desktop  
**Initial Version:** 0.1.0

# YOUR ROLE

You are my senior software architect, AI engineer, Python developer, desktop application designer, research systems developer, and quality assurance engineer.

We are starting a completely new software project called **Draft2Done**.

Your objective is to design, develop, test, and package a real, functional Windows desktop application that helps students finish academic papers and assignments they have already started writing.

This must be a practical, fully functioning application, not a mockup, demonstration, or collection of incomplete scripts.

Begin by examining the development environment, establishing the architecture, and implementing the software.

Do not stop after writing a development plan.

---

# 1. PROJECT ISOLATION AND SAFETY

The entire project must be created and maintained inside:

`D:\Draft2Done`

This application is unrelated to any of my existing projects.

Strict rules:

- Do not modify existing projects.
- Do not modify GhostCoder AI Studio, YourIPTV, or any other existing software.
- Do not overwrite files outside the Draft2Done directory.
- Do not change existing system-wide Python installations.
- Use a project-specific Python virtual environment.
- Do not alter existing AI model files.
- Existing local AI engines may be accessed through their documented interfaces.
- Create a dedicated Git repository for this project.
- Maintain source-code checkpoints throughout development.
- Do not publish the project to GitHub without permission.
- Inspect the destination directory before writing anything.
- Preserve existing files if the directory already contains data.

All project assets, dependencies, documentation, tests, configurations, and generated builds should be organized within the project directory wherever practical.

# 2. APPLICATION PURPOSE

Draft2Done is designed for students who have started an assignment, essay, research paper, or term paper but need help finishing it.

The application must not begin by creating an entire paper from nothing.

Instead, it must examine a document that the student has already written.

From that document, Draft2Done must determine:

- What subject the student is writing about.
- What the assignment is attempting to accomplish.
- What arguments or ideas have already been developed.
- What additional information is needed.
- How the student writes.
- How the paper is structured.
- Whether the paper has a logical progression.
- What research is relevant.
- What sections need additional development.
- What is necessary to complete the assignment.

The software must use a combination of:

1. Local artificial intelligence.
2. Online research and search engines.
3. Encyclopedic knowledge.
4. Academic and scholarly sources.
5. Source verification.
6. Analysis of the student's existing writing.

Its two primary modes will be:

**HELPER MODE** — Researches the subject and helps the student finish the writing themselves.

**COMPLETE MODE** — Researches the subject and generates a continuation of the existing paper, using the original writer's style, flow, vocabulary, and natural writing patterns.

Both modes must be built into the application.

# 3. HELPER MODE

Helper Mode should act like a knowledgeable research assistant or professor who helps a student finish an assignment.

The software must first read and analyze the existing document.

It should identify the main topic, thesis, writing structure, current arguments, and remaining gaps.

Then it should perform relevant research and provide actionable suggestions.

## Helper Mode features

Provide:

- Ideas for the next paragraph.
- Suggested directions for the argument.
- Supporting evidence.
- Relevant research findings.
- Important facts and statistics.
- Missing perspectives.
- Suggestions for improving transitions.
- Questions the student should answer.
- Suggestions for introducing research.
- Recommendations for developing the conclusion.
- Guidance for meeting assignment requirements.
- Academic citations and reference suggestions.

For example:

A student writes 750 words of a 2,000-word research paper discussing anxiety disorders and their relationship to other mental health conditions.

Helper Mode should read the 750 words, identify the arguments already presented, and research relevant evidence.

It might suggest discussing additional research studies, differences between correlation and causation, conflicting findings, or implications of the evidence.

The student remains responsible for writing the additional material.

## Interactive assistance

Each suggestion should be selectable.

The student should be able to request:

- Explain this idea.
- Find more research.
- Show supporting evidence.
- Find an academic source.
- Explain how this connects to my paper.
- Suggest a transition.
- Show an example of the next paragraph's structure.

Helper Mode should never automatically replace the student's writing.

# 4. COMPLETE MODE

Complete Mode is one of the application's most important features.

This mode must carefully analyze the student's existing paper, research the subject, and generate a continuation that matches the original writer's style.

The purpose is not to create generic academic text.

The purpose is to produce a continuation that fits naturally with what has already been written.

All generated material must remain editable, reviewable, and distinguishable from original text.

## Step A — Examine the existing writing

Before generating any new content, analyze the entire existing document.

Examine:

- Subject and topic.
- Central argument or thesis.
- Sentence construction.
- Sentence length.
- Paragraph length.
- Vocabulary.
- Word selection.
- Preferred expressions.
- Formality.
- Grammar patterns.
- Spelling tendencies.
- Punctuation.
- Writing flow.
- Transitions.
- Argument development.
- Narrative voice.
- Existing citations.
- Overall writing proficiency.

Use this information to construct a writing-style profile.

Do not assume every student writes like a professional academic.

Some students use simple sentence structures, informal transitions, repeated phrases, or unusual grammar.

The writing-style engine should preserve natural characteristics of the student's voice instead of automatically converting everything into polished academic prose.

Retain the student's original spelling and grammar unless they request corrections.

When generating new text, reproduce their natural language patterns without intentionally introducing fabricated errors.

## Step B — Minimum word count

When the user selects Complete Mode, ask:

**What is the minimum required word count for this assignment?**

Display:

- Current word count.
- Required minimum word count.
- Additional words needed.
- Optional maximum word count.

For example:

Original draft: 825 words  
Minimum required: 2,000 words  
Additional words needed: 1,175

The system must use the requested minimum as its completion target.

If no maximum exists, generate enough substantive material to reach the minimum without padding the assignment with repetitive information.

Word counting must be accurate.

By default, bibliography entries, reference pages, and title-page information should not count toward the essay body minimum.

Allow these counting rules to be configured.

## Step C — Assignment requirements

Allow the student to optionally enter or import:

- Assignment instructions.
- Grading rubric.
- Citation requirements.
- Academic level.
- Required sources.
- Specific instructor directions.
- Required headings.
- Maximum word count.

When requirements are supplied, the system must consider them during research and generation.

## Step D — Research

Complete Mode must determine what information is necessary to finish the paper.

It must then research that information.

Research should prioritize relevant, reliable sources and avoid unsupported claims.

The application should not invent:

- Academic studies.
- Authors.
- Statistics.
- Research findings.
- Publications.
- Page numbers.
- Citations.
- URLs.

Every important research claim should be traceable to a real source.

If a source cannot be verified, mark it as unverified rather than presenting it as established evidence.

## Step E — Complete the unfinished paper

Generate a continuation using:

1. The original paper.
2. The student's writing-style profile.
3. The assignment requirements.
4. The requested word count.
5. Relevant verified research.
6. The paper's existing thesis.
7. The existing argument and paragraph flow.

The continuation must:

- Follow logically from the existing writing.
- Maintain the original topic.
- Use compatible vocabulary.
- Preserve the established tone.
- Follow the student's writing patterns.
- Avoid unnecessary repetition.
- Incorporate relevant evidence.
- Include citations when required.
- Produce a logical conclusion.
- Reach the requested minimum word count when sufficient relevant content can be supported.

Do not simply append unrelated paragraphs to meet the word count.

If the draft contains an unfinished sentence or paragraph, identify an appropriate continuation point and allow the user to review it.

Do not silently rewrite existing paragraphs.

## Step F — Review and approval

Once generation finishes, display the original paper together with the suggested continuation.

The interface should visually distinguish:

**Original writing:** The student's existing document.

**Generated continuation:** Newly proposed content.

Allow the student to:

- Review the continuation.
- Edit generated paragraphs.
- Accept or reject sections.
- Regenerate individual sections.
- Request more research.
- Adjust the writing style.
- Change the target word count.
- Improve transitions.
- Check citations.
- Preview the final document.

Never overwrite the original document automatically.

The user must approve changes before they are merged into the working document.

Retain revision history and identify generated contributions so students can comply with their institution's academic integrity rules.

# 5. WRITER DNA — WRITING STYLE PROFILING

Create a dedicated feature called:

**Writer DNA**

This will be Draft2Done's writing-style analysis engine.

Its purpose is to identify and reproduce the characteristics of the existing writer.

Analyze measurable patterns, including:

- Average sentence length.
- Average paragraph length.
- Common vocabulary.
- Vocabulary complexity.
- Frequently used expressions.
- Sentence structure.
- Common transitions.
- Grammar tendencies.
- Spelling tendencies.
- Punctuation habits.
- Formality.
- First-person and third-person usage.
- Argument development.
- Typical paragraph organization.

Construct a structured writing profile from this information.

Allow users to inspect the profile.

## Writing preferences

Provide two options:

**Original Voice**

Maintain the student's existing writing style as closely as practical, including their natural phrasing, vocabulary level, and grammatical tendencies.

**Polished Voice**

Maintain the student's recognizable voice while improving clarity, spelling, grammar, and overall readability.

Default to Original Voice.

## Persistent profiles

Allow the user to optionally save a writing profile for future assignments.

Saved profiles should be reusable and editable.

Allow multiple profiles for different writing contexts.

For example:

- General school assignments.
- Research papers.
- Personal essays.
- Formal academic writing.

Do not require a persistent profile.

The existing document must be sufficient to start analysis when it contains enough writing.

If the document is too short to establish a reliable style, clearly report that limitation.

# 6. RESEARCH ENGINE

Create a comprehensive research system that works alongside the local AI engine.

Investigate integration with appropriate sources, including:

- Wikipedia.
- Encyclopedic reference material.
- Crossref.
- OpenAlex.
- Semantic Scholar.
- PubMed.
- Government research databases.
- University publications.
- Public academic repositories.
- Scholarly journal abstracts.
- General web search providers.

Determine which sources provide legitimate free APIs and which require optional accounts or API keys.

Use official APIs wherever possible.

Do not rely on unauthorized scraping or bypass access restrictions.

## Research workflow

The system must:

1. Analyze the existing paper.
2. Identify missing information.
3. Generate research questions.
4. Search relevant sources.
5. Retrieve credible results.
6. Extract useful information.
7. Evaluate relevance and credibility.
8. Detect conflicting evidence.
9. Connect findings to the paper.
10. Provide references for claims.
11. Save findings in a research library.

The software should favor original academic research and authoritative sources over encyclopedic summaries where appropriate.

Encyclopedias may be used to establish background knowledge.

## Research panel

Create an interface where users can see:

- Source titles.
- Authors.
- Publication dates.
- Source types.
- Relevant findings.
- Source links.
- Citation details.
- Verification status.

Allow users to open the original source in their browser.

Make it possible to exclude a source from generation.

Research findings must remain inspectable after the document is saved and reopened.

# 7. CITATION MANAGER

Build an integrated citation system supporting:

- APA.
- MLA.
- Chicago.

Allow further citation styles to be added later.

The citation manager must:

- Store accurate bibliographic metadata.
- Associate research claims with references.
- Produce in-text citations.
- Generate bibliography entries.
- Detect missing citation information.
- Avoid fictional references.
- Allow manual corrections.
- Handle multiple references for a claim.

Never invent missing page numbers.

Do not imply that a full publication has been examined when only an abstract or summary was accessible.

Keep citation data with the saved project.

# 8. LOCAL ARTIFICIAL INTELLIGENCE

Draft2Done must prioritize local AI processing.

I want to avoid mandatory monthly AI subscriptions or paid API usage.

Investigate support for:

- Ollama.
- llama.cpp.
- LM Studio.
- Existing local GGUF models.
- Compatible local OpenAI-style API servers.

Use a modular AI backend design.

The software should not be locked to one model or provider.

## Local AI requirements

Provide:

- AI backend selection.
- Available model discovery.
- Model selection.
- Connection testing.
- Model status.
- Generation progress.
- Cancellation support.
- Error handling.
- Configurable context limits.
- Configurable generation settings.
- CPU fallback when practical.

Prefer efficient quantized models.

The development computer may have approximately:

- 8 GB GPU VRAM.
- 8 GB system RAM.
- Windows 10 Pro.

Detect the actual hardware rather than assuming the configuration is unchanged.

Select practical models based on available resources.

Do not automatically download large AI models without permission.

Do not duplicate models already installed elsewhere when they can be safely reused.

Existing AI services may be queried but not modified.

# 9. OFFLINE AND ONLINE OPERATION

Draft2Done must remain functional without internet access.

## Offline Mode

Support:

- Document importing.
- Document editing.
- Local saving.
- Local AI analysis.
- Writer DNA.
- Word counting.
- Helper Mode using available local knowledge.
- Complete Mode using available local knowledge.
- Previously saved research.
- Document exporting.

Clearly identify factual claims that have not been externally verified.

## Online Research Mode

Enable:

- Web searches.
- Scholarly research.
- Encyclopedic searches.
- Research verification.
- Citation retrieval.
- Source discovery.

Online activity must be optional and transparent.

Do not automatically upload the user's full paper to online AI services.

Use minimal research queries where possible.

No mandatory remote AI service may be introduced.

# 10. WINDOWS DESKTOP INTERFACE

Build a professional Windows desktop application.

Evaluate PySide6 / Qt as the preferred GUI technology.

Use a modern, responsive design with dark and light themes.

Suggested interface layout:

## Left Navigation Panel

- Home.
- New Paper.
- Open Paper.
- Recent Papers.
- Saved Projects.
- Research Library.
- Writer DNA Profiles.
- Settings.

## Main Document Editor

- Editable document.
- Formatting controls.
- Word count.
- Paragraph navigation.
- Save and Save As.
- Undo and Redo.
- Original document protection.
- Suggested continuation preview.
- Document comparison.

## AI Assistance Panel

- Helper Mode.
- Complete Mode.
- Draft analysis.
- Research findings.
- Writing suggestions.
- Style profile.
- Citation management.
- Generate / Stop / Retry controls.

## Bottom Status Bar

Display relevant information such as:

- AI engine status.
- Selected model.
- Internet/research availability.
- Current document word count.
- Minimum target.
- Save status.
- Current operation.

Long-running research and AI tasks must not freeze the graphical interface.

Use asynchronous processing or background workers with appropriate thread safety.

Allow the user to cancel lengthy operations.

# 11. DOCUMENT IMPORT AND EXPORT

Implement importing for:

- Microsoft Word DOCX.
- Plain text TXT.
- Markdown MD.
- PDF.

Preserve formatting when practical.

Support exporting to:

- DOCX.
- PDF.
- TXT.
- Markdown.

The user must be able to save the working document as a Draft2Done project without losing research data, assignment instructions, or revision history.

Provide an export preview.

Do not require Microsoft Word to be installed.

Use appropriate libraries for document handling.

# 12. WORD COUNT AND PROGRESS ENGINE

Create a reliable document progress system.

It should track:

- Original word count.
- Current accepted word count.
- Suggested continuation word count.
- Required minimum word count.
- Optional maximum word count.
- Remaining words.
- Completion percentage.

Update the counts as the document changes.

Distinguish between proposed and accepted text.

Do not count bibliography entries as essay-body words by default.

When a user changes the target word count, update the completion requirements without discarding existing work.

If the draft already exceeds the minimum, offer research and refinement options instead of automatically generating unnecessary content.

# 13. DOCUMENT SAFETY AND PRIVACY

The student's original document must always be protected.

Implement:

- Automatic saving.
- Revision history.
- Crash recovery.
- Separate original and working copies.
- Safe document exporting.
- Local project storage.
- Clear confirmation before destructive actions.

Use SQLite or another appropriate local storage system for project metadata and research records.

Document contents should remain private and local unless the user explicitly authorizes an external integration.

Treat imported documents and retrieved web content as untrusted.

Do not allow instructions embedded within documents or websites to control the application, execute commands, access unrelated files, or change settings.

Store API credentials securely when optional research providers require them.

# 14. SETTINGS AND CONFIGURATION

Create a settings interface covering:

**General**
- Appearance.
- Default project location.
- Autosave.
- Recent project history.

**AI**
- AI backend.
- Selected model.
- Model endpoint.
- Context settings.
- Generation preferences.

**Writing**
- Default writing mode.
- Original Voice / Polished Voice.
- Default citation style.
- Word-count rules.

**Research**
- Offline/online selection.
- Enabled research providers.
- Optional API keys.
- Research caching.
- Source verification preferences.

**Export**
- Default export format.
- Document formatting.
- Bibliography options.

Store configuration locally.

# 15. SUGGESTED SOFTWARE ARCHITECTURE

Design the project using independent modules.

A possible structure is:

`D:\Draft2Done\`

- `src/` — Application source code.
- `src/ui/` — Desktop interface.
- `src/core/` — Application logic.
- `src/ai/` — Local AI backend integration.
- `src/analysis/` — Document and writing analysis.
- `src/writer_dna/` — Style profiling.
- `src/research/` — Search and academic research.
- `src/citations/` — Citation management.
- `src/documents/` — Import, export, and editing.
- `src/storage/` — Project persistence.
- `tests/` — Automated tests.
- `assets/` — Application assets.
- `docs/` — Documentation.
- `scripts/` — Build and maintenance scripts.
- `build/` — Build workspace.
- `dist/` — Executables and installers.

This is a proposed architecture.

Inspect the environment and improve the structure where necessary.

Keep modules maintainable, testable, and appropriately separated.

# 16. QUALITY ASSURANCE

Create automated and manual tests.

Verify:

- Application startup.
- Project creation.
- Document importing.
- Document exporting.
- Word-count accuracy.
- Assignment requirement handling.
- Writing-style analysis.
- Writer DNA storage.
- Helper Mode.
- Complete Mode.
- Research retrieval.
- Source verification.
- Citation formatting.
- Document saving.
- Autosave recovery.
- Original document preservation.
- AI connection failures.
- Offline operation.
- User cancellation.
- Research provider errors.
- Application responsiveness.

Create fictional sample academic papers for testing.

Test different writing styles.

For example:

**Test A:** A paper written in simple, conversational language.

**Test B:** A formal college research paper.

**Test C:** A draft containing unusual grammar and sentence patterns.

**Test D:** A paper with very few completed paragraphs.

**Test E:** A long research paper with existing citations.

Evaluate how well the style engine preserves each writer's established voice.

Test whether generated material logically extends the draft.

Test whether sources are real, relevant, and correctly cited.

Never report that a feature has been successfully tested unless the test actually ran.

# 17. WINDOWS EXECUTABLE AND INSTALLER

The final application must run independently of Visual Studio Code.

Build a usable Windows executable.

Investigate PyInstaller or another appropriate packaging solution.

If practical, also create a Windows installer.

The installer should:

- Install Draft2Done.
- Create an application shortcut.
- Include required application files.
- Preserve existing user projects during updates.
- Support clean uninstallation.

External local AI engines and model files may be configured separately when bundling them is impractical.

Provide clear instructions for model setup.

Do not claim that the installer includes an AI model unless it actually does.

# 18. DEVELOPMENT PHASES

Follow this order.

## Phase 1 — Foundation

- Inspect the computer environment.
- Confirm available Python versions.
- Identify existing local AI backends.
- Inspect hardware resources.
- Create the isolated project.
- Create the Git repository.
- Establish the architecture.
- Implement the initial GUI.

## Phase 2 — Document Management

- Implement document import.
- Implement document editing.
- Implement word counting.
- Implement local saving.
- Implement document exporting.

## Phase 3 — Local AI

- Connect to a supported local AI backend.
- Discover available models.
- Test actual inference.
- Implement AI task management.
- Implement error handling and cancellation.

## Phase 4 — Writer DNA

- Build style analysis.
- Extract measurable writing characteristics.
- Generate writing profiles.
- Implement Original Voice and Polished Voice.
- Test style consistency.

## Phase 5 — Helper Mode

- Analyze drafts.
- Identify missing material.
- Produce useful writing suggestions.
- Implement interactive explanations.
- Integrate available research findings.

## Phase 6 — Research Engine

- Implement research providers.
- Implement source discovery.
- Implement evidence extraction.
- Implement research storage.
- Implement citation management.
- Implement source verification.

## Phase 7 — Complete Mode

- Request the minimum word count.
- Calculate additional material needed.
- Analyze document structure.
- Create a research plan.
- Generate a style-matched continuation.
- Add supported citations.
- Provide revision and approval controls.
- Build final document preview.

## Phase 8 — Integration Testing

- Test all major features together.
- Test real local AI inference.
- Test online and offline operation.
- Test saved project recovery.
- Test exports.
- Fix discovered failures.

## Phase 9 — Packaging

- Build the Windows executable.
- Create installer when practical.
- Prepare user documentation.
- Finalize version information.
- Perform launch and installation tests.

# 19. NON-NEGOTIABLE RULES

1. Do not modify unrelated projects.
2. Use `D:\Draft2Done` as the project root.
3. Build actual functioning software.
4. Do not require paid AI subscriptions.
5. Use local AI by default.
6. Make online research optional.
7. Never invent academic citations.
8. Preserve the student's original writing.
9. Keep generated additions reviewable and editable.
10. Do not replace the student's voice with generic academic language.
11. Do not generate an entire paper from an empty document.
12. Support both Helper Mode and Complete Mode.
13. Implement accurate word counting.
14. Store projects locally.
15. Maintain regular Git checkpoints.
16. Test features before declaring them complete.
17. Keep application features usable during staged development.
18. Do not publish externally without permission.
19. Never hide generation failures or unsupported research claims.
20. Ensure that the finished program can run without VS Code.

# 20. DEFINITION OF SUCCESS

Draft2Done is successful when a user can:

1. Open the Windows application.
2. Import an unfinished school paper.
3. Read and edit that paper.
4. Choose Helper Mode or Complete Mode.
5. Have the software analyze the existing writing.
6. Research the subject using available sources.
7. Receive relevant suggestions in Helper Mode.
8. Enter a minimum word count in Complete Mode.
9. Generate a continuation consistent with their original writing style.
10. Review, modify, and approve the generated additions.
11. Inspect supporting references.
12. Save the project.
13. Export the finished document.

All of this should be possible through a user-friendly graphical interface.

# 21. START IMPLEMENTATION

Begin immediately.

First, inspect the available development environment and determine which existing local AI systems can be safely reused.

Then create the Draft2Done project and begin implementing the application.

Make reasonable technical decisions without repeatedly asking for permission for ordinary development choices.

Ask for authorization before destructive operations, modifications outside the project directory, large model downloads, paid services, or publication.

At each major development checkpoint, report:

- What has been implemented.
- What is working.
- What has actually been tested.
- What remains incomplete.
- Any significant errors.
- The next implementation steps.

Maintain an accurate task list and continue development through the planned phases.

Do not claim that placeholder functionality is complete.

Do not stop at architecture diagrams or example code.

**Build Draft2Done into a working Windows desktop application with local AI, genuine research capabilities, intelligent writing-style analysis, Helper Mode, and Complete Mode.**

**PROJECT NAME: Draft2Done**

**TAGLINE: Your Draft. Your Voice. Done.**

**PROJECT ROOT: `D:\Draft2Done`**

Proceed with the project setup and implementation.