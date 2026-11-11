import { chromium } from "playwright";
import { Document, Packer, Paragraph } from "docx";
import mammoth from "mammoth";
import { createServer } from "node:http";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { extname, resolve } from "node:path";
import assert from "node:assert/strict";
import { citation } from "../web/core.js";
const root = resolve("web-dist");
const server = createServer(async (req, res) => {
  try {
    const path = resolve(
      root,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!path.startsWith(root)) throw new Error();
    const bytes = await readFile(path);
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
const errors = [],
  checks = [];
try {
  const page = await browser.newPage({ viewport: { width: 393, height: 852 } });
  page.on("pageerror", (e) => errors.push(e.message));
  await page.addInitScript(() => {
    window.exports = [];
    let account = {
      available: true,
      connected: false,
      accounts: [],
      active: "",
    };
    window.DraftNative = {
      loadState: () => localStorage.getItem("native-state") || "{}",
      saveState: (value) => {
        localStorage.setItem("native-state", value);
        return "ok";
      },
      chatgptStatus: () => JSON.stringify(account),
      cancelRequest: () => {},
      chatgptCancel: () => {},
      openExternal: (url) => (window.openedURL = url),
      printDocument: (html) => (window.printedHTML = html),
      exportDocument: (name, mime, base64) =>
        window.exports.push({ name, mime, base64 }),
      importDocument: () => {},
      chatgpt: (action, id, payload) => {
        let result = {};
        if (action === "signin") {
          account = {
            available: true,
            connected: true,
            active: "oaiapp_test",
            researchError: "A ChatGPT usage limit was reached. Check account or app usage controls.",
            accounts: [
              {
                id: "oaiapp_test",
                label: "Fictional test account",
                signedIn: true,
                planEnabled: true,
              },
            ],
          };
          result = account;
        } else if (action === "models")
          result = {
            models: [
              {
                slug: "test-research-model",
                display_name: "Fictional test model",
              },
            ],
          };
        else if (action === "research")
          result = {
            text: "A research summary with a source.",
            query: JSON.parse(payload).query,
            model: "test-research-model",
            completed: true,
            webSearchUsed: true,
            sources: [
              {
                title: "Fictional source for UI test",
                url: "https://example.org/source",
              },
            ],
            annotations: [
              {
                start: 26,
                end: 32,
                title: "Source",
                url: "https://example.org/source",
              },
            ],
            warnings: ["Simulated UI test; not a real finding."],
          };
        else if (action === "update-check")
          result = {
            available: false,
            message: "No public Draft2Done release is available yet.",
          };
        else if (action === "signout") {
          account.connected = false;
          result = { ...account, message: "Signed out." };
        }
        setTimeout(() => window.NativeEvents.addon(id, result, ""), 30);
      },
    };
  });
  await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
  async function importDocument(name, bytes) {
    await page.evaluate((payload) => window.NativeEvents.imported(payload), {
      name,
      mime: "application/octet-stream",
      base64: Buffer.from(bytes).toString("base64"),
    });
    await page.waitForTimeout(900);
  }
  const text =
    "This is an unfinished paper about public libraries. I think libraries give people a place to learn together, but I need to explain my argument with more care. The paper should consider who uses a library and which questions need research before I reach a conclusion.";
  await importDocument("My draft.txt", Buffer.from(text));
  assert.equal(await page.locator("#draft-editor").inputValue(), text);
  checks.push("TXT import");
  await importDocument("Markdown.md", Buffer.from("# My heading\n\n" + text));
  assert.match(
    await page.locator("#draft-editor").inputValue(),
    /# My heading/,
  );
  checks.push("Markdown import");
  const docx = await Packer.toBuffer(
    new Document({
      sections: [
        {
          children: [
            new Paragraph(text),
            new Paragraph("A second paragraph remains unfinished."),
          ],
        },
      ],
    }),
  );
  await importDocument("My Word draft.docx", docx);
  assert.match(
    await page.locator("#draft-editor").inputValue(),
    /A second paragraph/,
  );
  checks.push("DOCX import");
  const pdfPage = await browser.newPage();
  await pdfPage.setContent(
    "<h1>Fictional PDF assignment</h1><p>" + text + "</p>",
  );
  const pdf = await pdfPage.pdf();
  await pdfPage.close();
  await importDocument("My paper.pdf", pdf);
  assert.match(
    await page.locator("#draft-editor").inputValue(),
    /public libraries/,
  );
  checks.push("Text PDF import");
  await importDocument("Samsung export.pdf", await readFile(resolve("tests/fixtures/android-text-export.pdf")));
  const androidPdfText=(await page.locator("#draft-editor").inputValue()).replace(/\s+/g," ");
  assert.match(androidPdfText,/Draft2Done fictional document workflow sentinel/);
  assert.match(androidPdfText,/Fictional Unicode fixture: café and α/);
  assert.match(androidPdfText,/fit into everyday city planning/);
  assert.doesNotMatch(androidPdfText,/fi ctional|work fl ow|fi xture/);
  checks.push("Actual Samsung PDF export reimports ligatures, word boundaries and Unicode correctly");
  await importDocument("My paper.pdf", pdf);
  await page
    .getByRole("button", { name: "Paper actions", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Export & preview", exact: true })
    .click();
  await page.locator("#export-format").selectOption("docx");
  await page
    .getByRole("button", { name: "Export document", exact: true })
    .click();
  await page.waitForTimeout(300);
  const exported = await page.evaluate(() => window.exports.at(-1));
  const extracted = await mammoth.extractRawText({
    buffer: Buffer.from(exported.base64, "base64"),
  });
  assert.match(extracted.value, /public libraries/);
  checks.push("DOCX export and independent read-back");
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page
    .getByRole("button", { name: "Continue with ChatGPT", exact: true })
    .click();
  await page.waitForTimeout(200);
  assert.equal(await page.locator(".chatgpt-card .pill").innerText(), "Connected");
  assert.match(await page.locator(".chatgpt-card").innerText(), /Last AI request/);
  checks.push("Usage-limit notice keeps the authenticated account connected (simulated)");
  await page
    .getByRole("button", { name: "Load available models", exact: true })
    .click();
  await page.waitForTimeout(200);
  assert.equal(
    await page.locator("#chatgpt-model").inputValue(),
    "test-research-model",
  );
  checks.push("ChatGPT connect and model picker (simulated native bridge)");
  await page.getByRole("button", { name: "Research", exact: true }).click();
  await page.getByRole("button", { name: "Ask ChatGPT", exact: true }).click();
  await page.locator("#chatgpt-query").fill("What makes libraries accessible?");
  await page
    .getByRole("button", { name: "Send research request", exact: true })
    .click();
  await page.locator(".chatgpt-brief").waitFor();
  assert.match(
    await page.locator(".chatgpt-brief").innerText(),
    /research summary/,
  );
  checks.push("Opt-in research preview and cited brief (simulated)");
  await page.getByRole("button", { name: "Write", exact: true }).click();
  await page
    .getByRole("button", { name: "Paper actions", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Back up project (.d2d)", exact: true })
    .click();
  await page.waitForTimeout(100);
  const backup = await page.evaluate(() => window.exports.at(-1));
  const saved = JSON.parse(
    Buffer.from(backup.base64, "base64").toString("utf8"),
  );
  assert.equal(saved.project.researchBriefs.length, 1);
  assert.ok(!JSON.stringify(saved).includes("access_token"));
  await importDocument("Recovered.d2d", Buffer.from(backup.base64, "base64"));
  assert.equal(
    await page.evaluate(
      () => window.Draft2Done.getState().projects.at(-1).researchBriefs.length,
    ),
    1,
  );
  checks.push(
    "Project backup/import retains research and excludes credentials",
  );
  await page
    .getByRole("button", { name: "Paper actions", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Export & preview", exact: true })
    .click();
  await page.locator("#export-format").selectOption("pdf");
  await page
    .getByRole("button", { name: "Export document", exact: true })
    .click();
  assert.match(
    await page.evaluate(() => window.printedHTML),
    /public libraries/,
  );
  checks.push("PDF print preview bridge");
  const formatted = await page.evaluate(() => {
    const state = window.Draft2Done.getState();
    return state.projects.find((project) => project.id === state.activeId);
  });
  const firstSource = { id: "format-a", title: "First library study", authors: ["Smith, Jane"], year: "2024", doi: "10.1234/library-a", included: true };
  formatted.sources = [firstSource, { ...firstSource, id: "format-b", title: "Second library study", doi: "10.1234/library-b" }];
  formatted.contributions = [];
  formatted.citedSourceIds = [];
  formatted.workingText = "## Findings\n\nA **strong** point and *careful* interpretation about public libraries.\n\nReferences\n\n" + citation(firstSource, "APA").bibliography;
  formatted.exportFormatting = { fontFamily: "Arial", fontSize: 14, lineSpacing: 2 };
  await importDocument("Formatted.d2d", Buffer.from(JSON.stringify({ format: "draft2done", version: 1, project: formatted })));
  await page.getByRole("button", { name: "Paper actions", exact: true }).click();
  await page.getByRole("button", { name: "Export & preview", exact: true }).click();
  assert.equal((await page.locator(".export-preview").innerText()).match(/References/g).length, 1);
  assert.match(await page.locator(".export-preview").innerText(), /Second library study/);
  await page.locator("#export-references").uncheck();
  assert.doesNotMatch(await page.locator(".export-preview").innerText(), /Second library study/);
  assert.match(await page.locator(".export-preview").innerText(), /First library study/);
  await page.locator("#export-references").check();
  await page.locator("#export-format").selectOption("docx");
  await page.getByRole("button", { name: "Export document", exact: true }).click();
  await page.waitForTimeout(300);
  const formattedExport = await page.evaluate(() => window.exports.at(-1));
  const formattedReadback = await mammoth.convertToHtml({ buffer: Buffer.from(formattedExport.base64, "base64") });
  assert.match(formattedReadback.value, /<h2>Findings<\/h2>/);
  assert.match(formattedReadback.value, /<strong>strong<\/strong>/);
  assert.match(formattedReadback.value, /<em>careful<\/em>/);
  assert.equal((formattedReadback.value.match(/References/g) || []).length, 1);
  checks.push("Bibliography deduplication, live preview toggle and formatted DOCX independent read-back");
  await page.evaluate(() => Object.assign(window.Draft2Done.getState().settings, {
    defaultVoice: "polished", citationStyle: "MLA", defaultMode: "helper",
    excludeReferences: false, excludeTitle: false,
    exportFontFamily: "Georgia", exportFontSize: 11, exportLineSpacing: 1,
  }));
  for (const [name, bytes] of [["Defaults.txt", Buffer.from(text)], ["Defaults.md", Buffer.from(text)], ["Defaults.docx", docx], ["Defaults.pdf", pdf]]) {
    await importDocument(name, bytes);
    const imported = await page.evaluate(() => {
      const s = window.Draft2Done.getState();
      return s.projects.find(p => p.id === s.activeId);
    });
    assert.equal(imported.voice, "polished", name);
    assert.equal(imported.citationStyle, "MLA", name);
    assert.deepEqual(imported.countingRules, { excludeReferences: false, excludeTitle: false }, name);
    assert.deepEqual(imported.exportFormatting, { fontFamily: "Georgia", fontSize: 11, lineSpacing: 1 }, name);
    assert.equal(await page.getByRole("tab", { name: "Helper", exact: true }).getAttribute("aria-selected"), "true", name);
  }
  checks.push("TXT/MD/DOCX/PDF imports apply current writing, count, appearance and opening-mode defaults");
  await importDocument("Own settings.d2d", Buffer.from(JSON.stringify({ format: "draft2done", version: 1, project: formatted })));
  const restoredSettings = await page.evaluate(() => {
    const s = window.Draft2Done.getState();
    return s.projects.find(p => p.id === s.activeId);
  });
  assert.equal(restoredSettings.voice, formatted.voice);
  assert.equal(restoredSettings.citationStyle, formatted.citationStyle);
  assert.deepEqual(restoredSettings.countingRules, formatted.countingRules);
  assert.deepEqual(restoredSettings.exportFormatting, formatted.exportFormatting);
  checks.push("Project backup import preserves its own writing/count/export settings despite different global defaults");
  assert.deepEqual(errors, []);
  await mkdir("test-results", { recursive: true });
  await writeFile(
    "test-results/documents-report.json",
    JSON.stringify({ passed: true, checks, errors }, null, 2),
  );
  console.log(
    "Document and optional connection UI checks passed:",
    checks.length,
  );
} finally {
  await browser.close();
  await new Promise((r) => server.close(r));
}
