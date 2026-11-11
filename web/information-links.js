/**
 * Local research suggestions for a separate reading view. This module never
 * edits a project, fetches a resource, invents a destination, or emits HTML.
 * Positions use JavaScript UTF-16 offsets, just like slice() and the editor.
 */

const CONCEPTS = [
  "climate change",
  "global warming",
  "greenhouse effect",
  "carbon cycle",
  "carbon dioxide",
  "renewable energy",
  "biodiversity",
  "ecosystem",
  "photosynthesis",
  "cellular respiration",
  "natural selection",
  "evolution",
  "gene expression",
  "genetic engineering",
  "genome",
  "DNA",
  "RNA",
  "CRISPR",
  "microbiology",
  "epidemiology",
  "neuroplasticity",
  "neuroscience",
  "quantum mechanics",
  "general relativity",
  "thermodynamics",
  "entropy",
  "electromagnetism",
  "plate tectonics",
  "gravitational waves",
  "black hole",
  "artificial intelligence",
  "machine learning",
  "deep learning",
  "large language model",
  "neural network",
  "algorithmic bias",
  "data privacy",
  "cybersecurity",
  "cryptography",
  "AI",
  "COVID-19",
  "cognitive bias",
  "confirmation bias",
  "cognitive dissonance",
  "social identity",
  "classical conditioning",
  "operant conditioning",
  "working memory",
  "social learning theory",
  "attachment theory",
  "statistical significance",
  "confidence interval",
  "standard deviation",
  "randomized controlled trial",
  "systematic review",
  "meta-analysis",
  "peer review",
  "correlation",
  "causation",
  "qualitative research",
  "quantitative research",
  "ethnography",
  "informed consent",
  "supply and demand",
  "opportunity cost",
  "inflation",
  "monetary policy",
  "fiscal policy",
  "gross domestic product",
  "income inequality",
  "social contract",
  "separation of powers",
  "constitutional law",
  "human rights",
  "utilitarianism",
  "deontology",
  "existentialism",
  "Industrial Revolution",
  "French Revolution",
  "American Revolution",
  "Civil Rights Movement",
  "World War I",
  "World War II",
  "Cold War",
  "Great Depression",
  "Renaissance",
  "Enlightenment",
  "Holocaust",
  "United Nations",
  "World Health Organization",
  "European Union",
  "African Union",
  "World Bank",
  "International Monetary Fund",
  "National Aeronautics and Space Administration",
  "NASA",
  "UNESCO",
  "United States",
  "United Kingdom",
  "South Africa",
  "New Zealand",
  "Middle East",
  "Silicon Valley",
  "European Parliament",
  "Albert Einstein",
  "Marie Curie",
  "Charles Darwin",
  "Isaac Newton",
  "Ada Lovelace",
  "Alan Turing",
  "Nelson Mandela",
  "Rosa Parks",
  "Martin Luther King Jr.",
  "W. E. B. Du Bois",
  "René Descartes",
];
const STOP = new Set(
  "a an and are as at be been being but by can could did do does for from had has have how i if in into is it its may might more most my no not of on or our should so some than that the their them there these they this those to was we were what when where which who why will with would you your study studies research paper introduction conclusion references bibliography works cited".split(
    " ",
  ),
);
const LEADING =
  /^(?:A|An|And|As|At|But|For|From|In|It|On|Our|The|Their|These|This|Those|To|We|With)\s+/u;
