import test from "node:test";
import assert from "node:assert/strict";
import {
  informationLinks,
  informationParts,
  relatedInformation,
  safeInformationUrl,
} from "../web/information-links.js";

const source = (
  title,
  url = "https://en.wikipedia.org/wiki/Climate_change",
  extra = {},
) => ({
  id: "fixture",
  title,
  url,
  provider: "wikipedia",
  verification: "abstract",
  abstract: "",
  ...extra,
});

test("local links identify scientific, historical, academic and named concepts", () => {
  const text =
    "Photosynthesis converts light.\n\nThe French Revolution changed institutions.\n\nMarie Curie studied radioactivity.\n\nA confidence interval describes uncertainty.\n\nThe United Nations meets in New York.";
  const links = informationLinks(text, { minRepeatDistance: 0 });
  for (const name of [
    "Photosynthesis",
    "French Revolution",
    "Marie Curie",
    "confidence interval",
    "United Nations",
  ])
    assert(
      links.some((link) => link.label === name),
      name,
    );
  assert.equal(
    informationParts(text, links)
      .map((part) => part.text)
      .join(""),
    text,
  );
});

test("UTF-16 offsets retain emoji, Unicode names and decomposed accents", () => {
  const text =
    "🧑🏽‍🎓 📚 Émile Durkheim considered social identity.\n\nRene\u0301 Descartes is a philosopher.\n\nRené Descartes wrote about knowledge.";
  const links = informationLinks(text, { minRepeatDistance: 0 });
  assert(links.some((link) => link.label === "Émile Durkheim"));
  assert(links.some((link) => link.label === "Rene\u0301 Descartes"));
  assert(links.some((link) => link.label === "René Descartes"));
  for (const link of links)
    assert.equal(text.slice(link.start, link.end), link.label);
  assert.equal(
    informationParts(text, links)
      .map((part) => part.text)
      .join(""),
    text,
  );
});

test("overlapping names and terms resolve to the complete known concept", () => {
  const text = "The World Health Organization researches epidemiology.";
  const links = informationLinks(text);
  assert.equal(links[0].label, "World Health Organization");
  assert.equal(links.filter((link) => link.label.includes("World")).length, 1);
  for (let i = 1; i < links.length; i++)
    assert(links[i].start >= links[i - 1].end);
});

test("few links per reading passage and no nearby repeated concept", () => {
  const text =
    "Climate change affects biodiversity and the carbon cycle. Climate change also affects photosynthesis, evolution and ecosystems.";
  const links = informationLinks(text);
  assert(links.length <= 3);
  assert.equal(
    links.filter((link) => link.label.toLowerCase() === "climate change")
      .length,
    1,
  );
  const distant = `Climate change matters.\n\n${"ordinary text ".repeat(70)}\n\nClimate change matters again.`;
  assert.equal(
    informationLinks(distant).filter((link) => link.label === "Climate change")
      .length,
    2,
  );
});

test("explicit limits bound highlighting and can disable it", () => {
  const text =
    "Marie Curie studied photosynthesis.\n\nThe French Revolution changed the United Kingdom.\n\nDNA and RNA are studied in microbiology.";
  assert.equal(informationLinks(text, { maxLinks: 0 }).length, 0);
  assert.equal(informationLinks(text, { maxPerPassage: 0 }).length, 0);
  assert(informationLinks(text, { maxLinks: 2 }).length <= 2);
  assert(informationLinks(text, { maxPerPassage: 1 }).length <= 3);
});

test("URLs, email, code, formal citations and bibliography do not become concepts", () => {
  const text =
    "https://example.org/Photosynthesis `Marie Curie`\n```\nFrench Revolution\n```\nDNA@example.org [[cite:RNA]]\n\nPhotosynthesis matters.\n\nReferences\nMarie Curie. French Revolution. DNA.";
  const links = informationLinks(text);
  assert.deepEqual(
    links.map((link) => link.label),
    ["Photosynthesis"],
  );
});

test("plain prose and common sentence starters are not automatically linked", () => {
  assert.deepEqual(
    informationLinks(
      "This is my paper. The study can explain a simple question. I will add my own reasoning.",
    ),
    [],
  );
});

test("only retrieved source titles and confirmed web-brief concepts augment detection", () => {
  const text =
    "The amber migration hypothesis contrasts with the silver migration hypothesis and the violet migration hypothesis.";
  const links = informationLinks(text, {
    sources: [
      source("amber migration hypothesis"),
      source("silver migration hypothesis", undefined, {
        verification: "unverified",
      }),
    ],
    researchBriefs: [
      {
        query: "violet migration hypothesis",
        completed: true,
        webSearchUsed: true,
      },
    ],
  });
  assert(links.some((link) => link.label === "amber migration hypothesis"));
  assert(!links.some((link) => link.label === "silver migration hypothesis"));
  assert(links.some((link) => link.label === "violet migration hypothesis"));
});

