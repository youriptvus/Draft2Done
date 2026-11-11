/** Recommendations select only identifiers returned by the connected server. */
export const MODEL_GUIDANCE_DATE = "2026-11-10";
export const TESTED_LOCAL_WRITING_MODEL = "llama3:8b-instruct-q4_K_M";
const researchOrder = [
  {
    slug: "gpt-6-astra",
    role: "Deeper research",
    tradeoff:
      "Highest research capability among these recognized choices; expect greater latency and usage.",
  },
  {
    slug: "gpt-6.1-sol",
    role: "Balanced research",
    tradeoff: "Balances research capability, responsiveness and usage.",
  },
  {
    slug: "gpt-6.1-terra",
    role: "Natural writing",
    tradeoff:
      "A writing-focused choice for clear, natural explanations; deeper research may benefit from a stronger research model.",
  },
  {
    slug: "gpt-6-luna",
    role: "Efficient research",
    tradeoff:
      "Prioritizes speed and efficient usage; complex evidence synthesis may need closer review.",
  },
];
function eligible(model) {
  return (
    model &&
    model.disabled !== true &&
    model.eligible !== false &&
    model.available !== false &&
    model.visibility !== "hidden"
  );
}
export function recommendedResearchModel(catalog = []) {
  if (!Array.isArray(catalog)) return null;
  for (const choice of researchOrder) {
    const match = catalog.find(
      (model) => eligible(model) && model.slug === choice.slug,
    );
    if (match)
      return { ...choice, slug: match.slug, asOf: MODEL_GUIDANCE_DATE };
  }
  return null;
}
export function researchModelGuidance(slug) {
  const choice = researchOrder.find((choice) => choice.slug === slug);
  return choice ? { ...choice, asOf: MODEL_GUIDANCE_DATE } : null;
}
export function localModelNames(catalog = []) {
  if (!Array.isArray(catalog)) return [];
  return catalog
    .map((model) =>
      typeof model === "string" ? model : model?.id || model?.name,
    )
    .filter((name) => typeof name === "string" && name.trim());
}
export function preferredLocalModel(catalog = []) {
  const names = localModelNames(catalog);
  if (names.includes(TESTED_LOCAL_WRITING_MODEL))
    return TESTED_LOCAL_WRITING_MODEL;
  const general = names.filter(
    (name) => !/code|coder|embed|rerank/i.test(name),
  );
  return (
    general.find((name) => /instruct|chat/i.test(name)) ||
    general[0] ||
    names[0] ||
    ""
  );
}
export function localModelGuidance(catalog = []) {
  const names = localModelNames(catalog);
  return {
    recommended: preferredLocalModel(catalog),
    testedModelListed: names.includes(TESTED_LOCAL_WRITING_MODEL),
    testedModel: TESTED_LOCAL_WRITING_MODEL,
    asOf: MODEL_GUIDANCE_DATE,
    evidence:
      "On the development PC (about 8 GB GPU VRAM and 7 GB usable system RAM), the existing llama3 instruct model passed five fictional writing samples. Its prose was more suitable than the installed qwen coding model. These checks do not establish a universal best model or guarantee factual accuracy.",
  };
}
