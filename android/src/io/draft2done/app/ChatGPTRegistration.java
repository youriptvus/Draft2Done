package io.draft2done.app;

import org.json.JSONObject;

/** Issued client registration is reusable before any account has been verified. */
final class ChatGPTRegistration {
    private static final String PENDING = "pendingRegistration";
    private ChatGPTRegistration() {}
    static String pendingClient(JSONObject vault, String host) {
        JSONObject pending = vault.optJSONObject(PENDING);
        if (pending == null || !host.equals(pending.optString("hostId"))) return "";
        String client = pending.optString("client_id");
        return valid(client) ? client : "";
    }
    static String callbackClient(String requested, String issued) {
        if ("dynamic_agent_client".equals(requested)) {
            if (!valid(issued)) throw new SecurityException("ChatGPT registration did not return an issued client ID.");
            return issued;
        }
        if (!valid(requested) || (issued != null && !requested.equals(issued)))
            throw new SecurityException("ChatGPT returned a different client registration.");
        return requested;
    }
    static void remember(JSONObject vault, String host, String client) throws Exception {
        if (!valid(client) || host == null || host.isEmpty()) throw new SecurityException("Invalid ChatGPT client registration.");
        vault.put(PENDING, new JSONObject().put("hostId", host).put("client_id", client));
    }
    static void completed(JSONObject vault, String host, String client) {
        if (client.equals(pendingClient(vault, host))) vault.remove(PENDING);
    }
    private static boolean valid(String client) {
        return client != null && !client.trim().isEmpty() && client.length() <= 1024 && !"dynamic_agent_client".equals(client);
    }
}
