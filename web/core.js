/** Portable, deterministic document tools. No network calls or language model are used here. */
export const SCHEMA_VERSION = 1;
const WORD =
  /[\p{L}\p{N}]+(?:[’'][\p{L}\p{N}]+)*(?:[-‐‑][\p{L}\p{N}]+(?:[’'][\p{L}\p{N}]+)*)*/gu;
const REFERENCES =
  /^(?:[ \t]*#{1,6}[ \t]+)?[ \t]*(?:references|works[ \t]+cited|bibliography)[ \t]*:?[ \t]*#*[ \t]*$/im;
const STOP = new Set(
  "a an and are as at be been being but by can could did do does for from had has have he her hers him his how i if in into is it its may might more most my no not of on one or our ours she should so some than that the their theirs them then there these they this those through to too us was we were what when where which while who why will with would you your also such about because each other very many much however therefore".split(
    " ",
  ),
);
const TRANSITIONS = [
  "however",
  "therefore",
  "for example",
  "for instance",
  "in addition",
  "furthermore",
  "moreover",
  "as a result",
  "on the other hand",
  "in contrast",
  "in conclusion",
  "finally",
  "first",
  "second",
  "also",
  "so",
  "but",
];
const now = () => new Date().toISOString();
const id = () =>
  globalThis.crypto?.randomUUID?.() ||
  `d2d-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
const clone = (value) => JSON.parse(JSON.stringify(value));
const tokens = (text) => String(text || "").match(WORD) || [];
const round = (value) => Math.round(value * 10) / 10;

/** Title pages must end with <!-- title-end --> or a standalone --- near the top. */
export function bodyText(text, rules = {}) {
  let body = String(text ?? "");
  if (rules.excludeTitle) {
    const marker = /<!--\s*title-end\s*-->/i.exec(body);
    if (marker) body = body.slice(marker.index + marker[0].length);
    else {
      const delimiter = /^[ \t]*---[ \t]*\r?$/m.exec(body);
      if (
        delimiter &&
        body.slice(0, delimiter.index).split("\n").length <= 15
      ) {
        body = body.slice(delimiter.index + delimiter[0].length);
      }
    }
  }
  if (rules.excludeReferences !== false) {
    const reference = REFERENCES.exec(body);
    if (reference) body = body.slice(0, reference.index);
  }
  return body;
}

export function countWords(text, rules = {}) {
  return tokens(bodyText(text, rules)).length;
}

export function createProject(title = "Untitled paper", text = "") {
  const stamp = now();
  return {
    schemaVersion: SCHEMA_VERSION,
    id: id(),
    title: String(title || "Untitled paper"),
    originalText: String(text),
    workingText: String(text),
    instructions: "",
    rubric: "",
    academicLevel: "College",
    requiredHeadings: "",
    minimum: 1000,
    maximum: 0,
    voice: "original",
    citationStyle: "APA",
    countingRules: { excludeReferences: true, excludeTitle: true },
    exportFormatting: {
      fontFamily: "Times New Roman",
      fontSize: 12,
      lineSpacing: 1.5,
    },
    sources: [],
    citedSourceIds: [],
    proposals: [],
    revisions: [],
    contributions: [],
    profile: null,
    researchBriefs: [],
    helperNotes: "",
    createdAt: stamp,
    updatedAt: stamp,
  };
}

export function progress(project) {
  const original = countWords(project.originalText, project.countingRules);
  const current = countWords(project.workingText, project.countingRules);
  const proposed = (project.proposals || [])
    .filter((p) => p.status === "pending")
    .reduce((sum, p) => sum + countWords(p.text, project.countingRules), 0);
  const minimum = Number(project.minimum) || 0;
  const maximum = Number(project.maximum) || 0;
  return {
    original,
    current,
    proposed,
    minimum,
    maximum,
    remaining: Math.max(0, minimum - current),
    percent:
      minimum > 0 ? Math.min(100, round((current / minimum) * 100)) : 100,
  };
}

/** A proposed insertion can finish the existing tail without replacing any text. */
export function continuationPoint(project) {
  const text = String(project.workingText || "");
  const references = REFERENCES.exec(text);
  const body = text
    .slice(0, references ? references.index : text.length)
    .trimEnd();
  return {
    position: body.length,
    anchor: body.slice(-160),
    excerpt: body.slice(-260),
    unfinished: Boolean(body) && !/[.!?…”.’"')\]]$/.test(body),
  };
}

function paragraphs(text) {
  return bodyText(text)
    .split(/\r?\n\s*\r?\n/)
    .map((p) => p.trim())
    .filter(Boolean);
}

function sentences(text) {
  // Protect common initials, abbreviations, URLs and decimal points before splitting.
  const protectedText = bodyText(text)
    .replace(/\b(?:Dr|Mr|Mrs|Ms|Prof|Fig|No|vs|etc)\./gi, (m) =>
      m.replace(".", "\uE000"),
    )
    .replace(/\b(?:e\.g\.|i\.e\.|et al\.)/gi, (m) =>
      m.replaceAll(".", "\uE000"),
    )
    .replace(/\b[A-Z]\.(?=\s*[A-Z])/g, (m) => m.replace(".", "\uE000"))
    .replace(/(\d)\.(?=\d)/g, "$1\uE000")
    .replace(/https?:\/\/\S+/g, (m) => m.replaceAll(".", "\uE000"));
  return (protectedText.match(/[^.!?]+(?:[.!?]+[”"’']*(?=\s|$)|$)/gu) || [])
    .map((s) => s.replaceAll("\uE000", ".").trim())
    .filter((s) => tokens(s).length);
}

/** Descriptive measurements, not authorship detection or an AI style assessment. */
export function analyzeDNA(text) {
  const body = bodyText(text);
  const words = tokens(body);
  const normalizeWord = (word) =>
    word.normalize("NFC").toLowerCase().replace(/[‘’]/g, "'");
  const normalized = words.map(normalizeWord);
  const frequency = new Map();
  normalized.forEach((w) => frequency.set(w, (frequency.get(w) || 0) + 1));
  const paras = paragraphs(body);
  const sents = sentences(body);
  const person = {
    first: normalized.filter((w) => /^(i|me|my|mine|we|us|our|ours)$/.test(w))
      .length,
    second: normalized.filter((w) => /^(you|your|yours)$/.test(w)).length,
    third: normalized.filter((w) =>
      /^(he|him|his|she|her|hers|they|them|their|theirs|it|its)$/.test(w),
    ).length,
  };
  const highest = Math.max(person.first, person.second, person.third);
  person.dominant =
    highest === 0
      ? "No personal pronouns detected"
      : Object.keys(person)
          .filter((k) => person[k] === highest)
          .join(" / ");
  // Keep expression counts within uninterrupted pieces of a sentence. A phrase
  // never starts at the end of one paragraph and finishes in the next one.
  const phraseSegments = body
    .split(/[.!?;:,\n()[\]{}]+/u)
    .map((part) => tokens(part).map(normalizeWord))
    .filter((part) => part.length);
  const phraseCounts = new Map();
  for (const segment of phraseSegments) {
    for (let size = 2; size <= 3; size++) {
      for (let start = 0; start + size <= segment.length; start++) {
        const phrase = segment.slice(start, start + size).join(" ");
        phraseCounts.set(phrase, (phraseCounts.get(phrase) || 0) + 1);
      }
    }
  }
  const boilerplate = new Set([
    ...TRANSITIONS,
    "this paper",
    "this study",
    "the paper",
    "the study",
    "the data",
    "the results",
    "in this paper",
    "in this study",
    "the present study",
    "et al",
  ]);
  const expressionCandidates = [...phraseCounts.entries()]
    .filter(([, count]) => count >= 2)
    .map(([phrase, count]) => ({
      phrase,
      count,
      words: phrase.split(" ").length,
    }))
    .filter(({ phrase, count, words: size }) => {
      if (count < (size === 2 ? 3 : 2) || boilerplate.has(phrase)) return false;
      const parts = phrase.split(" ");
      const content = parts.filter(
        (word) => word.length > 2 && /\p{L}/u.test(word) && !STOP.has(word),
      );
      if (!content.length) return false;
      return (
        size === 3 ||
        content.length === 2 ||
        (/^(i|we|you|my|our|your)$/.test(parts[0]) &&
          content.includes(parts[1]))
      );
    });
  const coveredBigrams = new Map();
  for (const expression of expressionCandidates.filter(
    ({ words }) => words === 3,
  )) {
    const parts = expression.phrase.split(" ");
    for (let start = 0; start < 2; start++) {
      const phrase = parts.slice(start, start + 2).join(" ");
      coveredBigrams.set(
        phrase,
        Math.max(coveredBigrams.get(phrase) || 0, expression.count),
      );
    }
  }
  const repeatedExpressions = expressionCandidates
    .filter(
      (item) =>
        item.words === 3 || (coveredBigrams.get(item.phrase) || 0) < item.count,
    )
    .sort(
      (a, b) =>
        b.count - a.count ||
        b.words - a.words ||
        a.phrase.localeCompare(b.phrase),
    )
    .slice(0, 12);
  const countPhrase = (phrase) =>
    phrase.includes(" ")
      ? phraseCounts.get(phrase) || 0
      : frequency.get(phrase) || 0;
  const observedCues = (phrases) =>
    phrases
      .map((phrase) => ({ phrase, count: countPhrase(phrase) }))
      .filter(({ count }) => count)
      .sort((a, b) => b.count - a.count || a.phrase.localeCompare(b.phrase));
  const contractionFrequency = new Map();
  for (const word of normalized) {
    if (
      /^(?:[a-z]+n't(?:'(?:ve|ll|d))?|(?:i|you|we|they|he|she|it|that|there|here|what|who|where|how|when|why)'(?:m|re|ve|ll|d|s)|(?:could|would|should|might|must)'ve|let's|y'all)$/u.test(
        word,
      )
    ) {
      contractionFrequency.set(word, (contractionFrequency.get(word) || 0) + 1);
    }
  }
  const contractionCount = [...contractionFrequency.values()].reduce(
    (sum, count) => sum + count,
    0,
  );
  const formality = {
    contractions: {
      count: contractionCount,
      per100Words: words.length
        ? round((contractionCount / words.length) * 100)
        : 0,
      forms: [...contractionFrequency.entries()]
        .map(([form, count]) => ({ form, count }))
        .sort((a, b) => b.count - a.count || a.form.localeCompare(b.form))
        .slice(0, 12),
    },
    academicCues: observedCues([
      "therefore",
      "however",
      "moreover",
      "furthermore",
      "consequently",
      "nevertheless",
      "evidence",
      "analysis",
      "indicates",
      "suggests",
      "examines",
      "methodology",
    ]),
    conversationalCues: observedCues([
      "you know",
      "a lot",
      "kind of",
      "sort of",
      "pretty much",
      "really",
      "basically",
      "stuff",
      "gonna",
      "wanna",
    ]),
    note: "These are context-dependent word counts and recognized English contractions, not an overall formality score. Possessive apostrophes are not counted as contractions.",
  };
  const bodyParagraphs = paras
    .map((paragraph) =>
      paragraph
        .split(/\r?\n/)
        .filter((line) => !headingName(line))
        .join("\n")
        .trim(),
    )
    .filter(Boolean);
  const roleRules = [
    [
      /^(in conclusion|to conclude|in summary|overall|taken together|to sum up)\b/i,
      "Possible summary or conclusion",
    ],
    [
      /^(for example|for instance|to illustrate|consider)\b/i,
      "Example-led paragraph",
    ],
    [
      /^(however|nevertheless|in contrast|on the other hand|although|despite|whereas)\b/i,
      "Contrast-led paragraph",
    ],
    [
      /^(according to|a study|the study|studies|research|evidence|the results|results|findings)\b/i,
      "Possible evidence discussion",
    ],
    [/^(first|second|third|next|finally)\b/i, "Sequence-led paragraph"],
  ];
  const organization = bodyParagraphs.map((paragraph, index) => {
    const paragraphSentences = sentences(paragraph);
    const opening = paragraphSentences[0] || paragraph;
    const rule = roleRules.find(([pattern]) => pattern.test(opening));
    return {
      index: index + 1,
      wordCount: tokens(paragraph).length,
      sentenceCount: paragraphSentences.length,
      opening: opening.slice(0, 180),
      possibleRole: rule
        ? rule[1]
        : index === 0
          ? "Opening paragraph"
          : index === bodyParagraphs.length - 1
            ? "Final paragraph"
            : "Body paragraph",
      evidence: rule
        ? `Opens with “${opening.match(rule[0])[0]}”.`
        : "Position only; no explicit opening cue detected.",
    };
  });
  const cuedParagraphs = organization.filter(
    (paragraph) => !paragraph.evidence.startsWith("Position only"),
  ).length;
  const paragraphOrganization = {
    totalParagraphs: organization.length,
    paragraphs: organization.slice(0, 12),
    summary: organization.length
      ? `${cuedParagraphs} of ${organization.length} body paragraphs open with a recognized organization cue.`
      : "No body paragraphs are available to describe.",
    note: "Roles are tentative descriptions of opening phrases and positions, not a judgment of paragraph quality. Headings alone are excluded; only the first 12 paragraph previews are stored.",
    truncated: organization.length > 12,
  };
  const spellingFamilies = [
    [
      "color / colour",
      [
        "color",
        "colors",
        "colored",
        "coloring",
        "colour",
        "colours",
        "coloured",
        "colouring",
      ],
    ],
    [
      "behavior / behaviour",
      [
        "behavior",
        "behaviors",
        "behavioral",
        "behaviour",
        "behaviours",
        "behavioural",
      ],
    ],
    [
      "analyze / analyse",
      [
        "analyze",
        "analyzes",
        "analyzed",
        "analyzing",
        "analyse",
        "analysed",
        "analysing",
      ],
    ],
    [
      "organize / organise",
      [
        "organize",
        "organizes",
        "organized",
        "organizing",
        "organization",
        "organizations",
        "organise",
        "organises",
        "organised",
        "organising",
        "organisation",
        "organisations",
      ],
    ],
    [
      "center / centre",
      ["center", "centers", "centered", "centre", "centres", "centred"],
    ],
    [
      "recognize / recognise",
      [
        "recognize",
        "recognizes",
        "recognized",
        "recognizing",
        "recognise",
        "recognises",
        "recognised",
        "recognising",
      ],
    ],
    [
      "traveling / travelling",
      [
        "traveled",
        "traveling",
        "traveler",
        "travelers",
        "travelled",
        "travelling",
        "traveller",
        "travellers",
      ],
    ],
    ["gray / grey", ["gray", "grey"]],
    [
      "honor / honour",
      [
        "honor",
        "honors",
        "honored",
        "honoring",
        "honour",
        "honours",
        "honoured",
        "honouring",
      ],
    ],
    ["theater / theatre", ["theater", "theaters", "theatre", "theatres"]],
  ];
  const spellingVariants = {
    observations: spellingFamilies
      .map(([family, forms]) => ({
        family,
        forms: forms
          .map((form) => ({ form, count: frequency.get(form) || 0 }))
          .filter(({ count }) => count),
      }))
      .filter(({ forms }) => forms.length),
    note: "Only spellings actually present in this small English variant list are shown. They may reflect quotations, source material or word meaning; no spelling correction, preferred locale or proficiency is inferred.",
  };
  const confidence =
    words.length < 150 ? "low" : words.length < 600 ? "moderate" : "high";
  return {
    confidence,
    confidenceReason:
      confidence === "low"
        ? "Fewer than 150 words; a short sample cannot establish a stable style."
        : confidence === "moderate"
          ? "150–599 words provide useful measurements, but patterns may change in a longer draft."
          : "At least 600 words support more stable descriptive measurements; this is not a model confidence score.",
    wordCount: words.length,
    sentenceCount: sents.length,
    paragraphCount: paras.length,
    averageSentenceWords: round(words.length / (sents.length || 1)),
    averageParagraphWords: round(words.length / (paras.length || 1)),
    vocabulary: {
      uniqueWords: frequency.size,
      lexicalDiversity: words.length
        ? round((frequency.size / words.length) * 100)
        : 0,
      averageWordLength: words.length
        ? round(words.reduce((sum, w) => sum + [...w].length, 0) / words.length)
        : 0,
      repeatedWords: [...frequency.entries()]
        .filter(([w, n]) => n > 1 && !STOP.has(w))
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
        .slice(0, 12)
        .map(([word, count]) => ({ word, count })),
    },
    transitions: TRANSITIONS.map((phrase) => ({
      phrase,
      count: (body.match(new RegExp(`\\b${phrase}\\b`, "gi")) || []).length,
    })).filter((item) => item.count > 0),
    repeatedExpressions,
    formality,
    paragraphOrganization,
    spellingVariants,
    person,
    punctuation: {
      commas: (body.match(/,/g) || []).length,
      semicolons: (body.match(/;/g) || []).length,
      colons: (body.match(/:/g) || []).length,
      dashes: (body.match(/[—–]/g) || []).length,
      questions: (body.match(/\?/g) || []).length,
      exclamations: (body.match(/!/g) || []).length,
      parentheses: (body.match(/\(/g) || []).length,
      contractions: contractionCount,
    },
    limitations: [
      "Measured offline from the supplied text; no language model was called.",
      "Sentence boundaries and headings use rules and can misread abbreviations or lists.",
      "Vocabulary diversity depends on sample length and topic.",
      "Expression, register and spelling cues use English rules and may reflect an assignment, quotations or source material.",
      "These measurements do not establish writing ability, authorship, factual accuracy, or whether text was AI-generated.",
    ],
  };
}

function headingName(line) {
  const md = /^\s*#{1,6}\s+(.+?)\s*#*\s*$/.exec(line);
  if (md) return md[1];
  const plain = line.trim();
  if (
    /^(introduction|background|methods?|results?|discussion|conclusion|limitations|literature review|recommendations|references|works cited|bibliography)$/i.test(
      plain,
    )
  )
    return plain;
  return null;
}

function outlineOf(text) {
  const lines = bodyText(text).split(/\r?\n/);
  const sections = [];
  let heading = "Opening";
  let content = [];
  const flush = () => {
    if (content.some((line) => line.trim())) {
      const value = content.join("\n").trim();
      sections.push({
        heading,
        summary: (sentences(value)[0] || value).slice(0, 220),
        words: countWords(value),
      });
    }
    content = [];
  };
  for (const line of lines) {
    const found = headingName(line);
    if (found) {
      flush();
      heading = found;
    } else content.push(line);
  }
  flush();
  if (sections.length === 1 && sections[0].heading === "Opening") {
    return paragraphs(text).map((p, index) => ({
      heading: `Paragraph ${index + 1}`,
      summary: (sentences(p)[0] || p).slice(0, 220),
      words: countWords(p),
    }));
  }
  return sections;
}

/** Offline prompts grounded only in visible draft features. No findings are invented. */
export function analyzeDraft(project) {
  const text = bodyText(project.workingText || "", project.countingRules);
  const words = tokens(text);
  const counts = new Map();
  words.forEach((word) => {
    const lower = word.toLocaleLowerCase();
    if (lower.length > 3 && !STOP.has(lower))
      counts.set(lower, (counts.get(lower) || 0) + 1);
  });
  const keywords = [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 5)
    .map(([word]) => word);
  const firstHeading = text
    .split(/\r?\n/)
    .map(headingName)
    .find((h) => h && !/^(introduction|background)$/i.test(h));
  const topic =
    firstHeading ||
    keywords.join(", ") ||
    "Add an existing draft to identify a topic";
  const sents = sentences(text);
  const thesis =
    sents.find((s) =>
      /\b(argue|argues|argument|thesis|should|suggests?|demonstrates?|contend|claim|because)\b/i.test(
        s,
      ),
    ) ||
    sents[0] ||
    "";
  const outline = outlineOf(text);
  const gaps = [];
  const unfinished = [];
  const researchQuestions = [];
  const suggestions = [];
  const add = (key, title, detail, question) =>
    suggestions.push({ id: `offline-${key}`, title, detail, question });
  if (!words.length) {
    add(
      "draft",
      "Start with your existing writing",
      "Import or paste a draft you have already started. Offline analysis needs your own text to offer useful next steps.",
      "Which paragraph of your assignment have you already written?",
    );
    return {
      topic,
      thesis: "",
      outline: [],
      gaps: ["No draft text is available."],
      researchQuestions: [],
      unfinished: [],
      suggestions,
      method: "Offline rule-based analysis",
    };
  }
  add(
    "thesis",
    "Check the central claim",
    `Possible thesis, selected by sentence rules: “${thesis.slice(0, 300)}” Confirm that this is the argument you intend to develop.`,
    "What position should the reader understand and why does it matter?",
  );
  const status = progress(project);
  if (status.remaining > 0) {
    gaps.push(
      `${status.remaining} more body words are needed to meet the ${status.minimum}-word minimum.`,
    );
    add(
      "development",
      "Develop an underexplained point",
      `You have ${status.current} body words. Choose one claim to expand with evidence, an explanation of the evidence, and a link back to your thesis.`,
      "Which current claim needs more explanation or an example?",
    );
  }
  if (status.maximum > 0 && status.current > status.maximum)
    gaps.push(
      `The draft is ${status.current - status.maximum} words above the configured maximum.`,
    );
  const includedSources = (project.sources || []).filter(
    (source) => source.included !== false,
  );
  if (!includedSources.length) {
    gaps.push("No research sources are saved for this project.");
    add(
      "evidence",
      "Find evidence for a specific claim",
      "Pick an important claim in your draft, search for primary or scholarly research, and read the source before using it. Search metadata alone does not verify the claim.",
      `Which published study or primary source could support or challenge your point about ${keywords.slice(0, 3).join(", ") || "this topic"}?`,
    );
  } else if (
    !/\([^)]*\b(?:19|20)\d{2}[a-z]?\b[^)]*\)|\[\d+(?:\s*[,–-]\s*\d+)*\]|\([^)]*\bp\.?\s*\d+[^)]*\)/i.test(
      text,
    )
  ) {
    gaps.push(
      "No common author–date or numeric citation markers were detected; check your required style.",
    );
    add(
      "citations",
      "Connect a source to a claim",
      "Review one saved source and state exactly what it supports. Add the required citation and explain its relevance in your own words.",
      "Which sentence needs a citation and what does the source actually show?",
    );
  }
  if (
    !/\b(in conclusion|to conclude|in summary|taken together|overall)\b|^\s*#{0,6}\s*conclusion\s*$/im.test(
      text,
    )
  ) {
    gaps.push(
      "No conventional conclusion heading or signal was detected; a conclusion may still be present.",
    );
    add(
      "conclusion",
      "Plan a conclusion with a purpose",
      "Revisit the claim, bring together the evidence already discussed, and explain an implication or limitation. Avoid introducing unsupported evidence in the ending.",
      "What can the reader reasonably conclude from the evidence you have presented?",
    );
  }
  if (outline.length > 1)
    add(
      "flow",
      "Check the connection between paragraphs",
      "Read the opening sentence of each paragraph in order. Each should advance the same argument or explain a deliberate change in direction.",
      "What relationship connects your last developed paragraph to the next one?",
    );
  const required = Array.isArray(project.requiredHeadings)
    ? project.requiredHeadings
    : String(project.requiredHeadings || "").split(/\r?\n|,/);
  for (const item of required.map((h) => String(h).trim()).filter(Boolean)) {
    if (
      !text
        .split(/\r?\n/)
        .some(
          (line) =>
            (headingName(line) || line.trim()).toLocaleLowerCase() ===
            item.toLocaleLowerCase(),
        )
    )
      gaps.push(`Required heading not found: ${item}.`);
  }
  if (
    !String(project.instructions || "").trim() &&
    !String(project.rubric || "").trim()
  ) {
    gaps.push("Assignment instructions and rubric have not been added.");
    add(
      "requirements",
      "Check the assignment requirements",
      "Add the assignment prompt or rubric so you can compare your argument, evidence, structure, and word target with the actual requirements.",
      "What must the finished assignment demonstrate?",
    );
  }
  const markers =
    text.match(
      /\bTODO\b[^\n]*|\bTBD\b[^\n]*|\[\s*(?:add|insert|expand|citation needed|finish)[^\]]*\]/gi,
    ) || [];
  unfinished.push(...markers.map((m) => m.slice(0, 240)));
  if (text.trim() && !/[.!?…”’"')\]]$/.test(text.trim()))
    unfinished.push(
      "The final line has no sentence-ending punctuation; check whether the thought is unfinished.",
    );
  researchQuestions.push(
    `What evidence supports the main claim about ${keywords.slice(0, 3).join(", ") || "your topic"}?`,
    "Which credible source offers a different explanation or a counterargument?",
    "What limitation should qualify the argument, and how does it affect the conclusion?",
  );
  return {
    topic,
    thesis,
    outline,
    gaps,
    researchQuestions,
    unfinished,
    suggestions,
    method: "Offline rule-based analysis",
  };
}

function nameParts(name) {
  const value = String(name || "").trim();
  if (value.includes(",")) {
    const [family, ...given] = value.split(",");
    return { family: family.trim(), given: given.join(" ").trim() };
  }
  const pieces = value.split(/\s+/);
  return pieces.length > 1
    ? { family: pieces.pop(), given: pieces.join(" ") }
    : { family: value, given: "" };
}
function initials(value) {
  return value
    .split(/[\s-]+/)
    .filter(Boolean)
    .map((p) => `${[...p][0].toLocaleUpperCase()}.`)
    .join(" ");
}
function joinNames(values, conjunction) {
  return values.length < 2
    ? values[0] || ""
    : values.length === 2
      ? values.join(` ${conjunction} `)
      : `${values.slice(0, -1).join(", ")}, ${conjunction} ${values.at(-1)}`;
}
function identifier(source) {
  const doi = String(source.doi || "")
    .trim()
    .replace(/^(?:https?:\/\/(?:dx\.)?doi\.org\/|doi:\s*)/i, "");
  return doi ? `https://doi.org/${doi}` : String(source.url || "").trim();
}

