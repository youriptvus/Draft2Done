import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  createProject,
  countWords,
  bodyText,
  progress,
  continuationPoint,
  analyzeDNA,
  analyzeDraft,
  citation,
  citations,
  validateProject,
  loadProject,
  snapshot,
  acceptProposal,
  rejectProposal,
  revertRevision,
  updateWorkingText,
} from "../web/core.js";

const proposal = (
  text = "A new explanation connects the evidence to the claim.",
  id = "proposal-1",
) => ({
  id,
  text,
  status: "pending",
  sourceIds: ["source-1"],
  warnings: [],
  createdAt: new Date().toISOString(),
});
const source = {
  id: "source-1",
  title: "An example study",
  authors: ["Jane Doe", "John Roe"],
  year: 2024,
  doi: "10.1000/example",
  url: "https://example.org/study",
  verification: "metadata",
  included: true,
};

test("reviewed inline continuation preserves the unfinished draft and reference section", () => {
  const p = createProject(
    "Tail",
    "My own argument is\n\nReferences\nAn existing reference.",
  );
  const point = continuationPoint(p);
  assert.equal(point.unfinished, true);
  p.proposals.push({
    ...proposal("that access deserves attention."),
    insertion: {
      mode: "inline",
      position: point.position,
      anchor: point.anchor,
    },
  });
  const restored = loadProject(JSON.stringify(p));
  assert.deepEqual(restored.proposals[0].insertion, p.proposals[0].insertion);
  const accepted = acceptProposal(restored, "proposal-1");
  assert.equal(accepted.originalText, p.originalText);
  assert.equal(
    accepted.workingText,
    "My own argument is that access deserves attention.\n\nReferences\nAn existing reference.",
  );
  assert.equal(
    accepted.workingText.slice(
      accepted.contributions[0].start,
      accepted.contributions[0].end,
    ),
    "that access deserves attention.",
  );
});

test("new projects exclude explicit title blocks by default and normalize export preferences", () => {
  const p = createProject(
    "Marked",
    "Paper title\nAuthor name\n---\nMy actual argument has five words.",
  );
  assert.equal(p.countingRules.excludeTitle, true);
  assert.equal(progress(p).current, 6);
  assert.equal(
    progress(createProject("Unmarked", "Every unmarked word still counts."))
      .current,
    5,
  );
  p.exportFormatting = { fontFamily: "Arial", fontSize: 14, lineSpacing: 2 };
  assert.deepEqual(
    loadProject(JSON.stringify(p)).exportFormatting,
    p.exportFormatting,
  );
  p.exportFormatting = {
    fontFamily: "untrusted",
    fontSize: 99,
    lineSpacing: 99,
  };
  assert.deepEqual(loadProject(JSON.stringify(p)).exportFormatting, {
    fontFamily: "Times New Roman",
    fontSize: 12,
    lineSpacing: 1.5,
  });
});

test("changed continuation anchors block acceptance and manual citation links survive saves", () => {
  const p = createProject("Tail", "My own argument is");
  const point = continuationPoint(p);
  p.proposals.push({
    ...proposal("that access matters."),
    insertion: {
      mode: "inline",
      position: point.position,
      anchor: point.anchor,
    },
  });
  p.workingText = "A changed argument is";
  assert.throws(() => acceptProposal(p, "proposal-1"), /draft changed/);
  p.citedSourceIds = ["source-1", "source-2"];
  assert.deepEqual(
    loadProject(JSON.stringify(p)).citedSourceIds,
    p.citedSourceIds,
  );
  assert.equal(
    continuationPoint(createProject("Complete", "A finished thought."))
      .unfinished,
    false,
  );
});

test("Unicode, apostrophes, hyphens and punctuation produce stable body word counts", () => {
  assert.equal(
    countWords("Don’t re-write naïve ideas—use café examples. 中文写作 тест"),
    9,
  );
  assert.equal(
    countWords("State-of-the-art work isn’t a one-size-fits-all answer."),
    6,
  );
  assert.equal(countWords("  \n\t "), 0);
  assert.equal(countWords("a—b; c/d"), 4);
});

