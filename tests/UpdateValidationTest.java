package io.draft2done.app;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class UpdateValidationTest {
    private static int checks;
    private static final String ASSET = UpdateValidation.REPOSITORY + "/releases/download/v0.2.0/Draft2Done-0.2.0.apk";
    private static final byte[][] SIGNER = {new byte[]{11, 22, 33, 44}};
    private interface Checked { void run() throws Exception; }
    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label); checks++;
    }
    private static void rejects(Checked action, String label) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (Exception expected) { rejected = true; }
        expect(rejected, label);
    }
    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte part : bytes) value.append(String.format(Locale.ROOT, "%02x", part & 255));
        return value.toString();
    }
    private static JSONObject manifest() throws Exception {
        return new JSONObject().put("version", "0.2.0").put("versionCode", 2)
                .put("package", UpdateValidation.PACKAGE).put("apkUrl", ASSET)
                .put("sha256", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }
    public static void main(String[] args) throws Exception {
        expect(UpdateValidation.releaseAsset(ASSET, true).getHost().equals("github.com"), "Repository APK accepted");
        expect(UpdateValidation.releaseAsset(UpdateValidation.REPOSITORY + "/releases/download/v0.2.0/latest.json", false) != null, "Repository manifest accepted");
        expect(UpdateValidation.downloadUri("https://release-assets.githubusercontent.com/github-production-release-asset/123/asset?token=signed").getRawQuery() != null, "Signed GitHub CDN redirect accepted");
        expect(UpdateValidation.downloadUri("https://objects.githubusercontent.com/releases/asset") != null, "Older GitHub asset CDN accepted");
        expect(UpdateValidation.downloadUri("https://api.github.com/repos/youriptvus/Draft2Done/releases/latest") != null, "Official release API accepted");
        String[] invalidDownloads = {
                "http://github.com/youriptvus/Draft2Done/releases/download/v1/app.apk",
                "https://github.com.evil.example/asset.apk",
                "https://github.com@evil.example/asset.apk",
                "https://attacker@github.com/asset.apk",
                "https://github.com:8443/asset.apk",
                "https://release-assets.githubusercontent.com.evil.example/asset",
                "https://github.com/asset.apk#fragment",
                "https://github.com/releases/../another/asset.apk",
                "https://github.com/releases/%2e%2e/another/asset.apk",
                "https://github.com/releases/%5casset.apk",
                "file:///private/update.apk",
                "https://github.com/releases/%00asset.apk"
        };
        for (String value : invalidDownloads) rejects(() -> UpdateValidation.downloadUri(value), "Unsafe redirect rejected: " + value);
        String[] invalidAssets = {
                "https://github.com/attacker/Draft2Done/releases/download/v1/app.apk",
                "https://github.com/youriptvus/Draft2Done-copy/releases/download/v1/app.apk",
                UpdateValidation.REPOSITORY + "/releases/download/v1/app.apk?redirect=elsewhere",
                UpdateValidation.REPOSITORY + "/releases/download/v1/nested/app.apk",
                UpdateValidation.REPOSITORY + "/releases/download/v1%2fother/app.apk",
                UpdateValidation.REPOSITORY + "/releases/download/v1/%252e%252e.apk",
                UpdateValidation.REPOSITORY + "/releases/download//app.apk",
                UpdateValidation.REPOSITORY + "/releases/download/v1/app.exe",
                "https://release-assets.githubusercontent.com/releases/download/v1/app.apk"
        };
        for (String value : invalidAssets) rejects(() -> UpdateValidation.releaseAsset(value, true), "Unsafe repository asset rejected: " + value);
        rejects(() -> UpdateValidation.releaseAsset(ASSET, false), "Manifest must be latest.json");
        JSONObject normalized = UpdateValidation.manifest(manifest().put("sha256", manifest().getString("sha256").toUpperCase(Locale.ROOT)));
        expect(normalized.getLong("versionCode") == 2 && normalized.getString("sha256").equals(manifest().getString("sha256")), "Valid manifest normalized");
        rejects(() -> UpdateValidation.manifest(manifest().put("versionCode", "2")), "String version code rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("versionCode", 2.5)), "Fractional version code rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("versionCode", 0)), "Zero version code rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("versionCode", -1)), "Negative version code rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("versionCode", new java.math.BigInteger("9223372036854775808"))), "Overflow version code rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("package", "io.attacker.app")), "Wrong manifest package rejected");
        rejects(() -> { JSONObject value = manifest(); value.remove("package"); UpdateValidation.manifest(value); }, "Missing manifest package rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("sha256", "short")), "Truncated hash rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("sha256", "g123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")), "Non-hex hash rejected");
        rejects(() -> UpdateValidation.manifest(manifest().put("version", " ")), "Blank display version rejected");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("signed update bytes".getBytes(StandardCharsets.UTF_8));
        UpdateValidation.checksum(digest, hex(digest).toUpperCase(Locale.ROOT)); checks++;
        byte[] tampered = digest.clone(); tampered[8] ^= 1;
        rejects(() -> UpdateValidation.checksum(tampered, hex(digest)), "Changed APK bytes rejected");
        rejects(() -> UpdateValidation.checksum(new byte[31], hex(digest)), "Invalid digest length rejected");
        rejects(() -> UpdateValidation.checksum(digest, null), "Missing expected digest rejected");
        UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 2, SIGNER, new byte[][]{SIGNER[0].clone()}); checks++;
        rejects(() -> UpdateValidation.candidate("io.attacker.app", 1, 2, 2, SIGNER, SIGNER), "Different APK package rejected");
        rejects(() -> UpdateValidation.candidate(null, 1, 2, 2, SIGNER, SIGNER), "Unparseable APK rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 2, 1, 1, SIGNER, SIGNER), "Downgrade rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 2, 2, 2, SIGNER, SIGNER), "Same-version reinstall rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 3, SIGNER, SIGNER), "Tampered published version rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 2, SIGNER, new byte[][]{{99}}), "Wrong APK signing certificate rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 2, SIGNER, null), "Unsigned APK rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 2, SIGNER, new byte[][]{SIGNER[0], SIGNER[0]}), "Unexpected multi-signer APK rejected");
        rejects(() -> UpdateValidation.candidate(UpdateValidation.PACKAGE, 1, 2, 2, SIGNER, new byte[][]{new byte[0]}), "Empty APK certificate rejected");
        System.out.println("Update validation checks passed: " + checks);
    }
}
