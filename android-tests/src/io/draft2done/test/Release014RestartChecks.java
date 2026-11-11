package io.draft2done.test;

import android.app.Activity;
import android.util.AtomicFile;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/** Two separate harness invocations with a root-owned force-stop between them. */
final class Release014RestartChecks {
    private static final String STAGING = "draft2done-paste-draft";
    private static final String METADATA = "qa-release014-restart.json";
    private static final int PRIVATE_LIMIT = 32 * 1024 * 1024;
    private Release014RestartChecks() {}

    static void start(Activity activity, NativeDocumentWorkflowChecks.Evaluator js,
                      NativeDocumentWorkflowChecks.Clicker click,
                      NativeDocumentWorkflowChecks.Checker check) throws Exception {
        File metadataFile = new File(activity.getCacheDir(), METADATA);
        if (metadataFile.exists())
            throw new IOException("A private restart checkpoint needs verification or recovery before another run.");
        String prefix = "Draft2Done-QA-" + UUID.randomUUID().toString().replace("-", "");
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved =
            new NativeDocumentWorkflowChecks.WorkspaceSnapshot(activity, js, prefix);
        JSONObject record = new JSONObject().put("prefix", prefix).put("phase", "capturing");
        boolean mutationStarted = false;
        boolean awaitingRestart = false;
        try {
            String raw = decode(js.evaluate("localStorage.getItem(" + JSONObject.quote(STAGING) + ")"));
            String nativeRaw = loadNativeStage(js);
            // Invalid native data must remain untouched rather than being replaced
            // by a test fixture. This error contains no saved input or raw response.
            if (nativeRaw != null && (parseStage(nativeRaw) == null || !(parseStage(nativeRaw).opt("text") instanceof String)))
                throw new IOException("The native pending input is unreadable; restart QA left it untouched.");
            record.put("stagingPresent", raw != null)
                .put("stagingRaw", raw == null ? JSONObject.NULL : raw)
                .put("nativeStagingPresent", nativeRaw != null)
                .put("nativeStagingRaw", nativeRaw == null ? JSONObject.NULL : nativeRaw)
                .put("stagingText", decode(js.evaluate("document.querySelector('#paste-draft')?.value||''")))
                .put("accountFingerprint", accountFingerprint(js))
                .put("projectsFingerprint", hash(js.evaluate("JSON.stringify(window.Draft2Done.getState().projects)")))
                .put("profilesFingerprint", hash(js.evaluate("JSON.stringify(window.Draft2Done.getState().profiles)")))
                .put("notesFingerprint", hash(js.evaluate("JSON.stringify(window.Draft2Done.getState().notes)")))
                .put("phase", "prepared");
            write(metadataFile, record);
            // No mutation happens until both independent private recovery records exist.
            mutationStarted = true;
            js.evaluate("const s=window.Draft2Done.getState();s.settings.autoUpdates=false;s.settings.autosave=true;s.settings.defaultMode='draft';return true;");
            click.click("[data-action=page][data-id=home]");
            String draft = draft(prefix);
            setPaste(js, draft, 2, 12);
            click.click("[data-action=paste-use]");
            waitFor(js, "window.Draft2Done.info().page==='write'&&!!document.querySelector('#draft-editor')", 10000);
            String projectId = decode(js.evaluate("window.Draft2Done.getState().activeId"));
            if (projectId == null || !projectId.matches("[A-Za-z0-9-]{1,100}"))
                throw new IOException("The fictional restart project has no usable identifier.");
            record.put("projectId", projectId).put("draft", draft);
            write(metadataFile, record);
            click.click(".paper-title");
            js.evaluate("document.querySelector('#rename-input').value=" + JSONObject.quote(prefix) + ";return true;");
            click.click("[data-action=rename-save]");
            click.click("[data-action=tab][data-id=draft]");
            click.click("[data-action=save]");
            js.evaluate("window.NativeEvents.pause();return true;");
            String project = decode(js.evaluate("const s=window.Draft2Done.getState();return JSON.stringify(s.projects.find(p=>p.id===s.activeId));"));
            record.put("projectRecord", new JSONObject(project));
            check.check("true".equals(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p.title===" + JSONObject.quote(prefix) + "&&p.originalText===" + JSONObject.quote(draft) + "&&p.workingText===" + JSONObject.quote(draft) + "&&p.revisions.length>0;")),
                "Restart setup saves a separate fictional Unicode paper, protected original and revision checkpoint");
            click.click("[data-action=page][data-id=home]");
            String pending = pending(prefix);
            setPaste(js, pending, 3, 17);
            js.evaluate("window.NativeEvents.pause();return true;");
            record.put("pendingText", pending)
                .put("pendingRaw", decode(js.evaluate("localStorage.getItem(" + JSONObject.quote(STAGING) + ")")))
                .put("pendingNativeRaw", loadNativeStage(js))
                .put("phase", "awaiting-restart");
            write(metadataFile, record);
            check.check("true".equals(js.evaluate("return document.querySelector('#paste-draft').value===" + JSONObject.quote(pending) + "&&JSON.parse(localStorage.getItem('draft2done-paste-draft')).text===" + JSONObject.quote(pending) + ";")),
                "A second unsubmitted paste with leading blank lines is durably staged before process termination");
            check.check(record.has("pendingNativeRaw") && record.getString("pendingRaw").equals(record.getString("pendingNativeRaw")),
                "Normal paste input synchronously commits the exact native atomic record and matching browser mirror before termination");
            check.check(record.getString("accountFingerprint").equals(accountFingerprint(js)),
                "Restart setup preserves the connected account without an AI or research request");
            awaitingRestart = true;
        } finally {
            if (!awaitingRestart) {
                if (mutationStarted) restore(activity, js, click, check, saved, record);
                else {
                    saved.clear(activity);
                    new AtomicFile(metadataFile).delete();
                }
            }
        }
    }

    static void verify(Activity activity, NativeDocumentWorkflowChecks.Evaluator js,
                       NativeDocumentWorkflowChecks.Clicker click,
                       NativeDocumentWorkflowChecks.Checker check) throws Exception {
        JSONObject record = readMetadata(activity);
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved = boundCheckpoint(activity, record);
        try {
            if (!"awaiting-restart".equals(record.optString("phase")))
                throw new IOException("The restart setup did not finish; restoring its preserved workspace and staged input.");
            String projectId = record.getString("projectId");
            if (!projectId.matches("[A-Za-z0-9-]{1,100}"))
                throw new IOException("Refused an unknown restart project identifier.");
            String actualRaw = loadNativeStage(js);
            String expectedRaw = record.getString("pendingNativeRaw");
            JSONObject actualStage = parseStage(actualRaw), expectedStage = parseStage(expectedRaw);
            boolean textEqual = actualStage != null && actualStage.opt("text") instanceof String &&
                record.getString("pendingText").equals(actualStage.getString("text"));
            boolean validSelection = false;
            if (actualStage != null && actualStage.opt("text") instanceof String &&
                actualStage.opt("start") instanceof Number && actualStage.opt("end") instanceof Number) {
                double a = actualStage.getDouble("start"), b = actualStage.getDouble("end");
                validSelection = !Double.isNaN(a) && !Double.isInfinite(a) && !Double.isNaN(b) && !Double.isInfinite(b) &&
                    a == Math.floor(a) && b == Math.floor(b) && a >= 0 && b >= a && b <= actualStage.getString("text").length();
            }
            JSONObject coldStructure = new JSONObject().put("present", actualRaw != null)
                .put("rawEqual", expectedRaw.equals(actualRaw)).put("expectedTextEqual", textEqual)
                .put("startEqual", sameNumber(expectedStage, actualStage, "start"))
                .put("endEqual", sameNumber(expectedStage, actualStage, "end"))
                .put("scrollEqual", sameNumber(expectedStage, actualStage, "scroll"))
                .put("validSelection", validSelection);
            check.check(true, "Cold native pending-paste structure (booleans only): " + coldStructure.toString());
            check.check(expectedRaw.equals(actualRaw),
                "Cold process startup restores the exact native atomic pending-paste record");
            check.check(record.getString("pendingRaw").equals(decode(js.evaluate("localStorage.getItem(" + JSONObject.quote(STAGING) + ")"))),
                "Cold process startup reconstructs the exact pending-paste browser mirror from native durable input");
            check.check("true".equals(js.evaluate("return document.querySelector('#paste-draft').value===" + JSONObject.quote(record.getString("pendingText")) + ";")),
                "Cold process startup displays the pending paste with exact Unicode, whitespace and leading blank lines");
            JSONObject expected = record.getJSONObject("projectRecord");
            check.check("true".equals(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===" + JSONObject.quote(projectId) + ");const e=" + expected.toString() + ";return !!p&&s.activeId===p.id&&p.title===e.title&&p.originalText===e.originalText&&p.workingText===e.workingText&&JSON.stringify(p.revisions)===JSON.stringify(e.revisions)&&p.revisions.length>0;")),
                "Cold process startup retains the saved fictional paper, active selection, exact original, working text and revisions");
            check.check(record.getString("projectsFingerprint").equals(hash(js.evaluate("const s=window.Draft2Done.getState();return JSON.stringify(s.projects.filter(p=>p.id!==" + JSONObject.quote(projectId) + "));"))) &&
                        record.getString("profilesFingerprint").equals(hash(js.evaluate("JSON.stringify(window.Draft2Done.getState().profiles)"))) &&
                        record.getString("notesFingerprint").equals(hash(js.evaluate("JSON.stringify(window.Draft2Done.getState().notes)"))),
                "Cold process startup preserves every pre-existing paper, saved voice and note");
            check.check(record.getString("accountFingerprint").equals(accountFingerprint(js)),
                "Cold process startup retains the original connected-account identity without exposing credentials");
            click.click("[data-action=open][data-id=\"" + projectId + "\"]");
            click.click("[data-action=tab][data-id=draft]");
            check.check("true".equals(js.evaluate("return document.querySelector('#draft-editor').value===" + JSONObject.quote(record.getString("draft")) + ";")),
                "Reopening the saved paper restores its exact editable text after real process termination");
        } finally {
            restore(activity, js, click, check, saved, record);
        }
    }

    /** Recover only this class's bound durable checkpoint after an interrupted test. */
    static void recover(Activity activity, NativeDocumentWorkflowChecks.Evaluator js,
                        NativeDocumentWorkflowChecks.Clicker click,
                        NativeDocumentWorkflowChecks.Checker check) throws Exception {
        JSONObject record = readMetadata(activity);
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved = boundCheckpoint(activity, record);
        restore(activity, js, click, check, saved, record);
    }

    private static void restore(Activity activity, NativeDocumentWorkflowChecks.Evaluator js,
                                NativeDocumentWorkflowChecks.Clicker click,
                                NativeDocumentWorkflowChecks.Checker check,
                                NativeDocumentWorkflowChecks.WorkspaceSnapshot saved,
                                JSONObject record) throws Exception {
        // Refuse to clear or restore a checkpoint created by another QA workflow.
        boundCheckpoint(activity, record);
        saved.restore(activity, js, click);
        boolean present = record.getBoolean("stagingPresent");
        String original = present ? record.getString("stagingRaw") : null;
        boolean nativePresent = record.getBoolean("nativeStagingPresent");
        String nativeOriginal = nativePresent ? record.getString("nativeStagingRaw") : null;
        String authoritative = nativePresent ? nativeOriginal : original;
        // Update this retiring page's module memory as well as the raw record.
        // A late flush then sees the user's input rather than the QA fixture.
        js.evaluate("window.NativeEvents.pause=()=>{};window.addEventListener('pagehide',e=>e.stopImmediatePropagation(),true);document.addEventListener('visibilitychange',e=>e.stopImmediatePropagation(),true);" +
            "const e=document.querySelector('#paste-draft');if(e){e.value=" + JSONObject.quote(record.getString("stagingText")) + ";let p={};try{p=JSON.parse(" + JSONObject.quote(authoritative == null ? "null" : authoritative) + ")||{};}catch(_){}const a=Number.isInteger(p.start)?Math.max(0,Math.min(e.value.length,p.start)):0;const b=Number.isInteger(p.end)?Math.max(a,Math.min(e.value.length,p.end)):a;e.setSelectionRange(a,b);e.scrollTop=Number.isFinite(p.scroll)?Math.max(0,p.scroll):0;e.dispatchEvent(new Event('input',{bubbles:true}));}" +
            "const status=window.DraftNative.saveDraftInput(" + JSONObject.quote(nativePresent ? nativeOriginal : "") + ");if(status!=='ok')throw Error('Native pending input restoration failed');" +
            (present ? "localStorage.setItem('draft2done-paste-draft'," + JSONObject.quote(original) + ");" : "localStorage.removeItem('draft2done-paste-draft');") +
            "setTimeout(()=>location.reload(),80);return true;");
        Thread.sleep(1000);
        waitFor(js, "!!window.Draft2Done&&!!document.querySelector('#paste-draft')", 20000);
        check.check(saved.matchesDisk(activity),
            "Restart QA restores the exact original workspace and previous-save backup bytes");
        check.check(saved.matchesState(js),
            "Restart QA restores all original projects, preferences, saved voices, notes and active paper");
        String nativeRestored = loadNativeStage(js);
        check.check(nativePresent ? nativeOriginal.equals(nativeRestored) : nativeRestored == null,
            "Restart QA restores the user's exact prior native atomic pending input, including absence");
        String restored = decode(js.evaluate("localStorage.getItem(" + JSONObject.quote(STAGING) + ")"));
        boolean stageTextEqual = "true".equals(js.evaluate("return document.querySelector('#paste-draft').value===" + JSONObject.quote(record.getString("stagingText")) + ";"));
        JSONObject diagnostic = new JSONObject().put("expectedPresent", present).put("actualPresent", restored != null)
            .put("hashEqual", present ? restored != null && hash(original).equals(hash(restored)) : restored == null)
            .put("lenEqual", present ? restored != null && original.length() == restored.length() : restored == null)
            .put("stageTextEqual", stageTextEqual);
        check.check(true, "Private staging restoration structure (booleans only): " + diagnostic.toString());
        check.check(present ? original.equals(restored) : restored == null,
            "Restart QA restores the user's exact prior raw pending-paste record, including absence");
        check.check(stageTextEqual,
            "Reloaded normal application memory displays the user's original staged text without the fictional paste");
        check.check(record.getString("accountFingerprint").equals(accountFingerprint(js)),
            "Restart QA preserves the original connected account without authentication changes or inference");
        // Both files are private and bound to the exact unique prefix checked above.
        saved.clear(activity);
        new AtomicFile(new File(activity.getCacheDir(), METADATA)).delete();
    }

    private static NativeDocumentWorkflowChecks.WorkspaceSnapshot boundCheckpoint(Activity activity, JSONObject record) throws Exception {
        String prefix = record.getString("prefix");
        if (!prefix.matches("Draft2Done-QA-[a-f0-9]{32}"))
            throw new IOException("Refused an unknown private restart prefix.");
        File pending = new File(new File(activity.getCacheDir(), "qa-document-checkpoint"), "pending.json");
        JSONObject binding = new JSONObject(new String(read(pending, 8192), StandardCharsets.UTF_8));
        if (!prefix.equals(binding.getString("prefix")))
            throw new IOException("The restart metadata does not own the pending workspace checkpoint; files left untouched.");
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved = NativeDocumentWorkflowChecks.WorkspaceSnapshot.pending(activity);
        if (saved == null) throw new IOException("The preserved restart workspace checkpoint is unavailable.");
        return saved;
    }

    private static JSONObject readMetadata(Activity activity) throws Exception {
        return new JSONObject(new String(read(new File(activity.getCacheDir(), METADATA), PRIVATE_LIMIT), StandardCharsets.UTF_8));
    }

    private static JSONObject parseStage(String raw) {
        if (raw == null) return null;
        try { return new JSONObject(raw); } catch (Exception ignored) { return null; }
    }

    private static String loadNativeStage(NativeDocumentWorkflowChecks.Evaluator js) throws Exception {
        if (!"true".equals(js.evaluate("typeof window.DraftNative.loadDraftInput==='function'&&typeof window.DraftNative.saveDraftInput==='function'")))
            throw new IOException("This restart test requires the native durable-input bridge.");
        String raw = decode(js.evaluate("window.DraftNative.loadDraftInput()"));
        return raw == null || "null".equals(raw) ? null : raw;
    }

    private static boolean sameNumber(JSONObject expected, JSONObject actual, String key) {
        if (expected == null || actual == null || !(expected.opt(key) instanceof Number) || !(actual.opt(key) instanceof Number)) return false;
        double a = expected.optDouble(key, Double.NaN), b = actual.optDouble(key, Double.NaN);
        return !Double.isNaN(a) && !Double.isNaN(b) && !Double.isInfinite(a) && !Double.isInfinite(b) && a == b;
    }

    private static void setPaste(NativeDocumentWorkflowChecks.Evaluator js, String text, int start, int end) throws Exception {
        js.evaluate("const e=document.querySelector('#paste-draft');e.value=" + JSONObject.quote(text) + ";e.setSelectionRange(" + start + "," + end + ");e.dispatchEvent(new Event('input',{bubbles:true}));return true;");
    }

    private static String accountFingerprint(NativeDocumentWorkflowChecks.Evaluator js) throws Exception {
        return hash(js.evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};"));
    }

    private static String draft(String prefix) {
        return "\n\nPhotosynthesis is the subject of this fictional restart test. My existing classroom draft considers how a plant uses light while distinguishing an explanation from an experimental finding. This paragraph makes no claim about actual research and contains no personal information. I want to preserve my wording, spelling, punctuation and paragraph breaks while I develop the argument.\n\nThe French Revolution is a separate educational example. Fictional Unicode stays exact: caf\u00e9, \u03b1, \ud83e\uddec and an em dash\u2014with the original leading blank lines. " + prefix + ".\n";
    }

    private static String pending(String prefix) {
        return "\n\nThis is a second, unsubmitted fictional paste. It must survive actual process termination without becoming a new paper or replacing the saved draft. Unicode and paragraph breaks stay exact: caf\u00e9, \u03b2, \ud83d\udcda.\n\n" + prefix + " pending input.\n";
    }

    private static String hash(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }

    private static void write(File file, JSONObject value) throws Exception {
        byte[] content = value.toString().getBytes(StandardCharsets.UTF_8);
        if (content.length > PRIVATE_LIMIT) throw new IOException("Private restart recovery metadata exceeds its bound; original input left intact.");
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream output = null;
        try {
            output = atomic.startWrite();
            output.write(content);
            atomic.finishWrite(output);
        } catch (Exception error) {
            if (output != null) atomic.failWrite(output);
            throw error;
        }
    }

    private static byte[] read(File file, int maximum) throws Exception {
        try (InputStream input = new AtomicFile(file).openRead()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > maximum) throw new IOException("Private restart checkpoint is larger than its allowed bound.");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static void waitFor(NativeDocumentWorkflowChecks.Evaluator js, String expression, int timeout) throws Exception {
        long until = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < until) {
            if ("true".equals(js.evaluate(expression))) return;
            Thread.sleep(150);
        }
        throw new IOException("Timed out waiting for the restart UI; private recovery checkpoint retained.");
    }

    private static String decode(String json) throws Exception {
        Object value = new JSONTokener(json).nextValue();
        return value == JSONObject.NULL ? null : String.valueOf(value);
    }
}
