package io.draft2done.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;

/** Exercises production update validators using the phone's actual installed APK.
 * Never launches an installer, changes app state, or contacts a release server. */
public final class UpdateValidationInstrumentation extends Instrumentation {
    private final JSONArray checks = new JSONArray();
    private Class<?> validation;
    private interface Checked { void run() throws Exception; }

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle(); int code = Activity.RESULT_OK;
        JSONObject report = new JSONObject();
        try {
            validation = Class.forName("io.draft2done.app.UpdateValidation", true, getTargetContext().getClassLoader());
            verifyInstalledApk(); verifyCancellation(); verifyProvider();
            report.put("passed", true).put("checks", checks);
            result.putString("stream", "Update device checks passed: " + checks.length() + "\n");
        } catch (Throwable error) {
            code = Activity.RESULT_CANCELED;
            try { report.put("passed", false).put("checks", checks).put("error", error.toString()); }
            catch (Exception ignored) {}
            result.putString("stream", "Update verification failed: " + error + "\n");
        }
        try {
            File directory = new File(getTargetContext().getExternalFilesDir(null), "qa");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("QA report storage unavailable.");
            try (OutputStream out = new FileOutputStream(new File(directory, "update-validation.json"))) {
                out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception error) { result.putString("reportError", error.toString()); code = Activity.RESULT_CANCELED; }
        finish(code, result);
    }

