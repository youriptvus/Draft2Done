package io.draft2done.test;

import android.app.Activity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicitly opted-in account lifecycle checks; no inference requests. */
final class ChatGPTAccountChecks {
    interface Check { void run(boolean condition, String label) throws Exception; }
    private ChatGPTAccountChecks() {}

    /**
     * Root instrumentation must explicitly opt in and run this off the main thread.
     * Only expiration metadata is controlled in memory. Production code alone
     * exchanges/rotates tokens and writes its encrypted account store.
     */
    static void run(Activity activity, Check check) throws Exception {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            throw new IOException("Controlled ChatGPT refresh check requires the instrumentation worker thread.");
        }
        Field connectionField = field(activity.getClass(), "chatgpt");
        Object connection = connectionField.get(activity);
        if (connection == null) throw new IOException("ChatGPT connection is not initialized.");
        Class<?> type = connection.getClass();
        Method selected = method(type, "selected");
        Method enqueue = method(type, "enqueue", String.class);
        Method handle = method(type, "handle", String.class, String.class, JSONObject.class);
        Object refreshLock = field(type, "refreshLock").get(connection);
        Object registry = field(type, "requests").get(connection);
        Method activeExcept = method(registry.getClass(), "activeExcept", String.class);
        Object connections = field(type, "connections").get(connection);
        String requestId = "qa-refresh-" + UUID.randomUUID().toString();