test("only standalone reference headings exclude the trailing reference section", () => {
  for (const heading of [
    "References",
    "REFERENCES:",
    "## Works Cited",
    "Bibliography",
    "References\r",
  ]) {
    assert.equal(
      countWords(
        `Here are four words.\n\n${heading}\n\nDoe, Jane. Study title. 2024.`,
      ),
      4,
    );
  }
  assert.equal(
    countWords("The references explain the claim.\n\nAnother body sentence."),
    8,
  );
  assert.equal(
    countWords("Body words.\n# References\nOne more reference.", {
      excludeReferences: false,
    }),
    6,
  );
});

test("title exclusion requires an explicit marker, with independently configurable references", () => {
  const text =
    "Paper Title\nStudent Name\n---\nBody has four words.\n\nReferences\nOne source";
  assert.equal(countWords(text), 8);
  assert.equal(countWords(text, { excludeTitle: true }), 4);
  assert.equal(
    countWords(text, { excludeTitle: true, excludeReferences: false }),
    7,
  );
  assert.equal(countWords("My Title\nBody words.", { excludeTitle: true }), 4);
  assert.equal(
    bodyText("Name\n<!-- title-end -->\nBody text", {
      excludeTitle: true,
    }).trim(),
    "Body text",
  );
});

test("pending proposals are counted separately and cannot fulfill the accepted target", () => {
  const project = createProject("Paper", "One two three.");
  project.minimum = 10;
  project.proposals.push(proposal("Four five six seven."));
  assert.deepEqual(progress(project), {
    original: 3,
    current: 3,
    proposed: 4,
    minimum: 10,
    maximum: 0,
    remaining: 7,
    percent: 30,
  });
  const accepted = acceptProposal(project, "proposal-1");
  assert.equal(progress(accepted).remaining, 3);
  assert.equal(progress(accepted).proposed, 0);
  assert.equal(progress(project).current, 3);
});

test("acceptance preserves original bytes, records provenance, and precedes references", () => {
  const text = "Original\twording.  \n\n## References\nSaved reference.";
  const project = createProject("Paper", text);
  project.proposals.push(proposal("A supported continuation."));
  const accepted = acceptProposal(project, "proposal-1");
  assert.equal(accepted.originalText, text);
  assert.equal(project.workingText, text);
  assert.ok(
    accepted.workingText.indexOf("A supported continuation.") <
      accepted.workingText.indexOf("## References"),
  );
  assert.equal(countWords(accepted.workingText), 5);
  const span = accepted.contributions[0];
  assert.equal(
    accepted.workingText.slice(span.start, span.end),
    "A supported continuation.",
  );
  assert.deepEqual(span.sourceIds, ["source-1"]);
  assert.equal(accepted.revisions[0].workingText, text);
  assert.equal(accepted.proposals[0].status, "accepted");
  assert.throws(() => acceptProposal(accepted, "proposal-1"), /pending/);
});

test("rejecting a proposal never changes either draft and records the decision", () => {
  const project = createProject("Paper", "My own paragraph.");
  project.proposals.push(proposal());
  const rejected = rejectProposal(project, "proposal-1");
  assert.equal(rejected.workingText, project.workingText);
  assert.equal(rejected.originalText, project.originalText);
  assert.equal(rejected.proposals[0].status, "rejected");
  assert.equal(rejected.revisions.length, 1);
  assert.equal(progress(rejected).proposed, 0);
  assert.equal(project.proposals[0].status, "pending");
  assert.throws(() => rejectProposal(project, "missing"), /not be found/);
  assert.throws(
    () =>
      acceptProposal(
        { ...project, proposals: [proposal("   ")] },
        "proposal-1",
      ),
    /empty/,
  );
});

test("restoring a checkpoint restores text, statuses and provenance, retaining an audit trail", () => {
  let project = createProject("Paper", "My draft.");
  project.proposals.push(proposal());
  project = snapshot(project, "Before review");
  const checkpointId = project.revisions[0].id;
  project = acceptProposal(project, "proposal-1");
  const restored = revertRevision(project, checkpointId);
  assert.equal(restored.workingText, "My draft.");
  assert.equal(restored.originalText, "My draft.");
  assert.equal(restored.proposals[0].status, "pending");
  assert.deepEqual(restored.contributions, []);
  assert.equal(restored.revisions.length, 3);
  assert.ok(
    restored.revisions.at(-1).workingText.includes("A new explanation"),
  );
  assert.throws(() => revertRevision(project, "missing"), /not be found/);
});