    private void passed(String label) { checks.put(label); }
    private void expect(boolean value, String label) { if (!value) throw new AssertionError(label); passed(label); }
    private void rejects(Checked action, String label) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (Exception expected) { rejected = true; }
        expect(rejected, label);
    }
    private Object call(String name, Class<?>[] types, Object... values) throws Exception {
        Method method = validation.getDeclaredMethod(name, types); method.setAccessible(true);
        try { return method.invoke(null, values); }
        catch (InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof Exception) throw (Exception)cause;
            throw wrapper;
        }
    }
    private byte[][] signers(PackageInfo info) {
        Signature[] values = Build.VERSION.SDK_INT >= 28 ? info.signingInfo.getApkContentsSigners() : info.signatures;
        byte[][] result = new byte[values.length][];
        for (int i = 0; i < values.length; i++) result[i] = values[i].toByteArray();
        return result;
    }
    private long version(PackageInfo info) { return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode; }
    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
    private byte[] digest(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[32768]; int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return digest.digest();
    }

    private void verifyInstalledApk() throws Exception {
        Context context = getTargetContext(); PackageManager manager = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo installed = manager.getPackageInfo(context.getPackageName(), flags);
        File apk = new File(installed.applicationInfo.sourceDir);
        PackageInfo archive = manager.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        expect(archive != null && archive.packageName.equals(context.getPackageName()), "Android parses and verifies installed APK signing information");
        byte[] hash = digest(apk);
        call("checksum", new Class<?>[]{byte[].class, String.class}, hash, hex(hash));
        passed("Actual installed APK checksum accepted");
        byte[] changed = hash.clone(); changed[0] ^= 1;
        rejects(() -> call("checksum", new Class<?>[]{byte[].class, String.class}, changed, hex(hash)), "Changed APK checksum blocked");
        Class<?>[] signature = {String.class, long.class, long.class, long.class, byte[][].class, byte[][].class};
        byte[][] current = signers(installed), next = signers(archive);
        long code = version(archive);
        // Simulate an older installed version while retaining Android's actual package,
        // candidate version, and verified signing certificates from the real APK.
        call("candidate", signature, archive.packageName, code - 1, code, code, current, next);
        passed("Actual APK package, version and certificate accepted for older installed version");
        rejects(() -> call("candidate", signature, "io.attacker.app", code - 1, code, code, current, next), "Wrong APK package blocked");
        rejects(() -> call("candidate", signature, archive.packageName, code, code, code, current, next), "Same-version reinstall blocked");
        rejects(() -> call("candidate", signature, archive.packageName, code + 1, code, code, current, next), "APK downgrade blocked");
        rejects(() -> call("candidate", signature, archive.packageName, code - 1, code, code + 1, current, next), "Tampered published version blocked");
        byte[][] wrongSigner = {next[0].clone()}; wrongSigner[0][0] ^= 1;
        rejects(() -> call("candidate", signature, archive.packageName, code - 1, code, code, current, wrongSigner), "Different signing certificate blocked");
        String hashValue = hex(hash);
        JSONObject metadata = new JSONObject().put("version", installed.versionName).put("versionCode", code)
                .put("package", context.getPackageName()).put("sha256", hashValue)
                .put("apkUrl", "https://github.com/youriptvus/Draft2Done/releases/download/v"+installed.versionName+"/Draft2Done-"+installed.versionName+".apk");
        call("manifest", new Class<?>[]{JSONObject.class}, metadata); passed("Device Android JSON implementation accepts release metadata");
        rejects(() -> call("manifest", new Class<?>[]{JSONObject.class}, new JSONObject(metadata.toString()).put("versionCode", code + 0.5)), "Fractional manifest version blocked on Android");
        rejects(() -> call("downloadUri", new Class<?>[]{String.class}, "https://github.com.evil.example/update.apk"), "Untrusted redirect host blocked on Android");
        rejects(() -> call("releaseAsset", new Class<?>[]{String.class, boolean.class}, "https://github.com/other/Draft2Done/releases/download/v1/app.apk", true), "Other repository asset blocked on Android");
        File corrupted = File.createTempFile("draft2done-qa-corrupt-", ".apk", context.getCacheDir());
        try {
            try (InputStream in = new FileInputStream(apk); OutputStream out = new FileOutputStream(corrupted)) {
                byte[] buffer = new byte[32768]; int count; long offset = 0;
                while ((count = in.read(buffer)) != -1) {
                    if (offset == 0 && count > 2048) buffer[2048] ^= 1;
                    out.write(buffer, 0, count); offset += count;
                }
            }
            PackageInfo broken = manager.getPackageArchiveInfo(corrupted.getAbsolutePath(), flags);
            expect(broken == null || broken.signingInfo == null, "Android rejects archive with tampered signed bytes");
        } finally { if (!corrupted.delete()) throw new IOException("Could not remove QA APK fixture."); }
    }

    private void verifyCancellation() throws Exception {
        Class<?> updater = Class.forName("io.draft2done.app.AppUpdates", true, getTargetContext().getClassLoader());
        Constructor<?> constructor = updater.getDeclaredConstructor(Activity.class); constructor.setAccessible(true);
        Object instance = constructor.newInstance(new Object[]{null});
        Method enqueue = updater.getDeclaredMethod("enqueue", String.class), cancel = updater.getDeclaredMethod("cancel", String.class),
                handle = updater.getDeclaredMethod("handle", String.class, String.class), destroy = updater.getDeclaredMethod("destroy");
        enqueue.setAccessible(true); cancel.setAccessible(true); handle.setAccessible(true); destroy.setAccessible(true);
        Field requests = updater.getDeclaredField("requests"); requests.setAccessible(true);
        enqueue.invoke(instance, "queued-qa"); cancel.invoke(instance, "queued-qa");
        boolean cancelled = false;
        try { handle.invoke(instance, "update-check", "queued-qa"); }
        catch (InvocationTargetException error) { cancelled = error.getCause() instanceof InterruptedIOException; }
        expect(cancelled, "Cancelled queued update does not start networking");
        expect(((Map<?,?>)requests.get(instance)).isEmpty(), "Completed cancelled request is removed");
        for (int i = 0; i < 50; i++) cancel.invoke(instance, "unrelated-chatgpt-" + i);
        expect(((Map<?,?>)requests.get(instance)).isEmpty(), "Unrelated cancellation IDs do not accumulate");
        destroy.invoke(instance);
        rejects(() -> enqueue.invoke(instance, "after-destroy"), "Destroyed updater rejects new work");
    }

    private void verifyProvider() throws Exception {
        Context context = getTargetContext();
        byte[] bytes = "Draft2Done private updater provider QA fixture".getBytes(StandardCharsets.UTF_8);
        String name = "release-" + hex(MessageDigest.getInstance("SHA-256").digest(bytes)) + ".apk";
        File directory = new File(context.getCacheDir(), "updates");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("QA update storage unavailable.");
        File fixture = new File(directory, name);
        if (!fixture.createNewFile()) throw new IOException("QA fixture already exists; refusing to replace it.");
        String prefix = "content://" + context.getPackageName() + ".updates/";
        Uri uri = Uri.parse(prefix + name);
        try {
            try (OutputStream out = new FileOutputStream(fixture)) { out.write(bytes); }
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream copy = new ByteArrayOutputStream(); byte[] buffer = new byte[128]; int count;
                while ((count = in.read(buffer)) != -1) copy.write(buffer, 0, count);
                expect(java.util.Arrays.equals(copy.toByteArray(), bytes), "Private provider serves exact hash-named file bytes");
            }
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                expect(cursor != null && cursor.moveToFirst() && cursor.getLong(cursor.getColumnIndexOrThrow("_size")) == bytes.length, "Installer provider reports expected file size");
            }
            rejects(() -> { try (OutputStream ignored = context.getContentResolver().openOutputStream(uri)) {} }, "Installer provider denies write access");
            rejects(() -> { try (InputStream ignored = context.getContentResolver().openInputStream(Uri.parse(prefix + "../workspace.json"))) {} }, "Installer provider blocks traversal");
            rejects(() -> { try (InputStream ignored = context.getContentResolver().openInputStream(Uri.parse(uri + "?other=file"))) {} }, "Installer provider blocks query aliases");
            rejects(() -> { try (InputStream ignored = context.getContentResolver().openInputStream(Uri.parse(uri + "#fragment"))) {} }, "Installer provider blocks fragment aliases");
        } finally { if (!fixture.delete()) throw new IOException("Could not remove QA provider fixture."); }
    }
}
