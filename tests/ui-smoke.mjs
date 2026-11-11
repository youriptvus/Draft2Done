import { chromium } from "playwright";
import { createServer } from "node:http";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { extname, resolve } from "node:path";
import assert from "node:assert/strict";
process.env.PLAYWRIGHT_BROWSERS_PATH = resolve("tools/playwright");
const root = resolve("web-dist");
const server = createServer(async (req, res) => {
  try {
    const path = resolve(
      root,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!path.startsWith(root)) throw new Error();
    const bytes = await readFile(
      path.endsWith("web-dist") ? resolve(root, "index.html") : path,
    );
    res.setHeader(
      "Content-Type",
      {
        ".html": "text/html",
        ".js": "application/javascript",
        ".mjs": "application/javascript",
        ".css": "text/css",
      }[extname(path)] || "application/octet-stream",
    );
    res.end(bytes);
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
const errors = [];
await mkdir("test-results", { recursive: true });
try {
  const context = await browser.newContext({
    viewport: { width: 393, height: 852 },
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  await page.getByRole("button", { name: /Try a fictional sample/ }).click();
  await page.locator("#draft-editor").waitFor();
  const before = await page.evaluate(
    () => window.Draft2Done.getState().projects[0].originalText,
  );
  await page
    .locator("#draft-editor")
    .fill(before + "\n\nAccess also means listening to different people.");
  assert.equal(
    await page.evaluate(
      () => window.Draft2Done.getState().projects[0].originalText,
    ),
    before,
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.waitForTimeout(900);
  await page.getByRole("button", { name: "Writer DNA", exact: true }).click();
  const diversity = await page.locator(".metric-grid>div").nth(3).innerText();
  assert.ok(Number.parseInt(diversity) <= 100);
  await page.getByRole("button", { name: /Save profile/ }).click();
  await page
    .getByRole("button", { name: "Save profile", exact: true })
    .last()
    .click();
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().profiles.length),
    1,
  );
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator("[data-setting=theme]").selectOption("dark");
  assert.equal(await page.locator("html").getAttribute("data-theme"), "dark");
  assert.equal(
    await page.locator("[data-setting=autoUpdates]").isChecked(),
    true,
  );
  await page.locator("[data-setting=theme]").selectOption("light");
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("tab", { name: "Complete", exact: true }).click();
  await page.locator("#minimum-input").fill("190");
  await page.locator("#minimum-input").dispatchEvent("change");
  await page.getByRole("button", { name: /Generate continuation/ }).click();
  await page.locator("summary.advanced-title").click();
  await page.locator('[data-setting="backend"]').selectOption("ollama");
  await page.locator("summary.advanced-title").click();
  await page.route("**/api/tags", (route) =>
    route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({
        models: [
          { name: "qwen2.5-coder:3b" },
          { name: "llama3:8b-instruct-q4_K_M" },
        ],
      }),
    }),
  );
  await page.getByRole("button", { name: "Find models", exact: true }).click();
  await page.waitForFunction(() => !window.Draft2Done.info().busy);
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().settings.model),
    "llama3:8b-instruct-q4_K_M",
  );
  await page.locator("summary.advanced-title").click();
  await page.locator("[data-setting=model]").fill("test-model");
  await page.locator("[data-setting=model]").dispatchEvent("change");
  await page.getByRole("button", { name: "Find models", exact: true }).click();
  await page.waitForFunction(() => !window.Draft2Done.info().busy);
  assert.equal(
    await page.evaluate(() => window.Draft2Done.getState().settings.model),
    "test-model",
  );
  await page.locator("summary.advanced-title").click();
  await page.locator("[data-setting=context]").fill("8192");
  await page.locator("[data-setting=context]").dispatchEvent("change");
  await page.route("**/api/chat", async (route) =>
    route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({
        message: {
          content: JSON.stringify({
            paragraphs: [
              {
                text: "Another way to look at this is to ask the people who live nearby what they need. Some might want a quiet place to sit, while others might want space to meet friends. Listening to these different views can help keep the argument focused on everyday access rather than just the location of a park. This gives the discussion a clearer direction and a practical question to explore next.",
              },
            ],
            warnings: [],
          }),
        },
      }),
    }),
  );
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("tab", { name: "Complete", exact: true }).click();
  await page.getByRole("button", { name: /Generate continuation/ }).click();
  await page
    .getByRole("button", { name: "Generate with saved sources", exact: true })
    .click();
  await page.locator(".proposal-editor").first().waitFor({ timeout: 15000 });
  const originalCount = await page.evaluate(
    () => window.Draft2Done.getState().projects[0].contributions.length,
  );
  assert.equal(originalCount, 0);
  await page
    .getByRole("button", { name: "Accept section", exact: true })
    .first()
    .click();
  assert.equal(
    await page.evaluate(
      () => window.Draft2Done.getState().projects[0].contributions.length,
    ),
    1,
  );
  await page.getByRole("tab", { name: "My draft", exact: true }).click();
  await page.screenshot({
    path: "test-results/editor-mobile.png",
    fullPage: true,
  });
  await page.reload();
  await page.locator(".paper-card").first().click();
  await page.locator("#draft-editor").waitFor();
  assert.ok(
    (await page.locator("#draft-editor").inputValue()).includes(
      "Another way to look",
    ),
  );
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator("[data-setting=autosave]").uncheck();
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const beforeDisabledEdit = await page.locator("#draft-editor").inputValue();
  await page
    .locator("#draft-editor")
    .fill(beforeDisabledEdit + "\n\nManual-save-only change.");
  await page.evaluate(() => {
    window.NativeEvents.pause();
    window.dispatchEvent(new Event("pagehide"));
    Object.defineProperty(document, "hidden", {
      configurable: true,
      value: true,
    });
    document.dispatchEvent(new Event("visibilitychange"));
    delete document.hidden;
  });
  assert.equal(
    await page.evaluate(() => {
      const saved = JSON.parse(localStorage.getItem("draft2done-state"));
      return saved.projects.find((p) => p.id === saved.activeId).workingText;
    }),
    beforeDisabledEdit,
    "Backgrounding respects disabled autosave",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  assert.ok(
    await page.evaluate(() =>
      localStorage
        .getItem("draft2done-state")
        .includes("Manual-save-only change."),
    ),
    "Manual save writes immediately even with autosave disabled",
  );
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.locator("[data-setting=autosave]").check();
  await page.getByRole("button", { name: "Write", exact: true }).click();
  const immediateFlush = await page.evaluate(() => {
    const input = document.querySelector("#draft-editor");
    input.value += "\n\nSaved before the app backgrounds.";
    input.dispatchEvent(new Event("input", { bubbles: true }));
    const before = localStorage.getItem("draft2done-state");
    window.NativeEvents.pause();
    const after = localStorage.getItem("draft2done-state");
    return {
      before: before.includes("Saved before the app backgrounds."),
      after: after.includes("Saved before the app backgrounds."),
    };
  });
  assert.deepEqual(
    immediateFlush,
    { before: false, after: true },
    "Android pause flushes a pending edit before the debounce timer",
  );
  await page.getByRole("button", { name: "Research", exact: true }).click();
  await page.screenshot({
    path: "test-results/research-mobile.png",
    fullPage: true,
  });
  for (const width of [360, 393, 768]) {
    await page.setViewportSize({ width, height: 852 });
    assert.ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
      `Horizontal overflow at ${width}`,
    );
  }
  assert.deepEqual(errors, []);
  await writeFile(
    "test-results/ui-report.json",
    JSON.stringify(
      {
        passed: true,
        checks: [
          "startup",
          "sample import",
          "draft edit and original preservation",
          "local save and reload",
          "Writer DNA metrics",
          "persistent profile",
          "theme",
          "automatic updates default",
          "AI setup guidance",
          "proposal generation with simulated server",
          "acceptance and provenance",
          "disabled autosave respected on background",
          "immediate manual save with autosave disabled",
          "pending edit flushed on Android pause",
          "mobile widths",
        ],
        errors,
      },
      null,
      2,
    ),
  );
  console.log(
    "UI integration passed: edit, original protection, save/reload, DNA, profiles, theme, update default, AI proposal review, responsive widths. AI response was simulated for UI integration.",
  );
} finally {
  await browser.close();
  await new Promise((r) => server.close(r));
}