test("direct edits shift and split provenance without claiming user changes as generated text", () => {
  let project = createProject("Paper", "Mine.");
  project.proposals.push(proposal("Generated evidence."));
  project = acceptProposal(project, "proposal-1");
  const prefixed = updateWorkingText(
    project,
    `New opening. ${project.workingText}`,
  );
  assert.equal(prefixed.originalText, "Mine.");
  assert.equal(
    prefixed.contributions[0].start,
    project.contributions[0].start + 13,
  );
  const edited = updateWorkingText(
    project,
    project.workingText.replace(
      "Generated evidence.",
      "Generated user evidence.",
    ),
  );
  assert.equal(edited.contributions.length, 2);
  assert.equal(
    edited.contributions
      .map((c) => edited.workingText.slice(c.start, c.end))
      .join(""),
    "Generated evidence.",
  );
  assert.ok(
    edited.contributions.every(
      (c) => !edited.workingText.slice(c.start, c.end).includes("user"),
    ),
  );
  assert.equal(edited.originalText, project.originalText);
});

test("serialization normalizes legacy drafts and rejects invalid or future project data", () => {
  const legacy = loadProject(
    JSON.stringify({ title: "Older paper", draft: "Existing draft" }),
  );
  assert.equal(legacy.originalText, "Existing draft");
  assert.equal(legacy.workingText, "Existing draft");
  assert.equal(legacy.schemaVersion, 1);
  assert.equal(legacy.minimum, 1000);
  assert.deepEqual(legacy.countingRules, {
    excludeReferences: true,
    excludeTitle: true,
  });
  const project = createProject("Round trip", "Original");
  project.sources.push(source);
  const restored = loadProject(JSON.stringify(project));
  assert.equal(restored.originalText, "Original");
  assert.equal(restored.sources[0].verification, "metadata");
  assert.deepEqual(restored.sources[0].authors, ["Jane Doe", "John Roe"]);
  for (const invalid of [
    null,
    [],
    { schemaVersion: 2 },
    { sources: {} },
    { proposals: [null] },
    { minimum: -1 },
    { minimum: 200, maximum: 100 },
    { originalText: 123 },
  ])
    assert.throws(() => validateProject(invalid), TypeError);
  assert.throws(() => loadProject("{oops"), /valid project JSON/);
  assert.throws(
    () => validateProject({ ...project, proposals: [proposal(), proposal()] }),
    /unique/,
  );
  assert.throws(
    () =>
      validateProject({ ...project, contributions: [{ start: 3, end: 1000 }] }),
    /outside/,
  );
});

test("untrusted fields and prototype properties do not become project properties", () => {
  const loaded = loadProject(
    '{"title":"Safe","text":"Mine","__proto__":{"polluted":true},"unexpected":"discard"}',
  );
  assert.equal(loaded.polluted, undefined);
  assert.equal(loaded.unexpected, undefined);
  assert.equal({}.polluted, undefined);
});

test("source identifiers and reusable style guidance survive serialization and proposal acceptance", () => {
  const project = createProject("Saved context", "My original draft.");
  project.sources.push({
    ...source,
    pmid: "12345",
    pageId: "678",
    journal: "Real journal",
    volume: "3",
    issue: "2",
    pages: "12–18",
  });
  project.profile = {
    ...analyzeDNA("A separate saved style sample. Another short sentence."),
    userNotes: "Keep my short paragraphs.",
  };
  project.proposals.push(proposal());
  const restored = acceptProposal(
    loadProject(JSON.stringify(project)),
    "proposal-1",
  );
  assert.equal(restored.profile.userNotes, "Keep my short paragraphs.");
  assert.equal(
    restored.profile.averageSentenceWords,
    project.profile.averageSentenceWords,
  );
  assert.equal(restored.sources[0].pmid, "12345");
  assert.equal(restored.sources[0].journal, "Real journal");
});

