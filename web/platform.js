const pending = new Map();
let importHandler = null;
let noticeHandler = null;
export const native = typeof window !== "undefined" && !!window.DraftNative;
export function onImported(callback) {
  importHandler = callback;
}
export function onNotice(callback) {
  noticeHandler = callback;
}
export function addonStatus() {
  return native && window.DraftNative.chatgptStatus
    ? JSON.parse(window.DraftNative.chatgptStatus())
    : { available: false, accounts: [], connected: false };
}
export function addonRequest(action, payload = {}, signal) {
  if (!native || !window.DraftNative.chatgpt)
    return Promise.reject(
      new Error("ChatGPT plan sign-in is available in the Android app."),
    );
  const id = crypto.randomUUID();
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Cancelled", "AbortError"));
      return;
    }
    const abort = () => {
      window.DraftNative.chatgptCancel(id);
      pending.delete(id);
      reject(new DOMException("Cancelled", "AbortError"));
    };
    const cleanup = () => signal?.removeEventListener("abort", abort);
    pending.set(id, { resolve, reject, cleanup, addon: true });
    signal?.addEventListener("abort", abort, { once: true });
    try {
      window.DraftNative.chatgpt(action, id, JSON.stringify(payload));
    } catch (e) {
      pending.delete(id);
      cleanup();
      reject(e);
    }
  });
}
if (typeof window !== "undefined")
  window.NativeEvents = {
    imported(payload) {
      try {
        importHandler?.(
          typeof payload === "string" ? JSON.parse(payload) : payload,
        );
      } catch (e) {
        noticeHandler?.(`Import failed: ${e.message}`);
      }
    },
    response(id, status, body, error) {
      const p = pending.get(String(id));
      if (!p) return;
      pending.delete(String(id));
      p.cleanup();
      error
        ? p.reject(new Error(error))
        : p.resolve({ status: Number(status), body: String(body) });
    },
    notice(message) {
      noticeHandler?.(String(message));
    },
    keyboard(visible) {
      document.body.classList.toggle("keyboard-open", Boolean(visible));
      clearTimeout(window.NativeEvents.keyboardTimer);
      if (visible)
        window.NativeEvents.keyboardTimer = setTimeout(() => {
          const active = document.activeElement;
          if (active?.matches("textarea,input,select"))
            (active.closest(".editor-card") || active).scrollIntoView({
              block: "start",
              behavior: "instant",
            });
        }, 160);
    },
  };
if (typeof window !== "undefined")
  window.NativeEvents.addon = (id, result, error) => {
    const p = pending.get(String(id));
    if (!p) return;
    pending.delete(String(id));
    p.cleanup();
    if (error) p.reject(new Error(error));
    else
      try {
        p.resolve(typeof result === "string" ? JSON.parse(result) : result);
      } catch (e) {
        p.reject(e);
      }
  };
export function loadState() {
  const raw = native
    ? window.DraftNative.loadState()
    : localStorage.getItem("draft2done-state");
  if (!raw || raw === "null") return null;
  return JSON.parse(raw);
}
export function saveState(state) {
  const raw = JSON.stringify(state);
  if (native) {
    const result = window.DraftNative.saveState(raw);
    if (result && result !== "ok" && result !== "true") {
      try {
        const obj = JSON.parse(result);
        if (obj.error) throw new Error(obj.error);
      } catch (e) {
        if (result.startsWith("{")) throw e;
        else throw new Error(result);
      }
    }
  } else {
    const old = localStorage.getItem("draft2done-state");
    if (old) localStorage.setItem("draft2done-backup", old);
    localStorage.setItem("draft2done-state", raw);
  }
}
export async function transport({
  url,
  method = "GET",
  headers = {},
  body = "",
  signal,
}) {
  if (signal?.aborted) throw new DOMException("Cancelled", "AbortError");
  if (!native) {
    const result = await fetch(url, {
      method,
      headers,
      ...(method === "GET" ? {} : { body }),
      signal,
    });
    return { status: result.status, body: await result.text() };
  }
  const id = crypto.randomUUID();
  return new Promise((resolve, reject) => {
    const abort = () => {
      window.DraftNative.cancelRequest(id);
      pending.delete(id);
      reject(new DOMException("Cancelled", "AbortError"));
    };
    const cleanup = () => signal?.removeEventListener("abort", abort);
    pending.set(id, { resolve, reject, cleanup });
    signal?.addEventListener("abort", abort, { once: true });
    try {
      window.DraftNative.request(
        id,
        url,
        method,
        JSON.stringify(headers),
        body,
      );
    } catch (e) {
      pending.delete(id);
      cleanup();
      reject(e);
    }
  });
}
export function openExternal(url) {
  const parsed = new URL(url);
  if (!["https:", "http:"].includes(parsed.protocol))
    throw new Error("Only web links can be opened.");
  if (native) window.DraftNative.openExternal(parsed.href);
  else window.open(parsed.href, "_blank", "noopener,noreferrer");
}
export function bytesToBase64(bytes) {
  let value = "";
  for (let i = 0; i < bytes.length; i += 16384)
    value += String.fromCharCode(...bytes.subarray(i, i + 16384));
  return btoa(value);
}
export function base64ToBytes(value) {
  return Uint8Array.from(atob(value), (c) => c.charCodeAt(0));
}
export function exportFile(name, mime, bytes) {
  if (native)
    window.DraftNative.exportDocument(name, mime, bytesToBase64(bytes));
  else {
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([bytes], { type: mime }));
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  }
}
export function importFile() {
  if (native) window.DraftNative.importDocument();
  else {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".txt,.md,.docx,.pdf,.d2d,.json";
    input.onchange = async () => {
      const file = input.files[0];
      if (!file) return;
      importHandler?.({
        name: file.name,
        mime: file.type,
        base64: bytesToBase64(new Uint8Array(await file.arrayBuffer())),
      });
    };
    input.click();
  }
}
export function printHTML(html) {
  if (native) window.DraftNative.printDocument(html);
  else {
    const w = window.open("", "_blank");
    if (!w) throw new Error("Allow a popup to open the print preview.");
    w.document.write(html);
    w.document.close();
    w.print();
  }
}
