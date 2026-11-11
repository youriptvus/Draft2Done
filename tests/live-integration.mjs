import { AIClient, ResearchEngine } from "../web/services.js";
import {
  createProject,
  analyzeDraft,
  analyzeDNA,
  countWords,
} from "../web/core.js";
import { writeFile, readFile, mkdir } from "node:fs/promises";
const results = {
  at: new Date().toISOString(),
  research: {},
  ai: {},
  errors: [],
};
const transport = async ({
  url,
  method = "GET",
  headers = {},
  body,
  signal,
}) => {
  const r = await fetch(url, {
    method,
    headers,
    ...(method === "GET" ? {} : { body }),
    signal,
  });
  return { status: r.status, body: await r.text() };
};
const engine = new ResearchEngine(transport);
const retrievedSources = [];
for (const provider of ["crossref", "pubmed", "wikipedia"]) {
  try {
    const r = await engine.search("urban green space wellbeing", [provider]);
    retrievedSources.push(...r.sources);
    results.research[provider] = {
      sources: r.sources.length,
      errors: r.errors,
      firstTitle: r.sources[0]?.title,
      statuses: r.sources.map((s) => s.verification),
    };
    if (r.sources[0]) {
      const verified = await engine.verify(r.sources[0]);
      results.research[provider].verification = verified.verification;
    }
  } catch (e) {
    results.research[provider] = { error: e.message };
  }
}
const p = createProject(
  "Fictional sample",
  await readFile("samples/04-evidence-cautious.md", "utf8"),
);
p.sources = retrievedSources.filter((source) => source.abstract).sort((a, b) => (a.provider === "wikipedia") - (b.provider === "wikipedia")).slice(0, 4);
results.ai.evidence = p.sources.map(({ id, title, doi, abstract }) => ({ id, title, doi, abstract }));
p.minimum = countWords(p.workingText) + 70;
const ai = new AIClient(
  {
    backend: "ollama",
    endpoint: "http://127.0.0.1:11435",
    model: "llama3:8b-instruct-q4_K_M",
    context: 8192,
    maxTokens: 1200,
    temperature: 0.4,
  },
  transport,
);
try {
  results.ai.models = await ai.discover();
  console.log("Real model discovery:", results.ai.models.join(", "));
  const test = await ai.test();
  results.ai.inference = { success: test.inference, response: test.response };
  console.log("Real inference:", test.response);
  const helper = await ai.helper(p, analyzeDraft(p), analyzeDNA(p.workingText));
  results.ai.helper = {
    success: true,
    characters: helper.length,
    text: helper,
  };
  console.log("Real Helper Mode returned", helper.length, "characters");
  const proposals = await ai.complete(
    p,
    analyzeDraft(p),
    analyzeDNA(p.workingText),
    { onProgress: (v) => console.log(v.message) },
  );
  results.ai.complete = {
    success: true,
    proposals,
    originalUnchanged: p.originalText === p.workingText,
    proposedWords: proposals.reduce((n, s) => n + countWords(s.text), 0),
    remaining: Math.max(
      0,
      p.minimum -
        countWords(p.workingText) -
        proposals.reduce((n, s) => n + countWords(s.text), 0),
    ),
  };
  console.log("Real Complete Mode returned", proposals.length, "proposal(s)");
  const controller = new AbortController();
  const pending = ai.helper(
    p,
    {},
    {},
    "Suggest a next step",
    controller.signal,
  );
  setTimeout(() => controller.abort(), 100);
  try {
    await pending;
    results.ai.cancellation = "Request completed before cancellation";
  } catch (e) {
    results.ai.cancellation = e.name;
  }
} catch (e) {
  results.errors.push(e.message);
  console.log("Live check issue:", e.message);
}
await mkdir("test-results", { recursive: true });
await writeFile(
  "test-results/live-report.json",
  JSON.stringify(results, null, 2),
);
console.log(
  JSON.stringify(
    {
      research: results.research,
      ai: {
        inference: results.ai.inference,
        helper: results.ai.helper?.success,
        complete: results.ai.complete?.success,
        cancellation: results.ai.cancellation,
      },
      errors: results.errors,
    },
    null,
    2,
  ),
);
