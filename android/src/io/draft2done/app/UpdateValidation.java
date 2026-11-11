package io.draft2done.app;

import org.json.JSONObject;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Locale;

/** Update trust rules shared by the Android downloader and host-side tests. */
final class UpdateValidation {
    static final String PACKAGE = "io.draft2done.app";
    static final String REPOSITORY = "https://github.com/youriptvus/Draft2Done";
    private static final String RELEASE_PATH = "/youriptvus/Draft2Done/releases/download/";

    private UpdateValidation() {}

    static URI downloadUri(String value) throws Exception {
        URI uri = new URI(value);
        String host = uri.getHost();
        if (!"https".equals(uri.getScheme()) || uri.getRawUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getRawFragment() != null
                || !("api.github.com".equals(host) || "github.com".equals(host)
                || "objects.githubusercontent.com".equals(host)
                || "release-assets.githubusercontent.com".equals(host))) {
            throw new SecurityException("Untrusted update download address.");
        }
        String path = uri.getPath();
        if (path == null || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0) {
            throw new SecurityException("Invalid update download path.");
        }
        for (String part : path.split("/", -1)) {
            if (".".equals(part) || "..".equals(part)) {
                throw new SecurityException("Invalid update download path.");
            }
        }
        return uri;
    }

    static URI releaseAsset(String value, boolean apk) throws Exception {
        URI uri = downloadUri(value);
        String path = uri.getPath();
        String rawPath = uri.getRawPath().toLowerCase(Locale.ROOT);
        if (!"github.com".equals(uri.getHost()) || uri.getRawQuery() != null
                || !path.startsWith(RELEASE_PATH) || rawPath.contains("%2f")
                || rawPath.contains("%25")) {
            throw new SecurityException("Update assets must belong to the Draft2Done repository.");
        }
        String[] parts = path.substring(RELEASE_PATH.length()).split("/", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()
                || (apk ? !parts[1].endsWith(".apk") : !"latest.json".equals(parts[1]))) {
            throw new SecurityException("Invalid Draft2Done release asset.");
        }
        return uri;
    }

    static JSONObject manifest(JSONObject value) throws Exception {
        Object codeValue = value.get("versionCode");
        if (!(codeValue instanceof Number) || !codeValue.toString().matches("[1-9][0-9]*")) {
            throw new SecurityException("The update version code must be a positive integer.");
        }
        long code;
        try { code = Long.parseLong(codeValue.toString()); }
        catch (NumberFormatException invalid) { throw new SecurityException("The update version code is invalid."); }
        if (!PACKAGE.equals(value.get("package"))) {
            throw new SecurityException("The update manifest names a different application.");
        }
        Object version = value.get("version"), apk = value.get("apkUrl"), hash = value.get("sha256");
        if (!(version instanceof String) || ((String)version).trim().isEmpty()
                || ((String)version).length() > 80 || !(apk instanceof String)
                || !(hash instanceof String) || !((String)hash).matches("[a-fA-F0-9]{64}")) {
            throw new SecurityException("The published update manifest is invalid.");
        }
        releaseAsset((String)apk, true);
        return new JSONObject().put("version", version).put("versionCode", code)
                .put("package", PACKAGE).put("apkUrl", apk)
                .put("sha256", ((String)hash).toLowerCase(Locale.ROOT));
    }

    static void checksum(byte[] actual, String expected) {
        if (actual == null || actual.length != 32 || expected == null
                || !expected.matches("[a-fA-F0-9]{64}")) {
            throw new SecurityException("The update checksum is invalid.");
        }
        byte[] wanted = new byte[32];
        for (int i = 0; i < wanted.length; i++) {
            wanted[i] = (byte)Integer.parseInt(expected.substring(i * 2, i * 2 + 2), 16);
        }
        if (!MessageDigest.isEqual(actual, wanted)) {
            throw new SecurityException("Update checksum did not match. Installation was blocked.");
        }
    }

    static void candidate(String actualPackage, long installedVersion, long candidateVersion,
                          long publishedVersion, byte[][] installedSigners, byte[][] nextSigners) {
        if (!PACKAGE.equals(actualPackage) || candidateVersion != publishedVersion
                || candidateVersion <= installedVersion || publishedVersion <= 0) {
            throw new SecurityException("Downloaded APK is not the expected Draft2Done update.");
        }
        if (installedSigners == null || nextSigners == null || installedSigners.length != 1
                || nextSigners.length != 1 || installedSigners[0] == null || nextSigners[0] == null
                || installedSigners[0].length == 0 || nextSigners[0].length == 0
                || !MessageDigest.isEqual(installedSigners[0], nextSigners[0])) {
            throw new SecurityException("Update signing identity differs from this app. Installation was blocked.");
        }
    }
}
