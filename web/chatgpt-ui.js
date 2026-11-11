import { escapeHTML as esc } from "./documents.js";
import {
  recommendedResearchModel,
  researchModelGuidance,
  MODEL_GUIDANCE_DATE,
} from "./model-policy.js";
function modelRecommendation(modelList, selectedModel) {
  const recommended = recommendedResearchModel(modelList),
    selected = researchModelGuidance(selectedModel);
  return `<div class="alert"><strong>${recommended ? `Recommended for deeper research: ${esc(recommended.slug)}` : "Choose from your available research models"}</strong><p>${esc(recommended?.tradeoff || "No returned model is covered by the current recommendation guide. The complete server list remains available for manual selection.")}</p><button class="secondary" data-action="chatgpt-recommend-model" ${recommended ? "" : "disabled"}>Best available for research</button>${selected && selected.slug !== recommended?.slug ? `<p>Selected: ${esc(selected.role)}. ${esc(selected.tradeoff)}</p>` : ""}<small>Guidance as of ${MODEL_GUIDANCE_DATE}. Recommendations use only models returned for this account. Availability, speed and plan usage vary; this button changes your selection without sending a research request.</small></div>`;
}
export function accountPanel(status, modelList = [], selectedModel = "") {
  const requestError = status.lastRequestError || status.researchError;
  const researchNotice = requestError
    ? `<div class="alert"><strong>Last AI request</strong><p>${esc(requestError)}</p><small>Editing, Writer DNA, draft observations and free source search remain available.</small></div>`
    : "";
  const accounts = status.accounts || [];
  return `<section class="card chatgpt-card"><div class="section-heading"><h3>ChatGPT on your phone</h3><span class="pill">${status.connected ? "Connected" : "Not connected"}</span></div><p>Connect your own eligible ChatGPT account for writing and research directly on your phone. No computer connection is required. Your plan’s usage limits apply. A free sign-in alone does not include AI usage. Editing, Writer DNA, draft observations and free source search remain available.</p>${researchNotice}${status.error ? `<div class="alert">${esc(status.error)}</div>` : ""}${accounts.length ? `<label class="field-label">Your ChatGPT account<select id="chatgpt-account">${accounts.map((a) => `<option value="${esc(a.id)}" ${a.id === status.active ? "selected" : ""}>${esc(a.label)}${a.signedIn ? "" : " (signed out)"}</option>`).join("")}</select></label>` : ""}<div class="account-actions"><button class="chatgpt-signin" data-action="chatgpt-signin" data-oai-siwc-button ${status.available ? "" : "disabled"}>Continue with ChatGPT</button>${accounts.length ? '<button class="text-button" data-action="chatgpt-add">Add another account</button>' : ""}</div>${status.connected ? `<label class="field-label">Writing and research model<select id="chatgpt-model"><option value="">Choose after loading available models</option>${modelList.map((m) => `<option value="${esc(m.slug)}" ${m.slug === selectedModel ? "selected" : ""}>${esc(m.display_name)}</option>`).join("")}</select></label>${modelRecommendation(modelList, selectedModel)}<div class="two-buttons"><button class="secondary" data-action="chatgpt-models">Load available models</button><button class="text-button" data-action="chatgpt-signout">Sign out</button></div>` : accounts.length ? '<p>Sign in and approve ChatGPT plan usage to enable AI writing and research.</p><button class="text-button" data-action="chatgpt-signout">Clear this session</button>' : ""}<button class="text-button" data-action="chatgpt-usage">Manage ChatGPT usage & access ↗</button><small>${status.available ? "Sign-in opens your browser. Credentials are encrypted using Android Keystore and never included in project exports." : "Sign-in is available in the Android APK; this is a browser preview."} There is no automatic paid API fallback.</small></section>`;
}
export function researchBrief(result) {
  if (!result) return "";
  const text = String(result.text || ""),
    annotations = [...(result.annotations || [])]
      .filter(
        (a) =>
          Number.isInteger(a.start) &&
          Number.isInteger(a.end) &&
          a.end > a.start &&
          a.start >= 0 &&
          a.end <= text.length &&
          /^https?:\/\//.test(a.url),
      )
      .sort((a, b) => a.start - b.start);
  let html = "",
    position = 0;
  for (const a of annotations) {
    if (a.start < position) continue;
    html += esc(text.slice(position, a.start));
    html += `<button class="citation-link" data-action="chatgpt-link" data-url="${esc(a.url)}" aria-label="Open ${esc(a.title || "cited source")}">${esc(text.slice(a.start, a.end) || a.title || "Source")}</button>`;
    position = a.end;
  }
  html += esc(text.slice(position));
  return `<section class="response-card chatgpt-brief"><div class="section-heading"><h3>ChatGPT research brief</h3><span class="pill">${result.webSearchUsed ? "Web search used" : "No web search"}</span></div><p class="muted">Question: ${esc(result.query)}</p><div class="pre-wrap research-answer">${html}</div><div class="research-links">${(
    result.sources || []
  )
    .filter((s) => /^https?:\/\//.test(s.url))
    .map(
      (s) =>
        `<button class="source-link" data-action="chatgpt-link" data-url="${esc(s.url)}">${esc(s.title || s.url)} ↗</button>`,
    )
    .join(
      "",
    )}</div><details class="warning-details"><summary>Verification notes</summary><p>${esc((result.warnings || []).join("\n"))}</p></details><small>These results are saved with this workspace. Open the cited sources before relying on a claim. A returned link is not independent source verification.</small></section>`;
}