/** Draft citations from saved metadata. Missing details stay explicit; never invented. */
export function citation(source, style = "APA") {
  if (Array.isArray(source)) return citations(source, style);
  const authors = (Array.isArray(source.authors) ? source.authors : [])
    .map((name) => String(name).trim())
    .filter(Boolean);
  const names = authors.map((name) =>
    source.provider === "wikipedia" || source.corporateAuthor === true
      ? { family: name, given: "" }
      : nameParts(name),
  );
  const title = String(source.title || "").trim();
  const year = String(source.year || "").trim();
  const missing = [];
  if (!authors.length) missing.push("authors");
  if (!title) missing.push("title");
  if (!year) missing.push("year");
  const label = names.length
    ? names[0].family
    : `“${(title || "Untitled source").slice(0, 55)}”`;
  const date = year || "n.d.";
  const link = identifier(source);
  const ending = [link].filter(Boolean).join(" ");
  const safeTitle = title || "[Title missing]";
  const journal = String(source.journal || "").trim(),
    volume = String(source.volume || "").trim(),
    issue = String(source.issue || "").trim(),
    pages = String(source.pages || "").trim();
  if (source.type === "journal-article" && !journal) missing.push("journal");
  const mode = String(style).toLocaleUpperCase();
  let inText;
  let bibliography;
  if (mode === "MLA") {
    const authorLabel =
      names.length > 2
        ? `${label} et al.`
        : names.length === 2
          ? `${label} and ${names[1].family}`
          : label;
    inText = `(${authorLabel})`;
    const list =
      authors.length > 2
        ? `${names[0].family}${names[0].given ? `, ${names[0].given}` : ""}, et al.`
        : joinNames(
            names.map((n, i) =>
              i === 0
                ? `${n.family}${n.given ? `, ${n.given}` : ""}`
                : `${n.given ? `${n.given} ` : ""}${n.family}`,
            ),
            "and",
          );
    const publication = journal
      ? `${journal}${volume ? `, vol. ${volume}` : ""}${issue ? `, no. ${issue}` : ""}${year ? `, ${year}` : ""}${pages ? `, pp. ${pages}` : ""}. `
      : year
        ? `${year}. `
        : "[Date missing]. ";
    bibliography =
      `${list ? `${list}. ` : ""}“${safeTitle}.” ${publication}${ending}`.trim();
  } else if (mode === "CHICAGO") {
    const authorLabel =
      names.length > 3
        ? `${label} et al.`
        : joinNames(
            names.map((n) => n.family),
            "and",
          ) || label;
    inText = `(${authorLabel} ${date})`;
    const list = joinNames(
      names.map((n, i) =>
        i === 0
          ? `${n.family}${n.given ? `, ${n.given}` : ""}`
          : `${n.given ? `${n.given} ` : ""}${n.family}`,
      ),
      "and",
    );
    const publication = journal
      ? `${journal}${volume ? ` ${volume}` : ""}${issue ? ` (${issue})` : ""}${pages ? `: ${pages}` : ""}. `
      : "";
    bibliography =
      `${list ? `${list}. ` : ""}${date}. “${safeTitle}.” ${publication}${ending}`.trim();
  } else if (mode === "IEEE") {
    const referenceNumber =
      Number.isInteger(source.referenceNumber) && source.referenceNumber > 0
        ? source.referenceNumber
        : 1;
    inText = `[${referenceNumber}]`;
    const list = joinNames(
      names.map((n) => `${initials(n.given)} ${n.family}`.trim()),
      "and",
    );
    bibliography = `[${referenceNumber}] ${list ? `${list}, ` : ""}“${safeTitle},” ${date}.${ending ? ` ${ending}` : ""}`;
  } else {
    const authorLabel =
      names.length > 2
        ? `${label} et al.`
        : joinNames(
            names.map((n) => n.family),
            mode === "HARVARD" ? "and" : "&",
          ) || label;
    inText = `(${authorLabel}, ${date})`;
    const list = joinNames(
      names.map((n) => `${n.family}${n.given ? `, ${initials(n.given)}` : ""}`),
      mode === "HARVARD" ? "and" : "&",
    );
    const publication = journal
      ? ` ${journal}${volume ? `, ${volume}` : ""}${issue ? `(${issue})` : ""}${pages ? `, ${pages}` : ""}.`
      : "";
    bibliography = `${list ? `${list} ` : ""}(${date}). ${safeTitle}.${publication}${ending ? ` ${ending}` : ""}`;
  }
  return { inText, bibliography, missing };
}

