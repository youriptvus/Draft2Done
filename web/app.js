import { accountPanel, researchBrief } from "./chatgpt-ui.js";
import {
  preferredLocalModel,
  localModelGuidance,
  recommendedResearchModel,
} from "./model-policy.js";
import {
  createProject,
  countWords,
  progress,
  analyzeDNA,
  analyzeDraft,
  citation,
  citations,
  continuationPoint,
  loadProject,
  snapshot,
  acceptProposal,
  rejectProposal,
  revertRevision,
  updateWorkingText,
} from "./core.js";
import { AIClient, ResearchEngine, evidenceNotes } from "./services.js";
import {
  informationLinks,
  informationParts,
  relatedInformation,
  safeInformationUrl,
} from "./information-links.js";
import {
  native,
  loadState,
  saveState,
  loadDraftInput,
  saveDraftInput,
  transport,
  onImported,
  onNotice,
  importFile,
  openExternal,
  exportFile,
  addonStatus,
  addonRequest,
} from "./platform.js";
import {
  readDocument,
  writeDocument,
  previewHTML,
  exportText,
  escapeHTML as esc,
} from "./documents.js";

const paths = {
  pen: "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L9 17l-4 1 1-4Z",
  book: "M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-3H4Zm16 0h-4a3 3 0 0 0-3 3v14a4 4 0 0 1 4-3h3Z",
  search: "m21 21-4.3-4.3M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0",
  dna: "M7 3c10 5 0 13 10 18M17 3C7 8 17 16 7 21M8 6h8M8 18h8M9 10h6M9 14h6",
  folder: "M3 7V4h6l2 3h10v13H3Z",
  settings:
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1Z",
  plus: "M12 5v14M5 12h14",
  upload: "M12 16V3m-5 5 5-5 5 5M4 16v5h16v-5",
  arrow: "M5 12h14m-5-5 5 5-5 5",
  back: "M19 12H5m5-5-5 5 5 5",
  spark:
    "m12 3 2.6 6.4L21 12l-6.4 2.6L12 21l-2.6-6.4L3 12l6.4-2.6ZM21 2v4m-2-2h4",
  check: "m5 12 4 4L19 6",
  chevron: "m9 5 7 7-7 7",
  close: "m6 6 12 12M6 18 18 6",
  shield: "M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6ZM8 12l3 3 5-6",
  clock: "M12 7v5l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0",
  export: "M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5",
  stop: "M5 5h14v14H5Z",
  menu: "M4 6h16M4 12h16M4 18h16",
  link: "m10 13 4-4M8 16l-2 2a4 4 0 0 1-6-6l4-4a4 4 0 0 1 6 0m4 0 2-2a4 4 0 0 1 6 6l-4 4a4 4 0 0 1-6 0",
  light:
    "M12 3v2m0 14v2M3 12h2m14 0h2M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
};
const icon = (name, size = 20) =>
  `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[name] || paths.book}"/></svg>`;
const button = (action, label, cls = "secondary", extra = "") =>
  `<button class="${cls}" data-action="${action}" ${extra}>${label}</button>`;
const uid = () => crypto.randomUUID();
const APP_VERSION = __DRAFT2DONE_VERSION__;
const DEDICATION =
  "Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.";
const DOCUMENT_BYTE_LIMIT = 10 * 1024 * 1024;
const WORKSPACE_BYTE_LIMIT = 4 * 1024 * 1024;
const byteLength = (value) => new TextEncoder().encode(value).length;
let pasteDraft = { text: "", start: 0, end: 0, scroll: 0 },
  pasteError = "",
  pasteRecovery = false;
try {
  const saved = loadDraftInput();
  if (saved) {
    const value = JSON.parse(saved);
    if (
      typeof value.text !== "string" ||
      byteLength(value.text) > DOCUMENT_BYTE_LIMIT
    )
      throw new Error("The saved paste is invalid or exceeds the 10 MB limit.");
    pasteDraft = {
      text: value.text,
      start: Math.max(0, Math.min(value.text.length, Number(value.start) || 0)),
      end: Math.max(0, Math.min(value.text.length, Number(value.end) || 0)),
      scroll: Math.max(0, Number(value.scroll) || 0),
    };
  }
} catch (error) {
  pasteRecovery = true;
  pasteError = `The previous paste could not be restored: ${error.message}`;
}
const defaults = {
  autoUpdates: true,
  theme: "dark",
  online: false,
  providers: ["crossref", "pubmed", "wikipedia"],
  backend: "chatgpt",
  endpoint: "http://127.0.0.1:11434",
  model: "",
  context: 8192,
  temperature: 0.6,
  maxTokens: 1200,
  cpuOnly: false,
  autosave: true,
  defaultVoice: "original",
  citationStyle: "APA",
  defaultExport: "docx",
  defaultMode: "draft",
  excludeReferences: true,
  excludeTitle: true,
  exportFontFamily: "Times New Roman",
  exportFontSize: 12,
  exportLineSpacing: 1.5,
  informationLinks: false,
};
let state = {
  version: 1,
  projects: [],
  profiles: [],
  activeId: null,
  settings: { ...defaults },
  notes: {},
};
let recovery = "";
try {
  const stored = loadState();
  if (stored) {
    state = {
      ...state,
      ...stored,
      settings: { ...defaults, ...stored.settings },
    };
    state.projects = (state.projects || []).map((p) => loadProject(p));
    // Older USB development settings remain available as an explicit advanced route.
    if (native && !stored.settings?.aiRouteChosen)
      state.settings.backend = "chatgpt";
  }
} catch (e) {
  state = {
    version: 1,
    projects: [],
    profiles: [],
    activeId: null,
    settings: { ...defaults },
    notes: {},
  };
  recovery = `Stored data could not be read: ${e.message}. The saved file has not been replaced. Import a valid .d2d backup to restore your workspace.`;
}
let page = "home",
  tab = "draft",
  busy = null,
  saveTimer = null,
  dirty = false,
  selectedSuggestion = null,
  models = [],
  chatgptModels = [],
  lastError = "";
let remoteConsent = null,
  approvedRemote = null;
const app = document.querySelector("#app");
const getProject = () =>
  state.projects.find((p) => p.id === state.activeId) || null;