test("recomputing after editing produces fresh positions; stale ranges are ignored", () => {
  const before = "Photosynthesis matters.",
    old = informationLinks(before);
  const after = "My own paragraph. Photosynthesis matters.";
  assert.equal(
    informationParts(after, old).filter((part) => part.link).length,
    0,
  );
  const fresh = informationLinks(after);
  assert.equal(fresh[0].start, after.indexOf("Photosynthesis"));
  assert.equal(
    informationParts(after, fresh)
      .map((part) => part.text)
      .join(""),
    after,
  );
});

test("parts reject malformed/overlapping ranges and preserve literal text", () => {
  const text = "<script>Photosynthesis</script> 🧬\r\nOwn “words”.";
  const start = text.indexOf("Photosynthesis"),
    valid = { start, end: start + 14, label: "Photosynthesis" };
  const parts = informationParts(text, [
    null,
    valid,
    { ...valid },
    { start: -1, end: 3, label: "<sc" },
    { start: 1.5, end: 4, label: "scr" },
    { start: 100, end: 110, label: "missing" },
  ]);
  assert.equal(parts.filter((part) => part.link).length, 1);
  assert.equal(parts.map((part) => part.text).join(""), text);
  assert.equal(
    informationParts("")
      .map((part) => part.text)
      .join(""),
    "",
  );
});

test("large drafts remain untouched and suggestion work is bounded", () => {
  const text = "Photosynthesis and climate change.\n\n".repeat(20_000);
  const links = informationLinks(text, { maxLinks: 1000 });
  assert(links.length <= 40);
  assert(links.every((link) => link.end <= 500_000));
  assert.equal(
    informationParts(text, links)
      .map((part) => part.text)
      .join(""),
    text,
  );
});

test("URL guard accepts public HTTP(S) without adding guessed destinations", () => {
  for (const url of [
    "https://en.wikipedia.org/wiki/Marie_Curie#Research",
    "https://www.ed.ac.uk/research",
    "https://epa.gov/climate-change",
    "https://doi.org/10.1000/example",
    "http://example.org/paper?query=climate%20change",
    "https://[2606:4700:4700::1111]/",
  ])
    assert.equal(safeInformationUrl(url), new URL(url).href, url);
  assert.equal(safeInformationUrl("Climate change"), null);
});

test("URL guard rejects scripts, credentials, local/private/malformed addresses", () => {
  const bad = [
    "javascript:alert(1)",
    "data:text/html,hello",
    "file:///etc/passwd",
    "content://draft",
    "//example.org/page",
    "https://user:pass@example.org/",
    "https://localhost/",
    "https://x.localhost/",
    "https://intranet/",
    "https://a.local/",
    "https://a.test/",
    "https://127.0.0.1/",
    "https://2130706433/",
    "https://0x7f000001/",
    "https://10.5.4.3/",
    "https://192.168.1.1/",
    "https://172.20.1.1/",
    "https://169.254.169.254/",
    "https://100.64.1.2/",
    "https://198.18.0.1/",
    "https://[::1]/",
    "https://[::ffff:127.0.0.1]/",
    "https://[2001:db8::1]/",
    "https://[fc00::1]/",
    "https://[fe80::1]/",
    "https://a..edu/",
    "https://-bad.edu/",
    "https://example.org\\@localhost/",
    "https://example.org/\nfile",
    " https://example.org/",
  ];
  for (const url of bad) assert.equal(safeInformationUrl(url), null, url);
});

test("related sources require concept relevance, retrieval and public URL", () => {
  const sources = [
    source("Climate change and health"),
    source("A different title", "https://example.edu/health", {
      abstract: "Health impacts of climate change are under review.",
      provider: "crossref",
    }),
    source("Inflation and wages", "https://example.gov/wages"),
    source("Climate change", "javascript:alert(1)"),
    source("Climate change", "https://example.org/manual", {
      verification: "unverified",
    }),
  ];
  const results = relatedInformation("climate change", { sources });
  assert.equal(results.length, 2);
  assert(
    results.every((record) =>
      ["abstract", "metadata"].includes(record.verification),
    ),
  );
  assert(results.every((record) => record.kind === "saved-source"));
});

test("single terms use whole words rather than accidental substrings", () => {
  assert.deepEqual(
    relatedInformation("AI", {
      sources: [source("Said differently", "https://example.edu/said")],
    }),
    [],
  );
  assert.deepEqual(
    relatedInformation("evolution", {
      sources: [source("The French Revolution")],
    }),
    [],
  );
  assert.equal(
    relatedInformation("AI", { sources: [source("AI and its limitations")] })
      .length,
    1,
  );
});

