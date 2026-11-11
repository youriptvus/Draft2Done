import mammoth from "mammoth/mammoth.browser.js";
import { Document, Packer, Paragraph, TextRun, HeadingLevel } from "docx";
import * as pdfjs from "pdfjs-dist/build/pdf.mjs";
import { base64ToBytes, exportFile, printHTML } from "./platform.js";
import { exportText, exportFormatting, documentBlocks } from "./document-text.js";
export { bibliography, exportText } from "./document-text.js";
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdf.worker.min.mjs",
  location.href,
).href;
export const escapeHTML = (text) =>
  String(text ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
function checkDOCXArchive(bytes) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let end = -1;
  for (let i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--)
    if (view.getUint32(i, true) === 0x06054b50) {
      end = i;
      break;
    }
  if (end < 0) throw new Error("This DOCX is not a valid document archive.");
  const count = view.getUint16(end + 10, true);
  let at = view.getUint32(end + 16, true),
    total = 0;
  if (count > 2000 || count === 65535)
    throw new Error("This DOCX has too many archive entries.");
  for (let i = 0; i < count; i++) {
    if (at + 46 > bytes.length || view.getUint32(at, true) !== 0x02014b50)
      throw new Error("The DOCX archive directory is invalid.");
    total += view.getUint32(at + 24, true);
    if (total > 40 * 1024 * 1024)
      throw new Error(
        "This DOCX expands beyond the 40 MB document safety limit.",
      );
    at +=
      46 +
      view.getUint16(at + 28, true) +
      view.getUint16(at + 30, true) +
      view.getUint16(at + 32, true);
  }
}
export async function readDocument(payload) {
  const { name, base64 } = payload;
  const bytes = base64ToBytes(base64),
    extension = name.split(".").pop().toLowerCase();
  if (bytes.length > 10 * 1024 * 1024)
    throw new Error("Choose a document smaller than 10 MB.");
  if (["txt", "md", "d2d", "json"].includes(extension))
    return {
      name,
      text: new TextDecoder().decode(bytes),
      project: ["d2d", "json"].includes(extension),
    };
  if (extension === "docx") {
    checkDOCXArchive(bytes);
    const result = await mammoth.extractRawText({ arrayBuffer: bytes.buffer });
    if (result.value.length > 500000)
      throw new Error(
        "The imported text is too large. Split it into smaller assignments.",
      );
    return {
      name,
      text: result.value,
      warning:
        "DOCX text and paragraph breaks imported. Review formatting in the editor; images, tables and page layout are not retained.",
    };
  }
  if (extension === "pdf") {
    const task = pdfjs.getDocument({
      data: bytes,
      isEvalSupported: false,
      useSystemFonts: true,
    });
    const pdf = await task.promise;
    try {
      const paragraphs = [];
      if (pdf.numPages > 250)
        throw new Error("PDF import is limited to 250 pages.");
      for (let p = 1; p <= pdf.numPages; p++) {
        const page = await pdf.getPage(p);
        const content = await page.getTextContent();
        let line = "";
        for (const item of content.items) {
          // PDF.js supplies whitespace items. Adjacent glyph/font runs can be
          // parts of one word (including fi/fl ligatures), so do not add gaps.
          line += item.str + (item.hasEOL ? "\n" : "");
        }
        paragraphs.push(line.trim());
      }
      const text = paragraphs.join("\n\n");
      if (!text.trim())
        throw new Error(
          "This PDF has no extractable text. Scanned pages need OCR before importing.",
        );
      return {
        name,
        text,
        warning:
          "PDF text extracted. Check reading order and line breaks; scanned pages and page layout are not retained.",
      };
    } finally {
      await pdf.destroy();
    }
  }
  throw new Error(
    "Supported files: TXT, Markdown, DOCX, text PDF and Draft2Done project (.d2d).",
  );
}
export function previewHTML(project, includeReferences = true) {
  const text = exportText(project, includeReferences);
  const format = exportFormatting(project);
  const content = documentBlocks(text).map((block) => {
    const tag = block.heading ? `h${block.heading}` : "p";
    const runs = block.runs.map((run) => {
      let html = escapeHTML(run.text);
      if (run.bold) html = `<strong>${html}</strong>`;
      if (run.italics) html = `<em>${html}</em>`;
      return html;
    }).join("");
    return `<${tag}>${runs}</${tag}>`;
  }).join("");
  return `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHTML(project.title)}</title><style>body{font:${format.fontSize}pt "${format.fontFamily}",serif;line-height:${format.lineSpacing};max-width:750px;margin:48px auto;padding:0 25px;color:#171b21}h1{font-size:20pt}p{white-space:pre-wrap;overflow-wrap:anywhere}@page{margin:22mm}</style></head><body><h1>${escapeHTML(project.title)}</h1>${content}</body></html>`;
}
export async function writeDocument(project, format, includeReferences = true) {
  const filename =
    project.title.replace(/[^\p{L}\p{N} _-]/gu, "").slice(0, 80) ||
    "Draft2Done paper";
  if (format === "pdf") {
    printHTML(previewHTML(project, includeReferences));
    return;
  }
  if (format === "project") {
    exportFile(
      filename + ".d2d",
      "application/octet-stream",
      new TextEncoder().encode(
        JSON.stringify({ format: "draft2done", version: 1, project }, null, 2),
      ),
    );
    return;
  }
  const text = exportText(project, includeReferences);
  if (format === "docx") {
    const typography = exportFormatting(project);
    const children = [
      new Paragraph({ text: project.title, heading: HeadingLevel.TITLE }),
      ...documentBlocks(text)
        .map(
          (block) =>
            new Paragraph({
              ...(block.heading ? { heading: HeadingLevel[`HEADING_${block.heading}`] } : {}),
              children: block.runs.flatMap((run) => run.text.split(/\r?\n/).map((line, index) => new TextRun({
                text: line, font: typography.fontFamily, size: typography.fontSize * 2,
                bold: run.bold, italics: run.italics, ...(index > 0 ? { break: 1 } : {}),
              }))),
              spacing: { after: 160, line: Math.round(typography.lineSpacing * 240) },
            }),
        ),
    ];
    const doc = new Document({
      creator: "Draft2Done",
      title: project.title,
      sections: [{ properties: {}, children }],
    });
    exportFile(
      filename + ".docx",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      new Uint8Array(await Packer.toArrayBuffer(doc)),
    );
    return;
  }
  exportFile(
    filename + "." + (format === "md" ? "md" : "txt"),
    "text/plain",
    new TextEncoder().encode(text),
  );
}
