import test from "node:test";
import assert from "node:assert/strict";
import { analyzeDNA, createProject, loadProject } from "../web/core.js";

test("applied saved voice retains descriptive DNA after a different paper reopens", () => {
  const saved = analyzeDNA(
    "In my experience, urban gardens need care. In my experience, color changes behaviour.\n\nHowever, urban gardens don't wait. Urban gardens grow.",
  );
  const project = createProject(
    "Different assignment",
    "A new draft has entirely different words and organization.",
  );
  project.profile = saved;
  const reopened = loadProject(JSON.stringify(project));
  for (const key of [
    "repeatedExpressions",
    "formality",
    "spellingVariants",
    "paragraphOrganization",
  ])
    assert.deepEqual(reopened.profile[key], saved[key]);
  assert.notDeepEqual(
    reopened.profile.paragraphOrganization,
    analyzeDNA(project.originalText).paragraphOrganization,
  );
});

test("imported descriptive DNA validates bounded lists, fields and numeric rates", () => {
  const project = createProject("Profile validation", "A draft remains local.");
  project.profile = analyzeDNA(
    "I'm comparing color and colour. In my experience, a garden matters.",
  );
  project.profile.formality.unrecognized = { arbitrary: { nested: "ignored" } };
  assert.equal(
    "unrecognized" in loadProject(JSON.stringify(project)).profile.formality,
    false,
  );
  project.profile.formality.contractions.forms = Array.from(
    { length: 13 },
    () => ({ form: "i'm", count: 1 }),
  );
  assert.throws(
    () => loadProject(JSON.stringify(project)),
    /contraction forms/,
  );
  project.profile = analyzeDNA("I'm considering a garden.");
  project.profile.formality.contractions.per100Words = "unknown";
  assert.throws(() => loadProject(JSON.stringify(project)), /contraction rate/);
  project.profile = analyzeDNA("A garden matters.");
  project.profile.paragraphOrganization.paragraphs[0].opening = "x".repeat(181);
  assert.throws(
    () => loadProject(JSON.stringify(project)),
    /180-character limit/,
  );
});

test("DNA shows recurring expressions without sentence-boundary or boilerplate artifacts", () => {
  const profile =
    analyzeDNA(`In my experience, urban gardens help neighbors. In my experience, they need patient care.

Urban gardens offer shade. Urban gardens invite birds. This paper examines soil. This paper compares seeds. This paper reviews water.

Creative thinking matters. Creative. Thinking changes. Creative. Thinking grows.`);
  assert.ok(
    profile.repeatedExpressions.some(
      ({ phrase, count }) => phrase === "in my experience" && count === 2,
    ),
  );
  assert.ok(
    profile.repeatedExpressions.some(
      ({ phrase, count }) => phrase === "urban gardens" && count === 3,
    ),
  );
  assert.ok(
    !profile.repeatedExpressions.some(({ phrase }) => phrase === "this paper"),
  );
  assert.ok(
    !profile.repeatedExpressions.some(
      ({ phrase }) => phrase === "creative thinking",
    ),
  );
  assert.ok(
    profile.repeatedExpressions.every(
      ({ count, words }) => count >= (words === 2 ? 3 : 2),
    ),
  );
});

test("DNA distinguishes observed contractions from possessives and normalizes apostrophes", () => {
  const profile = analyzeDNA(
    "I'm ready. I’m patient. We don't rush the researcher's notes. The student's notebook is here. We shouldn't've skipped it; it's useful.",
  );
  assert.equal(profile.formality.contractions.count, 5);
  assert.equal(profile.punctuation.contractions, 5);
  assert.deepEqual(
    profile.formality.contractions.forms.find(({ form }) => form === "i'm"),
    { form: "i'm", count: 2 },
  );
  assert.ok(
    !profile.formality.contractions.forms.some(
      ({ form }) => form === "researcher's" || form === "student's",
    ),
  );
  assert.ok(profile.formality.contractions.per100Words > 0);
  assert.match(profile.formality.note, /not an overall formality score/i);
});

