import { chromium } from "playwright";
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { resolve, extname } from "node:path";
import assert from "node:assert/strict";

const root = resolve("web-dist");
const server = createServer(async (req, res) => {
  try {
    let path = resolve(
      root,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (path === root) path = resolve(root, "index.html");
    if (!path.startsWith(root)) throw new Error();
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
await new Promise((r) => server.listen(0, "127.0.0.1", r));
const browser = await chromium.launch({
  headless: true,
  executablePath: resolve(
    "tools/playwright/chromium-1194/chrome-win/chrome.exe",
  ),
});
try {
  const page = await browser.newPage({ viewport: { width: 393, height: 852 } });
  const errors = [],
    aiCalls = [],
    searches = [];
  let failNextAI = true;
  page.on("pageerror", (e) => errors.push(e.message));
  await page.route("**/api/chat", async (route) => {
    if (failNextAI) {
      failNextAI = false;
      await route.fulfill({
        status: 503,
        contentType: "application/json",
        headers: { "Access-Control-Allow-Origin": "*" },
        body: JSON.stringify({ error: "Temporary QA connection failure" }),
      });
      return;
    }
    const request = JSON.parse(route.request().postData());
    const data = JSON.parse(request.messages[1].content);
    aiCalls.push(data);
    const content =
      data.task === "helper"
        ? {
            suggestions: [
              {
                text: "Compare access barriers in the next paragraph, then connect them to the thesis.",
              },
              {
                text: "Ask whose perspective is absent before choosing supporting evidence.",
              },
            ],
            warnings: [],
          }
        : {
            paragraphs: [
              {
                text: "that access deserves a closer look before the argument can be completed.",
              },
            ],
            warnings: [],
          };
    await route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({ message: { content: JSON.stringify(content) } }),
    });
  });
  await page.route("https://api.crossref.org/**", async (route) => {
    const url = new URL(route.request().url());
    searches.push(url.searchParams.get("query.bibliographic"));
    await route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({
        message: {
          items: [
            {
              DOI: "10.1000/qa-access",
              title: ["QA access barriers fixture"],
              author: [{ given: "Ada", family: "Example" }],
              issued: { "date-parts": [[2024]] },
              abstract:
                "<p>A fictional test abstract describing access barriers.</p>",
              URL: "https://example.org/qa-access",
            },
          ],
        },
      }),
    });
  });
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().settings.backend),
    "chatgpt",
  );
  await page.getByRole("button", { name: /Try a fictional sample/ }).click();
  const original = await page.evaluate(
    () => window.Draft2Done.getState().projects[0].originalText,
  );
  await page.locator("#draft-editor").fill(original + "\n\nAnother concern is");
  await page.evaluate(() => {
    const state = window.Draft2Done.getState();
    Object.assign(state.settings, {
      backend: "ollama",
      model: "test",
      endpoint: "http://127.0.0.1:11435",
      online: true,
      providers: ["crossref"],
    });
    state.projects[0].minimum = 170;
  });
  await page.getByRole("tab", { name: "Helper", exact: true }).click();
  await page.getByText("Your draft at a glance", { exact: true }).waitFor();
  assert.equal(
    await page.getByText("Check the unfinished parts", { exact: true }).count(),
    1,
  );
  await page
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await page
    .getByRole("dialog", { name: "Couldn’t finish this step", exact: true })
    .waitFor();
  assert.ok(await page.evaluate(() => window.Draft2Done.info().lastError));
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  await page
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await page.locator('[data-action="ai-suggestion"]').first().waitFor();
  assert.equal(
    await page.evaluate(() => window.Draft2Done.info().lastError),
    "",
  );
  await page.locator('[data-action="ai-suggestion"]').first().click();
  assert.match(
    await page.locator(".modal-body").innerText(),
    /Compare access barriers/,
  );
  await page
    .getByRole("button", { name: "Show supporting evidence", exact: true })
    .click();
  assert.equal(await page.locator("#research-query").count(), 1);
  assert.equal(searches.length, 0);

  await page.getByRole("button", { name: "Add a source", exact: true }).click();
  await page.locator("#source-title").fill("QA manually supplied source");
  await page.locator("#source-authors").fill("Example, Bea");
  await page.locator("#source-year").fill("2023");
  await page
    .getByText("Publication details (optional)", { exact: true })
    .click();
  await page.locator("#source-doi").fill("10.1000/qa-manual");
  await page.locator("#source-journal").fill("Fictional QA Journal");
  await page.locator("#source-volume").fill("2");
  await page.locator("#source-issue").fill("3");
  await page.locator("#source-pages").fill("10–14");
  await page.getByRole("button", { name: "Save source", exact: true }).click();
  assert.equal(
    await page.evaluate(
      () => window.Draft2Done.getState().projects[0].sources[0].journal,
    ),
    "Fictional QA Journal",
  );

  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("tab", { name: "Complete", exact: true }).click();
  const priorAI = aiCalls.length;
  await page
    .getByRole("button", { name: "Generate continuation", exact: true })
    .click();
  assert.equal(aiCalls.length, priorAI);
  assert.equal(searches.length, 0);
  await page.locator(".plan-query").first().fill("urban parks access");
  await page.locator(".plan-query").nth(1).fill("");
  assert.deepEqual(
    await page
      .locator(".plan-query")
      .evaluateAll((els) => els.map((el) => el.value)),
    ["urban parks access", ""],
  );
  await page
    .getByRole("button", { name: "Find sources, then review", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .waitFor();
  assert.equal(aiCalls.length, priorAI);
  assert.deepEqual(searches, ["urban parks access"]);
  assert.equal(
    await page.evaluate(
      () => window.Draft2Done.getState().projects[0].sources.length,
    ),
    2,
  );
  await page.locator('[name="continuation-mode"][value="inline"]').check();
  assert.equal(
    await page
      .locator('[name="continuation-mode"][value="inline"]')
      .isChecked(),
    true,
  );
  await page
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  await page.locator(".proposal-editor").first().waitFor();
  assert.equal(aiCalls[priorAI].continuationPoint.mode, "inline");
  assert.equal(aiCalls[priorAI].evidence.length, 1);
  await page
    .getByRole("button", { name: "Accept section", exact: true })
    .first()
    .click();
  let state = await page.evaluate(() => window.Draft2Done.getState());
  assert.equal(state.projects[0].originalText, original);
  assert.match(
    state.projects[0].workingText,
    /Another concern is that access deserves/,
  );

  await page.getByRole("button", { name: "Research", exact: true }).click();
  await page
    .getByRole("button", { name: "Insert citation", exact: true })
    .click();
  await page.locator("[data-cite-source]").first().check();
  await page.locator("[data-cite-source]").nth(1).check();
  await page
    .getByRole("button", { name: "Insert selected citation", exact: true })
    .click();
  state = await page.evaluate(() => window.Draft2Done.getState());
  assert.equal(state.projects[0].citedSourceIds.length, 2);
  assert.match(state.projects[0].workingText, /Example, 2023; Example, 2024/);
  await page
    .getByRole("button", { name: "Paper actions", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Export & preview", exact: true })
    .click();
  await page.locator("#export-references").uncheck();
  assert.equal(
    await page.locator(".export-preview .pre-wrap").innerText(),
    state.projects[0].workingText,
  );
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  const acceptedText = state.projects[0].workingText;
  const contributions = state.projects[0].contributions;
  await page
    .locator("#draft-editor")
    .fill(acceptedText + "\n\nA new note from the writer.");
  await page.getByRole("tab", { name: "Helper", exact: true }).click();
  await page.getByRole("tab", { name: "My draft", exact: true }).click();
  await page.getByRole("button", { name: "Undo", exact: true }).click();
  assert.equal(await page.locator("#draft-editor").inputValue(), acceptedText);
  assert.deepEqual(
    await page.evaluate(
      () => window.Draft2Done.getState().projects[0].contributions,
    ),
    contributions,
  );
  await page.getByRole("button", { name: "Redo", exact: true }).click();
  assert.match(
    await page.locator("#draft-editor").inputValue(),
    /A new note from the writer/,
  );
  await page.locator("#draft-editor").press("Control+z");
  assert.equal(await page.locator("#draft-editor").inputValue(), acceptedText);
  await page.locator("#paragraph-nav").selectOption("0");
  assert.equal(
    await page.locator("#draft-editor").evaluate((el) => el.selectionStart),
    0,
  );

  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator('[data-setting="defaultMode"]').selectOption("helper");
  await page
    .getByText("Default counting and document appearance", { exact: true })
    .click();
  await page.locator('[data-setting="excludeReferences"]').uncheck();
  await page.locator('[data-setting="exportFontFamily"]').selectOption("Arial");
  await page.locator('[data-setting="exportFontSize"]').fill("14");
  await page.locator('[data-setting="exportFontSize"]').dispatchEvent("change");
  await page.locator('[data-setting="exportLineSpacing"]').selectOption("2");
  await page.getByRole("button", { name: "Papers", exact: true }).click();
  await page
    .getByRole("button", { name: "New workspace", exact: true })
    .click();
  assert.equal(
    await page
      .getByRole("tab", { name: "Helper", exact: true })
      .getAttribute("aria-selected"),
    "true",
  );
  const fresh = await page.evaluate(() =>
    window.Draft2Done.getState().projects.at(-1),
  );
  assert.deepEqual(fresh.countingRules, {
    excludeReferences: false,
    excludeTitle: true,
  });
  assert.deepEqual(fresh.exportFormatting, {
    fontFamily: "Arial",
    fontSize: 14,
    lineSpacing: 2,
  });
  const modelPage = await browser.newPage();
  await modelPage.addInitScript(() => {
    window.modelCalls = [];
    window.assistRequests = [];
    window.failAssist = false;
    window.deferAssist = false;
    window.activeTestAccount = "test-account";
    window.DraftNative = {
      loadState: () =>
        JSON.stringify({
          projects: [],
          settings: {
            autoUpdates: false,
            backend: "ollama",
            model: "old-dev-coder",
            endpoint: "http://127.0.0.1:11435",
            chatgptModel: "gpt-6.1-sol",
          },
        }),
      saveState: () => "ok",
      setAppearance: () => {},
      chatgptCancel: () => {},
      chatgptStatus: () =>
        JSON.stringify({
          available: true,
          connected: true,
          accounts: [],
          active: window.activeTestAccount,
        }),
      chatgpt(action, id, payload) {
        window.modelCalls.push(action);
        if (action === "assist") {
          const request = JSON.parse(payload);
          window.assistRequests.push(request);
          if (window.failAssist) {
            window.failAssist = false;
            setTimeout(
              () =>
                window.NativeEvents.addon(
                  id,
                  {},
                  "ChatGPT usage limit reached. No fallback was attempted.",
                ),
              0,
            );
            return;
          }
          const record = JSON.parse(request.messages[1].content);
          const response =
            record.task === "helper"
              ? {
                  suggestions: [
                    {
                      text: `Compare the argument with a different perspective before choosing a conclusion. Request fixture ${window.assistRequests.length}.`,
                    },
                  ],
                  warnings: [],
                }
              : {
                  paragraphs: [
                    {
                      text: record.earlierSuggestions?.length
                        ? "A second proposed section considers the same question from another angle and explains why the argument still needs evidence before a conclusion can follow."
                        : "Another proposed section connects the next question with the student's existing reasoning.",
                    },
                  ],
                  warnings: [],
                };
          const deliver = () =>
            window.NativeEvents.addon(
              id,
              {
                text: JSON.stringify(response),
                completed: true,
                model: request.model,
              },
              "",
            );
          if (window.deferAssist) window.finishAssist = deliver;
          else setTimeout(deliver, 0);
          return;
        }
        const result = {
          models: [
            { slug: "gpt-6-luna", display_name: "Efficient" },
            { slug: "gpt-6.1-sol", display_name: "Balanced" },
            { slug: "gpt-6-astra", display_name: "Research" },
          ],
        };
        setTimeout(() => window.NativeEvents.addon(id, result, ""), 0);
      },
    };
  });
  await modelPage.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  assert.equal(
    await modelPage.evaluate(
      () => window.Draft2Done.getState().settings.backend,
    ),
    "chatgpt",
  );
  assert.equal(
    await modelPage.evaluate(() => window.Draft2Done.getState().settings.model),
    "old-dev-coder",
  );
  await modelPage
    .getByRole("button", { name: "Settings", exact: true })
    .click();
  await modelPage
    .getByRole("button", { name: "Load available models", exact: true })
    .click();
  await modelPage.waitForFunction(() => !window.Draft2Done.info().busy);
  assert.equal(
    await modelPage.locator("#chatgpt-model").inputValue(),
    "gpt-6.1-sol",
  );
  assert.deepEqual(
    await modelPage
      .locator("#chatgpt-model option")
      .evaluateAll((els) => els.map((el) => el.value)),
    ["", "gpt-6-luna", "gpt-6.1-sol", "gpt-6-astra"],
  );
  const callsBeforeRecommendation = await modelPage.evaluate(
    () => window.modelCalls.length,
  );
  await modelPage
    .getByRole("button", { name: "Best available for research", exact: true })
    .click();
  assert.equal(
    await modelPage.locator("#chatgpt-model").inputValue(),
    "gpt-6-astra",
  );
  assert.equal(
    await modelPage.evaluate(() => window.modelCalls.length),
    callsBeforeRecommendation,
  );
  await modelPage
    .getByRole("button", { name: "Draft2Done home", exact: true })
    .click();
  await modelPage
    .getByRole("button", { name: /Try a fictional sample/ })
    .click();
  const phoneOriginal = await modelPage.locator("#draft-editor").inputValue();
  await modelPage.evaluate(() => {
    const p = window.Draft2Done.getState().projects[0];
    p.instructions =
      "Fictional assignment instructions for the consent fixture.";
    p.rubric = "Explain the link to the thesis.";
    p.sources.push({
      id: "qa-evidence",
      title: "Fictional consent source",
      authors: ["Example, Ada"],
      year: 2024,
      provider: "crossref",
      type: "article",
      url: "https://example.org/qa",
      doi: "10.1000/qa",
      abstract:
        "This is a fictional evidence excerpt used only in the consent test.",
      verification: "abstract",
      included: true,
    });
  });
  await modelPage.getByRole("tab", { name: "Helper", exact: true }).click();
  await modelPage
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  const previewPayload = JSON.parse(
    await modelPage.locator("#remote-payload-preview").textContent(),
  );
  const previewData = JSON.parse(previewPayload.messages[1].content);
  assert.equal(previewData.draft, phoneOriginal);
  assert.match(previewData.assignment.instructions, /consent fixture/);
  assert.ok(previewData.styleProfile.averageSentenceWords);
  assert.match(
    previewData.evidence[0].abstractExcerpt,
    /fictional evidence excerpt/,
  );
  assert.equal(
    await modelPage.locator("#remote-draft-preview").inputValue(),
    phoneOriginal,
  );
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 0);
  await modelPage
    .getByRole("button", { name: "Keep this on my phone", exact: true })
    .click();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 0);
  await modelPage
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  await modelPage.evaluate(() => {
    window.Draft2Done.getState().projects[0].instructions =
      "Changed after preview";
  });
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage
    .getByRole("dialog", { name: "Couldn’t finish this step", exact: true })
    .waitFor();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 0);
  await modelPage
    .getByRole("button", { name: "Close dialog", exact: true })
    .click();
  await modelPage
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  assert.match(
    await modelPage.locator("#remote-payload-preview").textContent(),
    /Changed after preview/,
  );
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage.locator('[data-action="ai-suggestion"]').first().waitFor();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 1);
  assert.equal(
    await modelPage.evaluate(
      () => window.Draft2Done.getState().projects[0].originalText,
    ),
    phoneOriginal,
  );
  await modelPage.locator('[data-action="ai-suggestion"]').first().click();
  await modelPage
    .getByRole("button", { name: "Explain this idea", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  await modelPage
    .getByRole("button", { name: "Keep this on my phone", exact: true })
    .click();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 1);
  await modelPage.getByRole("tab", { name: "Complete", exact: true }).click();
  const currentPhoneWords = Number(
    (await modelPage.locator("#current-count").innerText()).replace(/,/g, ""),
  );
  await modelPage
    .locator("#minimum-input")
    .fill(String(currentPhoneWords + 24));
  await modelPage.locator("#minimum-input").dispatchEvent("change");
  await modelPage
    .getByRole("button", { name: "Generate continuation", exact: true })
    .click();
  await modelPage
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 2);
  assert.match(
    await modelPage.locator("#remote-payload-preview").textContent(),
    /Another proposed section/,
  );
  await modelPage.evaluate(() => {
    window.Draft2Done.getState().projects[0].rubric =
      "Changed during a later generation round";
  });
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage
    .getByRole("dialog", { name: "Couldn’t finish this step", exact: true })
    .waitFor();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 2);
  assert.equal(
    await modelPage.evaluate(
      () => window.Draft2Done.getState().projects[0].proposals.length,
    ),
    0,
  );
  await modelPage
    .getByRole("button", { name: "Close dialog", exact: true })
    .click();
  await modelPage.locator("#minimum-input").fill(String(currentPhoneWords + 5));
  await modelPage.locator("#minimum-input").dispatchEvent("change");
  await modelPage
    .getByRole("button", { name: "Generate continuation", exact: true })
    .click();
  await modelPage
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage.locator(".proposal-editor").first().waitFor();
  await modelPage
    .getByRole("button", { name: "Regenerate", exact: true })
    .first()
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  assert.match(
    await modelPage.locator("#remote-payload-preview").textContent(),
    /sectionToRevise/,
  );
  await modelPage
    .getByRole("button", { name: "Keep this on my phone", exact: true })
    .click();
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 3);
  await modelPage.getByRole("tab", { name: "Helper", exact: true }).click();
  await modelPage.evaluate(() => {
    window.failAssist = true;
  });
  await modelPage
    .getByRole("button", { name: "Analyze my draft", exact: true })
    .click();
  await modelPage.locator("#remote-draft-preview").waitFor();
  await modelPage
    .getByRole("button", { name: "Send to ChatGPT", exact: true })
    .click();
  await modelPage
    .getByRole("dialog", { name: "Couldn’t finish this step", exact: true })
    .waitFor();
  assert.match(await modelPage.getByRole("dialog").innerText(), /usage limit/);
  assert.equal(await modelPage.evaluate(() => window.assistRequests.length), 4);
  await modelPage
    .getByRole("button", { name: "Close dialog", exact: true })
    .click();
  for (const change of ["helper-clone", "complete-clone", "model", "account"]) {
    await modelPage.evaluate(() => {
      window.deferAssist = true;
      window.finishAssist = null;
    });
    const complete = change === "complete-clone";
    await modelPage
      .getByRole("tab", { name: complete ? "Complete" : "Helper", exact: true })
      .click();
    if (complete) {
      const words = Number(
        (await modelPage.locator("#current-count").innerText()).replace(
          /,/g,
          "",
        ),
      );
      await modelPage.locator("#minimum-input").fill(String(words + 5));
      await modelPage.locator("#minimum-input").dispatchEvent("change");
      await modelPage
        .getByRole("button", { name: "Generate continuation", exact: true })
        .click();
      await modelPage
        .getByRole("button", {
          name: "Generate with saved sources",
          exact: true,
        })
        .click();
    } else
      await modelPage
        .getByRole("button", { name: "Analyze my draft", exact: true })
        .click();
    await modelPage.locator("#remote-draft-preview").waitFor();
    const before = await modelPage.evaluate(() => {
      const p = window.Draft2Done.getState().projects[0];
      return {
        notes: p.helperNotes,
        proposals: JSON.stringify(p.proposals),
        text: p.workingText,
      };
    });
    await modelPage
      .getByRole("button", { name: "Send to ChatGPT", exact: true })
      .click();
    await modelPage.waitForFunction(
      () => typeof window.finishAssist === "function",
    );
    if (change.endsWith("clone")) {
      await modelPage
        .getByRole("tab", { name: "My draft", exact: true })
        .click();
      await modelPage
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await modelPage
        .locator("#draft-editor")
        .fill(
          before.text +
            "\n\nThe writer added this while the response was pending.",
        );
    } else
      await modelPage.evaluate((kind) => {
        if (kind === "model")
          window.Draft2Done.getState().settings.chatgptModel = "gpt-6.1-sol";
        else window.activeTestAccount = "another-account";
      }, change);
    const current = await modelPage.evaluate(
      () => window.Draft2Done.getState().projects[0].workingText,
    );
    await modelPage.evaluate(() => window.finishAssist());
    await modelPage
      .getByRole("dialog", { name: "Couldn’t finish this step", exact: true })
      .waitFor();
    assert.match(await modelPage.getByRole("dialog").innerText(), /discarded/);
    const after = await modelPage.evaluate(
      () => window.Draft2Done.getState().projects[0],
    );
    assert.equal(
      after.helperNotes,
      before.notes,
      change + " cannot replace notes",
    );
    assert.equal(
      JSON.stringify(after.proposals),
      before.proposals,
      change + " cannot merge proposals",
    );
    assert.equal(
      after.workingText,
      current,
      change + " preserves current editing",
    );
    await modelPage
      .getByRole("button", { name: "Close dialog", exact: true })
      .click();
  }
  await modelPage.close();
  assert.deepEqual(errors, []);
  console.log(
    "Review UI passed: local workflow plus standalone-phone default/migration, catalog recommendations, exact-data consent, cancellation, changed-data refusal, Helper/follow-up, every Complete round, regeneration, quota failures and no fallback.",
  );
} finally {
  await browser.close();
  await new Promise((r) => server.close(r));
}