        // accessToken uses this same lock. Holding it prevents another refresh
        // from racing the controlled expiry, while normal account guards remain.
        synchronized (refreshLock) {
            JSONObject original;
            String client;
            String subject;
            long previousExpiry;
            boolean hadExpiry;
            boolean renewable;
            synchronized (connection) {
                original = selectedOrNull(selected, connection);
                renewable = connected(original) && hasRefresh(original);
                client = original == null ? "" : original.optString("client_id");
                subject = original == null ? "" : original.optString("subject");
                hadExpiry = original != null && original.has("expires_at");
                previousExpiry = original == null ? 0 : original.optLong("expires_at");
            }
            check.run(renewable && !client.isEmpty() && !subject.isEmpty(),
                "Existing connected ChatGPT account has a renewable session; no credentials recorded");

            boolean forcedExpiry = false;
            boolean completed = false;
            boolean catalogReturned = false;
            boolean accountPreserved = false;
            boolean expiryUpdated = false;
            boolean noArtificialExpiry = false;
            boolean cleaned = false;
            try {
                synchronized (connection) {
                    JSONObject current = selectedOrNull(selected, connection);
                    if (!sameAccount(current, original, client, subject)) {
                        throw new IOException("Active ChatGPT account changed before the controlled check.");
                    }
                    current.put("expires_at", 0);
                    forcedExpiry = current.optLong("expires_at", -1) == 0;
                }
                check.run(forcedExpiry,
                    "Controlled expires_at=0 was set only in memory to exercise real production refresh");
                enqueue.invoke(connection, requestId);
                // handle performs its own finally cleanup on either outcome.
                JSONObject result = (JSONObject)handle.invoke(connection, "models", requestId, new JSONObject());
                JSONArray models = result.optJSONArray("models");
                catalogReturned = models != null && models.length() > 0;
                if (catalogReturned) {
                    for (int index = 0; index < models.length(); index++) {
                        JSONObject model = models.optJSONObject(index);
                        if (model == null || model.optString("slug").isEmpty()) catalogReturned = false;
                    }
                }
                completed = true;
            } catch (Exception ignored) {
                // Never forward reflection/provider errors that might contain
                // account data. Static boolean checks below report the outcome.
            } finally {
                synchronized (connection) {
                    JSONObject current = selectedOrNull(selected, connection);
                    boolean same = sameAccount(current, original, client, subject);
                    // Restore only the test's unchanged expiration metadata on
                    // transient failure. Never restore any token or a session
                    // that production invalidated, replaced or refreshed.
                    if (!completed && forcedExpiry && same && current.has("expires_at") &&
                        current.optLong("expires_at", -1) == 0 && connected(current) && hasRefresh(current)) {
                        if (hadExpiry) current.put("expires_at", previousExpiry);
                        else current.remove("expires_at");
                    }
                    accountPreserved = same && connected(current) && hasRefresh(current);
                    expiryUpdated = same && current.optLong("expires_at") > System.currentTimeMillis();
                    noArtificialExpiry = !forcedExpiry || !same || !connected(current) ||
                        !current.has("expires_at") || current.optLong("expires_at", -1) != 0 ||
                        (hadExpiry && previousExpiry == 0);
                }
                Set<?> active = (Set<?>)activeExcept.invoke(registry, "");
                cleaned = !active.contains(requestId) && !((Map<?, ?>)connections).containsKey(requestId);
            }
            check.run(noArtificialExpiry,
                "Failure cleanup leaves no test-created zero expiry on the unchanged connected account");
            check.run(cleaned, "Production request finally cleanup removed the unique refresh/catalog request");
            check.run(accountPreserved, "The original renewable ChatGPT account remains connected and selected");
            check.run(completed, "Normal models action completed its real refresh-grant and catalog requests");
            check.run(expiryUpdated, "Production refresh replaced the controlled expiry with a future expiration");
            check.run(catalogReturned, "Refreshed access token returned a nonempty eligible account model catalog");
        }
    }

    /**
     * Destructive account test, invoked only by a dedicated explicit mode.
     * Root must immediately offer normal browser sign-in afterward. This method
     * never runs from run(), restores credentials, or changes account selection.
     */
    static void signOut(Activity activity, Check check) throws Exception {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            throw new IOException("ChatGPT sign-out check requires the instrumentation worker thread.");
        }
        Object connection = field(activity.getClass(), "chatgpt").get(activity);
        if (connection == null) throw new IOException("ChatGPT connection is not initialized.");
        Class<?> type = connection.getClass();
        Method selected = method(type, "selected");
        Method status = method(type, "status");
        Method enqueue = method(type, "enqueue", String.class);
        Method handle = method(type, "handle", String.class, String.class, JSONObject.class);
        Method json = method(type, "json", String.class, String.class, String.class, String.class, String.class, boolean.class);
        Object registry = field(type, "requests").get(connection);
        Method activeExcept = method(registry.getClass(), "activeExcept", String.class);
        Method finish = method(registry.getClass(), "finish", String.class);
        Map<?, ?> connections = (Map<?, ?>)field(type, "connections").get(connection);
        Object refreshLock = field(type, "refreshLock").get(connection);

        synchronized (refreshLock) {
            JSONObject original;
            String client;
            String subject;
            String oldRefresh;
            synchronized (connection) {
                original = selectedOrNull(selected, connection);
                client = original == null ? "" : original.optString("client_id");
                subject = original == null ? "" : original.optString("subject");
                oldRefresh = original == null ? "" : original.optString("refresh_token");
            }
            boolean renewable = connected(original) && !client.isEmpty() && !subject.isEmpty() && !oldRefresh.isEmpty();
            check.run(renewable, "Explicit sign-out check starts with a connected renewable session; credentials stay in memory");
            if (!renewable) throw new IOException("A connected renewable ChatGPT session is required for this explicit sign-out check.");

            String signOutId = "qa-signout-" + UUID.randomUUID().toString();
            boolean completed = false;
            boolean revocationAcknowledged = false;
            boolean disconnected = false;
            boolean localCleared = false;
            boolean normalFinallyCleaned = false;
            boolean cleanupSucceeded;
            try {
                synchronized (connection) {
                    if (!sameAccount(selectedOrNull(selected, connection), original, client, subject)) {
                        throw new IOException("Active ChatGPT account changed before the explicit sign-out check.");
                    }
                    enqueue.invoke(connection, signOutId);
                    JSONObject result = (JSONObject)handle.invoke(connection, "signout", signOutId, new JSONObject());
                    completed = true;
                    revocationAcknowledged = "Signed out; renewable session revoked.".equals(result.optString("message"));
                }
            } catch (Exception ignored) {
                // Static checks report failure without forwarding provider data.
            } finally {
                normalFinallyCleaned = requestClean(registry, activeExcept, connections, signOutId);
                cleanupSucceeded = cleanupRequest(registry, finish, activeExcept, connections, signOutId);
                synchronized (connection) {
                    JSONObject current = selectedOrNull(selected, connection);
                    localCleared = sameAccount(current, original, client, subject) && credentialsCleared(current);
                    try { disconnected = !new JSONObject((String)status.invoke(connection)).optBoolean("connected", true); }
                    catch (Exception ignored) { disconnected = false; }
                }
            }
            check.run(normalFinallyCleaned && cleanupSucceeded, "Production sign-out request and connection cleanup completed");
            check.run(completed, "Normal production sign-out action completed without restoring credentials");
            check.run(disconnected && localCleared, "Sign-out reports disconnected and clears local access, refresh, identity tokens and catalog");
            check.run(revocationAcknowledged, "Remote revocation was explicitly acknowledged by normal production sign-out");
            if (!completed || !disconnected || !localCleared || !revocationAcknowledged) {
                throw new IOException("Explicit sign-out verification is incomplete; remote revocation is not claimed. Reconnect through normal browser sign-in.");
            }

            // One old-refresh probe only after production confirms revocation.
            // This calls the existing HTTP helper directly so neither an error
            // nor an unexpected successful response can write old/new tokens.
            Class<?> protocol = Class.forName("io.draft2done.app.OAuthProtocol", true, activity.getClassLoader());
            String issuer = (String)field(protocol, "ISSUER").get(null);
            String resource = (String)field(protocol, "RESOURCE").get(null);
            Map<String, String> values = new LinkedHashMap<>();
            values.put("grant_type", "refresh_token");
            values.put("client_id", client);
            values.put("refresh_token", oldRefresh);
            values.put("resource", resource);
            String body = (String)method(protocol, "form", Map.class).invoke(null, values);
            String probeId = "qa-revoked-refresh-" + UUID.randomUUID().toString();
            boolean oldRefreshRejected = false;
            boolean probeCleaned;
            try {
                enqueue.invoke(connection, probeId);
                json.invoke(connection, probeId, issuer + "/api/accounts/oauth/token", "POST", "", body, true);
                // Discard an unexpected success; never persist returned tokens.
            } catch (Exception error) {
                oldRefreshRejected = invalidGrant(error);
            } finally {
                probeCleaned = cleanupRequest(registry, finish, activeExcept, connections, probeId);
                values.clear();
                oldRefresh = "";
                body = "";
            }
            check.run(probeCleaned, "Single revoked-refresh probe cleaned its own request and connection");
            check.run(oldRefreshRejected, "Server rejects the captured revoked refresh token with invalid_grant; no token response persisted");
            synchronized (connection) {
                JSONObject current = selectedOrNull(selected, connection);
                check.run(sameAccount(current, original, client, subject) && credentialsCleared(current),
                    "Revocation verification leaves local credentials cleared for normal browser reauthentication");
            }
        }
    }

    private static boolean credentialsCleared(JSONObject account) {
        if (account == null) return false;
        for (String name : new String[]{"access_token", "refresh_token", "id_token", "scope", "expires_at", "modelCatalog", "modelCatalogAt"}) {
            if (account.has(name)) return false;
        }
        return true;
    }
    private static boolean invalidGrant(Throwable error) {
        for (int depth = 0; error != null && depth < 8; depth++, error = error.getCause()) {
            String message = error.getMessage();
            if (message != null && message.matches("(?s).*\\binvalid_grant\\b.*")) return true;
        }
        return false;
    }
    private static boolean requestClean(Object registry, Method activeExcept, Map<?, ?> connections, String id) {
        try { return !((Set<?>)activeExcept.invoke(registry, "")).contains(id) && !connections.containsKey(id); }
        catch (Exception ignored) { return false; }
    }
    private static boolean cleanupRequest(Object registry, Method finish, Method activeExcept, Map<?, ?> connections, String id) {
        try {
            Object pending = connections.remove(id);
            if (pending instanceof HttpURLConnection) ((HttpURLConnection)pending).disconnect();
            finish.invoke(registry, id);
            return requestClean(registry, activeExcept, connections, id);
        } catch (Exception ignored) { return false; }
    }

    private static boolean connected(JSONObject account) {
        if (account == null || account.optString("access_token").isEmpty()) return false;
        String scope = " " + account.optString("scope") + " ";
        return scope.contains(" chatgpt.tokens.use.direct ") && scope.contains(" resource.invoke ");
    }
    private static boolean hasRefresh(JSONObject account) {
        return account != null && !account.optString("refresh_token").isEmpty();
    }
    private static boolean sameAccount(JSONObject current, JSONObject original, String client, String subject) {
        return current != null && current == original && client.equals(current.optString("client_id")) &&
            subject.equals(current.optString("subject"));
    }
    private static JSONObject selectedOrNull(Method selected, Object connection) {
        try { return (JSONObject)selected.invoke(connection); }
        catch (Exception ignored) { return null; }
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Method method(Class<?> type, String name, Class<?>... arguments) throws Exception {
        Method method = type.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        return method;
    }
}