export function citations(sources, style = "APA") {
  const unique = [
    ...new Map(
      sources.map((source, index) => [
        source.id ||
          source.doi ||
          source.url ||
          `${source.title}|${source.year}|${index}`,
        source,
      ]),
    ).values(),
  ];
  const entries = unique.map((source, index) =>
    citation({ ...source, referenceNumber: index + 1 }, style),
  );
  const numeric = String(style).toLocaleUpperCase() === "IEEE";
  return {
    inText: numeric
      ? entries.map((entry) => entry.inText).join(", ")
      : entries.length
        ? `(${entries.map((entry) => entry.inText.slice(1, -1)).join("; ")})`
        : "",
    bibliography: entries.map((entry) => entry.bibliography).join("\n\n"),
    missing: [...new Set(entries.flatMap((entry) => entry.missing))],
    entries,
  };
}

function string(value, fallback = "", limit = 4000000) {
  if (value == null) return fallback;
  if (typeof value !== "string")
    throw new TypeError("Expected a text field in the project.");
  if (value.length > limit)
    throw new TypeError(
      `A project text field exceeds the ${limit}-character limit.`,
    );
  return value;
}
function array(value, name, limit = 10000) {
  if (value == null) return [];
  if (!Array.isArray(value) || value.length > limit)
    throw new TypeError(`Invalid ${name} list in the project.`);
  return value;
}
function object(value, name) {
  if (!value || typeof value !== "object" || Array.isArray(value))
    throw new TypeError(`Invalid ${name} in the project.`);
  return value;
}
function number(value, fallback, label) {
  if (value == null || value === "") return fallback;
  const n = Number(value);
  if (!Number.isInteger(n) || n < 0 || n > 10000000)
    throw new TypeError(`Invalid ${label}; use a non-negative whole number.`);
  return n;
}
function stamp(value) {
  return typeof value === "string" && Number.isFinite(Date.parse(value))
    ? value
    : now();
}
function normalizeSource(value) {
  const source = object(value, "source");
  const authors =
    typeof source.authors === "string"
      ? source.authors
          .split(";")
          .map((a) => a.trim())
          .filter(Boolean)
      : array(source.authors, "source authors", 500).map((a) => string(a));
  return {
    id: string(source.id, id(), 200),
    title: string(source.title),
    authors,
    year: source.year == null ? "" : String(source.year),
    url: string(source.url),
    doi: string(source.doi),
    provider: string(source.provider),
    type: string(source.type, "article"),
    abstract: string(source.abstract),
    verification: ["metadata", "abstract", "unverified"].includes(
      source.verification,
    )
      ? source.verification
      : "unverified",
    included: source.included !== false,
    accessedAt: stamp(source.accessedAt),
    notes: string(source.notes),
    journal: string(source.journal),
    volume: string(source.volume),
    issue: string(source.issue),
    pages: string(source.pages),
    pmid: source.pmid == null ? "" : String(source.pmid),
    pageId: source.pageId == null ? "" : String(source.pageId),
    verifiedAt: source.verifiedAt ? stamp(source.verifiedAt) : "",
    relevance: Number.isFinite(source.relevance)
      ? Math.max(0, Math.min(1, source.relevance))
      : 0,
  };
}
function normalizeProfile(value, text) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const result = analyzeDNA(text),
    finite = (value, fallback) =>
      Number.isFinite(value) && value >= 0 && value < 10000000
        ? value
        : fallback;
  for (const key of [
    "wordCount",
    "sentenceCount",
    "paragraphCount",
    "averageSentenceWords",
    "averageParagraphWords",
  ])
    result[key] = finite(value[key], result[key]);
  if (["low", "moderate", "high"].includes(value.confidence))
    result.confidence = value.confidence;
  result.confidenceReason = string(
    value.confidenceReason,
    result.confidenceReason,
    2000,
  );
  result.userNotes = string(value.userNotes, "", 10000);
  if (value.vocabulary && typeof value.vocabulary === "object") {
    for (const key of ["uniqueWords", "lexicalDiversity", "averageWordLength"])
      result.vocabulary[key] = finite(
        value.vocabulary[key],
        result.vocabulary[key],
      );
    result.vocabulary.repeatedWords = array(
      value.vocabulary.repeatedWords,
      "profile vocabulary",
      100,
    ).map((w) => ({
      word: string(w.word, "", 200),
      count: number(w.count, 0, "word frequency"),
    }));
  }
  if (value.person && typeof value.person === "object") {
    for (const key of ["first", "second", "third"])
      result.person[key] = finite(value.person[key], result.person[key]);
    result.person.dominant = string(
      value.person.dominant,
      result.person.dominant,
      200,
    );
  }
  if (value.punctuation && typeof value.punctuation === "object")
    for (const key of Object.keys(result.punctuation))
      result.punctuation[key] = finite(
        value.punctuation[key],
        result.punctuation[key],
      );
  if (Array.isArray(value.transitions))
    result.transitions = array(
      value.transitions,
      "profile transitions",
      100,
    ).map((t) => ({
      phrase: string(t.phrase, "", 200),
      count: number(t.count, 0, "transition count"),
    }));
  const forms = (values, label, limit = 12) =>
    array(values, label, limit).map((value) => {
      const entry = object(value, label);
      return {
        form: string(entry.form, "", 100),
        count: number(entry.count, 0, label + " count"),
      };
    });
  const cues = (values, label) =>
    array(values, label, 16).map((value) => {
      const entry = object(value, label);
      return {
        phrase: string(entry.phrase, "", 200),
        count: number(entry.count, 0, label + " count"),
      };
    });
  if (value.repeatedExpressions != null)
    result.repeatedExpressions = array(
      value.repeatedExpressions,
      "repeated expressions",
      12,
    ).map((value) => {
      const entry = object(value, "repeated expression");
      if (![2, 3].includes(entry.words))
        throw new TypeError(
          "A repeated expression must contain two or three words.",
        );
      return {
        phrase: string(entry.phrase, "", 200),
        count: number(entry.count, 0, "expression count"),
        words: entry.words,
      };
    });
  if (value.formality != null) {
    const entry = object(value.formality, "profile language cues");
    const contractions =
      entry.contractions == null
        ? result.formality.contractions
        : object(entry.contractions, "profile contractions");
    const rate =
      contractions.per100Words ?? result.formality.contractions.per100Words;
    if (!Number.isFinite(rate) || rate < 0 || rate > 100)
      throw new TypeError("Invalid contraction rate in the profile.");
    result.formality = {
      contractions: {
        count: number(contractions.count, 0, "contraction count"),
        per100Words: rate,
        forms: forms(contractions.forms, "contraction forms"),
      },
      academicCues: cues(entry.academicCues, "academic language cues"),
      conversationalCues: cues(
        entry.conversationalCues,
        "conversational language cues",
      ),
      note: string(entry.note, result.formality.note, 1000),
    };
  }
  if (value.spellingVariants != null) {
    const entry = object(
      value.spellingVariants,
      "profile spelling observations",
    );
    result.spellingVariants = {
      observations: array(
        entry.observations,
        "spelling variant families",
        10,
      ).map((value) => {
        const item = object(value, "spelling variant family");
        return {
          family: string(item.family, "", 200),
          forms: forms(item.forms, "observed spelling forms"),
        };
      }),
      note: string(entry.note, result.spellingVariants.note, 1000),
    };
  }
  if (value.paragraphOrganization != null) {
    const entry = object(
      value.paragraphOrganization,
      "profile paragraph organization",
    );
    result.paragraphOrganization = {
      totalParagraphs: number(
        entry.totalParagraphs,
        0,
        "profile paragraph total",
      ),
      paragraphs: array(entry.paragraphs, "profile paragraph previews", 12).map(
        (value) => {
          const item = object(value, "profile paragraph preview");
          return {
            index: number(item.index, 0, "paragraph index"),
            wordCount: number(item.wordCount, 0, "paragraph words"),
            sentenceCount: number(item.sentenceCount, 0, "paragraph sentences"),
            opening: string(item.opening, "", 180),
            possibleRole: string(item.possibleRole, "", 120),
            evidence: string(item.evidence, "", 300),
          };
        },
      ),
      summary: string(
        entry.summary,
        result.paragraphOrganization.summary,
        1000,
      ),
      note: string(entry.note, result.paragraphOrganization.note, 1000),
      truncated: entry.truncated === true,
    };
  }
  return result;
}
function normalizeBrief(value) {
  const b = object(value, "research brief");
  const safeURL = (value) => {
    const text = string(value);
    try {
      const u = new URL(text);
      return ["http:", "https:"].includes(u.protocol) &&
        !u.username &&
        !u.password
        ? u.href
        : "";
    } catch {
      return "";
    }
  };
  return {
    text: string(b.text),
    query: string(b.query, "", 250),
    model: string(b.model, "", 200),
    createdAt: stamp(b.createdAt),
    completed: b.completed === true,
    webSearchUsed: b.webSearchUsed === true,
    warnings: array(b.warnings, "research notes", 100).map((w) => string(w)),
    sources: array(b.sources, "brief sources", 200)
      .map((s) => ({ title: string(s.title), url: safeURL(s.url) }))
      .filter((s) => s.url),
    annotations: array(b.annotations, "brief citations", 500)
      .map((a) => ({
        start: number(a.start, 0, "citation start"),
        end: number(a.end, 0, "citation end"),
        url: safeURL(a.url),
        title: string(a.title),
      }))
      .filter((a) => a.url),
  };
}
function normalizeContribution(value, text) {
  const item = object(value, "contribution");
  const start = number(item.start, 0, "contribution start");
  const end = number(item.end, start, "contribution end");
  if (end < start || end > text.length)
    throw new TypeError("A contribution range is outside the working draft.");
  return {
    id: string(item.id, id(), 200),
    proposalId: string(item.proposalId, "", 200),
    start,
    end,
    text: string(item.text, text.slice(start, end)),
    sourceIds: array(item.sourceIds, "contribution sources").map((s) =>
      string(s),
    ),
    acceptedAt: stamp(item.acceptedAt),
  };
}