function updateProject(value) {
  const i = state.projects.findIndex((p) => p.id === value.id);
  if (i < 0) state.projects.push(value);
  else state.projects[i] = value;
  value.updatedAt = new Date().toISOString();
  persist();
}
function toast(message) {
  const el = document.querySelector("#toast");
  el.textContent = message;
  el.classList.add("visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.remove("visible"), 4500);
}
function persist(now = false) {
  dirty = true;
  clearTimeout(saveTimer);
  if (recovery) return;
  if (now) flushSave();
  else if (state.settings.autosave) saveTimer = setTimeout(flushSave, 650);
  updateSaveIndicator();
}
function flushSave() {
  if (!dirty || recovery) return;
  try {
    saveState(state);
    dirty = false;
    updateSaveIndicator();
  } catch (e) {
    lastError = e.message;
    toast(`Save failed: ${e.message}`);
  }
}
function flushAutosave() {
  if (!state.settings.autosave) return;
  clearTimeout(saveTimer);
  flushSave();
}
function updateSaveIndicator() {
  const el = document.querySelector("#save-status");
  if (el) el.textContent = dirty ? "Unsaved changes" : "Saved on device";
}
function activeProfile(p) {
  return p.profile || analyzeDNA(p.workingText);
}
function navButton(id, label, name) {
  return `<button data-action="page" data-id="${id}" class="nav-item ${page === id ? "active" : ""}" ${page === id ? 'aria-current="page"' : ""}>${icon(name)}<span>${label}</span></button>`;
}
function shell(content) {
  document.documentElement.dataset.theme = state.settings.theme;
  window.DraftNative?.setAppearance?.(state.settings.theme);
  app.innerHTML = `<header class="app-header"><button class="brand" data-action="page" data-id="home" aria-label="Draft2Done home"><span class="brand-icon">${icon("book", 22)}</span><span>Draft<span class="brand-accent">2</span>Done<small>YOUR DRAFT. YOUR VOICE. DONE.</small></span></button><span class="local-badge">${icon("shield", 13)} On-device storage</span></header><main>${recovery ? `<div class="alert danger">${esc(recovery)}</div>` : ""}${content}</main>${busy ? `<div class="task-bar" role="status"><span class="spinner"></span><span id="task-message">${esc(busy.message)}</span>${button("stop", icon("stop", 16) + " Stop", "text-button")}</div>` : ""}<nav class="bottom-nav" aria-label="Main navigation">${navButton("write", "Write", "pen")}${navButton("research", "Research", "search")}${navButton("dna", "Writer DNA", "dna")}${navButton("projects", "Papers", "folder")}${navButton("settings", "Settings", "settings")}</nav>`;
}
function empty(title, detail, action = "import", label = "Import a draft") {
  return `<div class="empty-state"><span class="empty-icon">${icon("book", 34)}</span><h2>${title}</h2><p>${detail}</p>${button(action, label, "primary")}</div>`;
}
function render() {
  capturePasteInput();
  if (page === "home") renderHome();
  else if (page === "write") renderWrite();
  else if (page === "research") renderResearch();
  else if (page === "dna") renderDNA();
  else if (page === "projects") renderProjects();
  else renderSettings();
}
function capturePasteInput() {
  const input = document.querySelector("#paste-draft");
  if (!input) return;
  pasteDraft = {
    text: input.value,
    start: input.selectionStart,
    end: input.selectionEnd,
    scroll: input.scrollTop,
  };
}
function pasteFeedback() {
  const status = document.querySelector("#paste-status");
  if (!status) return;
  status.textContent =
    pasteError ||
    (pasteDraft.text
      ? "Paste saved on this device. Use it to create a separate protected paper."
      : "Your paste stays on this device. Existing papers are kept separately.");
  status.classList.toggle("paste-warning", Boolean(pasteError));
}
function savePasteDraft(edited = false) {
  capturePasteInput();
  if (pasteRecovery && !edited) {
    pasteFeedback();
    return;
  }
  try {
    if (byteLength(pasteDraft.text) > DOCUMENT_BYTE_LIMIT)
      throw new Error("This paste exceeds 10 MB. Split it into smaller papers.");
    saveDraftInput(pasteDraft.text ? JSON.stringify(pasteDraft) : "");
    pasteRecovery = false;
    pasteError = "";
  } catch (error) {
    pasteError = `Paste not saved: ${error.message} Keep a copy before leaving the app.`;
  }
  pasteFeedback();
}
function pasteEntry() {
  return `<section class="paste-card" aria-labelledby="paste-heading"><label id="paste-heading" for="paste-draft">Paste Your Draft</label><p>Pick up where your own writing leaves off.</p><textarea id="paste-draft" rows="5" aria-describedby="paste-status" placeholder="Already started your paper? Paste what you've written here to get started." spellcheck="true">${esc(pasteDraft.text)}</textarea><small id="paste-status" role="status"></small>${button("paste-use", "Use This Draft", "primary large", busy ? "disabled" : "")}</section>`;
}
function usePastedDraft() {
  capturePasteInput();
  const text = pasteDraft.text;
  if (busy) throw new Error("Finish or stop the current task first.");
  if (!text.trim()) throw new Error("Paste some of your writing first.");
  if (byteLength(text) > DOCUMENT_BYTE_LIMIT)
    throw new Error("This paste exceeds 10 MB. Split it into smaller papers.");
  if (recovery)
    throw new Error("Restore your workspace from a project backup before adding a paper. Your paste is kept separately.");
  const title = text.match(/[^\r\n]+/)?.[0].trim().slice(0, 80) || "Pasted draft";
  const project = projectFromDraft(title, text);
  const next = { ...state, projects: [...state.projects, project], activeId: project.id };
  if (byteLength(JSON.stringify(next)) > WORKSPACE_BYTE_LIMIT)
    throw new Error("This paper would exceed the 4 MB workspace limit. Back up and remove older papers, or split this draft. Your paste is still here.");
  // This explicit action saves the new paper before clearing its separate staging copy.
  // A failed write leaves the existing workspace and the paste untouched.
  saveState(next);
  state = next;
  clearTimeout(saveTimer);
  dirty = false;
  document.querySelector("#paste-draft").value = "";
  pasteDraft = { text: "", start: 0, end: 0, scroll: 0 };
  try {
    saveDraftInput("");
    pasteRecovery = false;
    pasteError = "";
  } catch {
    pasteError = "Your paper is saved. The old paste could not be cleared from this device.";
  }
  page = "write";
  tab = defaultWritingTab();
  render();
  toast("New paper saved. Your starting draft is protected.");
}
function paperCards() {
  return [...state.projects]
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    .map((p) => {
      const n = progress(p);
      return `<button class="paper-card" data-action="open" data-id="${esc(p.id)}"><span class="paper-icon">${icon("book", 23)}</span><span class="paper-info"><strong>${esc(p.title)}</strong><small>${n.current.toLocaleString()} words · ${new Date(p.updatedAt).toLocaleDateString()}</small><span class="mini-track"><i style="width:${Math.min(100, n.percent)}%"></i></span></span>${icon("chevron", 18)}</button>`;
    })
    .join("");
}
function renderHome() {
  shell(
    `<section class="welcome"><div class="eyebrow">A LITTLE PROGRESS GOES A LONG WAY</div><h1>Your ideas.<br>Your voice.<br><em>Let’s finish this.</em></h1><p>A calmer space to pick up your paper<br>and take it all the way.</p><div class="welcome-art" aria-hidden="true"><div class="art-page"><span></span><span></span><span></span><span></span><b>${icon("check", 22)}</b></div><i class="orbit one"></i><i class="orbit two"></i></div></section>${pasteEntry()}<div class="start-actions">${button("import", icon("upload") + " Import your draft", "primary large")}${button("new", icon("plus") + " New workspace", "secondary large")}</div><div class="reassurance">${icon("shield", 16)} Your writing stays yours. Original drafts are protected.</div><section class="section"><div class="section-heading"><h2>${state.projects.length ? "Pick up where you left off" : "Start with something you’ve written"}</h2>${state.projects.length ? button("page", "View all", "text-button", 'data-id="projects"') : ""}</div>${state.projects.length ? paperCards() : `<div class="intro-card"><span class="tinted-icon">${icon("spark")}</span><div><strong>A research partner, at your pace.</strong><p>Bring a draft. Explore ideas, find sources, and review every proposed addition.</p>${button("sample", "Try a fictional sample " + icon("arrow", 15), "text-button")}</div></div>`}</section><div class="version-footer">DRAFT2DONE · ${APP_VERSION}</div>`,
  );
  const input = document.querySelector("#paste-draft");
  // Assign the value explicitly: HTML parsing discards a textarea's first newline.
  input.value = pasteDraft.text;
  input.setSelectionRange(pasteDraft.start, pasteDraft.end);
  input.scrollTop = pasteDraft.scroll;
  pasteFeedback();
}
function renderWrite() {
  const p = getProject();
  if (!p) {
    shell(
      empty(
        "Your next chapter starts here",
        "Import an unfinished paper or create a workspace and paste your existing writing.",
      ),
    );
    return;
  }
  const n = progress(p);
  shell(
    `<div class="workspace-heading"><div><div class="eyebrow">YOUR WORKSPACE</div><h1 class="paper-title" data-action="rename" tabindex="0">${esc(p.title)}</h1><span class="save-line"><i></i><span id="save-status">${dirty ? "Unsaved changes" : "Saved on device"}</span></span></div>${button("paper-menu", icon("menu"), "icon-button", 'aria-label="Paper actions"')}</div><div class="progress-card"><div><span><strong id="current-count">${n.current.toLocaleString()}</strong> <span class="muted">/ <span id="minimum-count">${n.minimum.toLocaleString()}</span> words</span></span>${button("requirements", "Set goal " + icon("chevron", 14), "text-button")}</div><div class="progress-track"><i id="word-progress" style="width:${Math.min(100, n.percent)}%"></i></div><small id="remaining-label">${n.remaining > 0 ? `${n.remaining.toLocaleString()} words to your goal` : "Minimum reached · ready to review"}${n.maximum ? ` · maximum ${n.maximum.toLocaleString()}` : ""}</small></div><div class="segmented" role="tablist">${["draft", "helper", "complete"].map((t) => `<button role="tab" aria-selected="${tab === t}" data-action="tab" data-id="${t}" class="${tab === t ? "selected" : ""}">${t === "draft" ? "My draft" : t === "helper" ? "Helper" : "Complete"}${t === "complete" && p.proposals.some((s) => s.status === "pending") ? '<i class="notification-dot"></i>' : ""}</button>`).join("")}</div>${tab === "draft" ? draftPanel(p) : tab === "helper" ? helperPanel(p) : completePanel(p)}`,
  );
  const editor = document.querySelector("#draft-editor");
  if (editor) editor.value = p.workingText;
}
const editorHistories = new Map();
function editingHistory(p) {
  let h = editorHistories.get(p.id);
  if (!h || h.currentText !== p.workingText) {
    h = { undo: [], redo: [], currentText: p.workingText, at: 0, kind: "" };
    editorHistories.set(p.id, h);
    if (editorHistories.size > 10)
      editorHistories.delete(editorHistories.keys().next().value);
  }
  return h;
}
function editorSnapshot(p) {
  return {
    text: p.workingText,
    contributions: JSON.parse(JSON.stringify(p.contributions)),
  };
}
function trackEdit(p, kind) {
  const h = editingHistory(p),
    time = Date.now();
  if (!kind || time - h.at > 750 || h.kind !== kind || !h.undo.length)
    h.undo.push(editorSnapshot(p));
  while (
    h.undo.length > 1 &&
    (h.undo.length > 30 ||
      h.undo.reduce((n, e) => n + e.text.length, 0) > 500000)
  )
    h.undo.shift();
  h.redo = [];
  h.at = time;
  h.kind = kind;
  return h;
}
function updateUndoButtons(p) {
  const h = editingHistory(p);
  for (const key of ["undo", "redo"]) {
    const el = document.querySelector(`[data-action="editor-${key}"]`);
    if (el) el.disabled = !h[key].length;
  }
}
function restoreEditor(p, direction) {
  const h = editingHistory(p),
    entry = h[direction].pop();
  if (!entry) return;
  h[direction === "undo" ? "redo" : "undo"].push(editorSnapshot(p));
  const restored = updateWorkingText(p, entry.text);
  restored.contributions = entry.contributions;
  h.currentText = restored.workingText;
  h.at = 0;
  updateProject(restored);
  render();
  document.querySelector("#draft-editor")?.focus({ preventScroll: true });
}
function paragraphOptions(text) {
  const starts = [
    0,
    ...[...text.matchAll(/\n\s*\n/g)].map((m) => m.index + m[0].length),
  ];
  return starts
    .filter((start) => text.slice(start).trim())
    .map(
      (start, i) =>
        `<option value="${start}">${i + 1}. ${esc(text.slice(start, start + 65).replace(/\n/g, " "))}</option>`,
    )
    .join("");
}
function draftPanel(p) {
  const h = editingHistory(p);
  return `<section class="editor-card"><div class="editor-toolbar"><div>${button("format", "B", "format-button", 'data-format="bold" aria-label="Bold Markdown"')}${button("format", "<em>I</em>", "format-button", 'data-format="italic" aria-label="Italic Markdown"')}${button("format", "H", "format-button", 'data-format="heading" aria-label="Heading Markdown"')}</div><div>${button("editor-undo", "Undo", "text-button", h.undo.length ? "" : "disabled")}${button("editor-redo", "Redo", "text-button", h.redo.length ? "" : "disabled")}</div>${button("original", icon("shield", 16), "icon-button small", 'aria-label="View protected original"')}</div><div class="paragraph-navigation"><label for="paragraph-nav">Go to paragraph</label><select id="paragraph-nav"><option value="">Choose a paragraph</option>${paragraphOptions(p.workingText)}</select></div><textarea id="draft-editor" aria-label="Working draft" spellcheck="true" placeholder="Paste the paper you’ve already started. Your writing is the starting point.">${esc(p.workingText)}</textarea><div class="editor-footer"><span>${icon("pen", 13)} Working copy · Markdown friendly</span>${button("save", icon("check", 14) + " Save", "text-button")}</div></section>${informationReader(p)}<div class="context-card"><span class="tinted-icon">${icon("spark")}</span><div><strong>A little direction?</strong><p>Find your next idea with Helper, or review a continuation in Complete.</p></div></div>`;
}
function informationContext(p) {
  return { sources: p.sources, researchBriefs: p.researchBriefs };
}
function informationCopy(p) {
  const links = informationLinks(p.workingText, informationContext(p));
  return informationParts(p.workingText, links)
    .map((part) => part.link
      ? `<button class="information-link" data-action="information-concept" data-index="${links.indexOf(part.link)}" data-start="${part.link.start}" data-end="${part.link.end}" aria-label="Explore ${esc(part.text)}">${esc(part.text)}</button>`
      : esc(part.text))
    .join("");
}
function informationReader(p) {
  return `<section class="information-reader"><label class="checkbox-row"><span>Read with information links</span><input id="information-links-enabled" type="checkbox" ${state.settings.informationLinks ? "checked" : ""}></label><small>Optional research suggestions in a separate reading view. Your draft and exported citations stay unchanged. No query or page opens automatically.</small>${state.settings.informationLinks ? `<div class="information-copy" tabindex="0" aria-label="Draft reading preview">${informationCopy(p)}</div><small>Suggestions are identified on this device and may miss a concept or its meaning. Tap a term to review saved sources or a search query.</small>` : ""}</section>`;
}
function showInformation(p, target) {
  const link = informationLinks(p.workingText, informationContext(p))[Number(target.dataset.index)];
  if (!link || link.start !== Number(target.dataset.start) || link.end !== Number(target.dataset.end)) {
    toast("The draft changed. Choose a term from the updated reading view.");
    return;
  }
  const related = relatedInformation(link, informationContext(p));
  modal(
    `Explore ${link.label}`,
    `<p>Information links help you explore a concept; they are separate from formal citations. Check that the suggested meaning fits your paragraph.</p><div class="information-sources">${related.map((source) => `<article><h3>${esc(source.title)}</h3><small>${esc(source.verification === "web-returned" ? "Returned by web research" : source.verification === "abstract" ? "Record and abstract/extract retrieved" : "Bibliographic record retrieved")}</small><p>${esc(source.detail)}</p><small>${esc(source.url)}</small>${button("information-open", "Open saved source", "secondary", `data-url="${esc(source.url)}"`)}</article>`).join("") || "<p>No relevant retrieved source is saved for this term yet. Review a short query to find one.</p>"}</div><label class="field-label" for="information-query">Research query to review<input id="information-query" maxlength="240" value="${esc(link.query)}"></label><small>${state.settings.online ? "Review this query in Research before choosing Search. Nothing is sent by opening this view." : "Source search is off. You can review a query in Research and choose whether to enable Online. Opening a saved web page requires internet."}</small>`,
    button("information-research", "Review research query", "primary"),
  );
}
function draftReview(p, analysis) {
  return `<section class="card"><h3>Your draft at a glance</h3><details><summary>Argument and paragraph outline</summary><ol>${analysis.outline.map((s) => `<li><strong>${esc(s.heading)}</strong> · ${s.words} words<p>${esc(s.summary)}</p></li>`).join("")}</ol></details><details open><summary>What still needs attention</summary><ul>${analysis.gaps.map((g) => `<li>${esc(g)}</li>`).join("") || "<li>No rule-based gaps detected. Check the assignment yourself.</li>"}</ul></details>${analysis.unfinished.length ? `<div class="alert"><strong>Check the unfinished parts</strong><ul>${analysis.unfinished.map((g) => `<li>${esc(g)}</li>`).join("")}</ul><p>Complete lets you review whether to finish the last thought or start a new paragraph.</p></div>` : ""}<details><summary>Research questions to explore</summary>${analysis.researchQuestions.map((q, i) => `<p>${esc(q)}</p>${button("research-question", "Review this search", "text-button", `data-index="${i}"`)}`).join("")}</details><small>These are visible draft observations, not a grading or factual assessment.</small></section>`;
}
function helperSuggestions(note) {
  return String(note)
    .split("Review notes:")[0]
    .split(/(?:^|\n)\d+\.\s+/)
    .map((s) => s.trim())
    .filter(Boolean);
}
function helperAdvice(note) {
  const review = String(note)
    .split("Review notes:")
    .slice(1)
    .join("Review notes:");
  return `<div class="suggestion-list">${helperSuggestions(note)
    .map(
      (s, i) =>
        `<button class="suggestion-card" data-action="ai-suggestion" data-index="${i}"><span><strong>${esc(s)}</strong><small>Choose for an explanation, evidence or a transition.</small></span>${icon("chevron", 16)}</button>`,
    )
    .join(
      "",
    )}</div>${review ? `<details><summary>Evidence and model limits</summary><p class="pre-wrap">${esc(review)}</p></details>` : ""}`;
}
function showSuggestion(suggestion) {
  selectedSuggestion = suggestion;
  modal(
    suggestion.title,
    `<p>${esc(suggestion.detail)}</p><p><strong>A question to consider</strong></p><p>${esc(suggestion.question)}</p><div class="action-menu">${["Explain this idea", "Connect it to my paper", "Suggest a transition", "Outline the next paragraph", "Find more research", "Show supporting evidence", "Find an academic source"].map((x, i) => button("helper-action", x, "secondary", `data-index="${i}"`)).join("")}</div>`,
  );
}
function helperPanel(p) {
  return helperContent(p) + informationReader(p);
}
function helperContent(p) {
  const analysis = analyzeDraft(p),
    note = p.helperNotes || state.notes[p.id] || "";
  return `<section class="mode-intro"><span class="eyebrow">YOU WRITE. WE HELP YOU FORWARD.</span><h2>A fresh perspective on your draft.</h2><p>Explore the argument, ask better questions, and find a next step that feels like you.</p>${button("analyze", icon("spark", 17) + " Analyze my draft", "primary")}</section>${countWords(p.workingText) < 40 ? '<div class="alert">Add at least 40 words of your own writing for useful draft assistance.</div>' : `<div class="analysis-summary"><span class="pill">Draft observations · on device</span><h3>${esc(analysis.topic)}</h3><p>${esc(analysis.thesis)}</p><small>Topic and thesis are candidates inferred from your words. Check that they reflect your intention.</small></div>${draftReview(p, analysis)}<h3 class="subheading">Ways to move forward</h3><div class="suggestion-list">${analysis.suggestions.map((s, i) => `<button class="suggestion-card" data-action="suggestion" data-index="${i}"><span class="suggestion-number">0${i + 1}</span><span><strong>${esc(s.title)}</strong><small>${esc(s.detail)}</small></span>${icon("chevron", 16)}</button>`).join("")}</div>`}${note ? `<section class="response-card"><div class="section-heading"><h3>AI assistance</h3><span class="pill">Review carefully</span></div>${helperAdvice(note)}<small>Model suggestions may contain errors. Verify factual claims against the research library.</small></section>` : ""}`;
}
function completePanel(p) {
  return completeContent(p) + informationReader(p);
}
function completeContent(p) {
  const n = progress(p);
  return `<section class="mode-intro"><span class="eyebrow">YOUR VOICE, CONTINUED</span><h2>Keep the momentum going.</h2><p>A proposed continuation, guided by your draft and Writer DNA. Every section is yours to review.</p><div class="inline-options"><label>Voice<select id="voice-select"><option value="original" ${p.voice === "original" ? "selected" : ""}>Original Voice</option><option value="polished" ${p.voice === "polished" ? "selected" : ""}>Polished Voice</option></select></label><label>Minimum words<input id="minimum-input" type="number" min="1" max="50000" value="${p.minimum}"></label></div>${n.remaining ? button("generate", icon("spark", 17) + " Generate continuation", "primary") : `<div class="alert success">You’ve reached your minimum. Use Helper for research and refinement, or update your goal.</div>`}<div class="mode-footnote">${icon("shield", 14)} Proposed text stays separate until you accept it.</div></section><p class="muted">${n.original} original; ${n.current} accepted; ${n.proposed} proposed body words</p><div class="source-summary">${icon("book", 16)} ${p.sources.filter((s) => s.included !== false).length} included sources ${button("page", "Review sources", "text-button", 'data-id="research"')}</div>${p.proposals.filter((s) => s.status === "pending").length ? `<div class="section-heading"><h3>Suggested additions</h3><span class="pill">${n.proposed} proposed words</span></div>` : ""}${p.proposals
    .filter((s) => s.status === "pending")
    .map(
      (s, i) =>
        `<section class="proposal-card" data-proposal="${esc(s.id)}"><div class="section-heading"><h3>Section ${i + 1}</h3><span class="pill generated">AI generated</span></div>${s.insertion?.mode === "inline" ? `<div class="alert">Finishes your existing thought at the reviewed insertion point. Your current words remain intact.</div>${button("proposal-paragraph", "Use as a new paragraph instead", "text-button", 'data-id="' + esc(s.id) + '"')}` : ""}${s.warnings?.length ? `<details class="warning-details"><summary>Evidence and generation notes</summary><p>${esc(s.warnings.join("\n"))}</p></details>` : ""}<textarea class="proposal-editor" aria-label="Edit generated section ${i + 1}" data-id="${esc(s.id)}">${esc(s.text)}</textarea><div class="proposal-actions">${button("accept", icon("check", 16) + " Accept section", "primary", 'data-id="' + esc(s.id) + '"')}${button("reject", "Reject", "text-button", 'data-id="' + esc(s.id) + '"')}${button("regenerate", "Regenerate", "text-button", 'data-id="' + esc(s.id) + '"')}</div></section>`,
    )
    .join(
      "",
    )}${p.contributions.length ? `<div class="alert success">${p.contributions.length} generated section(s) accepted. Contribution records are retained with your project.</div>` : ""}`;
}
function renderResearch() {
  const connection = addonStatus();
  const p = getProject();
  if (!p) {
    shell(
      empty(
        "Research with a purpose",
        "Open a paper first so findings can stay with your project.",
      ),
    );
    return;
  }
  const analysis = analyzeDraft(p);
  shell(
    `<div class="page-heading"><span class="eyebrow">EVIDENCE, WITH CONTEXT</span><h1>Your research desk.</h1><p>Real sources. Clear provenance.<br>Keep the evidence behind your ideas.</p></div><section class="search-card"><div class="section-heading"><h3>Find a source</h3><label class="switch-label"><input id="research-online" type="checkbox" ${state.settings.online ? "checked" : ""}> Online</label></div><label class="field-label" for="research-query">Only this query is sent to research providers</label><div class="search-input"><input id="research-query" value="${esc(state.lastQuery || analysis.topic || "")}" maxlength="240" placeholder="A topic or research question">${button("search", icon("search"), "primary square", 'aria-label="Search scholarly sources"')}</div><div class="provider-chips">${["crossref", "pubmed", "wikipedia"].map((id) => `<label><input type="checkbox" data-provider="${id}" ${state.settings.providers.includes(id) ? "checked" : ""}>${{ crossref: "Crossref", pubmed: "PubMed", wikipedia: "Wikipedia" }[id]}</label>`).join("")}</div><small>${state.settings.online ? "Search uses official APIs. Bibliographic records and abstracts are not full-text review." : "Offline: saved sources remain available. Enable Online to search."}</small></section><div class="chatgpt-research-entry"><div><strong>Want help connecting the evidence?</strong><p>Ask your connected ChatGPT account for a research brief.</p></div>${button("chatgpt-research", "Ask ChatGPT", "secondary")}</div>${researchBrief(p.researchBriefs?.at(-1) || state.chatgptBriefs?.[p.id])}<div class="section-heading"><h2>Source library</h2><span class="count-badge">${p.sources.length}</span></div><div class="two-buttons">${button("source-add", "Add a source", "secondary")}${p.sources.length ? button("citation-group", "Insert citation", "secondary") : ""}</div>${evidenceNotes(
      p.sources.filter((s) => s.included !== false),
    )
      .map((n) => `<div class="alert">${esc(n.text)}</div>`)
      .join(
        "",
      )}${p.sources.length ? p.sources.map((s, i) => `<article class="source-card"><div class="source-top"><span class="pill ${s.verification === "unverified" ? "warning" : ""}">${esc(s.provider)} · ${esc(s.verification === "abstract" ? "abstract retrieved" : s.verification === "metadata" ? "record retrieved" : "unverified")}</span><label class="include-label"><input type="checkbox" data-source="${esc(s.id)}" ${s.included !== false ? "checked" : ""}> Use</label></div><h3>${esc(s.title)}</h3><div class="source-meta">${esc(s.authors?.slice(0, 3).join(", ") || "Author unavailable")} · ${esc(s.year || "Date unavailable")}</div>${s.abstract ? `<details><summary>Read available evidence</summary><p>${esc(s.abstract)}</p></details>` : '<p class="muted">Metadata only. No accessible abstract; this record alone cannot support a research finding.</p>'}<small>${esc(s.notes || "Source existence does not establish that a claim is supported.")}</small><div class="source-actions">${button("source-open", "Open source " + icon("arrow", 14), "text-button", 'data-id="' + esc(s.id) + '"')}${button("source-citation", "Citation", "text-button", 'data-id="' + esc(s.id) + '"')}${button("verify", "Verify", "text-button", 'data-id="' + esc(s.id) + '"')}</div></article>`).join("") : empty("Build an evidence trail", "Search for a relevant study, or keep writing offline. Sources are saved with this paper.", "research-tip", "Research tips")}<p class="legal-note">PubMed data: <button data-action="ncbi" class="inline-link">NCBI disclaimer and copyright</button>. Check publisher access and usage terms before sharing abstracts.</p>`,
  );
}
function renderDNA() {
  const p = getProject();
  if (!p) {
    shell(
      empty(
        "Your voice is the starting point",
        "Open a paper to see its writing patterns.",
      ),
    );
    return;
  }
  const dna = activeProfile(p);
  const vocab = dna.vocabulary || {};
  shell(
    `<div class="page-heading dna-heading"><span class="eyebrow">WRITER DNA</span><h1>Sounds like you.</h1><p>A closer look at the patterns<br>that make your writing your own.</p><span class="dna-watermark">${icon("dna", 100)}</span></div><div class="confidence-card"><span class="tinted-icon">${icon("dna", 24)}</span><div><strong>${esc(dna.confidence || "low")} style confidence</strong><p>${esc(dna.confidenceReason || "Add more original writing for a more reliable profile.")}</p></div></div><div class="metric-grid"><div><strong>${Number(dna.averageSentenceWords || 0).toFixed(1)}</strong><span>words / sentence</span></div><div><strong>${Number(dna.averageParagraphWords || 0).toFixed(1)}</strong><span>words / paragraph</span></div><div><strong>${vocab.uniqueWords || 0}</strong><span>distinct words</span></div><div><strong>${Math.round(vocab.lexicalDiversity || 0)}%</strong><span>lexical diversity</span></div></div><section class="card"><h3>Your language patterns</h3><dl><dt>Point of view</dt><dd>${esc(dna.person?.dominant || "Not enough evidence")}</dd><dt>Common vocabulary</dt><dd>${esc(
      (vocab.repeatedWords || [])
        .slice(0, 8)
        .map((w) => (typeof w === "string" ? w : w.word))
        .join(", ") || "Still learning your vocabulary",
    )}</dd><dt>Transitions</dt><dd>${esc((dna.transitions || []).map((t) => t.phrase).join(", ") || "No repeated transitions detected")}</dd></dl><label class="field-label" for="profile-notes">Style guidance (optional)</label><textarea id="profile-notes" rows="3" placeholder="For example: keep my plain language and short paragraphs.">${esc(dna.userNotes || "")}</textarea><small>Metrics describe this sample; they do not diagnose proficiency, grammar or spelling ability.</small></section>${dnaDetails(dna)}<div class="two-buttons">${button("dna-refresh", icon("dna", 16) + " Reanalyze draft", "secondary")}${button("profile-save", icon("plus", 16) + " Save profile", "primary")}</div>${dna.limitations?.length ? `<div class="alert">${esc(dna.limitations.join(" "))}</div>` : ""}<div class="section-heading"><h2>Saved voices</h2><span class="count-badge">${state.profiles.length}</span></div>${state.profiles.map((pr) => `<div class="profile-card"><span class="tinted-icon">${icon("dna")}</span><div><strong>${esc(pr.name)}</strong><small>${esc(pr.profile.confidence)} confidence · ${pr.profile.wordCount || 0} sample words</small></div>${button("profile-use", "Use", "text-button", 'data-id="' + esc(pr.id) + '"')}${button("profile-edit", icon("pen", 16), "icon-button small", 'data-id="' + esc(pr.id) + '" aria-label="Edit profile"')}</div>`).join("") || '<p class="muted">Save a profile to reuse its patterns on another assignment.</p>'}`,
  );
}
function renderProjects() {
  shell(
    `<div class="page-heading"><span class="eyebrow">YOUR PAPERS</span><h1>A place for every draft.</h1><p>Your work and research, saved together.</p></div><div class="two-buttons">${button("import", icon("upload", 17) + " Import draft", "primary")}${button("new", icon("plus", 17) + " New workspace", "secondary")}</div><section class="section">${state.projects.length ? paperCards() : empty("A clean page, a clear mind", "Your papers will appear here. Import something you’ve started.")} </section><div class="context-card"><span class="tinted-icon">${icon("shield")}</span><div><strong>Keep a backup you can take with you.</strong><p>Export a Draft2Done project to keep your original, working draft, research and revision history together.</p></div></div>`,
  );
}
function renderSettings() {
  const connection = addonStatus();
  const s = state.settings;
  shell(
    `<div class="page-heading"><span class="eyebrow">MAKE IT YOURS</span><h1>Your writing space.</h1><p>Saved on your phone. Shared when you approve.</p></div><section class="card settings-card"><h3>${icon("light", 18)} General</h3><label>Appearance<select data-setting="theme"><option value="light" ${s.theme === "light" ? "selected" : ""}>Light · paper & ink</option><option value="dark" ${s.theme === "dark" ? "selected" : ""}>Dark · midnight desk</option></select></label><label class="checkbox-row"><span>Autosave locally</span><input type="checkbox" data-setting="autosave" ${s.autosave ? "checked" : ""}></label></section><section class="card"><h3>Writing AI on this phone</h3><p>ChatGPT connects directly from your phone. No computer, shared network or local server is needed. You review and approve the full content before each AI writing request.</p><p>${s.backend === "chatgpt" ? "ChatGPT is selected for Helper and Complete." : "An optional computer/server backend is selected."}</p>${s.backend !== "chatgpt" ? button("chatgpt-use-writing", "Use ChatGPT for writing", "primary") : ""}<small>Editing, local saving, Writer DNA and draft observations work without a ChatGPT connection. Free scholarly source search is separate.</small></section>${accountPanel(connection, chatgptModels, s.chatgptModel)}<section class="card settings-card"><details><summary class="advanced-title">Optional computer or server AI (advanced)</summary><div class="section-heading"><h3>${icon("spark", 18)} Local AI</h3><span class="pill">No model included</span></div><p>Connect to Ollama, LM Studio or a llama.cpp server. On a phone, <strong>127.0.0.1 means the phone</strong>. For a computer on your Wi-Fi, enter its private LAN address.</p><p>For essays, choose a general writing or instruct model. Coding models can struggle with natural prose and Writer DNA. Review the wording and evidence with any model. Find models prefers an installed general model when you have not selected one.</p><label>Backend<select data-setting="backend"><option value="chatgpt" ${s.backend === "chatgpt" ? "selected" : ""}>ChatGPT directly on this phone</option><option value="ollama" ${s.backend === "ollama" ? "selected" : ""}>Ollama</option><option value="openai" ${s.backend === "openai" ? "selected" : ""}>Local OpenAI-compatible (LM Studio / llama.cpp)</option></select></label><label>Server address<input data-setting="endpoint" type="url" value="${esc(s.endpoint)}" placeholder="http://192.168.1.20:11434" autocapitalize="off"></label><div class="two-buttons">${button("discover", "Find models", "secondary")}${button("test-ai", "Test connection", "primary")}</div><label>Selected model<input data-setting="model" list="model-list" value="${esc(s.model)}" placeholder="Discover or enter an installed model"><datalist id="model-list">${models.map((m) => `<option value="${esc(typeof m === "string" ? m : m.id || m.name)}">`).join("")}</datalist></label>${localRecommendationNote()}<div class="inline-options"><label>Context tokens<input type="number" min="2048" max="32768" data-setting="context" value="${s.context}"></label><label>Output tokens / step<input type="number" min="64" max="4096" data-setting="maxTokens" value="${s.maxTokens}"></label></div><label>Temperature<input type="number" min="0" max="1.5" step="0.1" data-setting="temperature" value="${s.temperature}"></label><label class="checkbox-row"><span>CPU only (Ollama)</span><input type="checkbox" data-setting="cpuOnly" ${s.cpuOnly ? "checked" : ""}></label><small>8,192 context tokens suits medium drafts; smaller contexts use less memory. Model size and context must fit the server’s memory. This app never downloads a model or changes a server.</small></details></section><section class="card settings-card"><h3>App updates</h3><label class="checkbox-row"><span>Check for updates automatically</span><input type="checkbox" data-setting="autoUpdates" ${s.autoUpdates ? "checked" : ""}></label><p>Updates come from youriptvus/Draft2Done on GitHub. A check runs when you open the app, at most once a day. Android asks you to confirm installation.</p><p>${esc(state.updateInfo?.message || "No update check yet.")}</p><div class="two-buttons">${button("updates-check", "Check now", "secondary")}${state.updateInfo?.available ? button("updates-install", "Install update", "primary") : ""}</div></section><section class="card settings-card"><h3>${icon("pen", 18)} Writing & export</h3>${writingPreferences(s)}<label>Default voice<select data-setting="defaultVoice"><option value="original" ${s.defaultVoice === "original" ? "selected" : ""}>Original Voice</option><option value="polished" ${s.defaultVoice === "polished" ? "selected" : ""}>Polished Voice</option></select></label><label>Default citation style<select data-setting="citationStyle">${["APA", "MLA", "Chicago"].map((v) => `<option ${s.citationStyle === v ? "selected" : ""}>${v}</option>`).join("")}</select></label><label>Default export<select data-setting="defaultExport">${["docx", "pdf", "txt", "md"].map((v) => `<option ${s.defaultExport === v ? "selected" : ""}>${v}</option>`).join("")}</select></label></section><section class="card"><h3>Privacy in plain language</h3><p>Your drafts are stored in this app’s private storage. ChatGPT writing sends the exact draft and context you approve each time. Optional server AI sends the draft to your configured endpoint. Free source search sends only the visible short query. Export a project backup before uninstalling or clearing app data.</p><p>Generated text and source metadata need your review. Follow your institution’s policy for AI assistance.</p>${button("about", "About Draft2Done", "text-button")}</section>`,
  );
}
function localRecommendationNote() {
  const guidance = localModelGuidance(models);
  return `<details><summary>Writing model recommendation</summary><p>${esc(guidance.evidence)}</p><p>${guidance.recommended ? `Recommended from this server's current list: <strong>${esc(guidance.recommended)}</strong>.` : "Find models to inspect the server's installed choices."} ${!guidance.testedModelListed ? "The tested reference model is not currently listed by this server, so this recommendation cannot select it." : "The tested general writing model is available on this server."}</p><small>Guidance as of ${esc(guidance.asOf)}. Model fit depends on the server's memory and context. Choosing a model never downloads or changes it.</small></details>`;
}
function exportAppearanceFields(format, settings = false) {
  const attr = (key) =>
    settings
      ? `data-setting="export${key[0].toUpperCase() + key.slice(1)}"`
      : `id="export-${key}"`;
  return `<label>Font<select ${attr("fontFamily")}>${["Times New Roman", "Georgia", "Arial"].map((v) => `<option ${v === format.fontFamily ? "selected" : ""}>${v}</option>`).join("")}</select></label><label>Font size (pt)<input ${attr("fontSize")} type="number" min="10" max="16" step="1" value="${format.fontSize || 12}"></label><label>Line spacing<select ${attr("lineSpacing")}>${[1, 1.5, 2].map((v) => `<option value="${v}" ${Number(format.lineSpacing || 1.5) === v ? "selected" : ""}>${v === 1 ? "Single" : v === 2 ? "Double" : "1.5 lines"}</option>`).join("")}</select></label>`;
}
function writingPreferences(s) {
  return `<label>Open papers in<select data-setting="defaultMode">${[
    ["draft", "My draft"],
    ["helper", "Helper"],
    ["complete", "Complete"],
  ]
    .map(
      ([value, label]) =>
        `<option value="${value}" ${s.defaultMode === value ? "selected" : ""}>${label}</option>`,
    )
    .join(
      "",
    )}</select></label><details><summary>Default counting and document appearance</summary><p>These defaults apply to new papers. Existing papers keep their own goals and formatting.</p><label class="checkbox-row"><span>Exclude reference sections from body words</span><input type="checkbox" data-setting="excludeReferences" ${s.excludeReferences !== false ? "checked" : ""}></label><label class="checkbox-row"><span>Exclude explicitly marked title blocks</span><input type="checkbox" data-setting="excludeTitle" ${s.excludeTitle !== false ? "checked" : ""}></label><small>A title block ends with a standalone --- or &lt;!-- title-end --&gt;. Unmarked writing is counted in full.</small>${exportAppearanceFields({ fontFamily: s.exportFontFamily, fontSize: s.exportFontSize, lineSpacing: s.exportLineSpacing }, true)}</details>`;
}
function dnaDetails(dna) {
  const repeated = dna.repeatedExpressions || [],
    formality = dna.formality,
    spelling = dna.spellingVariants,
    organization = dna.paragraphOrganization;
  return `<details class="card"><summary>More about your writing patterns</summary><dl><dt>Repeated expressions</dt><dd>${esc(repeated.map((e) => `${e.phrase} (${e.count})`).join(", ") || "No recurring expression met the sample threshold.")}</dd>${formality ? `<dt>Observed language cues</dt><dd>${formality.contractions.count} contractions (${formality.contractions.per100Words} per 100 words). ${esc(formality.note)}</dd><dt>Academic phrasing</dt><dd>${esc(formality.academicCues.map((e) => e.phrase).join(", ") || "No listed cues detected")}</dd><dt>Conversational phrasing</dt><dd>${esc(formality.conversationalCues.map((e) => e.phrase).join(", ") || "No listed cues detected")}</dd>` : "<dd>Reanalyze the draft to refresh these sample observations.</dd>"}${spelling ? `<dt>Observed spelling variants</dt><dd>${esc(spelling.observations.map((o) => o.forms.map((f) => `${f.form} (${f.count})`).join(", ")).join("; ") || "No tracked spelling variants observed.")} ${esc(spelling.note)}</dd>` : ""}</dl>${organization ? `<h3>Paragraph organization</h3><p>${esc(typeof organization.summary === "string" ? organization.summary : organization.note)}</p><ol>${organization.paragraphs.map((p) => `<li><strong>Paragraph ${p.index}</strong>: ${esc(p.possibleRole)}<p>${esc(p.opening)}</p></li>`).join("")}</ol><small>${esc(organization.note)}</small>` : ""}</details>`;
}
function updateExportAppearance(p) {
  const fontSize = Number(document.querySelector("#export-fontSize").value);
  if (!Number.isInteger(fontSize) || fontSize < 10 || fontSize > 16)
    throw new Error("Choose a font size from 10 to 16 points.");
  p.exportFormatting = {
    fontFamily: document.querySelector("#export-fontFamily").value,
    fontSize,
    lineSpacing: Number(document.querySelector("#export-lineSpacing").value),
  };
  const preview = document.querySelector(".export-preview");
  Object.assign(preview.style, {
    fontFamily: p.exportFormatting.fontFamily,
    fontSize: `${fontSize}pt`,
    lineHeight: String(p.exportFormatting.lineSpacing),
  });
  Object.assign(preview.querySelector(".pre-wrap").style, {
    fontFamily: "inherit",
    fontSize: "inherit",
    lineHeight: "inherit",
  });
}

function modal(title, body, footer = "") {
  if (remoteConsent) finishRemoteConsent(false);
  document.querySelector("#modal-root").innerHTML =
    `<div class="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="modal-heading"><h2>${esc(title)}</h2>${button("modal-close", icon("close"), "icon-button", 'aria-label="Close dialog"')}</div><div class="modal-body">${body}</div>${footer ? `<div class="modal-footer">${footer}</div>` : ""}</section></div>`;
  setTimeout(() => {
    const dialog = document.querySelector(".modal");
    if (dialog && !dialog.contains(document.activeElement))
      dialog
        .querySelector(
          "input:not([readonly]),textarea:not([readonly]),select,button",
        )
        ?.focus({ preventScroll: true });
  }, 20);
}
function closeModal() {
  if (remoteConsent) {
    finishRemoteConsent(false);
    return;
  }
  document.querySelector("#modal-root").innerHTML = "";
}
function requirements() {
  const p = getProject();
  modal(
    "Assignment & word goal",
    `<label class="field-label">What is the minimum required word count?<input id="req-minimum" type="number" min="1" max="50000" value="${p.minimum}"></label><label class="field-label">Optional maximum (0 = no maximum)<input id="req-maximum" type="number" min="0" max="100000" value="${p.maximum}"></label><label class="field-label">Assignment instructions<textarea id="req-instructions" rows="4">${esc(p.instructions)}</textarea></label>${button("requirements-import", icon("upload", 15) + " Import assignment text", "text-button")}<label class="field-label">Rubric<textarea id="req-rubric" rows="3">${esc(p.rubric)}</textarea></label><label class="field-label">Academic level<input id="req-level" value="${esc(p.academicLevel)}"></label><label class="field-label">Required headings / required sources<textarea id="req-headings" rows="2">${esc(p.requiredHeadings)}</textarea></label><label class="field-label">Citation style<select id="req-citation">${["APA", "MLA", "Chicago"].map((v) => `<option ${p.citationStyle === v ? "selected" : ""}>${v}</option>`).join("")}</select></label><label class="checkbox-row"><span>Exclude references / bibliography from body count</span><input id="req-references" type="checkbox" ${p.countingRules.excludeReferences ? "checked" : ""}></label><label class="checkbox-row"><span>Exclude title block before “---” or &lt;!-- title-end --&gt;</span><input id="req-title" type="checkbox" ${p.countingRules.excludeTitle ? "checked" : ""}></label><small>Reference exclusion recognizes a separate References, Works Cited or Bibliography heading. Mark title information with a separator before enabling title exclusion.</small>`,
    button("requirements-save", "Save requirements", "primary"),
  );
}
function generationPlan() {
  const p = getProject(),
    a = analyzeDraft(p),
    point = continuationPoint(p);
  const query = (state.lastQuery || a.topic)
    .replace(/[\r\n]/g, " ")
    .slice(0, 240);
  modal(
    "Review your continuation plan",
    `<p>Keep your thesis: ${esc(a.thesis)}</p><p>${progress(p).remaining} more body words are needed. New sections stay separate until you approve them.</p>${a.gaps.length ? `<details><summary>Draft gaps guiding the plan</summary><ul>${a.gaps.map((g) => `<li>${esc(g)}</li>`).join("")}</ul></details>` : ""}${point.unfinished ? `<div class="alert"><strong>Your final thought may be unfinished</strong><p class="pre-wrap">…${esc(point.excerpt)}</p><label class="checkbox-row"><span>Start a new paragraph</span><input type="radio" name="continuation-mode" value="paragraph" checked></label><label class="checkbox-row"><span>Finish this thought without changing my words</span><input type="radio" name="continuation-mode" value="inline"></label></div>` : ""}<h3>Evidence to use</h3><p>${p.sources.length ? "Review the sources you want included. Only retrieved abstracts or extracts can support model research claims." : "No sources are saved yet. Without evidence, generation develops your existing reasoning and flags claims needing research."}</p>${p.sources.map((s) => `<label class="checkbox-row"><span>${esc(s.title)} <small>(${esc(s.verification)})</small></span><input type="checkbox" data-source="${esc(s.id)}" ${s.included !== false ? "checked" : ""}></label>`).join("")}<details ${state.settings.online ? "open" : ""}><summary>Find evidence before writing</summary><p>Only the short queries below are sent to the selected scholarly providers. Review and edit them. Free source search sends only these queries. ChatGPT writing has a separate full-content approval before every request.</p><label class="field-label">Topic query<input class="plan-query" value="${esc(query)}" maxlength="240"></label><label class="field-label">A different perspective (optional)<input class="plan-query" value="${esc((a.topic + " conflicting evidence limitations").slice(0, 240))}" maxlength="240"></label><small>${state.settings.online ? "Online research is enabled. Results will be saved for you to inspect before generation." : "Enable Online in Research to search. You can continue with saved evidence while offline."}</small>${state.settings.online ? button("generate-research", "Find sources, then review", "secondary") : button("page", "Open Research", "text-button", 'data-id="research"')}</details>`,
    button("generate-confirm", "Generate with saved sources", "primary"),
  );
}
async function generateContinuation(p, continuation) {
  closeModal();
  await runTask("Planning a continuation in your voice…", async (signal) => {
    const proposals = await ai().complete(
      p,
      analyzeDraft(p),
      activeProfile(p),
      {
        signal,
        continuation,
        onProgress(value) {
          if (busy) {
            busy.message =
              value.message ||
              `Writing step ${value.round} · ${value.currentWords} words`;
            const el = document.querySelector("#task-message");
            if (el) el.textContent = busy.message;
          }
        },
      },
    );
    const latest = state.projects.find((item) => item.id === p.id);
    if (!latest)
      throw new Error("This project was removed. Results were not merged.");
    latest.proposals.push(...proposals);
    updateProject(latest);
    toast(`${proposals.length} proposed section(s) ready for review.`);
  });
}
function sourceFields(s = {}) {
  return `<label class="field-label">Title<input id="source-title" value="${esc(s.title || "")}"></label><label class="field-label">Authors (one per line)<textarea id="source-authors" rows="3">${esc((s.authors || []).join("\n"))}</textarea></label><label class="field-label">Year<input id="source-year" value="${esc(s.year || "")}"></label><label class="field-label">Source URL<input id="source-url" type="url" value="${esc(s.url || "")}"></label><details><summary>Publication details (optional)</summary>${[
    ["doi", "DOI"],
    ["journal", "Journal / publisher"],
    ["volume", "Volume"],
    ["issue", "Issue"],
    ["pages", "Publication page range"],
    ["type", "Source type"],
  ]
    .map(
      ([key, label]) =>
        `<label class="field-label">${label}<input id="source-${key}" value="${esc(s[key] || "")}"></label>`,
    )
    .join(
      "",
    )}<small>Enter only known metadata. Publication pages do not supply a page locator for a quotation.</small><label class="field-label">Available abstract / excerpt<textarea id="source-abstract" rows="4">${esc(s.abstract || "")}</textarea></label></details><label class="field-label">Research notes<textarea id="source-notes" rows="3">${esc(s.notes || "")}</textarea></label>`;
}
function saveSourceFields(s) {
  const fields = Object.fromEntries(
    [
      "title",
      "year",
      "url",
      "doi",
      "journal",
      "volume",
      "issue",
      "pages",
      "type",
      "abstract",
      "notes",
    ].map((key) => [
      key,
      document.querySelector(`#source-${key}`).value.trim(),
    ]),
  );
  if (!fields.title) throw new Error("Enter a source title.");
  if (fields.url && !/^https?:\/\//i.test(fields.url))
    throw new Error("Use an http or https source URL.");
  fields.doi = fields.doi.replace(
    /^(?:https?:\/\/(?:dx\.)?doi\.org\/|doi:\s*)/i,
    "",
  );
  if (fields.doi && !/^10\.\d{4,9}\/\S+$/i.test(fields.doi))
    throw new Error("Enter a DOI such as 10.1234/example, or leave it blank.");
  if (!fields.url && fields.doi) fields.url = `https://doi.org/${fields.doi}`;
  Object.assign(s, fields, {
    authors: document
      .querySelector("#source-authors")
      .value.split("\n")
      .map((v) => v.trim())
      .filter(Boolean),
    verification: "unverified",
  });
}
function citationDialog(selectedId = "") {
  const p = getProject(),
    body = p.workingText.slice(0, continuationPoint(p).position);
  const positions = [...body.matchAll(/\n\s*\n/g)]
    .map((m) => m.index)
    .concat(body.length);
  modal(
    "Insert a citation",
    `<p>Select the sources supporting one claim. This inserts only a citation in your working copy.</p>${p.sources.map((s) => `<label class="checkbox-row"><span>${esc(s.title)}</span><input type="checkbox" data-cite-source="${esc(s.id)}" ${s.id === selectedId ? "checked" : ""}></label>`).join("")}<label class="field-label">Add after this paragraph<select id="citation-position">${positions.map((position, i) => `<option value="${position}" ${i === positions.length - 1 ? "selected" : ""}>${i + 1}: ${esc(body.slice(Math.max(0, position - 100), position).replace(/\n/g, " "))}</option>`).join("")}</select></label><small>Check that each source supports the selected paragraph. Missing bibliographic details remain visible.</small>`,
    button("citation-insert", "Insert selected citation", "primary"),
  );
}
async function runTask(message, fn) {
  if (busy) {
    toast("An operation is already running. Stop it before starting another.");
    return;
  }
  lastError = "";
  busy = { message, controller: new AbortController() };
  render();
  try {
    await fn(busy.controller.signal);
  } catch (e) {
    if (e.name === "AbortError" || busy?.controller.signal.aborted)
      toast("Operation cancelled. Your draft is unchanged.");
    else {
      lastError = e.message;
      toast(e.message);
      modal(
        "Couldn’t finish this step",
        `<p>${esc(e.message)}</p><p>Your draft and saved sources are preserved. Check your connection or settings and try again.</p>`,
        button("modal-close", "Close", "secondary"),
      );
    }
  } finally {
    busy = null;
    render();
  }
}
function ai() {
  const settings = {
    ...state.settings,
    model:
      state.settings.backend === "chatgpt"
        ? state.settings.chatgptModel
        : state.settings.model,
  };
  return new AIClient(settings, transport, {
    beforeRemoteRequest: previewRemoteRequest,
    async requestChatGPT(payload, signal) {
      const receipt = approvedRemote;
      approvedRemote = null;
      if (
        !receipt ||
        receipt.payload !== JSON.stringify(payload) ||
        receipt.state !== remoteState()
      )
        throw remoteRequestChanged(
          "The request changed after approval. Review it again before sending.",
        );
      const result = await addonRequest("assist", payload, signal);
      if (receipt.state !== remoteState())
        throw remoteRequestChanged(
          "Your draft, requirements, style, sources or AI account changed while ChatGPT was responding. Its result was discarded; review a fresh request.",
        );
      return result;
    },
    listChatGPTModels: (signal) => addonRequest("models", {}, signal),
  });
}
function remoteState() {
  const account = addonStatus();
  return JSON.stringify({
    project: getProject(),
    activeId: state.activeId,
    account: account.active || "",
    accountConnected: account.connected === true,
    model: state.settings.chatgptModel,
    backend: state.settings.backend,
  });
}
function remoteRequestChanged(message) {
  const error = new Error(message);
  error.code = "CONSENT_CHANGED";
  return error;
}
function previewRemoteRequest(payload, signal) {
  approvedRemote = null;
  if (signal?.aborted)
    return Promise.reject(new DOMException("Cancelled", "AbortError"));
  const recordText = payload.messages
    .filter((m) => m.role === "user")
    .map((m) => m.content)
    .join("\n");
  let record;
  try {
    record = JSON.parse(recordText);
  } catch {
    record = null;
  }
  if (
    typeof record?.draft === "string" &&
    record.draft !== getProject()?.workingText
  )
    return Promise.reject(
      remoteRequestChanged(
        "Your draft changed before the preview. Start the request again to review the current text.",
      ),
    );
  if (
    payload.model !== state.settings.chatgptModel ||
    state.settings.backend !== "chatgpt"
  )
    return Promise.reject(
      remoteRequestChanged(
        "Your AI selection changed. Review a new request before sending.",
      ),
    );
  const snapshot = remoteState(),
    exact = JSON.stringify(payload);
  const context = record ? { ...record } : null;
  if (context) delete context.draft;
  modal(
    "Review what ChatGPT will receive",
    `<p>Your phone will connect directly to ChatGPT using your selected account and <strong>${esc(payload.model)}</strong>. Your plan's limits apply.</p><div class="alert"><strong>This includes your writing</strong><p>${record?.draft != null ? "The full working draft, assignment requirements, Writer DNA and included source excerpts below will be sent." : "The exact request below will be sent."} Approve this request only if you want to share this material with ChatGPT.</p></div>${record?.draft != null ? `<label class="field-label">Full working draft<textarea id="remote-draft-preview" rows="10" readonly>${esc(record.draft)}</textarea></label><details><summary>Assignment, Writer DNA, sources and other request context</summary><pre class="pre-wrap">${esc(JSON.stringify(context, null, 2))}</pre></details>` : ""}<details><summary>Exact approved writing request</summary><p>The displayed system message becomes API instructions; the user message becomes input. The phone adds no further prompt content. The request uses store:false and stream:true, with no tools.</p><pre id="remote-payload-preview" class="pre-wrap">${esc(JSON.stringify(payload, null, 2))}</pre></details><small>Nothing has been sent yet. This approval applies once to this exact request. A further generation step asks again. Cancel keeps your draft on the phone.</small>`,
    button("remote-send", "Send to ChatGPT", "primary") +
      button("remote-cancel", "Keep this on my phone", "secondary"),
  );
  return new Promise((resolve, reject) => {
    const abort = () => finishRemoteConsent(false);
    remoteConsent = {
      resolve,
      reject,
      signal,
      abort,
      state: snapshot,
      payload: exact,
    };
    signal?.addEventListener("abort", abort, { once: true });
  });
}
function finishRemoteConsent(approve) {
  const pending = remoteConsent;
  if (!pending) return;
  remoteConsent = null;
  pending.signal?.removeEventListener("abort", pending.abort);
  document.querySelector("#modal-root").innerHTML = "";
  if (!approve || pending.signal?.aborted) {
    approvedRemote = null;
    pending.reject(new DOMException("Cancelled", "AbortError"));
  } else if (pending.state !== remoteState()) {
    approvedRemote = null;
    pending.reject(
      remoteRequestChanged(
        "Your draft, requirements, style, sources or AI account changed. Start again to review the updated request.",
      ),
    );
  } else {
    approvedRemote = { state: pending.state, payload: pending.payload };
    pending.resolve(true);
  }
}
function configuredModel() {
  return state.settings.backend === "chatgpt"
    ? state.settings.chatgptModel
    : state.settings.model;
}
function requireAI() {
  if (
    state.settings.backend === "chatgpt" &&
    (!addonStatus().connected || !state.settings.chatgptModel)
  ) {
    closeModal();
    page = "settings";
    render();
    toast(
      addonStatus().connected
        ? "Load your available ChatGPT models and choose one."
        : "Continue with ChatGPT to enable AI writing directly on this phone.",
    );
    return false;
  }
  if (!configuredModel()) {
    closeModal();
    toast("Choose an installed model in Settings first.");
    page = "settings";
    render();
    return false;
  }
  return true;
}
function requirementSave() {
  const p = getProject();
  const min = Number(document.querySelector("#req-minimum").value),
    max = Number(document.querySelector("#req-maximum").value);
  if (
    !Number.isInteger(min) ||
    min < 1 ||
    min > 50000 ||
    !Number.isInteger(max) ||
    max < 0 ||
    (max && max < min)
  ) {
    toast(
      "Enter a minimum from 1 to 50,000 and a maximum of 0 or at least the minimum.",
    );
    return;
  }
  Object.assign(p, {
    minimum: min,
    maximum: max,
    instructions: document.querySelector("#req-instructions").value,
    rubric: document.querySelector("#req-rubric").value,
    academicLevel: document.querySelector("#req-level").value,
    requiredHeadings: document.querySelector("#req-headings").value,
    citationStyle: document.querySelector("#req-citation").value,
    countingRules: {
      excludeReferences: document.querySelector("#req-references").checked,
      excludeTitle: document.querySelector("#req-title").checked,
    },
  });
  updateProject(p);
  closeModal();
  render();
}
function openChatGPTResearch() {
  const p = getProject(),
    status = addonStatus();
  if (!status.connected) {
    page = "settings";
    render();
    toast(
      "Connect your ChatGPT account in Settings to enable this optional research tool.",
    );
    return;
  }
  if (!state.settings.chatgptModel) {
    page = "settings";
    render();
    toast("Load your available ChatGPT models, then choose one for research.");
    return;
  }
  const sources = p.sources
    .filter((s) => s.included !== false && s.abstract)
    .slice(0, 6);
  modal(
    "Ask ChatGPT for research",
    `<p>Your ChatGPT plan’s usage limits apply. This sends the question below and only the source excerpts you select. Your paper is not sent.</p><label class="field-label">Research question<input id="chatgpt-query" maxlength="240" value="${esc(document.querySelector("#research-query")?.value || state.lastQuery || analyzeDraft(p).topic)}"></label><label class="checkbox-row"><span>Use web search (if your model and account support it)</span><input type="checkbox" id="chatgpt-web" checked></label><h3>Optional source excerpts</h3>${sources.length ? sources.map((s) => `<label class="checkbox-row"><span>${esc(s.title)}<small>${esc(s.abstract.slice(0, 160))}…</small></span><input type="checkbox" data-chatgpt-source="${esc(s.id)}"></label>`).join("") : "<p>No retrieved source excerpts are available. Web research can still be requested.</p>"}<small>Model: ${esc(state.settings.chatgptModel)}. No API-key fallback or automatic paper generation.</small>`,
    button("chatgpt-research-send", "Send research request", "primary"),
  );
}
async function sendChatGPTResearch() {
  const p = getProject(),
    query = document.querySelector("#chatgpt-query").value.trim(),
    webSearch = document.querySelector("#chatgpt-web").checked;
  if (!query || query.length > 240) {
    toast("Enter a research question of up to 240 characters.");
    return;
  }
  const selected = [
    ...document.querySelectorAll("[data-chatgpt-source]:checked"),
  ].map((el) => el.dataset.chatgptSource);
  const evidence = p.sources
    .filter((s) => selected.includes(s.id))
    .map((s) => ({
      id: s.id,
      title: s.title,
      url: s.url,
      excerpt: s.abstract.slice(0, 2400),
    }));
  closeModal();
  await runTask("ChatGPT is researching your question…", async (signal) => {
    const result = await addonRequest(
      "research",
      { query, webSearch, evidence, model: state.settings.chatgptModel },
      signal,
    );
    if (!state.projects.some((item) => item.id === p.id))
      throw new Error("The project was removed while research was running.");
    const latest = state.projects.find((item) => item.id === p.id);
    latest.researchBriefs ??= [];
    latest.researchBriefs.push(result);
    updateProject(latest);
    toast("Research brief ready. Review the evidence and links.");
  });
}
let importPurpose = "draft";
onNotice(toast);
onImported(async (payload) => {
  try {
    const doc = await readDocument(payload);
    if (importPurpose === "instructions") {
      importPurpose = "draft";
      const p = getProject();
      p.instructions = doc.text;
      updateProject(p);
      requirements();
      toast("Assignment instructions imported.");
      return;
    }
    if (doc.project) {
      const decoded = JSON.parse(doc.text);
      const p = loadProject(decoded.project || decoded);
      if (recovery) {
        recovery = "";
        toast(
          "Backup imported. The unreadable saved file is retained as the previous workspace backup.",
        );
      }
      if (state.projects.some((s) => s.id === p.id)) p.id = uid();
      updateProject(p);
      state.activeId = p.id;
    } else {
      const title = doc.name.replace(/\.[^.]+$/, "");
      const p = projectFromDraft(title, doc.text);
      updateProject(p);
      state.activeId = p.id;
      if (doc.warning) toast(doc.warning);
    }
    persist();
    page = "write";
    tab = defaultWritingTab();
    render();
  } catch (e) {
    toast(`Import failed: ${e.message}`);
  }
});
function defaultWritingTab() {
  return ["draft", "helper", "complete"].includes(state.settings.defaultMode)
    ? state.settings.defaultMode
    : "draft";
}
function projectFromDraft(title, text) {
  const p = createProject(title, text);
  p.countingRules = {
    excludeReferences: state.settings.excludeReferences !== false,
    excludeTitle: state.settings.excludeTitle !== false,
  };
  p.exportFormatting = {
    fontFamily: state.settings.exportFontFamily,
    fontSize: state.settings.exportFontSize,
    lineSpacing: state.settings.exportLineSpacing,
  };
  p.voice = state.settings.defaultVoice;
  p.citationStyle = state.settings.citationStyle;
  return p;
}
function newPaper(text = "", title = "Untitled paper") {
  const p = projectFromDraft(title, text);
  state.projects.push(p);
  state.activeId = p.id;
  page = "write";
  tab = defaultWritingTab();
  persist();
  render();
}
async function handleAction(action, target) {
  const p = getProject(),
    id = target.dataset.id;
  switch (action) {
    case "information-concept":
      if (p && state.settings.informationLinks) showInformation(p, target);
      break;
    case "information-open": {
      const url = safeInformationUrl(target.dataset.url);
      if (!url) throw new Error("This source does not have a valid public web address.");
      openExternal(url);
      break;
    }
    case "information-research": {
      const query = document.querySelector("#information-query").value.trim();
      if (!query || query.length > 240 || /[\r\n]/.test(query))
        throw new Error("Use a short research query of up to 240 characters.");
      state.lastQuery = query;
      persist();
      closeModal();
      page = "research";
      render();
      toast("Review the query and providers, then choose Search when ready.");
      break;
    }
    case "paste-use":
      usePastedDraft();
      break;
    case "remote-send":
      finishRemoteConsent(true);
      break;
    case "remote-cancel":
      finishRemoteConsent(false);
      break;
    case "chatgpt-use-writing":
      state.settings.backend = "chatgpt";
      state.settings.aiRouteChosen = true;
      persist();
      render();
      break;
    case "page":
      closeModal();
      page = id;
      render();
      break;
    case "chatgpt-link":
      openExternal(target.dataset.url);
      break;
    case "chatgpt-usage":
      openExternal("https://chatgpt.com/settings/usage");
      break;
    case "chatgpt-signin":
    case "chatgpt-add":
      closeModal();
      await runTask(
        "Complete sign-in in your browser, then return here…",
        async (signal) => {
          const status = await addonRequest(
            "signin",
            {
              account:
                action === "chatgpt-add" ? "" : addonStatus().active || "",
            },
            signal,
          );
          chatgptModels = [];
          state.settings.chatgptModel = "";
          state.settings.backend = "chatgpt";
          persist();
          toast(
            status.connected
              ? "ChatGPT connected. Load available models to choose one."
              : "Signed in. ChatGPT plan permission is not enabled.",
          );
        },
      );
      break;
    case "chatgpt-models":
      await runTask(
        "Loading your available ChatGPT models…",
        async (signal) => {
          const result = await addonRequest("models", {}, signal);
          chatgptModels = result.models || [];
          state.settings.chatgptModels = chatgptModels;
          if (
            !chatgptModels.some((m) => m.slug === state.settings.chatgptModel)
          )
            state.settings.chatgptModel =
              recommendedResearchModel(chatgptModels)?.slug ||
              chatgptModels[0]?.slug ||
              "";
          persist();
          toast(chatgptModels.length + " ChatGPT model(s) available.");
        },
      );
      break;
    case "chatgpt-recommend-model": {
      const recommended = recommendedResearchModel(chatgptModels);
      if (!recommended) {
        toast("Load your available models and choose one from the list.");
        break;
      }
      state.settings.chatgptModel = recommended.slug;
      persist();
      render();
      toast(`AI model selected: ${recommended.slug}. No AI request was sent.`);
      break;
    }
    case "chatgpt-signout":
      await runTask("Signing out of ChatGPT…", async (signal) => {
        const result = await addonRequest("signout", {}, signal);
        chatgptModels = [];
        state.settings.chatgptModel = "";
        state.settings.chatgptModels = [];
        persist();
        toast(result.message || "Signed out.");
      });
      break;
    case "chatgpt-research":
      openChatGPTResearch();
      break;
    case "updates-check":
      await runTask("Checking for a Draft2Done update…", async (signal) => {
        await checkForUpdates(true, signal);
      });
      break;
    case "updates-install":
      await runTask("Downloading and verifying the update…", async (signal) => {
        const result = await addonRequest("update-install", {}, signal);
        state.updateInfo = result;
        persist();
        toast(result.message);
      });
      break;
    case "chatgpt-research-send":
      await sendChatGPTResearch();
      break;
    case "modal-close":
      closeModal();
      break;
    case "import":
      importPurpose = "draft";
      importFile();
      break;
    case "requirements-import":
      importPurpose = "instructions";
      importFile();
      break;
    case "new":
      newPaper();
      break;
    case "sample":
      newPaper(
        "When people talk about cities, they usually talk about roads and buildings. I think green spaces matter just as much. A small park can give people somewhere to walk, meet their neighbours, and take a break from a busy day. This paper looks at why access to green space should be part of how we plan a city.\n\nOne reason is that not everyone has their own garden. For someone living in a flat, a public park might be the closest place to spend time outside. But having a park nearby is only part of the question. It also needs to feel safe and be easy to get to. If we want green spaces to help the whole community, we need to think about who actually gets to use them.",
        "Green spaces and everyday city life",
      );
      toast(
        "Fictional sample loaded. Replace it with your own draft when ready.",
      );
      break;
    case "open":
      state.activeId = id;
      page = "write";
      tab = ["draft", "helper", "complete"].includes(state.settings.defaultMode)
        ? state.settings.defaultMode
        : "draft";
      persist();
      render();
      break;
    case "tab":
      tab = id;
      render();
      break;
    case "save":
      if (p) {
        updateProject(snapshot(p, "Manual save"));
        persist(true);
        toast("Saved locally with a revision checkpoint.");
      }
      break;
    case "rename":
      modal(
        "Name your paper",
        `<label class="field-label">Paper title<input id="rename-input" maxlength="160" value="${esc(p.title)}"></label>`,
        button("rename-save", "Save title", "primary"),
      );
      break;
    case "rename-save":
      p.title =
        document.querySelector("#rename-input").value.trim() ||
        "Untitled paper";
      updateProject(p);
      closeModal();
      render();
      break;
    case "requirements":
      requirements();
      break;
    case "requirements-save":
      requirementSave();
      break;
    case "original":
      modal(
        "Protected original",
        `<div class="alert success">This is the imported starting text. Editing your working draft never changes it.</div><p class="pre-wrap original-text">${esc(p.originalText || "No imported original. Paste or type your own draft, then choose “Protect current draft” in Paper actions.")}</p>`,
      );
      break;
    case "paper-menu":
      modal(
        "Paper actions",
        `<div class="action-menu">${button("rename", icon("pen") + " Rename paper")}${button("requirements", icon("check") + " Assignment & word goal")}${button("original", icon("shield") + " View protected original")}${button("protect", icon("shield") + " Protect current draft")}${button("history", icon("clock") + " Revision history & contributions")}${button("export", icon("export") + " Export & preview")}${button("backup", icon("folder") + " Back up project (.d2d)")}${button("delete", icon("close") + " Delete local project", "danger-button")}</div>`,
      );
      break;
    case "protect":
      if (p.originalText) {
        toast(
          "The original is already protected and cannot be replaced. Import a new project for a different original.",
        );
        break;
      }
      if (!p.workingText.trim()) {
        toast("Add your own writing first.");
        break;
      }
      p.originalText = p.workingText;
      updateProject(p);
      closeModal();
      toast("Starting draft protected.");
      break;
    case "history":
      modal(
        "History & contributions",
        `<h3>Generated contributions</h3>${p.contributions.map((c) => `<div class="history-item"><strong>Accepted ${new Date(c.acceptedAt || c.createdAt || Date.now()).toLocaleString()}</strong><p>${esc((c.text || "").slice(0, 180))}</p><small>Source IDs: ${esc((c.sourceIds || []).join(", ") || "No external evidence attached")}</small></div>`).join("") || "<p>No generated sections accepted.</p>"}<h3>Revision checkpoints</h3>${
          p.revisions
            .slice()
            .reverse()
            .map(
              (r) =>
                `<div class="history-item"><strong>${esc(r.label || r.reason || "Checkpoint")}</strong><small>${new Date(r.createdAt || r.timestamp || Date.now()).toLocaleString()}</small>${button("restore", "Restore working copy", "text-button", 'data-id="' + esc(r.id) + '"')}</div>`,
            )
            .join("") ||
          "<p>Use Save to create a checkpoint. Accepting additions also records a revision.</p>"
        }`,
      );
      break;
    case "restore":
      modal(
        "Restore this revision?",
        `<p>Your current working copy will be recorded in history before restoring this checkpoint. Your protected original is retained.</p>`,
        button(
          "restore-confirm",
          "Restore revision",
          "primary",
          'data-id="' + esc(id) + '"',
        ),
      );
      break;
    case "restore-confirm":
      updateProject(revertRevision(p, id));
      closeModal();
      render();
      toast("Revision restored.");
      break;
    case "delete":
      modal(
        "Delete this local project?",
        `<p>“${esc(p.title)}” and its saved research will be removed from this app. Export a project backup first if you want to keep it.</p>`,
        button("delete-confirm", "Delete project", "danger-button") +
          button("modal-close", "Keep project", "secondary"),
      );
      break;
    case "delete-confirm":
      state.projects = state.projects.filter((s) => s.id !== p.id);
      state.activeId = state.projects[0]?.id || null;
      persist();
      closeModal();
      page = "projects";
      render();
      break;
    case "format": {
      const el = document.querySelector("#draft-editor");
      const { selectionStart: a, selectionEnd: b } = el;
      const selected = el.value.slice(a, b);
      const value =
        target.dataset.format === "bold"
          ? `**${selected || "text"}**`
          : target.dataset.format === "italic"
            ? `*${selected || "text"}*`
            : `\n## ${selected || "Heading"}\n`;
      el.setRangeText(value, a, b, "select");
      el.dispatchEvent(new Event("input", { bubbles: true }));
      el.focus();
      break;
    }
    case "editor-undo":
      restoreEditor(p, "undo");
      break;
    case "editor-redo":
      restoreEditor(p, "redo");
      break;
    case "analyze":
      if (countWords(p.workingText) < 40) {
        toast("Add at least 40 words from your existing draft first.");
        break;
      }
      p.profile = {
        ...analyzeDNA(p.workingText),
        userNotes: p.profile?.userNotes || "",
      };
      updateProject(p);
      if (
        !configuredModel() ||
        (state.settings.backend === "chatgpt" && !addonStatus().connected)
      ) {
        toast(
          "Draft observations updated on this device. Continue with ChatGPT for deeper assistance.",
        );
        render();
        break;
      }
      if (requireAI())
        await runTask("Reading your draft…", async (signal) => {
          p.helperNotes = await ai().helper(
            p,
            analyzeDraft(p),
            activeProfile(p),
            "Analyze thesis, structure, remaining gaps and assignment requirements. Give practical next steps.",
            signal,
          );
          const latest = state.projects.find((v) => v.id === p.id);
          if (latest) {
            latest.helperNotes = p.helperNotes;
            updateProject(latest);
          }
        });
      break;
    case "suggestion":
      showSuggestion(analyzeDraft(p).suggestions[Number(target.dataset.index)]);
      break;
    case "ai-suggestion": {
      const text = helperSuggestions(p.helperNotes || state.notes[p.id] || "")[
        Number(target.dataset.index)
      ];
      showSuggestion({
        title: "Explore this suggestion",
        detail: text,
        question: analyzeDraft(p).researchQuestions[0],
      });
      break;
    }
    case "research-question":
      state.lastQuery = analyzeDraft(p).researchQuestions[
        Number(target.dataset.index)
      ].slice(0, 240);
      page = "research";
      render();
      break;
    case "helper-action": {
      const actions = [
        "Explain this idea",
        "Explain how this connects to my paper",
        "Suggest a transition without replacing my writing",
        "Show the structure of a possible next paragraph, not a complete paper",
        "Find more research",
      ];
      if (Number(target.dataset.index) >= 4) {
        state.lastQuery = selectedSuggestion.question.slice(0, 250);
        page = "research";
        closeModal();
        render();
        break;
      }
      if (!requireAI()) break;
      const request = `${actions[Number(target.dataset.index)]}: ${selectedSuggestion.title}. ${selectedSuggestion.detail}`;
      closeModal();
      await runTask("Exploring your next step…", async (signal) => {
        p.helperNotes = await ai().helper(
          p,
          analyzeDraft(p),
          activeProfile(p),
          request,
          signal,
        );
        const latest = state.projects.find((v) => v.id === p.id);
        if (latest) {
          latest.helperNotes = p.helperNotes;
          updateProject(latest);
        }
      });
      break;
    }
    case "generate":
      if (countWords(p.workingText) < 40) {
        toast("Complete starts from at least 40 words of your own writing.");
        break;
      }
      if (!p.minimum) {
        requirements();
        break;
      }
      if (!requireAI()) break;
      generationPlan();
      break;
    case "generate-confirm": {
      const inline =
        document.querySelector('[name="continuation-mode"]:checked')?.value ===
        "inline";
      const point = continuationPoint(p);
      await generateContinuation(
        p,
        inline
          ? { mode: "inline", position: point.position, anchor: point.anchor }
          : null,
      );
      break;
    }
    case "generate-research": {
      if (!state.settings.online)
        throw new Error("Enable Online in Research first.");
      const queries = [...document.querySelectorAll(".plan-query")]
        .map((el) => el.value.trim())
        .filter(Boolean);
      if (!queries.length) throw new Error("Enter a short search query first.");
      closeModal();
      let failures = [];
      let completed = false;
      await runTask("Finding evidence for your plan…", async (signal) => {
        const engine = new ResearchEngine(transport);
        for (const query of [...new Set(queries)]) {
          const result = await engine.search(
            query,
            state.settings.providers,
            signal,
          );
          const latest = state.projects.find((item) => item.id === p.id);
          if (!latest)
            throw new Error("The project was removed during research.");
          for (const source of result.sources)
            if (
              !latest.sources.some(
                (old) =>
                  (old.doi && old.doi === source.doi) ||
                  (old.url && old.url === source.url),
              )
            )
              latest.sources.push(source);
          failures.push(...result.errors.map((e) => e.message));
          updateProject(latest);
        }
        completed = true;
      });
      if (completed) {
        generationPlan();
        if (failures.length)
          toast("Some providers failed: " + failures.join("; "));
      }
      break;
    }
    case "proposal-paragraph":
      p.proposals.find((s) => s.id === id).insertion = null;
      updateProject(p);
      render();
      break;
    case "accept":
      if (
        /\[(?:citation unavailable|unverified (?:link|citation) removed)\]/i.test(
          p.proposals.find((s) => s.id === id)?.text || "",
        )
      ) {
        toast(
          "Edit the unresolved source placeholders before accepting this section.",
        );
        break;
      }
      updateProject(acceptProposal(p, id));
      render();
      toast("Section accepted into your working copy.");
      break;
    case "reject":
      updateProject(rejectProposal(p, id));
      render();
      toast("Proposal rejected. Your working draft is unchanged.");
      break;
    case "regenerate": {
      const s = p.proposals.find((s) => s.id === id);
      if (!requireAI()) break;
      await runTask("Regenerating this section…", async (signal) => {
        const revised = await ai().regenerate(
          p,
          analyzeDraft(p),
          activeProfile(p),
          s,
          signal,
        );
        const latest = state.projects.find((item) => item.id === p.id),
          current = latest?.proposals.find((item) => item.id === id);
        if (!current || current.status !== "pending")
          throw new Error(
            "The proposal changed while regeneration was running. Its accepted text was preserved.",
          );
        Object.assign(current, revised);
        updateProject(latest);
      });
      break;
    }
    case "stop":
      busy?.controller.abort();
      break;
    case "search": {
      if (!state.settings.online) {
        toast("Enable Online to send this search query to research providers.");
        break;
      }
      const query = document.querySelector("#research-query").value.trim();
      state.lastQuery = query;
      await runTask("Searching scholarly sources…", async (signal) => {
        const result = await new ResearchEngine(transport).search(
          query,
          state.settings.providers,
          signal,
        );
        const latest = state.projects.find((item) => item.id === p.id);
        if (!latest)
          throw new Error("The project was removed during research.");
        for (const s of result.sources) {
          if (
            !latest.sources.some(
              (old) => (old.doi && old.doi === s.doi) || old.url === s.url,
            )
          )
            latest.sources.push(s);
        }
        updateProject(latest);
        if (result.errors?.length)
          modal(
            "Research results",
            `<p>${result.sources.length} sources found.</p><p>${esc(result.errors.map((e) => (typeof e === "string" ? e : e.message || JSON.stringify(e))).join("\n"))}</p><p>Available results were saved. Failed providers can be retried.</p>`,
          );
        else toast(`${result.sources.length} sources found.`);
      });
      break;
    }
    case "source-open":
      openExternal(p.sources.find((s) => s.id === id).url);
      break;
    case "source-add":
      modal(
        "Add a source",
        `<p>Enter a real source you have found. Manual records remain unverified until a supported provider confirms them.</p>${sourceFields()}`,
        button("source-add-save", "Save source", "primary"),
      );
      break;
    case "source-add-save": {
      const source = {
        id: uid(),
        provider: "manual",
        included: true,
        accessedAt: new Date().toISOString(),
        pmid: "",
        pageId: "",
      };
      saveSourceFields(source);
      p.sources.push(source);
      updateProject(p);
      closeModal();
      render();
      break;
    }
    case "source-citation": {
      const s = p.sources.find((s) => s.id === id),
        c = citation(s, p.citationStyle);
      modal(
        `${p.citationStyle} citation`,
        `<p class="pre-wrap">${esc(c.inText)}</p><p class="pre-wrap">${esc(c.bibliography)}</p>${c.missing.length ? `<div class="alert">Missing metadata: ${esc(c.missing.join(", "))}</div>` : ""}${sourceFields(s)}`,
        button(
          "source-save",
          "Save metadata",
          "primary",
          `data-id="${esc(id)}"`,
        ) +
          button(
            "citation-group",
            "Insert this citation",
            "secondary",
            `data-id="${esc(id)}"`,
          ),
      );
      break;
    }
    case "source-save":
      saveSourceFields(p.sources.find((s) => s.id === id));
      updateProject(p);
      closeModal();
      render();
      break;
    case "citation-group":
      citationDialog(id);
      break;
    case "citation-insert": {
      const ids = [
        ...document.querySelectorAll("[data-cite-source]:checked"),
      ].map((el) => el.dataset.citeSource);
      if (!ids.length)
        throw new Error("Select at least one source supporting your claim.");
      const sources = p.sources.filter((s) => ids.includes(s.id));
      const c = citations(sources, p.citationStyle),
        position = Number(document.querySelector("#citation-position").value);
      const saved = snapshot(p, "Before inserting a citation");
      saved.citedSourceIds = [
        ...new Set([...(p.citedSourceIds || []), ...ids]),
      ];
      const text =
        p.workingText.slice(0, position).trimEnd() +
        " " +
        c.inText +
        p.workingText.slice(position);
      updateProject(updateWorkingText(saved, text));
      closeModal();
      page = "write";
      tab = "draft";
      render();
      toast(
        c.missing.length
          ? "Citation inserted. Check missing metadata: " + c.missing.join(", ")
          : "Citation inserted in your working copy.",
      );
      break;
    }
    case "verify":
      if (!state.settings.online) {
        toast("Enable Online to verify this record.");
        break;
      }
      await runTask("Checking source record…", async (signal) => {
        const s = p.sources.find((s) => s.id === id);
        const result = await new ResearchEngine(transport).verify(s, signal);
        const latest = state.projects.find((item) => item.id === p.id),
          current = latest?.sources.find((item) => item.id === id);
        if (!current)
          throw new Error("This source is no longer in the project.");
        Object.assign(current, result.source || result);
        updateProject(latest);
        toast(
          result.message || "Source record checked. Review verification notes.",
        );
      });
      break;
    case "research-tip":
      modal(
        "Research that strengthens a paper",
        "<p>Search one question at a time. Prefer original studies for evidence, and use encyclopedias for background. Read the available abstract, compare methods and limitations, and check the original source before drawing conclusions.</p><p>Conflicting findings need context. A retrieved record is evidence that a publication exists; it does not validate every claim in a draft.</p>",
      );
      break;
    case "ncbi":
      openExternal("https://www.ncbi.nlm.nih.gov/About/disclaimer.html");
      break;
    case "dna-refresh":
      p.profile = {
        ...analyzeDNA(p.workingText),
        userNotes: p.profile?.userNotes || "",
      };
      updateProject(p);
      render();
      toast("Style metrics updated from this draft.");
      break;
    case "profile-save":
      modal(
        "Save a writing profile",
        '<label class="field-label">Profile name<input id="profile-name" maxlength="100" value="My writing voice"></label>',
        button("profile-save-confirm", "Save profile", "primary"),
      );
      break;
    case "profile-save-confirm":
      state.profiles.push({
        id: uid(),
        name:
          document.querySelector("#profile-name").value.trim() ||
          "My writing voice",
        profile: activeProfile(p),
      });
      persist();
      closeModal();
      render();
      break;
    case "profile-use":
      p.profile = JSON.parse(
        JSON.stringify(state.profiles.find((pr) => pr.id === id).profile),
      );
      updateProject(p);
      render();
      toast("Saved voice applied to this project.");
      break;
    case "profile-edit": {
      const pr = state.profiles.find((pr) => pr.id === id);
      modal(
        "Edit saved voice",
        `<label class="field-label">Name<input id="edit-profile-name" value="${esc(pr.name)}"></label><label class="field-label">Style guidance<textarea id="edit-profile-notes" rows="5">${esc(pr.profile.userNotes || "")}</textarea></label>`,
        button(
          "profile-edit-save",
          "Save voice",
          "primary",
          'data-id="' + esc(id) + '"',
        ),
      );
      break;
    }
    case "profile-edit-save": {
      const pr = state.profiles.find((pr) => pr.id === id);
      pr.name =
        document.querySelector("#edit-profile-name").value.trim() || pr.name;
      pr.profile.userNotes = document.querySelector(
        "#edit-profile-notes",
      ).value;
      persist();
      closeModal();
      render();
      break;
    }
    case "discover":
      if (state.settings.backend === "chatgpt") {
        await handleAction("chatgpt-models", target);
        break;
      }
      await runTask("Looking for installed models…", async (signal) => {
        models = await ai().discover(signal);
        if (models.length && !state.settings.model) {
          state.settings.model = preferredLocalModel(models);
        }
        persist();
        toast(
          models.length
            ? `${models.length} installed model(s) found.`
            : "Connected, but no installed models were reported.",
        );
      });
      break;
    case "test-ai":
      await runTask("Testing the AI connection…", async (signal) => {
        const result = await ai().test(signal);
        toast(
          typeof result === "string"
            ? result
            : result.message || "Local AI server is reachable.",
        );
      });
      break;
    case "export":
      modal(
        "Export & preview",
        `<label class="field-label">File format<select id="export-format">${["docx", "pdf", "txt", "md"].map((v) => `<option ${state.settings.defaultExport === v ? "selected" : ""}>${v}</option>`).join("")}</select></label><label class="checkbox-row"><span>Append bibliography from included sources</span><input id="export-references" type="checkbox" checked></label><details><summary>Document appearance</summary>${exportAppearanceFields(p.exportFormatting || { fontFamily: "Times New Roman", fontSize: 12, lineSpacing: 1.5 })}</details><p>Only your accepted working draft is exported. Pending proposals stay in the project.</p><div class="export-preview"><h3>${esc(p.title)}</h3><p class="pre-wrap">${esc(exportText(p, true))}</p></div>`,
        button(
          "export-confirm",
          icon("export", 16) + " Export document",
          "primary",
        ),
      );
      updateExportAppearance(p);
      break;
    case "export-confirm": {
      const format = document.querySelector("#export-format").value,
        refs = document.querySelector("#export-references").checked;
      updateExportAppearance(p);
      updateProject(p);
      await writeDocument(p, format, refs);
      closeModal();
      toast(
        format === "pdf"
          ? "Choose Save as PDF in the Android print dialog."
          : "Choose where to save the exported file.",
      );
      break;
    }
    case "backup":
      await writeDocument(p, "project");
      closeModal();
      break;
    case "about":
      modal(
        `Draft2Done ${APP_VERSION}`,
        `<p><strong>Your Draft. Your Voice. Done.</strong></p><p>Android release. Private on-device projects, direct ChatGPT writing with approval before each request, and optional scholarly source search. Computer/server AI is an advanced option. No AI model is bundled.</p><p class="about-dedication">${DEDICATION}</p><p>${native ? "Native Android storage and document picker are active." : "Browser preview: data is stored in this browser only."}</p><p>Windows desktop development is deferred.</p>`,
      );
      break;
  }
}
document.addEventListener("keydown", (e) => {
  if (
    e.target.id === "draft-editor" &&
    (e.ctrlKey || e.metaKey) &&
    ["z", "y"].includes(e.key.toLowerCase())
  ) {
    e.preventDefault();
    restoreEditor(
      getProject(),
      e.key.toLowerCase() === "y" || e.shiftKey ? "redo" : "undo",
    );
  }
});
document.addEventListener("beforeinput", (e) => {
  if (
    e.target.id === "draft-editor" &&
    ["historyUndo", "historyRedo"].includes(e.inputType) &&
    e.cancelable
  ) {
    e.preventDefault();
    restoreEditor(
      getProject(),
      e.inputType === "historyUndo" ? "undo" : "redo",
    );
  }
});
document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-action]");
  if (!t) return;
  Promise.resolve(handleAction(t.dataset.action, t)).catch((e) => {
    lastError = e.message;
    toast(e.message);
  });
});
document.addEventListener("input", (e) => {
  const p = getProject(),
    el = e.target;
  if (el.id === "paste-draft") {
    savePasteDraft(true);
  } else if (el.id === "draft-editor" && p) {
    const history = trackEdit(p, e.inputType);
    const edited = updateWorkingText(p, el.value);
    history.currentText = edited.workingText;
    state.projects[state.projects.findIndex((v) => v.id === p.id)] = edited;
    p.workingText = edited.workingText;
    p.contributions = edited.contributions;
    p.updatedAt = new Date().toISOString();
    persist();
    updateUndoButtons(edited);
    const jump = document.querySelector("#paragraph-nav");
    if (jump)
      jump.innerHTML =
        '<option value="">Choose a paragraph</option>' +
        paragraphOptions(edited.workingText);
    const n = progress(p);
    document.querySelector("#current-count").textContent =
      n.current.toLocaleString();
    document.querySelector("#word-progress").style.width =
      Math.min(100, n.percent) + "%";
    document.querySelector("#remaining-label").textContent = n.remaining
      ? `${n.remaining.toLocaleString()} words to your goal`
      : "Minimum reached · ready to review";
    const reading = document.querySelector(".information-copy");
    if (reading) reading.innerHTML = informationCopy(edited);
  } else if (el.classList.contains("proposal-editor")) {
    p.proposals.find((s) => s.id === el.dataset.id).text = el.value;
    persist();
  } else if (el.id === "profile-notes") {
    p.profile = { ...activeProfile(p), userNotes: el.value };
    persist();
  }
});
document.addEventListener("change", (e) => {
  const el = e.target,
    p = getProject();
  if (el.id === "information-links-enabled") {
    state.settings.informationLinks = el.checked;
    persist();
    render();
    return;
  }
  if (el.id === "paragraph-nav" && el.value !== "") {
    const editor = document.querySelector("#draft-editor"),
      start = Number(el.value);
    const match = /\n\s*\n/.exec(editor.value.slice(start));
    editor.focus();
    editor.setSelectionRange(
      start,
      match ? start + match.index : editor.value.length,
    );
    editor.scrollTop =
      (editor.scrollHeight * start) / Math.max(1, editor.value.length);
    return;
  }
  if (el.id === "export-references" && p) {
    document.querySelector(".export-preview .pre-wrap").textContent =
      exportText(p, el.checked);
    return;
  }
  if (
    el.id.startsWith("export-") &&
    ["fontFamily", "fontSize", "lineSpacing"].includes(el.id.slice(7))
  ) {
    try {
      updateExportAppearance(p);
      updateProject(p);
    } catch (error) {
      toast(error.message);
    }
    return;
  }
  if (el.id === "chatgpt-model") {
    state.settings.chatgptModel = el.value;
    persist();
    return;
  }
  if (el.id === "chatgpt-account") {
    runTask("Switching ChatGPT account…", async (signal) => {
      await addonRequest("select", { account: el.value }, signal);
      chatgptModels = [];
      state.settings.chatgptModel = "";
      state.settings.chatgptModels = [];
      persist();
    });
    return;
  }
  if (el.dataset.setting) {
    const key = el.dataset.setting;
    if (key === "backend") state.settings.aiRouteChosen = true;
    if (
      key === "exportFontSize" &&
      (!Number.isInteger(Number(el.value)) ||
        Number(el.value) < 10 ||
        Number(el.value) > 16)
    ) {
      toast("Choose a font size from 10 to 16 points.");
      el.value = state.settings[key];
      return;
    }
    state.settings[key] =
      el.type === "checkbox"
        ? el.checked
        : el.type === "number" || key === "exportLineSpacing"
          ? Number(el.value)
          : el.value;
    persist();
    if (["theme", "backend"].includes(key)) render();
  } else if (el.id === "research-online") {
    state.settings.online = el.checked;
    persist();
    render();
  } else if (el.dataset.provider) {
    state.settings.providers = el.checked
      ? [...new Set([...state.settings.providers, el.dataset.provider])]
      : state.settings.providers.filter((v) => v !== el.dataset.provider);
    persist();
  } else if (el.dataset.source) {
    p.sources.find((s) => s.id === el.dataset.source).included = el.checked;
    persist();
  } else if (el.id === "voice-select") {
    p.voice = el.value;
    updateProject(p);
  } else if (el.id === "minimum-input") {
    const n = Number(el.value);
    if (
      !Number.isInteger(n) ||
      n < 1 ||
      n > 50000 ||
      (p.maximum && n > p.maximum)
    ) {
      toast("Choose a valid word goal compatible with the maximum.");
      el.value = p.minimum;
      return;
    }
    p.minimum = n;
    updateProject(p);
    render();
  }
});
function flushLocalChanges() {
  savePasteDraft();
  flushAutosave();
}
window.addEventListener("pagehide", flushLocalChanges);
window.addEventListener("error", (e) => {
  lastError = e.message;
  console.error(e.error || e.message);
});
document.addEventListener("visibilitychange", () => {
  if (document.hidden) flushLocalChanges();
});
chatgptModels = state.settings.chatgptModels || [];
window.NativeEvents.pause = flushLocalChanges;
window.NativeEvents.back = () => {
  if (document.querySelector(".modal-backdrop")) {
    closeModal();
    return true;
  }
  if (page !== "home") {
    page = "home";
    render();
    return true;
  }
  return false;
};
window.Draft2Done = {
  getState: () => state,
  info: () => ({ page, tab, native, busy: !!busy, lastError }),
  render,
};
render();

async function checkForUpdates(manual = false, signal) {
  if (!native) {
    if (manual) toast("Update checks are available in the Android APK.");
    return;
  }
  if (
    !manual &&
    (!state.settings.autoUpdates ||
      Date.now() - (state.lastUpdateCheck || 0) < 86400000)
  )
    return;
  state.lastUpdateCheck = Date.now();
  try {
    state.updateInfo = await addonRequest("update-check", {}, signal);
    persist();
    if (manual || state.updateInfo.available) toast(state.updateInfo.message);
  } catch (error) {
    state.updateInfo = {
      available: false,
      message: "Could not check for updates. " + error.message,
    };
    persist();
    if (manual) throw error;
  }
}
setTimeout(() => checkForUpdates().catch(() => {}), 1500);
