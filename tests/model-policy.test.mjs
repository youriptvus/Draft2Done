import test from "node:test";
import assert from "node:assert/strict";
import {
  preferredLocalModel,
  recommendedResearchModel,
  researchModelGuidance,
  localModelGuidance,
  TESTED_LOCAL_WRITING_MODEL,
} from "../web/model-policy.js";

test("research recommendation uses strongest recognized eligible returned slug without changing catalog order", () => {
  const catalog = [
    { slug: "gpt-6-luna" },
    { slug: "gpt-6.1-terra" },
    { slug: "gpt-6.1-sol" },
    { slug: "gpt-6-astra" },
  ];
  const before = JSON.stringify(catalog);
  assert.equal(recommendedResearchModel(catalog).slug, "gpt-6-astra");
  assert.equal(JSON.stringify(catalog), before);
  assert.equal(
    recommendedResearchModel(catalog.slice(0, 3)).slug,
    "gpt-6.1-sol",
  );
  assert.equal(
    recommendedResearchModel([
      { slug: "gpt-6-astra", eligible: false },
      { slug: "gpt-6.1-terra" },
    ]).slug,
    "gpt-6.1-terra",
  );
});
test("unknown or unavailable research models remain manual and do not invent API identifiers", () => {
  assert.equal(recommendedResearchModel([{ slug: "some-future-model" }]), null);
  assert.equal(
    recommendedResearchModel([{ slug: "gpt-6-astra", disabled: true }]),
    null,
  );
  assert.equal(recommendedResearchModel([]), null);
  assert.equal(researchModelGuidance("not-in-policy"), null);
  assert.match(researchModelGuidance("gpt-6-luna").tradeoff, /speed/);
});
test("local selection favors a tested installed writing model without introducing an absent model", () => {
  assert.equal(
    preferredLocalModel(["qwen2.5-coder:3b", TESTED_LOCAL_WRITING_MODEL]),
    TESTED_LOCAL_WRITING_MODEL,
  );
  assert.equal(
    preferredLocalModel([{ id: "code-model" }, { id: "general-instruct" }]),
    "general-instruct",
  );
  assert.equal(preferredLocalModel(["only-coder"]), "only-coder");
  assert.equal(preferredLocalModel([]), "");
  assert.equal(localModelGuidance(["general-chat"]).testedModelListed, false);
  assert.equal(
    localModelGuidance([TESTED_LOCAL_WRITING_MODEL]).testedModelListed,
    true,
  );
});
