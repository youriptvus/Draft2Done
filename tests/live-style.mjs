import assert from "node:assert/strict";
import { mkdir, readFile, readdir, writeFile } from "node:fs/promises";
import { AIClient } from "../web/services.js";
import {
  analyzeDNA,
  analyzeDraft,
  countWords,
  createProject,
} from "../web/core.js";

// Explicitly invoked integration check. It never downloads a model, changes a
// paper, accepts a proposal, or contacts a public AI endpoint.
const settings = {
  backend: "ollama",
  endpoint: "http://127.0.0.1:11435",
  model: process.env.DRAFT2DONE_STYLE_MODEL || "qwen2.5-coder:3b",
  context: 8192,
  maxTokens: Number(process.env.DRAFT2DONE_STYLE_OUTPUT || 700),
  temperature: 0.3,
};
const reportPath =
  process.env.DRAFT2DONE_STYLE_REPORT || "test-results/style-live.json";
assert.ok(
  ["qwen2.5-coder:3b", "llama3:8b-instruct-q4_K_M"].includes(settings.model),
  "Only existing authorized models are allowed",
);
assert.ok(
  [500, 700, 1200].includes(settings.maxTokens),
  "Only the authorized bounded output budgets are allowed",
);
assert.match(reportPath, /^test-results\/style-live(?:-[a-z]+)?\.json$/);
const started = Date.now();
const report = {
  at: new Date().toISOString(),
  settings,
  method:
    "Five actual local-model continuation checks, one per fictional style sample; at most one inference call per sample, 60 additional body words requested. Any shortfall is retained and reported rather than padded or retried.",
  limitations: [
    "Descriptive DNA comparisons and literal topic anchors cannot establish style fidelity, reasoning quality, factual support or authorship.",
    "The installed local model is a capability check, not evidence that every future response will preserve a writer's voice.",
    "No source abstracts, assigned literary text, personal history or new research evidence were supplied. New factual details, personal anecdotes and quotations require review.",
    "Paragraph organization and vocabulary measures vary strongly with the shorter continuation length; numerical similarity alone is not an acceptance criterion.",
    "Comparisons that change both model and output-token budget cannot isolate which change caused a difference.",
  ],
  samples: [],
  shortDraft: null,
};
const reviewChecks = {
  "01-personal-reflection.md": {
    anchors: [
      "help",
      "writing",
      "group",
      "instructor",
      "assignment",
      "learning",
    ],
    question:
      "Does the proposal develop the change in asking for help, retain a personal reflective voice, and avoid inventing a specific autobiographical event as established fact?",
  },
  "02-formal-argument.md": {
    anchors: [
      "transportation",
      "transit",
      "route",
      "service",
      "access",
      "frequency",
    ],
    question:
      "Does the proposal develop the access-versus-cost argument cautiously, without pretending a local transit example or outside research has been supplied?",
  },
  "03-plain-explanation.md": {
    anchors: ["soil", "water", "garden", "test", "compaction", "conditions"],
    question:
      "Does the proposal continue the proposed fair comparison in plain language without inventing experimental results?",
  },
  "04-evidence-cautious.md": {
    anchors: [
      "survey",
      "respondent",
      "association",
      "schedule",
      "question",
      "study",
    ],
    question:
      "Does the proposal keep the survey example hypothetical, preserve uncertainty, and avoid turning association into causation?",
  },
  "05-literary-analysis.md": {
    anchors: ["window", "narrator", "passage", "glass", "light", "quotation"],
    question:
      "Does the proposal connect the window interpretation to the unfinished close reading while acknowledging that the assigned text and exact quotation are absent?",
  },
};

function summary(profile) {
  return {
    wordCount: profile.wordCount,
    sentenceCount: profile.sentenceCount,
    paragraphCount: profile.paragraphCount,
    averageSentenceWords: profile.averageSentenceWords,
    averageParagraphWords: profile.averageParagraphWords,
    lexicalDiversity: profile.vocabulary.lexicalDiversity,
    person: profile.person,
    contractions: profile.formality.contractions,
    academicCues: profile.formality.academicCues,
    conversationalCues: profile.formality.conversationalCues,
    transitions: profile.transitions,
  };
}

