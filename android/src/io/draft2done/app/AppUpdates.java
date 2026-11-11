package io.draft2done.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Public releases only; Android's package installer controls final update approval. */
final class AppUpdates {
    static final String REPO = UpdateValidation.REPOSITORY;
    static final String API = "https://api.github.com/repos/youriptvus/Draft2Done/releases/latest";
    private static final int APK_LIMIT = 80 * 1024 * 1024;
    private final Activity activity;
    private final Map<String, Request> requests = new ConcurrentHashMap<>();
    private final AtomicBoolean installing = new AtomicBoolean();
    private volatile boolean destroyed;

    private static final class Request {
        volatile boolean cancelled;
        HttpURLConnection connection;
    }

    AppUpdates(Activity activity) { this.activity = activity; }

    /** Called before the worker is queued, so a quick Cancel cannot revive the task. */
    void enqueue(String id) {
        if (destroyed) throw new IllegalStateException("Draft2Done is closing.");
        if (requests.putIfAbsent(id, new Request()) != null) {
            throw new IllegalArgumentException("This update request is already running.");
        }
    }

    void cancel(String id) {
        Request request = requests.get(id);
        if (request == null) return;
        synchronized (request) {
            request.cancelled = true;
            if (request.connection != null) request.connection.disconnect();
        }
    }

    void destroy() {
        destroyed = true;
        for (String id : requests.keySet()) cancel(id);
    }

    JSONObject handle(String action, String id) throws Exception {
        Request request = requests.get(id);
        if (request == null) { enqueue(id); request = requests.get(id); }
        try {
            checkCancelled(request);
            if ("update-check".equals(action)) return check(id);
            if ("update-install".equals(action)) return install(id);
            throw new IllegalArgumentException("Unknown update action.");
        } finally {
            synchronized (request) {
                if (request.connection != null) request.connection.disconnect();
            }
            requests.remove(id, request);
        }
    }

    private void checkCancelled(Request request) throws InterruptedIOException {
        if (destroyed || request == null || request.cancelled || Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Update cancelled.");
        }
    }

