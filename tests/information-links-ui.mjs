// Browser integration only: every provider/AI response is a labeled fixture.
// This suite is deliberately not evidence of live search or actual inference.
import { chromium } from "playwright";
import { createServer } from "node:http";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { resolve, extname, sep } from "node:path";
import assert from "node:assert/strict";
import { exportText } from "../web/document-text.js";

const root = resolve("web-dist");
const server = createServer(async (req, res) => {
  try {
    const path = resolve(
      root,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!path.startsWith(root + sep)) throw new Error("Outside test web root");
    res.setHeader(
      "Content-Type",
      {
        ".html": "text/html",
        ".js": "application/javascript",
        ".mjs": "application/javascript",
        ".css": "text/css",
      }[extname(path)] || "application/octet-stream",
    );
    res.end(await readFile(path));
  } catch {
    res.statusCode = 404;
    res.end();
  }
});
await new Promise((done) => server.listen(0, "127.0.0.1", done));
const browser = await chromium.launch({
  headless: true,
  executablePath: resolve(
    "tools/playwright/chromium-1194/chrome-win/chrome.exe",
  ),
});
const checks = [],
  errors = [],
  externalRequests = [];
let failure;
try {
  const page = await browser.newPage({ viewport: { width: 393, height: 852 } });
  page.on("pageerror", (error) => errors.push(error.message));
  await page.route("**/*", async (route) => {
    if (new URL(route.request().url()).hostname === "127.0.0.1")
      return route.continue();
    externalRequests.push(route.request().url());
    return route.abort();
  });
  await page.addInitScript(() => {
    window.qa = {
      queries: [],
      assists: [],
      opened: [],
      exports: [],
      failSearch: false,
      pending: new Set(),
    };
    const initial = {
      version: 1,
      projects: [],
      profiles: [],
      activeId: null,
      notes: {},
      settings: {
        autoUpdates: false,
        backend: "chatgpt",
        chatgptModel: "qa-fixture-model",
        chatgptModels: [
          { slug: "qa-fixture-model", display_name: "Simulated QA model" },
        ],
        online: false,
        providers: ["crossref"],
      },
    };
    const account = {
      available: true,
      connected: true,
      active: "qa-fixture",
      accounts: [
        {
          id: "qa-fixture",
          label: "Simulated QA account",
          signedIn: true,
          planEnabled: true,
        },
      ],
    };
    window.DraftNative = {
      loadState: () =>
        localStorage.getItem("qa-native-state") || JSON.stringify(initial),
      saveState: (value) => {
        localStorage.setItem("qa-native-state", value);
        return "ok";
      },
      chatgptStatus: () => JSON.stringify(account),
      setAppearance: () => {},
      openExternal: (url) => window.qa.opened.push(url),
      exportDocument: (name, mime, base64) =>
        window.qa.exports.push({ name, mime, base64 }),
      importDocument: () => {},
      cancelRequest: (id) => window.qa.pending.delete(id),
      chatgptCancel: () => {},
      request: (id, url) => {
        window.qa.queries.push(url);
        window.qa.pending.add(id);
        const failed = window.qa.failSearch;
        setTimeout(() => {
          if (!window.qa.pending.delete(id)) return;
          const body = failed
            ? "Fixture HTTP failure; not a real network request."
            : JSON.stringify({
                message: {
                  items: [
                    {
                      DOI: "10.1000/qa-climate-014",
                      title: ["Climate change: fictional QA evidence"],
                      author: [{ given: "Ada", family: "Fixture" }],
                      issued: { "date-parts": [[2026]] },
                      abstract:
                        "<p>Simulated research fixture about climate change and photosynthesis. This abstract is test data and contains no research finding.</p>",
                      URL: "https://example.edu/qa-climate-014",
                    },
                  ],
                },
              });
          window.NativeEvents.response(id, failed ? 503 : 200, body, "");
        }, 25);
      },
      chatgpt: (action, id, serialized) => {
        let result = {};
        if (action === "models")
          result = {
            models: [
              { slug: "qa-fixture-model", display_name: "Simulated QA model" },
            ],
          };
        if (action === "assist") {
          const payload = JSON.parse(serialized),
            record = JSON.parse(
              payload.messages.find((message) => message.role === "user")
                .content,
            );
          window.qa.assists.push(payload);
          const answer =
            record.task === "helper"
              ? {
                  suggestions: [
                    {
                      text: "Compare the climate change concept with the saved source and state the limits of this simulated evidence.",
                    },
                  ],
                  warnings: ["Simulated QA response, not actual inference."],
                }
              : {
                  paragraphs: [
                    {
                      text: "A useful next step is to explain how the chosen concept connects to the paper's own argument. This continuation is a fictional software fixture. The writer should compare the saved evidence, describe its limits, and decide which questions need further investigation before reaching a conclusion.",
                    },
                  ],
                  warnings: ["Simulated QA response, not actual inference."],
                };
          result = {
            completed: true,
            model: payload.model,
            text: JSON.stringify(answer),
          };
        }
        setTimeout(() => window.NativeEvents.addon(id, result, ""), 25);
      },
    };
  });
  const active = () =>
    page.evaluate(() => {
      const state = window.Draft2Done.getState();
      return state.projects.find((project) => project.id === state.activeId);
    });
  const requests = () =>
    page.evaluate(() => ({
      queries: window.qa.queries,
      assists: window.qa.assists,
      opened: window.qa.opened,
    }));
  const close = () =>
    page.getByRole("button", { name: "Close dialog", exact: true }).click();
  const tab = (name) => page.getByRole("tab", { name, exact: true }).click();
  const concept = () =>
    page
      .locator('[data-action="information-concept"]')
      .filter({ hasText: /climate change/i })
      .first();
  const goResearch = async () => {
    await concept().click();
    await page
      .locator("#information-query")
      .fill("climate change photosynthesis");
    await page.locator('[data-action="information-research"]').click();
    assert.equal(
      await page.locator("#research-query").inputValue(),
      "climate change photosynthesis",
    );
  };
  const search = async () => {
    await page.locator('[data-action="search"]').click();
    await page.waitForFunction(() => !window.Draft2Done.info().busy);
  };
  const toDraft = async () => {
    await page.getByRole("button", { name: "Write", exact: true }).click();
    await tab("My draft");
  };
  const paste = async (text) => {
    await page
      .getByRole("button", { name: "Draft2Done home", exact: true })
      .click();
    await page.locator("#paste-draft").fill(text);
    await page.locator('[data-action="paste-use"]').click();
    await page.locator("#draft-editor").waitFor();
    assert.equal((await active()).originalText, text);
    assert.equal((await active()).workingText, text);
  };
  const reviewAndSend = async () => {
    await page
      .locator("#remote-payload-preview")
      .waitFor({ state: "attached" });
    const approved = JSON.parse(
      await page.locator("#remote-payload-preview").textContent(),
    );
    const before = (await requests()).assists.length;
    const record = JSON.parse(
      approved.messages.find((message) => message.role === "user").content,
    );
    assert.equal(record.draft, (await active()).workingText);
    assert.equal(record.evidence.length, 1);
    assert.match(
      record.evidence[0].abstractExcerpt,
      /Simulated research fixture/,
    );
    await page.locator('[data-action="remote-send"]').click();
    await page.waitForFunction(
      (count) =>
        window.qa.assists.length > count && !window.Draft2Done.info().busy,
      before,
    );
    assert.deepEqual((await requests()).assists.at(-1), approved);
    return record;
  };
  const firstText =
    "Climate change is the subject of my existing paper. I want to examine how photosynthesis connects living systems to the carbon cycle. My draft starts with this question and explains why clear evidence matters. I will compare sources carefully, describe uncertainty, and preserve my own wording while developing the argument.\n\nMarie Curie is a separate example of a research-worthy name. <img src=x onerror=alert(1)> stays literal in this fictional test.";
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  await paste(firstText);
  assert.equal(await page.locator(".information-copy").count(), 0);
  assert.equal(
    await page.locator("#information-links-enabled").isChecked(),
    false,
  );
  const beforeLinks = await active();
  await page.locator("#information-links-enabled").check();
  assert.equal(
    await page.locator(".information-copy").textContent(),
    firstText,
  );
  assert.equal(
    await page
      .locator(".information-copy img, .information-copy script")
      .count(),
    0,
  );
  assert.equal(await page.locator("#draft-editor").inputValue(), firstText);
  assert.deepEqual(await requests(), { queries: [], assists: [], opened: [] });
  assert.deepEqual((await active()).citedSourceIds, beforeLinks.citedSourceIds);
  assert.equal(
    exportText(await active(), false),
    exportText(beforeLinks, false),
  );
  checks.push(
    "Paste, local opt-in preview and escaped literal text preserve working/original text and export without any network or browser action",
  );

  await concept().click();
  assert.equal(
    await page.locator('[data-action="information-open"]').count(),
    0,
  );
  assert.match(
    await page.locator(".information-sources").innerText(),
    /No relevant retrieved source/,
  );
  assert.equal((await requests()).queries.length, 0);
  await page
    .locator("#information-query")
    .fill("climate change photosynthesis");
  await page.locator('[data-action="information-research"]').click();
  assert.equal((await requests()).queries.length, 0);
  await search();
  assert.equal((await requests()).queries.length, 0);
  await page.locator("#research-online").check();
  await search();
  assert.equal((await active()).sources.length, 1);
  const searchURL = new URL((await requests()).queries[0]);
  assert.equal(
    searchURL.searchParams.get("query.bibliographic"),
    "climate change photosynthesis",
  );
  assert(
    !searchURL.href.includes("My%20draft") && !searchURL.href.includes("Marie"),
  );
  checks.push(
    "Concept sheet and editable query handoff do not search; explicit Online and Search send only the short query to a simulated provider",
  );

  await toDraft();
  await concept().click();
  assert.equal(
    await page.locator('[data-action="information-open"]').count(),
    1,
  );
  assert.equal((await requests()).opened.length, 0);
  await page.locator('[data-action="information-open"]').click();
  assert.deepEqual((await requests()).opened, [
    "https://example.edu/qa-climate-014",
  ]);
  assert.equal((await active()).workingText, firstText);
  await close();
  await tab("Helper");
  assert.equal(
    await page.locator(".information-copy").textContent(),
    firstText,
  );
  await page.locator('[data-action="analyze"]').click();
  const helperRecord = await reviewAndSend();
  assert.equal(helperRecord.task, "helper");
  assert.match(
    (await active()).helperNotes,
    /Compare the climate change concept/,
  );
  assert.equal((await active()).workingText, firstText);
  assert.equal((await active()).originalText, firstText);
  await tab("My draft");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  checks.push(
    "Workflow A: pasted draft, analysis, explicit simulated source retrieval, information link, exact approved Helper evidence and save preserve the protected original",
  );

  const secondText =
    "Climate change deserves a careful argument supported by sources. Photosynthesis provides one way to consider how living systems respond to their surroundings. This is my own existing draft, and I want the continuation to preserve my wording and structure. I will examine the evidence, discuss its limits, and make the connection to my main question clear.";
  await paste(secondText);
  assert.equal(
    (await page.evaluate(() => window.Draft2Done.getState().projects)).length,
    2,
  );
  assert.equal(
    await page.locator("#information-links-enabled").isChecked(),
    true,
  );
  await goResearch();
  await search();
  await toDraft();
  await tab("Complete");
  assert.equal(
    await page.locator(".information-copy").textContent(),
    secondText,
  );
  const current = Number(
    (await page.locator("#current-count").innerText()).replace(/,/g, ""),
  );
  await page.locator("#minimum-input").fill(String(current + 10));
  await page.locator("#minimum-input").dispatchEvent("change");
  await page.locator('[data-action="generate"]').click();
  await page
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  const completeRecord = await reviewAndSend();
  assert.equal(completeRecord.task, "complete");
  await page.locator(".proposal-editor").first().waitFor();
  assert.equal((await active()).workingText, secondText);
  await page.locator('[data-action="accept"]').first().click();
  assert((await active()).workingText.startsWith(secondText));
  assert.equal((await active()).originalText, secondText);
  assert.equal((await active()).proposals[0].status, "accepted");
  await tab("My draft");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const savedProject = await active();
  await page.reload();
  await page
    .locator(`[data-action="open"][data-id="${savedProject.id}"]`)
    .click();
  assert.equal((await active()).workingText, savedProject.workingText);
  assert.equal((await active()).originalText, secondText);
  assert.deepEqual((await active()).revisions, savedProject.revisions);
  assert.equal(
    await page.locator("#information-links-enabled").isChecked(),
    true,
  );
  checks.push(
    "Workflow B/F: second pasted paper, simulated search, Complete reader, exact evidence consent, accepted section and saved revisions survive reopening without changing its original or the first paper",
  );

  const beforeEdit = await active();
  const edited =
    "🧑🏽‍🎓 A new opening about quantum mechanics.\n\n" + beforeEdit.workingText;
  const oldButton = await concept().evaluate((button) => ({
    index: button.dataset.index,
    start: button.dataset.start,
    end: button.dataset.end,
  }));
  await page.locator("#draft-editor").fill(edited);
  await page.waitForFunction(
    (expected) =>
      document.querySelector(".information-copy")?.textContent === expected,
    edited,
  );
  assert.equal(await page.locator("#draft-editor").inputValue(), edited);
  assert.equal(
    await concept().getAttribute("data-start"),
    String(edited.indexOf("Climate change")),
  );
  // Dispatch an old detached-view action to verify stale offsets cannot select a new meaning.
  await page.evaluate((old) => {
    const button = document.createElement("button");
    Object.assign(button.dataset, {
      action: "information-concept",
      index: old.index,
      start: old.start,
      end: old.end,
    });
    document.body.append(button);
    button.click();
    button.remove();
  }, oldButton);
  assert.equal(await page.locator("#information-query").count(), 0);
  assert.equal((await active()).originalText, secondText);
  await page.getByRole("button", { name: "Writer DNA", exact: true }).click();
  await page.locator('[data-action="dna-refresh"]').click();
  await toDraft();
  assert.equal(await page.locator(".information-copy").textContent(), edited);
  await page.locator("#information-links-enabled").uncheck();
  assert.equal(await page.locator(".information-copy").count(), 0);
  assert.equal(await page.locator("#draft-editor").inputValue(), edited);
  await page.locator("#information-links-enabled").check();
  checks.push(
    "Workflow D: Unicode editor changes immediately refresh offsets; stale actions are refused, DNA reanalysis works and disabling links preserves normal editable text",
  );

  const draftBeforeFailure = await active();
  await goResearch();
  await page.evaluate(() => {
    window.qa.failSearch = true;
  });
  await search();
  await page
    .getByRole("dialog", { name: "Research results", exact: true })
    .waitFor();
  assert.match(await page.locator(".modal-body").innerText(), /503/);
  assert.equal((await active()).workingText, draftBeforeFailure.workingText);
  assert.deepEqual((await active()).sources, draftBeforeFailure.sources);
  await close();
  await page.locator("#research-online").uncheck();
  const beforeOfflineSearch = (await requests()).queries.length;
  await search();
  assert.equal((await requests()).queries.length, beforeOfflineSearch);
  await page.locator("#research-online").check();
  await page.evaluate(() => {
    window.qa.failSearch = false;
  });
  await search();
  assert.equal((await active()).workingText, draftBeforeFailure.workingText);
  assert.equal((await active()).sources.length, 1);
  checks.push(
    "Workflow E: simulated provider HTTP failure and disabled-online search preserve draft/library; explicit retry succeeds without duplicate records",
  );

  const importedText =
    "Climate change and photosynthesis are the topics of this imported fictional draft. I will develop this paragraph using my own words, compare retrieved sources and keep the evidence separate from my argument. The existing text needs to stay unchanged when I explore a concept or open a saved source.";
  await page.evaluate(
    (text) =>
      window.NativeEvents.imported({
        name: "Information links fixture.txt",
        mime: "text/plain",
        base64: btoa(unescape(encodeURIComponent(text))),
      }),
    importedText,
  );
  await page.locator("#draft-editor").waitFor();
  assert.equal((await active()).originalText, importedText);
  await goResearch();
  await search();
  await toDraft();
  const beforeExplore = await active();
  await concept().click();
  assert.equal(
    await page.locator('[data-action="information-open"]').count(),
    1,
  );
  await page.locator('[data-action="information-open"]').click();
  await close();
  assert.equal((await active()).workingText, importedText);
  assert.equal(
    exportText(await active(), false),
    exportText(beforeExplore, false),
  );
  await page
    .getByRole("button", { name: "Paper actions", exact: true })
    .click();
  await page.locator('[data-action="export"]').click();
  await page.locator("#export-format").selectOption("txt");
  await page.locator("#export-references").uncheck();
  await page.locator('[data-action="export-confirm"]').click();
  const exported = await page.evaluate(() => window.qa.exports.at(-1));
  assert.equal(
    Buffer.from(exported.base64, "base64").toString("utf8"),
    importedText,
  );
  assert(
    !Buffer.from(exported.base64, "base64")
      .toString("utf8")
      .includes("information-link"),
  );
  assert.deepEqual(
    (await active()).citedSourceIds,
    beforeExplore.citedSourceIds,
  );
  checks.push(
    "Workflow C: TXT import, simulated research, explicit external open and return preserve original/editor/formal citations; actual exported TXT contains the exact draft and no reading links",
  );

  await page.setViewportSize({ width: 852, height: 393 });
  assert.equal(
    await page.locator(".information-copy").textContent(),
    importedText,
  );
  assert(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
  await page.setViewportSize({ width: 393, height: 852 });
  assert.equal(
    await page.locator(".information-copy").textContent(),
    importedText,
  );
  assert.deepEqual(errors, []);
  assert.deepEqual(externalRequests, []);
  checks.push(
    "Portrait/landscape reading remains intact without horizontal page overflow, JavaScript errors or actual external browser requests",
  );
  await mkdir("test-results", { recursive: true });
  await page.screenshot({
    path: "test-results/information-links-reader.png",
    fullPage: true,
  });
} catch (error) {
  failure = error;
} finally {
  await mkdir("test-results", { recursive: true });
  await writeFile(
    "test-results/information-links-ui-report.json",
    JSON.stringify(
      {
        passed: !failure,
        liveNetwork: false,
        actualInference: false,
        provider: "Native bridge fixtures only",
        checks,
        errors,
        externalRequests,
        ...(failure ? { failure: failure.stack } : {}),
      },
      null,
      2,
    ) + "\n",
  );
  await browser.close();
  await new Promise((done) => server.close(done));
}
if (failure) throw failure;
console.log(
  `Information-link browser integration checks passed: ${checks.length} (simulated providers only)`,
);