/** Normalize explicit fields only, including legacy {draft} / {text} exports. */
export function validateProject(value) {
  const source = object(value, "project");
  if (
    source.schemaVersion != null &&
    (!Number.isInteger(source.schemaVersion) ||
      source.schemaVersion > SCHEMA_VERSION ||
      source.schemaVersion < 0)
  )
    throw new TypeError("This project uses an unsupported schema version.");
  const base = createProject(
    string(source.title, "Untitled paper"),
    string(source.originalText ?? source.draft ?? source.text, ""),
  );
  const working = string(source.workingText, base.originalText);
  const minimum = number(source.minimum, 1000, "minimum word count");
  const maximum = number(source.maximum, 0, "maximum word count");
  if (maximum && maximum < minimum)
    throw new TypeError("The maximum word count cannot be below the minimum.");
  const rules =
    source.countingRules == null
      ? {}
      : object(source.countingRules, "counting rules");
  const proposals = array(source.proposals, "proposals").map((value) => {
    const item = object(value, "proposal");
    return {
      id: string(item.id, id(), 200),
      text: string(item.text),
      status: ["pending", "accepted", "rejected"].includes(item.status)
        ? item.status
        : "pending",
      sourceIds: array(item.sourceIds, "proposal sources").map((s) =>
        string(s),
      ),
      warnings: array(item.warnings, "proposal warnings").map((s) => string(s)),
      createdAt: stamp(item.createdAt),
      insertion:
        item.insertion?.mode === "inline"
          ? {
              mode: "inline",
              position: number(
                item.insertion.position,
                0,
                "continuation position",
              ),
              anchor: string(item.insertion.anchor, "", 160),
            }
          : null,
    };
  });
  if (new Set(proposals.map((p) => p.id)).size !== proposals.length)
    throw new TypeError("Proposal IDs must be unique.");
  const revisions = array(source.revisions, "revisions", 1000).map((value) => {
    const item = object(value, "revision");
    const text = string(item.workingText ?? item.text, "");
    const statuses =
      item.proposalStatuses &&
      typeof item.proposalStatuses === "object" &&
      !Array.isArray(item.proposalStatuses)
        ? Object.fromEntries(
            Object.entries(item.proposalStatuses).filter(
              ([key, status]) =>
                key !== "__proto__" &&
                ["pending", "accepted", "rejected"].includes(status),
            ),
          )
        : {};
    return {
      id: string(item.id, id(), 200),
      label: string(item.label, "Saved checkpoint"),
      kind: string(item.kind, "snapshot"),
      createdAt: stamp(item.createdAt),
      workingText: text,
      contributions: array(item.contributions, "revision contributions").map(
        (c) => normalizeContribution(c, text),
      ),
      proposalStatuses: statuses,
    };
  });
  const result = {
    ...base,
    id: string(source.id, base.id, 200),
    workingText: working,
    instructions: string(source.instructions),
    rubric: string(source.rubric),
    academicLevel: string(source.academicLevel, "College"),
    requiredHeadings: Array.isArray(source.requiredHeadings)
      ? source.requiredHeadings.map((h) => string(h)).join("\n")
      : string(source.requiredHeadings),
    minimum,
    maximum,
    voice: string(source.voice, "original"),
    citationStyle: string(source.citationStyle, "APA"),
    countingRules: {
      excludeReferences: rules.excludeReferences !== false,
      excludeTitle: rules.excludeTitle !== false,
    },
    exportFormatting: {
      fontFamily: ["Times New Roman", "Georgia", "Arial"].includes(
        source.exportFormatting?.fontFamily,
      )
        ? source.exportFormatting.fontFamily
        : "Times New Roman",
      fontSize:
        Number(source.exportFormatting?.fontSize) >= 10 &&
        Number(source.exportFormatting?.fontSize) <= 16
          ? Number(source.exportFormatting.fontSize)
          : 12,
      lineSpacing: [1, 1.5, 2].includes(
        Number(source.exportFormatting?.lineSpacing),
      )
        ? Number(source.exportFormatting.lineSpacing)
        : 1.5,
    },
    sources: array(source.sources, "sources").map(normalizeSource),
    citedSourceIds: array(source.citedSourceIds, "cited sources").map((s) =>
      string(s, "", 200),
    ),
    proposals,
    revisions,
    contributions: array(source.contributions, "contributions").map((c) =>
      normalizeContribution(c, working),
    ),
    profile: normalizeProfile(source.profile, base.originalText),
    researchBriefs: array(source.researchBriefs, "research briefs", 100).map(
      normalizeBrief,
    ),
    helperNotes: string(source.helperNotes),
    createdAt: stamp(source.createdAt),
    updatedAt: stamp(source.updatedAt),
  };
  return result;
}

