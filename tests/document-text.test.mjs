import test from "node:test";
import assert from "node:assert/strict";
import { createProject, citation } from "../web/core.js";
import { bibliography, exportText, exportFormatting, documentBlocks } from "../web/document-text.js";

const source = (id, title) => ({ id, title, authors: ["Smith, Jane"], year: "2024", doi: "10.1234/" + id, included: true });

test("export adds one reference heading and never duplicates an existing library entry", () => {
  const project = createProject("Paper", "The writer's own paragraph.");
  project.sources = [source("a", "First study"), source("b", "Second study")];
  const entry = citation(project.sources[0], "APA").bibliography;
  project.workingText += "\n\n## References\n\n" + entry;
  const output = exportText(project);
  assert.equal((output.match(/References/g) || []).length, 1);
  assert.equal((output.match(/First study/g) || []).length, 1);
  assert.match(output, /Second study/);
  assert.equal(project.workingText.endsWith(entry), true);
});

test("bibliography toggle preserves references already in the writer's text", () => {
  const project = createProject("Paper", "Body.\n\nWorks Cited\n\nWriter's manually entered reference.");
  project.sources = [source("a", "Library reference")];
  assert.equal(exportText(project, false), project.workingText);
  assert.match(exportText(project, true), /Writer's manually entered reference/);
  assert.equal((exportText(project, true).match(/Works Cited|References/g) || []).length, 1);
});

test("similar references with DOI prefixes are not silently discarded", () => {
  const project = createProject("Paper", "Argument.");
  const first = source("a", "Same study title");
  const second = { ...first, id: "ab", doi: "10.1234/ab" };
  project.sources = [first, second];
  project.workingText += "\n\nReferences\n\n" + citation(second, "APA").bibliography;
  const output = exportText(project);
  assert.equal((output.match(/Same study title/g) || []).length, 2);
});

test("manual citations remain in bibliography alongside accepted generated citations", () => {
  const project = createProject("Paper", "Existing argument.");
  project.sources = [source("a", "Generated support"), source("b", "Manual support"), source("c", "Excluded support")];
  project.sources[2].included = false;
  project.contributions = [{ sourceIds: ["a"] }];
  project.citedSourceIds = ["b", "c"];
  assert.equal(bibliography(project).length, 2);
  assert.match(exportText(project), /Manual support/);
  assert.doesNotMatch(exportText(project), /Excluded support/);
});

test("repeated source records produce only one bibliography entry", () => {
  const project = createProject("Paper", "Body.");
  project.sources = [source("a", "Same study"), { ...source("a", "Same study"), id: "duplicate" }];
  assert.equal(bibliography(project).length, 1);
});

test("export typography accepts supported values and bounds untrusted formatting", () => {
  assert.deepEqual(exportFormatting({ exportFormatting: { fontFamily: "Arial", fontSize: 14, lineSpacing: 2 } }), { fontFamily: "Arial", fontSize: 14, lineSpacing: 2 });
  assert.deepEqual(exportFormatting({ exportFormatting: { fontFamily: 'Arial";background:url(https://example.org)', fontSize: 900, lineSpacing: -3 } }), { fontFamily: "Times New Roman", fontSize: 16, lineSpacing: 1.5 });
});

test("editor Markdown renders to document runs while HTML remains inert text", () => {
  const blocks = documentBlocks("## Findings\n\nA **strong** point and *careful* wording.\n<script>danger()</script>");
  assert.equal(blocks[0].heading, 2);
  assert.ok(blocks[1].runs.some((run) => run.bold && run.text === "strong"));
  assert.ok(blocks[1].runs.some((run) => run.italics && run.text === "careful"));
  assert.ok(blocks[1].runs.some((run) => run.text.includes("<script>")));
});