test("credible source categories are preferred only after relevance", () => {
  const results = relatedInformation("climate change", {
    sources: [
      source("Climate change", "https://reference.example.org/climate", {
        provider: "manual",
      }),
      source("Climate change", "https://en.wikipedia.org/wiki/Climate_change"),
      source("Climate change", "https://www.example.edu/climate", {
        provider: "manual",
      }),
      source("Climate change", "https://agency.gov/climate", {
        provider: "manual",
      }),
      source("Climate change", "https://publisher.example.org/article", {
        provider: "crossref",
      }),
      source("Ocean currents", "https://agency.gov/ocean", {
        provider: "manual",
      }),
    ],
  });
  assert(
    results
      .slice(0, 3)
      .every((item) => /wikipedia\.org|\.edu\/|\.gov\//u.test(item.url)),
  );
  assert(!results.some((item) => item.title === "Ocean currents"));
  assert(
    results.every((item) => !/trusted|guaranteed|proven/iu.test(item.detail)),
  );
});

test("confirmed web-brief pages reuse titles and valid annotation context", () => {
  const text =
    "Photosynthesis converts light energy into chemical energy. [1] A separate question concerns taxes.";
  const start = text.indexOf("[1]");
  const brief = {
    completed: true,
    webSearchUsed: true,
    query: "photosynthesis evidence",
    text,
    sources: [
      { title: "Plant energy overview", url: "https://example.edu/plants" },
      { title: "Income tax", url: "https://example.gov/tax" },
    ],
    annotations: [
      {
        start,
        end: start + 3,
        title: "Plant energy overview",
        url: "https://example.edu/plants",
      },
    ],
  };
  const results = relatedInformation(
    { label: "photosynthesis" },
    { researchBriefs: [brief] },
  );
  assert.equal(results.length, 1);
  assert.equal(results[0].url, "https://example.edu/plants");
  assert.equal(results[0].verification, "web-returned");
  assert.match(results[0].detail, /not independently verified/u);
});

test("query match alone and unconfirmed/incomplete briefs do not promote links", () => {
  const brief = {
    completed: true,
    webSearchUsed: true,
    query: "climate change",
    text: "Climate change is discussed here.",
    sources: [{ title: "Unrelated taxes", url: "https://example.gov/taxes" }],
  };
  assert.deepEqual(
    relatedInformation("climate change", { researchBriefs: [brief] }),
    [],
  );
  for (const other of [
    { ...brief, webSearchUsed: false },
    { ...brief, completed: false },
  ]) {
    other.sources = [
      { title: "Climate change", url: "https://example.gov/climate" },
    ];
    assert.deepEqual(
      relatedInformation("climate change", { researchBriefs: [other] }),
      [],
    );
  }
});

test("invalid annotation positions are not evidence; removed sources do not remain cached", () => {
  const brief = {
    completed: true,
    webSearchUsed: true,
    text: "Climate change is under study.",
    sources: [{ title: "Reference", url: "https://example.edu/source" }],
    annotations: [
      {
        start: 0,
        end: 1000,
        title: "Reference",
        url: "https://example.edu/source",
      },
    ],
  };
  assert.deepEqual(
    relatedInformation("climate change", { researchBriefs: [brief] }),
    [],
  );
  const saved = [source("Climate change")];
  assert.equal(
    relatedInformation("climate change", { sources: saved }).length,
    1,
  );
  assert.deepEqual(relatedInformation("climate change", { sources: [] }), []);
});

test("same returned URL is shown once; opening fragment does not defeat deduplication", () => {
  const sources = [
    source("Climate change", "https://example.edu/climate#one"),
    source("Climate change", "https://example.edu/climate#two"),
  ];
  const brief = {
    completed: true,
    webSearchUsed: true,
    sources: [{ title: "Climate change", url: "https://example.edu/climate" }],
  };
  assert.equal(
    relatedInformation("climate change", { sources, researchBriefs: [brief] })
      .length,
    1,
  );
});

test("all functions leave input drafts, source records, briefs and link ranges unchanged", () => {
  const project = {
    workingText: "Photosynthesis and Marie Curie.\r\n\r\nOwn spelling stays.",
    originalText: "Original text.",
    mode: "helper",
    sources: [source("Photosynthesis")],
    researchBriefs: [],
  };
  const before = structuredClone(project),
    links = informationLinks(project.workingText, project),
    linkBefore = structuredClone(links);
  Object.freeze(project.sources[0]);
  Object.freeze(project.sources);
  Object.freeze(project.researchBriefs);
  Object.freeze(project);
  informationParts(project.workingText, links);
  relatedInformation(links[0], project);
  assert.deepEqual(project, before);
  assert.deepEqual(links, linkBefore);
  assert(!("citations" in project) && !("informationLinks" in project));
});
