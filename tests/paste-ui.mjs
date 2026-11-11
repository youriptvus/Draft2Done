import { chromium } from "playwright";
import { createServer } from "node:http";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { resolve, extname } from "node:path";
import assert from "node:assert/strict";

const root = resolve("web-dist"),
  stagingKey = "draft2done-paste-draft",
  dedication = "Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.",
  checks = [];
const server = createServer(async (req, res) => {
  try {
    let path = resolve(root, "." + new URL(req.url, "http://localhost").pathname);
    if (path === root) path = resolve(root, "index.html");
    if (!path.startsWith(root + "/") && !path.startsWith(root + "\\")) throw new Error();
    res.setHeader("Content-Type", { ".html": "text/html", ".js": "application/javascript", ".mjs": "application/javascript", ".css": "text/css" }[extname(path)] || "application/octet-stream");
    res.end(await readFile(path));
  } catch { res.statusCode = 404; res.end(); }
});
await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const browser = await chromium.launch({ headless: true, executablePath: resolve("tools/playwright/chromium-1194/chrome-win/chrome.exe") });
try {
  const page = await browser.newPage({ viewport: { width: 393, height: 852 } });
  const errors = [], requests = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => { if (!request.url().startsWith(`http://127.0.0.1:${server.address().port}/`)) requests.push(request.url()); });
  const home = () => page.getByRole("button", { name: "Draft2Done home", exact: true }).click();
  const active = () => page.evaluate(() => { const state = window.Draft2Done.getState(); return state.projects.find((p) => p.id === state.activeId); });
  const stage = () => page.evaluate((key) => localStorage.getItem(key), stagingKey);
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  assert.equal(await page.getByRole("textbox", { name: "Paste Your Draft", exact: true }).count(), 1);
  assert.equal(await page.locator("#paste-draft").getAttribute("placeholder"), "Already started your paper? Paste what you've written here to get started.");
  assert.ok(await page.evaluate(() => document.querySelector("#paste-draft").compareDocumentPosition(document.querySelector(".start-actions")) & Node.DOCUMENT_POSITION_FOLLOWING));
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  assert.equal(await page.evaluate(() => window.Draft2Done.getState().projects.length), 0);
  assert.match(await page.locator("#toast").innerText(), /Paste some of your writing/);
  checks.push("Exact paste copy appears above imports; empty input cannot create a project");

  await page.getByRole("button", { name: /Try a fictional sample/ }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator('[data-setting="autosave"]').uncheck();
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const previous = await active();
  const savedWorkspace = await page.evaluate(() => localStorage.getItem("draft2done-state"));
  await page.locator("#draft-editor").fill(previous.workingText + "\n\nUnsaved working-copy edit.");
  await home();
  const draft = "\n\n  My fictional classroom paper — café, α and 日本語.  \n\nI compare the practical access to a park with its distance from home. I have not collected evidence, so I want to distinguish a research question from a finding. My wording, spelling and punctuation stay as I wrote them.\nA deliberate single line break stays here.\n\nLiteral markup stays text: </textarea><img src=x onerror=alert(1)>\n  ";
  await page.locator("#paste-draft").fill(draft);
  assert.equal(JSON.parse(await stage()).text, draft);
  assert.equal(await page.evaluate(() => localStorage.getItem("draft2done-state")), savedWorkspace);
  assert.equal(await page.locator(".paste-card img").count(), 0);
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await home();
  assert.equal(await page.locator("#paste-draft").inputValue(), draft);
  await page.reload();
  assert.equal(await page.locator("#paste-draft").inputValue(), draft);
  assert.equal((await active()).workingText, previous.workingText);
  assert.equal(await page.evaluate(() => window.Draft2Done.getState().settings.autosave), false);
  checks.push("Paste persists across navigation/reload independently of disabled workspace autosave; literal markup is escaped");

  await page.evaluate(() => { const input = document.querySelector("#paste-draft"); input.value += "\nPaused before an input callback."; window.NativeEvents.pause(); });
  const interruptedDraft = await page.locator("#paste-draft").inputValue();
  assert.equal(JSON.parse(await stage()).text, interruptedDraft);
  await page.reload();
  assert.equal(await page.locator("#paste-draft").inputValue(), interruptedDraft);
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  const pasted = await active();
  assert.notEqual(pasted.id, previous.id);
  assert.equal(pasted.originalText, interruptedDraft);
  assert.equal(pasted.workingText, interruptedDraft);
  assert.equal(await page.locator("#draft-editor").inputValue(), interruptedDraft);
  assert.deepEqual(await page.evaluate((id) => window.Draft2Done.getState().projects.find((p) => p.id === id), previous.id), previous);
  assert.equal(await stage(), null);
  await page.reload();
  await page.getByRole("button", { name: "Write", exact: true }).click();
  assert.equal((await active()).originalText, interruptedDraft);
  assert.equal(await page.locator("#draft-editor").inputValue(), interruptedDraft);
  await page.locator("#draft-editor").fill(interruptedDraft + "\n\nA working-copy addition.");
  assert.equal((await active()).originalText, interruptedDraft);
  await page.getByRole("tab", { name: "Helper", exact: true }).click();
  assert.equal(await page.getByRole("button", { name: "Analyze my draft", exact: true }).count(), 1);
  await page.getByRole("tab", { name: "Complete", exact: true }).click();
  assert.equal(await page.locator("#minimum-input").count(), 1);
  await page.getByRole("button", { name: "Writer DNA", exact: true }).click();
  assert.equal(await page.locator(".metric-grid").count(), 1);
  checks.push("Pause retains text; Use This Draft durably creates a distinct protected paper usable in both modes and Writer DNA");

  await home();
  assert.equal(await page.locator("#paste-draft").inputValue(), "");
  const long = ("This fictional paragraph considers routes, opening hours and the limits of the available evidence. It reports no measured result.\n\n").repeat(1800);
  assert.equal(await page.locator("#paste-draft").isVisible(), true);
  await page.evaluate((text) => { const input = document.querySelector("#paste-draft"); input.value = text; input.dispatchEvent(new Event("input", { bubbles: true })); }, long);
  assert.equal(JSON.parse(await stage()).text, long);
  assert.ok(await page.locator("#paste-draft").evaluate((input) => input.scrollHeight > input.clientHeight));
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  assert.equal((await active()).originalText, long);
  await page.reload();
  assert.equal((await active()).workingText, long);
  checks.push("A long multi-paragraph paper remains scrollable and is saved/reopened without truncation");

  await home();
  const ids = await page.evaluate(() => window.Draft2Done.getState().projects.map((p) => p.id));
  const tooBigWorkspace = "測".repeat(750000);
  await page.evaluate((text) => { const input = document.querySelector("#paste-draft"); input.value = text; input.dispatchEvent(new Event("input", { bubbles: true })); }, tooBigWorkspace);
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  assert.match(await page.locator("#toast").innerText(), /4 MB workspace/);
  assert.equal(await page.locator("#paste-draft").inputValue(), tooBigWorkspace);
  assert.deepEqual(await page.evaluate(() => window.Draft2Done.getState().projects.map((p) => p.id)), ids);
  // Dispatch a real input with a payload over the existing 10 MiB import boundary.
  await page.evaluate(() => { const input = document.querySelector("#paste-draft"); input.value = "x".repeat(10 * 1024 * 1024 + 1); input.dispatchEvent(new Event("input", { bubbles: true })); });
  assert.match(await page.locator("#paste-status").innerText(), /exceeds 10 MB/);
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  assert.match(await page.locator("#toast").innerText(), /exceeds 10 MB/);
  assert.deepEqual(await page.evaluate(() => window.Draft2Done.getState().projects.map((p) => p.id)), ids);
  await page.locator("#paste-draft").fill("A separate fictional draft for a storage failure.");
  const candidate = await page.locator("#paste-draft").inputValue();
  await page.evaluate(() => { window.qaOriginalSetItem = Storage.prototype.setItem; Storage.prototype.setItem = function(key, value) { if (key === "draft2done-state") throw new Error("QA storage unavailable"); return window.qaOriginalSetItem.call(this, key, value); }; });
  await page.getByRole("button", { name: "Use This Draft", exact: true }).click();
  assert.match(await page.locator("#toast").innerText(), /QA storage unavailable/);
  assert.equal(await page.locator("#paste-draft").inputValue(), candidate);
  assert.equal(JSON.parse(await stage()).text, candidate);
  assert.deepEqual(await page.evaluate(() => window.Draft2Done.getState().projects.map((p) => p.id)), ids);
  await page.evaluate(() => { Storage.prototype.setItem = window.qaOriginalSetItem; delete window.qaOriginalSetItem; });
  checks.push("UTF-8 workspace/import limits and failed storage preserve all prior papers and unaccepted pasted text");

  const pkg = JSON.parse(await readFile("package.json", "utf8"));
  for (const viewport of [{ width: 320, height: 568 }, { width: 852, height: 393 }, { width: 768, height: 1024 }]) {
    await page.setViewportSize(viewport);
    await home();
    assert.equal(await page.locator("#paste-draft").inputValue(), candidate);
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1));
    await page.getByRole("button", { name: "Settings", exact: true }).click();
    await page.getByRole("button", { name: "About Draft2Done", exact: true }).click();
    assert.equal(await page.locator(".about-dedication").innerText(), dedication);
    assert.ok((await page.locator(".modal-heading").innerText()).includes(pkg.version));
    assert.ok(await page.locator(".about-dedication").evaluate((element) => { const box = element.getBoundingClientRect(); return box.width > 100 && box.right <= innerWidth && box.left >= 0 && getComputedStyle(element).fontSize === "15px"; }));
    await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  }
  checks.push("Portrait/landscape and tablet preserve the paste, show the current version and exact readable permanent dedication");

  // Retrieved titles can become suggested information-link labels. They remain
  // character data in the dialog heading and accessible name, never HTML.
  const punctuated = "Curie's Science & Evidence";
  const malicious = '<img src=x onerror="window.qaDialogExecuted=true">';
  await home();
  await page.locator("#paste-draft").fill(`${punctuated} is a fictional reference name. A separate literal reference is ${malicious}. These labels are untrusted text.`);
  await page.locator('[data-action="paste-use"]').click();
  await page.evaluate(([first, second]) => {
    const state = window.Draft2Done.getState(), project = state.projects.find((p) => p.id === state.activeId);
    project.sources = [first, second].map((title, index) => ({ id: `qa-modal-${index}`, title, provider: "crossref", authors: [], url: `https://example.org/paper-${index}`, abstract: "", verification: "metadata", included: true }));
  }, [punctuated, malicious]);
  await page.locator("#information-links-enabled").check();
  await page.getByRole("button", { name: `Explore ${punctuated}`, exact: true }).click();
  assert.equal(await page.locator(".modal-heading h2").textContent(), `Explore ${punctuated}`);
  assert.equal(await page.getByRole("dialog", { name: `Explore ${punctuated}`, exact: true }).count(), 1);
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  await page.getByRole("button", { name: `Explore ${malicious}`, exact: true }).click();
  assert.equal(await page.locator(".modal-heading h2").textContent(), `Explore ${malicious}`);
  assert.equal(await page.getByRole("dialog", { name: `Explore ${malicious}`, exact: true }).count(), 1);
  assert.equal(await page.locator(".modal img, .modal script").count(), 0);
  assert.equal(await page.evaluate(() => window.qaDialogExecuted), undefined);
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  checks.push("Suggested concept dialog titles preserve apostrophes/ampersands in visible and accessible names; malicious HTML remains inert");

  // Simulated synchronous Android storage, intentionally independent of the
  // browser mirror. Real force-stop/disk durability is checked by Android QA.
  const nativePage = await browser.newPage({ viewport: { width: 393, height: 852 } });
  nativePage.on("pageerror", (error) => errors.push(error.message));
  nativePage.on("request", (request) => { if (!request.url().startsWith(`http://127.0.0.1:${server.address().port}/`)) requests.push(request.url()); });
  const legacyRaw = JSON.stringify({ text: "\nLegacy pending paste — café and α.\n\nA second paragraph.", start: 3, end: 9, scroll: 0 }, null, 2);
  await nativePage.addInitScript(({ key, legacy }) => {
    if (!sessionStorage.getItem("qa-native-initialized")) {
      sessionStorage.setItem("qa-native-initialized", "yes");
      localStorage.setItem(key, legacy);
    }
    if (sessionStorage.getItem("qa-clear-mirror-on-load")) {
      localStorage.removeItem(key);
      sessionStorage.removeItem("qa-clear-mirror-on-load");
    }
    const stale = sessionStorage.getItem("qa-stale-mirror-on-load");
    if (stale) {
      localStorage.setItem(key, stale);
      sessionStorage.removeItem("qa-stale-mirror-on-load");
    }
    const corrupt = sessionStorage.getItem("qa-native-error-on-load");
    if (corrupt) {
      sessionStorage.setItem("qa-native-draft", corrupt);
      sessionStorage.removeItem("qa-native-error-on-load");
    }
    window.qaMirrorBeforeLoad = localStorage.getItem(key);
    window.qaDraftSaves = [];
    window.DraftNative = {
      loadState: () => sessionStorage.getItem("qa-native-workspace") || JSON.stringify({ version: 1, projects: [], profiles: [], activeId: null, notes: {}, settings: { autoUpdates: false, autosave: false } }),
      saveState(raw) { sessionStorage.setItem("qa-native-workspace", raw); return "ok"; },
      loadDraftInput: () => sessionStorage.getItem("qa-native-draft") || "null",
      hasDraftInputHistory: () => sessionStorage.getItem("qa-native-history") === "yes",
      saveDraftInput(raw) {
        window.qaDraftSaves.push(raw);
        if (window.qaDraftFailure) return JSON.stringify({ error: "QA atomic draft write failed" });
        sessionStorage.setItem("qa-native-draft", raw || "null");
        sessionStorage.setItem("qa-native-history", "yes");
        return "ok";
      },
      chatgptStatus: () => JSON.stringify({ available: false, connected: false, accounts: [] }),
      setAppearance() {},
    };
  }, { key: stagingKey, legacy: legacyRaw });
  await nativePage.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  assert.equal(await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft")), legacyRaw);
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), legacyRaw);
  assert.equal(await nativePage.locator("#paste-draft").inputValue(), JSON.parse(legacyRaw).text);
  assert.deepEqual(await nativePage.evaluate(() => window.qaDraftSaves), [legacyRaw]);
  const nativeText = "\n\nNative staged paper — 日本語, café and α.\n\nParagraph breaks and punctuation stay exactly here.";
  await nativePage.locator("#paste-draft").fill(nativeText);
  const nativeRaw = await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft"));
  assert.equal(JSON.parse(nativeRaw).text, nativeText);
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), nativeRaw);
  assert.equal(await nativePage.evaluate(() => window.qaDraftSaves.at(-1)), nativeRaw);
  await nativePage.evaluate(() => sessionStorage.setItem("qa-clear-mirror-on-load", "yes"));
  await nativePage.reload();
  assert.equal(await nativePage.evaluate(() => window.qaMirrorBeforeLoad), null);
  assert.equal(await nativePage.locator("#paste-draft").inputValue(), nativeText);
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), nativeRaw);
  checks.push("Simulated native bridge migrates legacy input once, commits the exact staged payload and restores from native when its browser mirror is absent");

  await nativePage.evaluate(() => { window.qaDraftFailure = true; });
  const failedText = nativeText + "\n\nA new edit during a failed atomic write.";
  await nativePage.locator("#paste-draft").fill(failedText);
  assert.match(await nativePage.locator("#paste-status").innerText(), /Paste not saved: QA atomic draft write failed/);
  assert.equal(await nativePage.locator("#paste-draft").inputValue(), failedText);
  assert.equal(await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft")), nativeRaw);
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), nativeRaw);
  await nativePage.evaluate(() => { window.qaDraftFailure = false; window.NativeEvents.pause(); });
  assert.equal(JSON.parse(await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft"))).text, failedText);
  assert.match(await nativePage.locator("#paste-status").innerText(), /Paste saved on this device/);
  await nativePage.locator('[data-action="paste-use"]').click();
  assert.equal(await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft")), "null");
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), null);
  await nativePage.evaluate((stale) => sessionStorage.setItem("qa-stale-mirror-on-load", stale), legacyRaw);
  await nativePage.reload();
  assert.equal(await nativePage.evaluate(() => window.qaMirrorBeforeLoad), legacyRaw);
  assert.equal(await nativePage.locator("#paste-draft").inputValue(), "");
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), null);
  assert.equal(await nativePage.evaluate(() => window.Draft2Done.getState().settings.autosave), false);
  assert.equal(await nativePage.evaluate(() => window.Draft2Done.getState().projects[0].originalText), failedText);
  checks.push("Native write failure retains unsaved input and the prior committed record; successful retry clears through a durable tombstone without resurrecting stale browser data");

  const corrupt = JSON.stringify({ error: "QA saved draft could not be read" });
  await nativePage.evaluate(([legacy, bad]) => { sessionStorage.setItem("qa-native-error-on-load", bad); sessionStorage.setItem("qa-stale-mirror-on-load", legacy); }, [legacyRaw, corrupt]);
  await nativePage.reload();
  assert.match(await nativePage.locator("#paste-status").innerText(), /previous paste could not be restored.*QA saved draft could not be read/);
  await nativePage.evaluate(() => window.NativeEvents.pause());
  assert.equal(await nativePage.evaluate(() => sessionStorage.getItem("qa-native-draft")), corrupt);
  assert.equal(await nativePage.evaluate((key) => localStorage.getItem(key), stagingKey), legacyRaw);
  assert.deepEqual(await nativePage.evaluate(() => window.qaDraftSaves), []);
  await nativePage.close();
  checks.push("A native load error preserves both stored records and is not silently cleared by pause/autosave");

  assert.deepEqual(errors, []);
  assert.deepEqual(requests, []);
  checks.push("Paste, dedication and offline workflow checks make no external requests and emit no page errors");
  await mkdir("test-results", { recursive: true });
  await writeFile("test-results/paste-ui.json", JSON.stringify({ passed: true, checks }, null, 2));
  console.log(`Paste/About workflows passed: ${checks.length} checks`);
} finally { await browser.close(); await new Promise((resolve) => server.close(resolve)); }
