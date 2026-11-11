package io.draft2done.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** Account-bound catalog and terminal refresh recovery, with no Android dependencies. */
final class ChatGPTSessionState {
    static final long CATALOG_TTL_MS=15*60*1000;
    private ChatGPTSessionState() {}
    static JSONObject active(JSONObject vault,String client,String subject,boolean cancelled)throws Exception {
        if(cancelled||!client.equals(vault.optString("active"))||subject.isEmpty())return null;
        JSONArray accounts=vault.optJSONArray("accounts");if(accounts==null)return null;
        for(int index=0;index<accounts.length();index++){
            JSONObject account=accounts.getJSONObject(index);
            if(client.equals(account.optString("client_id"))&&subject.equals(account.optString("subject")))return account;
        }
        return null;
    }
    static boolean freshCatalog(JSONObject account,long now){
        long saved=account.optLong("modelCatalogAt",0);
        return account.optJSONArray("modelCatalog")!=null&&saved>0&&now>=saved&&now-saved<CATALOG_TTL_MS;
    }
    static void requireModel(JSONObject account,String model,long now)throws Exception {
        if(!freshCatalog(account,now))throw new IllegalStateException("Load your available ChatGPT models in Settings before sending writing requests.");
        JSONArray models=account.getJSONArray("modelCatalog");
        for(int index=0;index<models.length();index++)if(model.equals(models.getJSONObject(index).optString("slug")))return;
        throw new IllegalStateException("This model is not in your current ChatGPT catalog. Load available models in Settings and choose one from the list. Nothing was sent.");
    }
    static boolean cacheCatalog(JSONObject vault,String client,String subject,boolean cancelled,JSONArray models,long now)throws Exception {
        JSONObject account=active(vault,client,subject,cancelled);if(account==null||account.optString("access_token").isEmpty())return false;
        account.put("modelCatalog",new JSONArray(models.toString())).put("modelCatalogAt",now);return true;
    }
    static boolean terminalRefresh(Exception error){
        String message=error.getMessage();return message!=null&&message.matches("(?s).*\\binvalid_grant\\b.*");
    }
    static boolean invalidate(JSONObject vault,String client,String subject,String attemptedRefresh,boolean cancelled)throws Exception {
        JSONObject account=active(vault,client,subject,cancelled);
        if(account==null||attemptedRefresh.isEmpty()||!attemptedRefresh.equals(account.optString("refresh_token")))return false;
        for(String field:new String[]{"access_token","refresh_token","id_token","scope","expires_at","modelCatalog","modelCatalogAt"})account.remove(field);
        return true;
    }
}