async function persist() {
  await mkdir("test-results", { recursive: true });
  await writeFile(reportPath, JSON.stringify(report, null, 2));
}

function transportFor(entry, original) {
  let inferenceCalls = 0;
  return async ({ url, method = "GET", headers = {}, body, signal }) => {
    const parsedURL = new URL(url);
    assert.equal(
      parsedURL.origin,
      settings.endpoint,
      "Only the authorized existing local AI server may be contacted",
    );
    if (method === "POST") {
      assert.equal(parsedURL.pathname, "/api/chat");
      if (inferenceCalls >= 1) {
        entry.boundedStop =
          "The check stops after its first inference. A partial usable proposal is retained; no padding or additional round was requested.";
        throw new Error(
          "Live style check is bounded to one inference per sample.",
        );
      }
      inferenceCalls++;
      entry.inferenceRequests = inferenceCalls;
      const request = JSON.parse(body);
      const record = JSON.parse(request.messages[1].content);
      assert.equal(
        record.draft,
        original,
        "The entire draft must be present unchanged in the request",
      );
      assert.deepEqual(
        record.evidence,
        [],
        "This check supplies no external evidence",
      );
      assert.equal(request.options.num_ctx, settings.context);
      assert.equal(request.options.num_predict, settings.maxTokens);
      entry.requestAudit = {
        wholeDraftRetained: record.draft === original,
        draftCharacters: original.length,
        promptCharacters: request.messages.reduce(
          (sum, message) => sum + message.content.length,
          0,
        ),
        configuredContext: request.options.num_ctx,
        maximumOutputTokens: request.options.num_predict,
        evidenceRecords: record.evidence.length,
        structuredSchema: typeof request.format === "object",
        responseFields:
          typeof request.format === "object" ? request.format.required : [],
        requestedAdditionalWords: Number(
          /approximately (\d+) words/.exec(request.messages[0].content)?.[1],
        ),
      };
    }
    const response = await fetch(url, {
      method,
      headers,
      ...(method === "GET" ? {} : { body }),
      signal,
    });
    const responseBody = await response.text();
    if (method === "POST" && response.ok) {
      const data = JSON.parse(responseBody);
      entry.rawModelText = String(data.message?.content || "").slice(0, 20000);
      entry.modelRuntime = {
        promptTokens: data.prompt_eval_count,
        outputTokens: data.eval_count,
        done: data.done,
        doneReason: data.done_reason,
      };
    }
    return { status: response.status, body: responseBody };
  };
}