const WORDS = /[\p{L}\p{N}]+(?:['’\-][\p{L}\p{N}]+)*/gu;
const MAX_TEXT = 500_000;
const MAX_CANDIDATES = 2_000;

function normalized(value) {
  return String(value ?? "")
    .normalize("NFKC")
    .toLocaleLowerCase("en")
    .replace(/\s+/gu, " ")
    .trim();
}

function terms(value) {
  const words = normalized(value).match(WORDS) || [];
  return [
    ...new Set(words.filter((word) => word.length > 1 && !STOP.has(word))),
  ];
}

function bounded(value, fallback, maximum) {
  return Number.isFinite(value)
    ? Math.max(0, Math.min(maximum, Math.floor(value)))
    : fallback;
}

function regexEscape(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * A syntactic public-address guard, not a DNS lookup or a claim of safety,
 * credibility or current availability. Native openExternal still checks URLs.
 */
export function safeInformationUrl(value) {
  if (
    typeof value !== "string" ||
    value.length > 4096 ||
    /[\u0000-\u0020\u007f\\]/u.test(value)
  )
    return null;
  try {
    const url = new URL(value);
    if (
      !["https:", "http:"].includes(url.protocol) ||
      url.username ||
      url.password
    )
      return null;
    const host = url.hostname.toLowerCase().replace(/\.$/u, "");
    if (
      !host ||
      /(?:^|\.)(?:localhost|local|internal|intranet|home|lan|onion|invalid|test)$/u.test(
        host,
      )
    )
      return null;
    if (host.startsWith("[")) {
      // Accept global-unicast literals only; reject local/mapped/documentation.
      if (
        !/^\[[23][0-9a-f]{3}:/u.test(host) ||
        /^\[2001:(?:db8|0):/u.test(host)
      )
        return null;
    } else if (/^\d+(?:\.\d+){3}$/u.test(host)) {
      const [a, b, c] = host.split(".").map(Number);
      if (
        a === 0 ||
        a === 10 ||
        a === 127 ||
        a >= 224 ||
        (a === 100 && b >= 64 && b <= 127) ||
        (a === 169 && b === 254) ||
        (a === 172 && b >= 16 && b <= 31) ||
        (a === 192 && (b === 168 || b === 0 || (b === 88 && c === 99))) ||
        (a === 198 && (b === 18 || b === 19 || (b === 51 && c === 100))) ||
        (a === 203 && b === 0 && c === 113)
      )
        return null;
    } else if (
      host.length > 253 ||
      !host.includes(".") ||
      !host
        .split(".")
        .every((part) =>
          /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/u.test(part),
        ) ||
      !/^(?:[a-z]{2,63}|xn--[a-z0-9-]+)$/u.test(host.split(".").at(-1))
    )
      return null;
    return url.href;
  } catch {
    return null;
  }
}

function excludedRanges(text) {
  const result = [];
  // URLs, email addresses, code and formal citation markers are not concepts.
  const pattern =
    /```[\s\S]*?(?:```|$)|`[^`\n]*`|https?:\/\/[^\s<>]+|\b[^\s@]+@[^\s@]+\.[^\s@]+|\[\[(?:cite|source):[^\]]*\]\]/giu;
  for (const match of text.matchAll(pattern))
    result.push([match.index, match.index + match[0].length]);
  const references =
    /(?:^|\n)\s*(?:#{1,6}\s*)?(?:references|bibliography|works cited)\s*:?\s*(?:\n|$)/iu.exec(
      text,
    );
  if (references) result.push([references.index, text.length]);
  return result.sort((a, b) => a[0] - b[0]);
}

/** Deterministic suggestions; detection is deliberately conservative, not NLP. */
export function informationLinks(text, options = {}) {
  text = String(text ?? "");
  const maxLinks = bounded(options.maxLinks, 12, 40);
  const maxPerPassage = bounded(options.maxPerPassage, 3, 5);
  if (!text || !maxLinks || !maxPerPassage) return [];
  // Cap analysis work for very large imports; the reader still shows all text.
  const scan = text.slice(0, MAX_TEXT),
    candidates = [],
    excluded = excludedRanges(scan);
  const add = (start, label, kind, priority) => {
    const end = start + label.length;
    if (
      candidates.length >= MAX_CANDIDATES ||
      !label ||
      label.length > 120 ||
      excluded.some(([a, b]) => start < b && end > a)
    )
      return;
    candidates.push({
      start,
      end,
      label,
      query: label.slice(0, 240),
      kind,
      priority,
    });
  };
  const phrases = [...CONCEPTS];
  for (const source of (Array.isArray(options.sources)
    ? options.sources
    : []
  ).slice(0, 200)) {
    if (
      !["abstract", "metadata"].includes(source?.verification) ||
      !safeInformationUrl(source.url)
    )
      continue;
    const title = String(source.title || "").trim();
    if (
      title.length >= 5 &&
      title.length <= 100 &&
      (title.match(WORDS) || []).length <= 10 &&
      terms(title).length
    )
      phrases.push(title);
  }
  for (const brief of (Array.isArray(options.researchBriefs)
    ? options.researchBriefs
    : []
  ).slice(-100)) {
    const query = String(brief?.query || "").trim();
    if (
      brief?.completed === true &&
      brief?.webSearchUsed === true &&
      query.length >= 5 &&
      query.length <= 80 &&
      (query.match(WORDS) || []).length <= 6 &&
      !/^(?:how|what|why|when|where|can|does|is|are)\b/iu.test(query)
    )
      phrases.push(query);
  }
  const phrasePattern = new RegExp(
    `(^|[^\\p{L}\\p{N}])(${[...new Set(phrases)]
      .sort((a, b) => b.length - a.length)
      .map(regexEscape)
      .join("|")})(?![\\p{L}\\p{N}])`,
    "giu",
  );
  for (const match of scan.matchAll(phrasePattern))
    add(match.index + match[1].length, match[2], "concept", 3);
  // Multiword names/places/organizations, including Unicode names and particles.
  const named =
    /(?<![\p{L}\p{N}])\p{Lu}[\p{L}\p{M}'’\-]+(?:[ \t]+(?:(?:of|the|de|van|von|da|del)[ \t]+)?\p{Lu}[\p{L}\p{M}'’\-]+){1,4}/gu;
  for (const match of scan.matchAll(named)) {
    const label = match[0].replace(LEADING, "");
    if ((label.match(WORDS) || []).length >= 2 && terms(label).length >= 2)
      add(match.index + match[0].length - label.length, label, "name", 2);
  }
  const technical =
    /(?<![\p{L}\p{N}])[\p{L}\p{M}]{2,}(?:ology|omics|ogenesis|synthesis|photonics|informatics)(?![\p{L}\p{N}])|(?<![\p{L}\p{N}])[A-Z]{2,7}(?:-[0-9]{1,4})?(?![\p{L}\p{N}])/gu;
  for (const match of scan.matchAll(technical)) {
    if (
      !STOP.has(normalized(match[0])) &&
      !/^(?:AM|PM|OK|II|III|IV|VI|VII|VIII|IX|XI|XII)$/u.test(match[0])
    )
      add(match.index, match[0], "term", 1);
  }
  // Resolve overlaps before bounding by passage. Prefer complete known concepts.
  const resolved = [];
  for (const candidate of candidates.sort(
    (a, b) =>
      b.priority - a.priority ||
      b.end - b.start - (a.end - a.start) ||
      a.start - b.start,
  )) {
    if (
      !resolved.some(
        (old) => candidate.start < old.end && candidate.end > old.start,
      )
    )
      resolved.push(candidate);
  }
  const result = [],
    lastOccurrence = new Map(),
    passageCount = new Map();
  const repeatDistance = bounded(options.minRepeatDistance, 600, 10_000);
  let passage = 0,
    nextBreak = /\n\s*\n/gu,
    boundary = nextBreak.exec(scan);
  for (const candidate of resolved.sort((a, b) => a.start - b.start)) {
    while (boundary && boundary.index < candidate.start) {
      passage++;
      boundary = nextBreak.exec(scan);
    }
    // Long single paragraphs are also bounded in 600-character reading passages.
    const bucket = `${passage}:${Math.floor(candidate.start / 600)}`;
    const key = normalized(candidate.label);
    if (
      (passageCount.get(bucket) || 0) >= maxPerPassage ||
      candidate.start - (lastOccurrence.get(key) ?? -Infinity) < repeatDistance
    )
      continue;
    const { priority, ...link } = candidate;
    result.push(link);
    lastOccurrence.set(key, candidate.start);
    passageCount.set(bucket, (passageCount.get(bucket) || 0) + 1);
    if (result.length === maxLinks) break;
  }
  return result;
}

/** Ignore stale/malformed ranges rather than ever replacing a different phrase. */
export function informationParts(text, links = []) {
  text = String(text ?? "");
  const parts = [];
  let position = 0;
  for (const link of (Array.isArray(links) ? [...links] : []).sort(
    (a, b) => (a?.start ?? 0) - (b?.start ?? 0),
  )) {
    if (
      !Number.isInteger(link?.start) ||
      !Number.isInteger(link?.end) ||
      link.start < position ||
      link.end <= link.start ||
      link.end > text.length ||
      text.slice(link.start, link.end) !== link.label
    )
      continue;
    if (position < link.start)
      parts.push({ text: text.slice(position, link.start) });
    parts.push({ text: text.slice(link.start, link.end), link });
    position = link.end;
  }
  if (position < text.length || !parts.length)
    parts.push({ text: text.slice(position) });
  return parts;
}

function relevance(label, value) {
  const key = normalized(label),
    haystack = normalized(value);
  if (!key || !haystack) return 0;
  // Whole-token matching prevents e.g. AI matching "said" or evolution matching
  // "revolution". NFKC here changes only matching, never original text/offsets.
  if (
    new RegExp(
      `(^|[^\\p{L}\\p{N}])${regexEscape(key)}(?![\\p{L}\\p{N}])`,
      "u",
    ).test(haystack)
  )
    return 4;
  const wanted = terms(label),
    available = new Set(terms(value));
  if (wanted.length < 2) return 0;
  const shared = wanted.filter((word) => available.has(word)).length;
  return shared >= Math.max(2, Math.ceil(wanted.length * 0.8))
    ? (shared / wanted.length) * 3
    : 0;
}

function preference(url, provider) {
  const host = new URL(url).hostname;
  if (/(?:^|\.)wikipedia\.org$/iu.test(host)) return 3;
  if (/(?:\.edu|\.gov|\.ac\.[a-z]{2}|\.gov\.[a-z]{2})$/iu.test(host)) return 3;
  if (
    ["crossref", "pubmed"].includes(provider) ||
    /(?:^|\.)(?:doi\.org|ncbi\.nlm\.nih\.gov)$/iu.test(host)
  )
    return 2;
  return 0;
}

/** Related *saved* evidence. No network validation, scraped pages or guessed URL. */
export function relatedInformation(concept, options = {}) {
  const label = String(
    typeof concept === "string"
      ? concept
      : concept?.label || concept?.query || "",
  )
    .slice(0, 240)
    .trim();
  const limit = bounded(options.limit, 5, 10);
  if (!label || !limit) return [];
  const candidates = [];
  const add = (
    record,
    value,
    kind,
    verification,
    provider = "",
    detail = "",
  ) => {
    const url = safeInformationUrl(record?.url),
      score = relevance(label, value);
    if (!url || !score) return;
    candidates.push({
      title: String(record.title || url).slice(0, 500),
      url,
      kind,
      verification,
      detail,
      score: score * 10 + preference(url, provider),
    });
  };
  for (const source of (Array.isArray(options.sources)
    ? options.sources
    : []
  ).slice(0, 200)) {
    if (!["abstract", "metadata"].includes(source?.verification)) continue;
    add(
      source,
      `${source.title || ""}\n${String(source.abstract || "").slice(0, 8000)}`,
      "saved-source",
      source.verification,
      source.provider,
      source.verification === "abstract"
        ? "Saved record and abstract or extract; review the source before relying on a claim."
        : "Saved bibliographic record; no abstract has been reviewed.",
    );
  }
  for (const brief of (Array.isArray(options.researchBriefs)
    ? options.researchBriefs
    : []
  ).slice(-100)) {
    if (brief?.completed !== true || brief?.webSearchUsed !== true) continue;
    const text = String(brief.text || ""),
      annotations = (
        Array.isArray(brief.annotations) ? brief.annotations : []
      ).slice(0, 500);
    const records = [
      ...(Array.isArray(brief.sources) ? brief.sources : []).slice(0, 200),
      ...annotations,
    ];
    for (const source of records) {
      const url = safeInformationUrl(source?.url);
      if (!url) continue;
      // A query match alone does not make every page in a broad brief relevant.
      const evidence = annotations
        .filter(
          (a) =>
            safeInformationUrl(a?.url) === url &&
            Number.isInteger(a.start) &&
            Number.isInteger(a.end) &&
            a.start >= 0 &&
            a.end > a.start &&
            a.end <= text.length,
        )
        .map(
          (a) =>
            `${a.title || ""}\n${text.slice(Math.max(0, a.start - 140), Math.min(text.length, a.end + 40))}`,
        )
        .join("\n");
      add(
        source,
        `${source.title || ""}\n${evidence}`,
        "research-brief",
        "web-returned",
        "",
        `Returned in a completed web research brief${brief.query ? ` about “${String(brief.query).slice(0, 240)}”` : ""}; not independently verified.`,
      );
    }
  }
  const seen = new Set();
  return candidates
    .sort((a, b) => b.score - a.score || a.title.localeCompare(b.title))
    .filter((item) => {
      const key = new URL(item.url);
      key.hash = "";
      if (seen.has(key.href)) return false;
      seen.add(key.href);
      return true;
    })
    .slice(0, limit)
    .map(({ score, ...item }) => item);
}
