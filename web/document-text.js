import { citation } from "./core.js";

const REFERENCES = /^(?:[ \t]*#{1,6}[ \t]+)?[ \t]*(?:references|works[ \t]+cited|bibliography)[ \t]*:?[ \t]*#*[ \t]*\r?$/im;
const referenceKey = (value) => String(value).normalize("NFKC").toLocaleLowerCase().replace(/\s+/g, " ").trim().replace(/[.]+$/, "");

export function bibliography(project) {
  const contributions = project.contributions || [];
  const used = new Set([
    ...contributions.flatMap((c) => c.sourceIds || []),
    ...(project.citedSourceIds || []),
  ]);
  const seen = new Set();
  return (project.sources || [])
    .filter((source) => source.included !== false && (used.has(source.id) || (contributions.length === 0 && used.size === 0)))
    .map((source) => citation(source, project.citationStyle).bibliography)
    .filter((entry) => {
      const key = referenceKey(entry);
      if (!key || seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((a, b) => a.localeCompare(b));
}

/** Append library entries without replacing the writer's existing references. */
export function exportText(project, includeReferences = true) {
  const text = String(project.workingText || "");
  if (!includeReferences) return text;
  const heading = REFERENCES.exec(text);
  const existingText = heading ? text.slice(heading.index + heading[0].length) : "";
  const existing = new Set(existingText.split(/\n\s*\n/).flatMap((block) => [block, ...block.split(/\r?\n/)]).map(referenceKey));
  const refs = bibliography(project).filter((entry) => !existing.has(referenceKey(entry)));
  if (!refs.length) return text;
  const title = project.citationStyle === "MLA" ? "Works Cited" : "References";
  return text.trimEnd() + "\n\n" + (heading ? "" : title + "\n\n") + refs.join("\n\n");
}

export function exportFormatting(project) {
  const input = project.exportFormatting || {};
  return {
    fontFamily: ["Times New Roman", "Georgia", "Arial"].includes(input.fontFamily) ? input.fontFamily : "Times New Roman",
    fontSize: Math.min(16, Math.max(10, Number(input.fontSize) || 12)),
    lineSpacing: [1, 1.5, 2].includes(Number(input.lineSpacing)) ? Number(input.lineSpacing) : 1.5,
  };
}

/** The editor's small Markdown subset; raw HTML stays plain text. */
export function documentBlocks(text) {
  return String(text).split(/\n\s*\n/).filter(Boolean).flatMap((paragraph) => {
    return paragraph.split(/\r?\n(?=#{1,6} )/).map((part) => {
      const heading = /^(#{1,6})[ \t]+([^\n]+)$/.exec(part);
      const body = heading ? heading[2] : part;
      const runs = [];
      const pattern = /\*\*([^*\n]+)\*\*|(?<!\*)\*([^*\n]+)\*(?!\*)/g;
      let position = 0;
      for (const match of body.matchAll(pattern)) {
        if (match.index > position) runs.push({ text: body.slice(position, match.index) });
        runs.push({ text: match[1] || match[2], bold: Boolean(match[1]), italics: Boolean(match[2]) });
        position = match.index + match[0].length;
      }
      if (position < body.length) runs.push({ text: body.slice(position) });
      return { heading: heading ? heading[1].length : 0, runs };
    });
  });
}