test("citations expose absent metadata, prefer DOI and vary author labels by style", () => {
  const apa = citation(source, "APA");
  assert.equal(apa.inText, "(Doe & Roe, 2024)");
  assert.match(apa.bibliography, /Doe, J\./);
  assert.match(apa.bibliography, /https:\/\/doi.org\/10.1000\/example/);
  assert.ok(!apa.bibliography.includes("example.org"));
  assert.deepEqual(apa.missing, []);
  assert.equal(citation(source, "MLA").inText, "(Doe and Roe)");
  assert.equal(citation(source, "Chicago").inText, "(Doe and Roe 2024)");
  assert.equal(
    citation({ ...source, authors: ["Jane Doe", "John Roe", "Alex Smith"] })
      .inText,
    "(Doe et al., 2024)",
  );
  assert.deepEqual(citation({ title: "Unknown author study" }).missing, [
    "authors",
    "year",
  ]);
  assert.equal(
    citation({ title: "Unknown author study" }).inText,
    "(“Unknown author study”, n.d.)",
  );
  assert.match(citation({}).bibliography, /Title missing/);
});

test("multiple references are deduplicated and preserve stable numeric citation ordering", () => {
  const second = {
    ...source,
    id: "source-2",
    authors: ["Alex Smith"],
    year: "2023",
    title: "Another study",
  };
  const combined = citations([source, second, source]);
  assert.equal(combined.inText, "(Doe & Roe, 2024; Smith, 2023)");
  assert.equal(combined.entries.length, 2);
  const numeric = citations([source, second], "IEEE");
  assert.equal(numeric.inText, "[1], [2]");
  assert.ok(numeric.bibliography.includes("[2] A. Smith"));
  assert.equal(citation([source, second]).inText, combined.inText);
});

test("empty analysis requests an existing draft and never fabricates a thesis or finding", () => {
  const analysis = analyzeDraft(createProject());
  assert.equal(analysis.thesis, "");
  assert.equal(analysis.suggestions.length, 1);
  assert.match(analysis.suggestions[0].detail, /already started/);
  assert.equal(analyzeDNA("").wordCount, 0);
  assert.equal(analyzeDNA("").confidence, "low");
});

test("draft analysis tracks missing headings, requirements and unfinished text", () => {
  const project = createProject(
    "Argument",
    "## Introduction\n\nStudents should compare evidence because claims need support.\n\nTODO add study",
  );
  project.requiredHeadings = "Introduction\nDiscussion";
  const analysis = analyzeDraft(project);
  assert.match(analysis.thesis, /should compare/);
  assert.ok(analysis.gaps.some((g) => g.includes("Discussion")));
  assert.ok(
    !analysis.gaps.some((g) =>
      g.includes("Required heading not found: Introduction"),
    ),
  );
  assert.ok(analysis.unfinished.some((u) => u.includes("TODO")));
  assert.ok(
    analysis.suggestions.every(
      (s) => s.id && s.title && s.detail && s.question,
    ),
  );
  assert.equal(analysis.method, "Offline rule-based analysis");
});

const samples = [
  "01-personal-reflection",
  "02-formal-argument",
  "03-plain-explanation",
  "04-evidence-cautious",
  "05-literary-analysis",
];
for (const name of samples)
  test(`style sample ${name} produces reproducible measurements and grounded prompts`, async () => {
    const text = await readFile(
      new URL(`../samples/${name}.md`, import.meta.url),
      "utf8",
    );
    const profile = analyzeDNA(text);
    const analysis = analyzeDraft(createProject(name, text));
    assert.ok(profile.wordCount > 100);
    assert.ok(profile.averageSentenceWords > 5);
    assert.ok(profile.paragraphCount >= 3);
    assert.ok(
      profile.vocabulary.lexicalDiversity > 0 &&
        profile.vocabulary.lexicalDiversity <= 100,
    );
    assert.deepEqual(profile, analyzeDNA(text));
    assert.ok(profile.limitations.some((l) => /no language model/i.test(l)));
    assert.ok(analysis.outline.length > 0);
    assert.ok(analysis.thesis.length > 10);
    assert.ok(analysis.suggestions.length >= 3);
    if (name === "01-personal-reflection")
      assert.ok(
        profile.person.first > 0 && profile.punctuation.contractions > 0,
      );
    if (name === "02-formal-argument")
      assert.ok(
        profile.punctuation.semicolons > 0 &&
          profile.transitions.some((t) => t.phrase === "however"),
      );
  });
