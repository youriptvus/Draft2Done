package io.draft2done.app;

import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Atomic workspace and previous-valid-save recovery, independent of the activity. */
final class WorkspaceStore {
    private final AtomicFile primary;
    private final AtomicFile recovery;
    private final int limit;

    WorkspaceStore(File directory, int limit) {
        primary = new AtomicFile(new File(directory, "workspace.json"));
        recovery = new AtomicFile(new File(directory, "workspace.backup.json"));
        this.limit = limit;
    }

    static final class Loaded {
        final String json;
        final boolean recovered;
        final boolean unreadable;
        Loaded(String json, boolean recovered, boolean unreadable) {
            this.json = json; this.recovered = recovered; this.unreadable = unreadable;
        }
    }

    synchronized Loaded load() {
        boolean hadSavedData = exists(primary) || exists(recovery);
        try { return new Loaded(readValid(primary), false, false); }
        catch (Exception primaryError) {
            try { return new Loaded(readValid(recovery), true, false); }
            catch (Exception recoveryError) {
                return new Loaded(hadSavedData ? null : "{}", false, hadSavedData);
            }
        }
    }

    private static boolean exists(AtomicFile file) {
        File base = file.getBaseFile();
        return base.exists() || new File(base.getPath() + ".bak").exists();
    }

    synchronized void save(String json) throws Exception {
        if (json == null || json.length() > limit)
            throw new IllegalArgumentException("Workspace is too large. Export completed work and remove unused projects.");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > limit) throw new IllegalArgumentException("Workspace exceeds the 4 MB storage limit.");
        new JSONObject(json);

        String previous = null;
        try { previous = readValid(primary); }
        catch (Exception ignored) {
            // A corrupt or missing primary must never overwrite a valid recovery copy.
        }
        if (previous != null) write(recovery, previous.getBytes(StandardCharsets.UTF_8));
        write(primary, bytes);
    }

    private String readValid(AtomicFile file) throws Exception {
        byte[] bytes;
        try (InputStream input = file.openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (out.size() + count > limit) throw new IllegalArgumentException("Saved workspace exceeds the storage limit.");
                out.write(buffer, 0, count);
            }
            bytes = out.toByteArray();
        }
        String json = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString();
        new JSONObject(json);
        return json;
    }

    private static void write(AtomicFile file, byte[] bytes) throws Exception {
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(bytes);
            file.finishWrite(output);
        } catch (Exception error) {
            if (output != null) file.failWrite(output);
            throw error;
        }
    }
}
