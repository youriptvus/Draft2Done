import { build } from "esbuild";
import { mkdir, copyFile, readFile, writeFile } from "node:fs/promises";
const pkg = JSON.parse(await readFile("package.json", "utf8"));
if (typeof pkg.version !== "string" || !/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/.test(pkg.version))
  throw new Error("package.json must declare a semantic application version.");
await mkdir("web-dist", { recursive: true });
await build({
  entryPoints: ["web/app.js"],
  bundle: true,
  outfile: "web-dist/app.js",
  format: "iife",
  target: ["chrome100"],
  minify: true,
  legalComments: "eof",
  define: { __DRAFT2DONE_VERSION__: JSON.stringify(pkg.version) },
});
await copyFile("web/styles.css", "web-dist/styles.css");
await copyFile("web/index.html", "web-dist/index.html");
await copyFile(
  "node_modules/pdfjs-dist/build/pdf.worker.min.mjs",
  "web-dist/pdf.worker.min.mjs",
);
await writeFile(
  "web-dist/version.json",
  JSON.stringify({ version: pkg.version, builtAt: new Date().toISOString() }),
);
console.log("Bundled offline web application to web-dist/");
