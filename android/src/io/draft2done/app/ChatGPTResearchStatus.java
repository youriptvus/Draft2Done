package io.draft2done.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** Account-specific research diagnostics contain fixed text, never server payloads. */
final class ChatGPTResearchStatus {
    static final String QUOTA_MESSAGE = "OpenAI paused this AI request because a ChatGPT usage limit was reached. Check ChatGPT Settings \u2192 Usage for account or app limits. Your account is still connected.";
    private static final String LEGACY_QUOTA_MESSAGE = "OpenAI paused this research request because a ChatGPT usage limit was reached. Check ChatGPT Settings \u2192 Usage for account or app limits. Your account is still connected.";
    private ChatGPTResearchStatus() {}
    static boolean isQuotaFailure(Exception error) {
        String message=error.getMessage();
        return message!=null&&message.matches("(?s).*\\b[A-Za-z0-9_]*usage_limit[A-Za-z0-9_]*\\b.*");
    }
    static String message(JSONObject account){
        String stored=account.optString("lastResearchError");
        return QUOTA_MESSAGE.equals(stored)||LEGACY_QUOTA_MESSAGE.equals(stored)?QUOTA_MESSAGE:"";
    }
    static boolean update(JSONObject vault,String client,String subject,boolean cancelled,String message)throws Exception {
        if(cancelled||subject.isEmpty()||!client.equals(vault.optString("active")))return false;
        JSONArray accounts=vault.optJSONArray("accounts");if(accounts==null)return false;
        for(int index=0;index<accounts.length();index++){
            JSONObject account=accounts.getJSONObject(index);
            if(!client.equals(account.optString("client_id"))||!subject.equals(account.optString("subject"))||account.optString("access_token").isEmpty())continue;
            if(message.isEmpty())return account.remove("lastResearchError")!=null;
            if(!QUOTA_MESSAGE.equals(message))throw new IllegalArgumentException("Unsupported research diagnostic.");
            if(message.equals(account.optString("lastResearchError")))return false;
            account.put("lastResearchError",message);return true;
        }
        return false;
    }
    static void retain(JSONObject previous,JSONObject replacement)throws Exception {
        if(previous.optString("client_id").equals(replacement.optString("client_id"))&&previous.optString("subject").equals(replacement.optString("subject"))&&!message(previous).isEmpty())
            replacement.put("lastResearchError",QUOTA_MESSAGE);
    }
}