export function loadProject(serializedOrObject) {
  let value = serializedOrObject;
  if (typeof value === "string") {
    try {
      value = JSON.parse(value);
    } catch {
      throw new TypeError("This file is not valid project JSON.");
    }
  }
  return validateProject(value);
}

function record(project, label, kind) {
  return {
    id: id(),
    label,
    kind,
    createdAt: now(),
    workingText: project.workingText,
    contributions: clone(project.contributions || []),
    proposalStatuses: Object.fromEntries(
      (project.proposals || []).map((p) => [p.id, p.status]),
    ),
  };
}

export function snapshot(project, label = "Saved checkpoint") {
  const result = validateProject(project);
  result.revisions.push(record(result, String(label), "snapshot"));
  result.updatedAt = now();
  return result;
}

/** Adds text only after a pending proposal is explicitly accepted by the caller. */
export function acceptProposal(project, proposalId) {
  const result = validateProject(project);
  const proposal = result.proposals.find((p) => p.id === proposalId);
  if (!proposal) throw new TypeError("That proposal could not be found.");
  if (proposal.status !== "pending")
    throw new TypeError("Only a pending proposal can be accepted.");
  const text = proposal.text.trim();
  if (!text) throw new TypeError("An empty proposal cannot be accepted.");
  result.revisions.push(
    record(result, "Before accepting a continuation", "accept"),
  );
  const match = REFERENCES.exec(result.workingText);
  const inline = proposal.insertion?.mode === "inline";
  const insertAt = inline
    ? proposal.insertion.position
    : match
      ? match.index
      : result.workingText.length;
  if (
    inline &&
    (!proposal.insertion.anchor ||
      result.workingText.slice(
        Math.max(0, insertAt - proposal.insertion.anchor.length),
        insertAt,
      ) !== proposal.insertion.anchor)
  )
    throw new TypeError(
      "The draft changed at the proposed continuation point. Regenerate this section or choose a new paragraph before accepting.",
    );
  const prefix = result.workingText.slice(0, insertAt);
  const suffix = result.workingText.slice(insertAt);
  const before = inline
    ? /\s$/.test(prefix) || /^[,.;:!?]/.test(text)
      ? ""
      : " "
    : !prefix || /\n\s*\n$/.test(prefix)
      ? ""
      : /\n$/.test(prefix)
        ? "\n"
        : "\n\n";
  const after = suffix && !/^\s*\n/.test(suffix) ? "\n\n" : "";
  const inserted = before + text + after;
  result.workingText = prefix + inserted + suffix;
  result.contributions.forEach((c) => {
    if (c.start >= insertAt) {
      c.start += inserted.length;
      c.end += inserted.length;
    } else if (c.end > insertAt) c.end += inserted.length;
  });
  result.contributions.push({
    id: id(),
    proposalId: proposal.id,
    start: insertAt + before.length,
    end: insertAt + before.length + text.length,
    text,
    sourceIds: [...proposal.sourceIds],
    acceptedAt: now(),
  });
  proposal.status = "accepted";
  result.updatedAt = now();
  return result;
}