test("DNA reports context-dependent register cues without rating the writer", () => {
  const profile = analyzeDNA(
    "However, the evidence suggests a connection. Evidence requires analysis. You know, it really takes a lot of care. Microanalysis does not contain a separate cue word.",
  );
  assert.deepEqual(
    profile.formality.academicCues.find(({ phrase }) => phrase === "evidence"),
    { phrase: "evidence", count: 2 },
  );
  assert.deepEqual(
    profile.formality.academicCues.find(({ phrase }) => phrase === "analysis"),
    { phrase: "analysis", count: 1 },
  );
  assert.deepEqual(
    profile.formality.conversationalCues.find(
      ({ phrase }) => phrase === "a lot",
    ),
    { phrase: "a lot", count: 1 },
  );
  assert.equal("score" in profile.formality, false);
  assert.equal("rating" in profile.formality, false);
});

test("DNA describes paragraph openings with evidence and keeps uncertain roles tentative", () => {
  const profile = analyzeDNA(
    "# Garden study\n\nGardens connect neighbors. Shared work takes time.\n\nFor example, a seed exchange can start a conversation.\n\nHowever, dry weather changes the plan.\n\nAccording to a study, watering schedules matter.\n\nOverall, the routine deserves attention.",
  );
  const organization = profile.paragraphOrganization;
  assert.equal(organization.totalParagraphs, 5);
  assert.equal(organization.paragraphs[0].possibleRole, "Opening paragraph");
  assert.equal(
    organization.paragraphs[1].possibleRole,
    "Example-led paragraph",
  );
  assert.match(organization.paragraphs[1].evidence, /For example/);
  assert.match(organization.paragraphs[2].possibleRole, /Contrast/);
  assert.match(organization.paragraphs[3].possibleRole, /^Possible evidence/);
  assert.match(organization.paragraphs[4].possibleRole, /^Possible summary/);
  assert.match(organization.summary, /4 of 5/);
  assert.match(organization.note, /tentative/);
  assert.ok(
    organization.paragraphs.every(
      ({ wordCount, sentenceCount, opening }) =>
        wordCount > 0 && sentenceCount > 0 && opening.length <= 180,
    ),
  );
});

test("DNA bounds paragraph previews while retaining full-sample counts", () => {
  const text = Array.from(
    { length: 20 },
    (_, index) => `Paragraph ${index + 1} presents an observation.`,
  ).join("\n\n");
  const profile = analyzeDNA(text);
  assert.equal(profile.paragraphOrganization.totalParagraphs, 20);
  assert.equal(profile.paragraphOrganization.paragraphs.length, 12);
  assert.equal(profile.paragraphOrganization.truncated, true);
  assert.equal(profile.wordCount, 100);
  assert.equal(profile.paragraphCount, 20);
});

test("DNA records only observed spelling forms and excludes reference material", () => {
  const profile = analyzeDNA(
    "The color changes behavior. Colour and color both appear here. The centre organized a meeting. Their analyses consider teh leaf.\n\nReferences\nThe theatre recognised an honour.",
  );
  const observations = profile.spellingVariants.observations;
  assert.deepEqual(
    observations.find(({ family }) => family === "color / colour").forms,
    [
      { form: "color", count: 2 },
      { form: "colour", count: 1 },
    ],
  );
  assert.deepEqual(
    observations.find(({ family }) => family === "center / centre").forms,
    [{ form: "centre", count: 1 }],
  );
  assert.ok(
    !observations.some(
      ({ family }) =>
        family === "theater / theatre" || family === "honor / honour",
    ),
  );
  assert.ok(
    !observations.some(({ forms }) =>
      forms.some(({ form }) => form === "teh" || form === "analyses"),
    ),
  );
  assert.match(
    profile.spellingVariants.note,
    /no spelling correction, preferred locale or proficiency is inferred/i,
  );
  assert.equal("errors" in profile.spellingVariants, false);
});

test("DNA details stay empty and finite for absent writing", () => {
  const profile = analyzeDNA("");
  assert.deepEqual(profile.repeatedExpressions, []);
  assert.deepEqual(profile.spellingVariants.observations, []);
  assert.equal(profile.formality.contractions.count, 0);
  assert.equal(profile.formality.contractions.per100Words, 0);
  assert.equal(profile.paragraphOrganization.totalParagraphs, 0);
  assert.deepEqual(profile.paragraphOrganization.paragraphs, []);
  assert.deepEqual(profile, analyzeDNA(""));
});