for (const filename of (await readdir("samples"))
  .filter((name) => name.endsWith(".md"))
  .sort()) {
  const original = await readFile(`samples/${filename}`, "utf8");
  const project = createProject(filename, original);
  const originalWords = countWords(original, project.countingRules);
  project.minimum = originalWords + 60;
  project.maximum = 0;
  const entry = {
    sample: filename,
    originalText: original,
    originalWords,
    requestedAdditionalWords: 60,
    status: "pending",
    inferenceRequests: 0,
    proposals: [],
    qualitativeReview: null,
  };
  report.samples.push(entry);
  const sampleStarted = Date.now();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 150000);
  try {
    const client = new AIClient(settings, transportFor(entry, original));
    const before = analyzeDNA(original);
    const proposals = await client.complete(
      project,
      analyzeDraft(project),
      before,
      { signal: controller.signal },
    );
    const proposedText = proposals.map(({ text }) => text).join("\n\n");
    const proposedWords = countWords(proposedText, project.countingRules);
    assert.equal(project.originalText, original);
    assert.equal(project.workingText, original);
    assert.ok(proposals.every(({ status }) => status === "pending"));
    assert.ok(
      proposals.every(({ warnings }) =>
        warnings.some((warning) =>
          /no retrieved evidence|unsupported|unverified/i.test(warning),
        ),
      ),
    );
    entry.status = "proposed";
    entry.proposals = proposals;
    entry.proposedText = proposedText;
    entry.proposedWords = proposedWords;
    entry.remainingToRequestedMinimum = Math.max(0, 60 - proposedWords);
    entry.originalUnchanged = true;
    entry.workingDraftUnchanged = true;
    entry.dnaComparison = {
      original: summary(before),
      continuationOnly: summary(analyzeDNA(proposedText)),
      interpretation:
        "Descriptive counts for different-length samples; no fidelity or quality score is inferred.",
    };
    const review = reviewChecks[filename];
    entry.topicObservations = {
      method:
        "Case-insensitive literal anchor matches only; these do not verify coherence or reasoning.",
      anchorsSeen: review.anchors.filter((anchor) =>
        new RegExp(`\\b${anchor}\\w*\\b`, "i").test(proposedText),
      ),
      anchorsAbsent: review.anchors.filter(
        (anchor) => !new RegExp(`\\b${anchor}\\w*\\b`, "i").test(proposedText),
      ),
      qualitativeReviewQuestion: review.question,
    };
    console.log(
      `${filename}: ${originalWords} draft words; ${proposedWords} proposed; ${entry.remainingToRequestedMinimum} shortfall; original unchanged; 1 bounded local inference.`,
    );
  } catch (error) {
    entry.status =
      entry.inferenceRequests === 0 &&
      /context|Nothing was sent|Add at least 40/i.test(error.message)
        ? "explicit-refusal"
        : /did not return any usable.*Model notes:.*(?:insufficient|missing|not supplied|not provided|cannot|can't|need|require|unavailable)/is.test(
              error.message,
            )
          ? "model-refusal"
          : "error";
    entry.error = error.message;
    entry.originalUnchanged = project.originalText === original;
    entry.workingDraftUnchanged = project.workingText === original;
    console.log(`${filename}: ${entry.status}; ${error.message}`);
  } finally {
    clearTimeout(timer);
    entry.elapsedSeconds = Math.round((Date.now() - sampleStarted) / 100) / 10;
    await persist();
  }
}

let shortCalls = 0;
const shortText =
  "I have started this draft, but I still need to explain my central point.";
const shortProject = createProject("Genuine short draft", shortText);
try {
  await new AIClient(settings, async () => {
    shortCalls++;
    throw new Error("Short draft must be refused before transport.");
  }).complete(shortProject, {}, {});
  report.shortDraft = {
    passed: false,
    text: shortText,
    error: "Short draft unexpectedly accepted.",
  };
} catch (error) {
  report.shortDraft = {
    passed: shortCalls === 0 && /Add at least 40 words/i.test(error.message),
    originalText: shortText,
    wordCount: countWords(shortText),
    padded: false,
    transportCalls: shortCalls,
    refusal: error.message,
    originalUnchanged:
      shortProject.originalText === shortText &&
      shortProject.workingText === shortText,
  };
}
report.elapsedSeconds = Math.round((Date.now() - started) / 100) / 10;
report.summary = {
  evaluated: report.samples.length,
  proposed: report.samples.filter(({ status }) => status === "proposed").length,
  explicitRefusals: report.samples.filter(
    ({ status }) => status === "explicit-refusal",
  ).length,
  modelRefusals: report.samples.filter(
    ({ status }) => status === "model-refusal",
  ).length,
  errors: report.samples.filter(({ status }) => status === "error").length,
  shortDraftRefusedBeforeTransport: report.shortDraft.passed,
};
await persist();
console.log(JSON.stringify(report.summary));
console.log(
  `Evidence saved to ${reportPath}; read each proposed text before making a qualitative style claim.`,
);
if (
  report.summary.errors ||
  !report.shortDraft.passed ||
  report.samples.length !== 5
)
  process.exitCode = 1;