export function rejectProposal(project, proposalId) {
  const result = validateProject(project);
  const proposal = result.proposals.find((p) => p.id === proposalId);
  if (!proposal) throw new TypeError("That proposal could not be found.");
  if (proposal.status !== "pending")
    throw new TypeError("Only a pending proposal can be rejected.");
  result.revisions.push(
    record(result, "Before rejecting a continuation", "reject"),
  );
  proposal.status = "rejected";
  result.updatedAt = now();
  return result;
}

export function revertRevision(project, revisionId) {
  const result = validateProject(project);
  const target = result.revisions.find((r) => r.id === revisionId);
  if (!target) throw new TypeError("That revision could not be found.");
  result.revisions.push(
    record(result, `Before restoring: ${target.label}`, "restore"),
  );
  result.workingText = target.workingText;
  result.contributions = clone(target.contributions);
  result.proposals.forEach((p) => {
    p.status =
      target.proposalStatuses[p.id] ||
      (p.status === "accepted" ? "pending" : p.status);
  });
  result.updatedAt = now();
  return result;
}

/** Reconcile provenance after a direct editor replacement using the smallest changed span. */
export function updateWorkingText(project, nextText) {
  const result = validateProject(project);
  const next = string(nextText);
  const old = result.workingText;
  let start = 0;
  while (
    start < old.length &&
    start < next.length &&
    old[start] === next[start]
  )
    start++;
  let oldEnd = old.length,
    nextEnd = next.length;
  while (
    oldEnd > start &&
    nextEnd > start &&
    old[oldEnd - 1] === next[nextEnd - 1]
  ) {
    oldEnd--;
    nextEnd--;
  }
  const delta = nextEnd - oldEnd;
  result.contributions = result.contributions.flatMap((c) => {
    if (c.end <= start) return [c];
    if (c.start >= oldEnd)
      return [{ ...c, start: c.start + delta, end: c.end + delta }];
    // Keep only untouched portions as generated provenance; edited content becomes user writing.
    const spans = [];
    if (c.start < start)
      spans.push({ ...c, end: start, text: next.slice(c.start, start) });
    if (c.end > oldEnd)
      spans.push({
        ...c,
        id: id(),
        start: nextEnd,
        end: c.end + delta,
        text: next.slice(nextEnd, c.end + delta),
      });
    return spans;
  });
  result.workingText = next;
  result.updatedAt = now();
  return result;
}