    private long version(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private JSONObject check(String id) throws Exception {
        HttpURLConnection connection = connection(id, API);
        JSONObject release;
        try {
            int status = connection.getResponseCode();
            if (status == 404) return new JSONObject().put("available", false)
                    .put("message", "No public Draft2Done release is available yet.").put("repository", REPO);
            if (status != 200) throw new IOException(status == 403
                    ? "GitHub update checks are temporarily limited. Try again later."
                    : "GitHub update check returned HTTP " + status + ".");
            try (InputStream in = connection.getInputStream()) {
                release = new JSONObject(new String(read(id, in, 1024 * 1024), StandardCharsets.UTF_8));
            }
        } finally { disconnect(id, connection); }
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
            return new JSONObject().put("available", false).put("message", "No stable update is published.");
        }
        JSONArray assets = release.optJSONArray("assets");
        String manifestURL = "";
        if (assets != null) for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if ("latest.json".equals(asset.optString("name"))) manifestURL = asset.optString("browser_download_url");
        }
        if (manifestURL.isEmpty()) return new JSONObject().put("available", false)
                .put("message", "The latest release has no update manifest. Check the release page.");
        UpdateValidation.releaseAsset(manifestURL, false);
        JSONObject manifest = UpdateValidation.manifest(new JSONObject(new String(
                download(id, manifestURL, 65536), StandardCharsets.UTF_8)));
        PackageInfo installed = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
        boolean available = manifest.getLong("versionCode") > version(installed);
        return manifest.put("available", available).put("repository", REPO)
                .put("message", available ? "A new Draft2Done version is ready." : "Draft2Done is up to date.");
    }

    private JSONObject install(String id) throws Exception {
        if (!installing.compareAndSet(false, true)) throw new IOException("An update installation is already in progress.");
        File temporary = null;
        try {
            JSONObject update = check(id);
            if (!update.optBoolean("available")) return update;
            File directory = new File(activity.getCacheDir(), "updates");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Update storage is unavailable.");
            temporary = File.createTempFile("release-", ".download", directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            HttpURLConnection connection = redirected(id, update.getString("apkUrl"));
            try {
                if (connection.getResponseCode() != 200) throw new IOException("Update download failed: HTTP " + connection.getResponseCode());
                if (connection.getContentLengthLong() > APK_LIMIT) throw new IOException("Update exceeds the 80 MB safety limit.");
                try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[32768]; long total = 0; int count;
                    while ((count = in.read(buffer)) != -1) {
                        checkCancelled(requests.get(id));
                        total += count;
                        if (total > APK_LIMIT) throw new IOException("Update exceeds the 80 MB safety limit.");
                        digest.update(buffer, 0, count); out.write(buffer, 0, count);
                    }
                }
            } finally { disconnect(id, connection); }
            checkCancelled(requests.get(id));
            UpdateValidation.checksum(digest.digest(), update.getString("sha256"));
            PackageManager manager = activity.getPackageManager();
            int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo current = manager.getPackageInfo(activity.getPackageName(), flags);
            PackageInfo candidate = manager.getPackageArchiveInfo(temporary.getAbsolutePath(), flags);
            if (candidate == null) throw new SecurityException("Downloaded APK could not be verified.");
            UpdateValidation.candidate(candidate.packageName, version(current), version(candidate),
                    update.getLong("versionCode"), signers(current), signers(candidate));
            // Each verified hash has its own URI. A later download cannot replace the file
            // that Android's installer is still reading from an earlier request.
            File apk = new File(directory, "release-" + update.getString("sha256") + ".apk");
            if (apk.exists()) {
                try (InputStream in = new FileInputStream(apk)) {
                    MessageDigest existing = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[32768]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        checkCancelled(requests.get(id)); existing.update(buffer, 0, count);
                    }
                    UpdateValidation.checksum(existing.digest(), update.getString("sha256"));
                }
            } else if (!temporary.renameTo(apk)) throw new IOException("Could not prepare update for installation.");
            checkCancelled(requests.get(id));
            if (Build.VERSION.SDK_INT >= 26 && !manager.canRequestPackageInstalls()) {
                launch(id, new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
                return update.put("message", "Allow Draft2Done to install updates, then return and tap Install update again.");
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("content://" + activity.getPackageName() + ".updates/" + apk.getName()),
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            launch(id, intent);
            return update.put("message", "Verified update is ready. Confirm installation in Android.");
        } finally {
            if (temporary != null && temporary.exists()) temporary.delete();
            installing.set(false);
        }
    }

    private byte[][] signers(PackageInfo info) {
        Signature[] signatures = Build.VERSION.SDK_INT >= 28
                ? (info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners()) : info.signatures;
        if (signatures == null) return null;
        byte[][] encoded = new byte[signatures.length][];
        for (int i = 0; i < signatures.length; i++) encoded[i] = signatures[i].toByteArray();
        return encoded;
    }

    private void launch(String id, Intent intent) throws Exception {
        Request request = requests.get(id);
        checkCancelled(request);
        CountDownLatch launched = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        activity.runOnUiThread(() -> {
            try {
                synchronized (request) {
                    checkCancelled(request);
                    if (activity.isFinishing() || activity.isDestroyed()) throw new IOException("Draft2Done is closing.");
                    activity.startActivity(intent);
                }
            } catch (Exception failure) { error.set(failure); }
            finally { launched.countDown(); }
        });
        try {
            if (!launched.await(10, TimeUnit.SECONDS)) {
                cancel(id); throw new IOException("Android's update installer did not open. Try again.");
            }
        } catch (InterruptedException interrupted) {
            cancel(id); Thread.currentThread().interrupt(); throw new InterruptedIOException("Update cancelled.");
        }
        if (error.get() != null) throw error.get();
    }

    private HttpURLConnection connection(String id, String value) throws Exception {
        Request request = requests.get(id);
        checkCancelled(request);
        URL url = UpdateValidation.downloadUri(value).toURL();
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        synchronized (request) {
            checkCancelled(request);
            request.connection = connection;
        }
        connection.setConnectTimeout(15000); connection.setReadTimeout(45000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", AppVersion.userAgent(activity));
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private void disconnect(String id, HttpURLConnection connection) {
        Request request = requests.get(id);
        if (request != null) synchronized (request) {
            if (request.connection == connection) request.connection = null;
        }
        connection.disconnect();
    }

    private HttpURLConnection redirected(String id, String value) throws Exception {
        for (int i = 0; i < 5; i++) {
            HttpURLConnection connection = connection(id, value);
            int status;
            try { status = connection.getResponseCode(); }
            catch (Exception failure) { disconnect(id, connection); throw failure; }
            if (status >= 300 && status < 400) {
                String next = connection.getHeaderField("Location");
                disconnect(id, connection);
                if (next == null) throw new IOException("Invalid release redirect.");
                value = new URL(new URL(value), next).toString();
                UpdateValidation.downloadUri(value);
            } else return connection;
        }
        throw new IOException("Too many update redirects.");
    }

    private byte[] download(String id, String url, int limit) throws Exception {
        HttpURLConnection connection = redirected(id, url);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Could not download the update manifest.");
            try (InputStream in = connection.getInputStream()) { return read(id, in, limit); }
        } finally { disconnect(id, connection); }
    }

    private byte[] read(String id, InputStream in, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = in.read(buffer)) != -1) {
            checkCancelled(requests.get(id));
            if (out.size() + count > max) throw new IOException("Update metadata is too large.");
            out.write(buffer, 0, count);
        }
        checkCancelled(requests.get(id));
        return out.toByteArray();
    }
}
