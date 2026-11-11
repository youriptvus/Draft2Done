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
    requests = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.route("**/api/chat", async (route) => {
    requests.push(JSON.parse(route.request().postData()));
    await route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({
        message: {
          content: JSON.stringify({
            paragraphs: [
              {
                text: "The next section can explain a different view of the question and show how the writer can connect it to the main argument.",
              },
            ],
            warnings: [],
          }),
        },
        done: true,
        done_reason: "stop",
      }),
    });
  });
  const active = () =>
    page.evaluate(() => {
      const state = window.Draft2Done.getState();
      return state.projects.find((p) => p.id === state.activeId);
    });
  const paperAction = async (action) => {
    await page
      .getByRole("button", { name: "Paper actions", exact: true })
      .click();
    await page.locator(`[data-action="${action}"]`).click();
  };
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  await page.getByRole("button", { name: /Try a fictional sample/ }).click();
  const original = await active();
  await page.getByRole("button", { name: /Set goal/ }).click();
  await page.locator("#req-minimum").fill("240");
  await page.locator("#req-maximum").fill("200");
  await page
    .locator("#req-instructions")
    .fill("Compare access with distance without inventing findings.");
  await page
    .locator("#req-rubric")
    .fill("Explain each connection to the thesis.");
  await page.locator("#req-level").fill("Undergraduate");
  await page.locator("#req-headings").fill("Discussion\nConclusion");
  await page.locator("#req-citation").selectOption("MLA");
  await page
    .getByRole("button", { name: "Save requirements", exact: true })
    .click();
  assert.equal(
    await page
      .getByRole("dialog", { name: "Assignment & word goal", exact: true })
      .count(),
    1,
  );
  assert.equal((await active()).minimum, original.minimum);
  assert.equal((await active()).maximum, original.maximum);
  await page.locator("#req-maximum").fill("280");
  await page
    .getByRole("button", { name: "Save requirements", exact: true })
    .click();
  const requirements = await active();
  assert.equal(requirements.minimum, 240);
  assert.equal(requirements.maximum, 280);
  assert.equal(requirements.rubric, "Explain each connection to the thesis.");
  assert.equal(requirements.requiredHeadings, "Discussion\nConclusion");
  assert.equal(requirements.academicLevel, "Undergraduate");
  assert.equal(requirements.citationStyle, "MLA");
  await page.getByRole("button", { name: /Set goal/ }).click();
  const choose = page.waitForEvent("filechooser");
  await page
    .getByRole("button", { name: "Import assignment text", exact: true })
    .click();
  const importedInstructions =
    "Imported fictional assignment: evaluate two explanations and state a limitation.";
  await (
    await choose
  ).setFiles({
    name: "assignment.txt",
    mimeType: "text/plain",
    buffer: Buffer.from(importedInstructions),
  });
  await page.waitForFunction(
    (expected) =>
      document.querySelector("#req-instructions")?.value === expected,
    importedInstructions,
  );
  assert.equal(
    await page.locator("#req-rubric").inputValue(),
    requirements.rubric,
  );
  assert.equal(await page.locator("#req-maximum").inputValue(), "280");
  await page
    .getByRole("button", { name: "Save requirements", exact: true })
    .click();
  assert.equal((await active()).instructions, importedInstructions);
  assert.equal((await active()).originalText, original.originalText);

  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator("summary.advanced-title").click();
  await page.locator('[data-setting="backend"]').selectOption("ollama");
  await page.locator("summary.advanced-title").click();
  await page.locator('[data-setting="model"]').fill("fictional-ui-model");
  await page.locator('[data-setting="model"]').dispatchEvent("change");
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("tab", { name: "Complete", exact: true }).click();
  await page.locator("#voice-select").selectOption("polished");
  const currentWords = Number(
    (await page.locator("#current-count").innerText()).replace(/,/g, ""),
  );
  await page.locator("#minimum-input").fill(String(currentWords + 10));
  await page.locator("#minimum-input").dispatchEvent("change");
  await page
    .getByRole("button", { name: "Generate continuation", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  await page.locator(".proposal-editor").first().waitFor();
  assert.equal(requests.length, 1);
  assert.match(requests[0].messages[0].content, /Use Polished Voice/);
  const record = JSON.parse(requests[0].messages[1].content);
  assert.equal(record.assignment.instructions, importedInstructions);
  assert.equal(record.assignment.rubric, requirements.rubric);
  assert.equal(record.assignment.academicLevel, requirements.academicLevel);
  const beforeReject = await active();
  await page
    .getByRole("button", { name: "Reject", exact: true })
    .first()
    .click();
  assert.equal(await page.locator(".proposal-editor").count(), 0);
  assert.equal((await active()).workingText, beforeReject.workingText);
  assert.equal((await active()).proposals[0].status, "rejected");
  assert.equal((await active()).originalText, original.originalText);

  await page.getByRole("tab", { name: "My draft", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const checkpoint = (await active()).revisions.at(-1).id;
  await page
    .locator("#draft-editor")
    .fill(original.workingText + "\n\nA temporary revision to restore later.");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await paperAction("history");
  await page
    .locator(`[data-action="restore"][data-id="${checkpoint}"]`)
    .click();
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  assert.match((await active()).workingText, /temporary revision/);
  await paperAction("history");
  await page
    .locator(`[data-action="restore"][data-id="${checkpoint}"]`)
    .click();
  await page
    .getByRole("button", { name: "Restore revision", exact: true })
    .click();
  assert.equal((await active()).workingText, original.workingText);
  assert.equal((await active()).originalText, original.originalText);
  assert.ok(
    (await active()).revisions.some((r) =>
      /temporary revision/.test(r.workingText),
    ),
  );

  await page.getByRole("button", { name: "Writer DNA", exact: true }).click();
  await page.locator("#profile-notes").fill("Keep my short paragraphs.");
  await page.getByRole("button", { name: "Save profile", exact: true }).click();
  await page.locator("#profile-name").fill("Fictional academic voice");
  await page.locator('[data-action="profile-save-confirm"]').click();
  await page.getByRole("button", { name: "Edit profile", exact: true }).click();
  await page.locator("#edit-profile-name").fill("Edited academic voice");
  await page
    .locator("#edit-profile-notes")
    .fill("Keep my short paragraphs and plain transitions.");
  await page.getByRole("button", { name: "Save voice", exact: true }).click();
  await page.getByRole("button", { name: "Papers", exact: true }).click();
  await page
    .getByRole("button", { name: "New workspace", exact: true })
    .click();
  const secondText =
    "I am working on a different paper. My draft uses shorter sentences. I need to compare two ideas carefully. This paragraph gives me enough existing writing to examine my own patterns and apply a saved writing profile for another assignment.";
  await page.locator("#draft-editor").fill(secondText);
  await page.getByRole("button", { name: "Writer DNA", exact: true }).click();
  await page.getByRole("button", { name: "Use", exact: true }).click();
  assert.equal(
    (await active()).profile.userNotes,
    "Keep my short paragraphs and plain transitions.",
  );
  const savedVoice = await page.evaluate(
    () => window.Draft2Done.getState().profiles[0],
  );
  assert.equal(savedVoice.name, "Edited academic voice");
  assert.equal(
    (await active()).profile.averageSentenceWords,
    savedVoice.profile.averageSentenceWords,
  );
  const secondId = (await active()).id;
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.reload();
  await page.locator(`[data-action="open"][data-id="${secondId}"]`).click();
  assert.equal(
    (await active()).profile.userNotes,
    savedVoice.profile.userNotes,
  );
  assert.equal(
    (await active()).profile.averageSentenceWords,
    savedVoice.profile.averageSentenceWords,
  );
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().profiles[0].name),
    savedVoice.name,
  );

  await paperAction("delete");
  await page.getByRole("button", { name: "Keep project", exact: true }).click();
  assert.equal((await active()).id, secondId);
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().projects.length),
    2,
  );
  await paperAction("delete");
  await page
    .getByRole("button", { name: "Delete project", exact: true })
    .click();
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().projects.length),
    1,
  );
  assert.equal((await active()).id, original.id);
  assert.equal((await active()).originalText, original.originalText);
  assert.deepEqual(errors, []);
  console.log(
    "Management UI passed: requirement import/rubric/maximum validation, Polished Voice prompt, reject, restore cancel/confirm, saved voice edit/apply/reopen, delete cancel/confirm. All inference was mocked.",
  );
} finally {
  await browser.close();
  await new Promise((r) => server.close(r));
}
