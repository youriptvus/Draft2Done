package io.draft2done.app;

import org.json.JSONObject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChatGPTConnectionSupportTest {
    private static int checks;
    private interface Action { void run() throws Exception; }
    private static void expect(boolean condition,String label) { if(!condition)throw new AssertionError(label);checks++; }
    private static void rejects(Action action,Class<? extends Exception> type,String label)throws Exception {
        try { action.run();throw new AssertionError(label); }
        catch(Exception error) { if(!type.isInstance(error))throw error;checks++; }
    }
    public static void main(String[] arguments)throws Exception {
        List<Long> delays=new ArrayList<>();int[] attempts={0};
        String result=DnsRetry.run(()->{if(++attempts[0]<3)throw new UnknownHostException("fictional.invalid");return "ok";},()->{},delays::add);
        expect("ok".equals(result)&&attempts[0]==3&&delays.equals(Arrays.asList(300L,900L)),"DNS retry succeeds on the last bounded attempt");
        attempts[0]=0;delays.clear();
        rejects(()->DnsRetry.run(()->{attempts[0]++;throw new UnknownHostException("fictional.invalid");},()->{},delays::add),UnknownHostException.class,"Permanent DNS failure is returned");
        expect(attempts[0]==3&&delays.size()==2,"Permanent DNS failure never exceeds three attempts");
        attempts[0]=0;delays.clear();
        rejects(()->DnsRetry.run(()->{attempts[0]++;throw new IOException("Read failed after sending");},()->{},delays::add),IOException.class,"Ambiguous transport errors are never replayed");
        expect(attempts[0]==1&&delays.isEmpty(),"A general I/O error has no retry delay");
        attempts[0]=0;
        rejects(()->DnsRetry.run(()->{attempts[0]++;throw new IllegalStateException("HTTP 429");},()->{},delays::add),IllegalStateException.class,"HTTP and plan-policy failures are never retried");
        expect(attempts[0]==1,"HTTP failure makes one request");
        attempts[0]=0;
        rejects(()->DnsRetry.run(()->{attempts[0]++;return "bad";},()->{throw new InterruptedIOException("Cancelled");},delays::add),InterruptedIOException.class,"Cancellation is checked before the first attempt");
        expect(attempts[0]==0,"Pre-cancelled request sends nothing");
        attempts[0]=0;boolean[] cancelled={false};
        rejects(()->DnsRetry.run(()->{attempts[0]++;throw new UnknownHostException("fictional.invalid");},()->{if(cancelled[0])throw new InterruptedIOException("Cancelled");},milliseconds->{cancelled[0]=true;}),InterruptedIOException.class,"Cancellation during retry delay prevents another attempt");
        expect(attempts[0]==1,"A cancelled DNS retry never reaches the next attempt");
        try {
            rejects(()->DnsRetry.run(()->{throw new UnknownHostException("fictional.invalid");},()->{},milliseconds->{throw new InterruptedException("interrupted");}),InterruptedIOException.class,"Interrupted retry delay reports cancellation");
            expect(Thread.currentThread().isInterrupted(),"Interrupted retry restores the thread interruption flag");
        } finally { Thread.interrupted(); }

        JSONObject vault=new JSONObject();String host="urn:uuid:fixture-host",client="oaiapp_fixture";
        expect(ChatGPTRegistration.pendingClient(vault,host).isEmpty(),"A new installation has no pending client");
        expect(client.equals(ChatGPTRegistration.callbackClient("dynamic_agent_client",client)),"Dynamic callback uses the issued registration");
        rejects(()->ChatGPTRegistration.callbackClient("dynamic_agent_client",null),SecurityException.class,"First registration requires an issued client ID");
        rejects(()->ChatGPTRegistration.callbackClient("dynamic_agent_client","dynamic_agent_client"),SecurityException.class,"The dynamic placeholder cannot become a saved registration");
        ChatGPTRegistration.remember(vault,host,client);
        expect(client.equals(ChatGPTRegistration.pendingClient(vault,host)),"Pending client survives a private-vault JSON roundtrip");
        JSONObject restored=new JSONObject(vault.toString());
        expect(client.equals(ChatGPTRegistration.pendingClient(restored,host)),"Pending registration is serializable before token exchange");
        expect(!vault.has("active")&&!vault.has("accounts")&&!vault.toString().contains("token"),"Pending registration creates no active account, identity or token fields");
        expect(ChatGPTRegistration.pendingClient(vault,"urn:uuid:other-host").isEmpty(),"Pending client cannot be reused with a different stable host");
        expect(client.equals(ChatGPTRegistration.callbackClient(client,null)),"Returning pending registration permits omitted callback client ID");
        expect(client.equals(ChatGPTRegistration.callbackClient(client,client)),"Returning registration accepts the same callback client ID");
        rejects(()->ChatGPTRegistration.callbackClient(client,"oaiapp_other"),SecurityException.class,"Returning registration rejects a substituted callback client ID");
        ChatGPTRegistration.completed(vault,host,"oaiapp_other");
        expect(client.equals(ChatGPTRegistration.pendingClient(vault,host)),"Completing another account does not discard the pending client");
        ChatGPTRegistration.completed(vault,"urn:uuid:other-host",client);
        expect(client.equals(ChatGPTRegistration.pendingClient(vault,host)),"A different host cannot clear a pending registration");
        ChatGPTRegistration.completed(vault,host,client);
        expect(!vault.has("pendingRegistration"),"Verified completion clears only its matching pending registration");

        expect(ChatGPTResearchStatus.isQuotaFailure(new IllegalStateException("Code: subscription_sharing_usage_limit_exceeded.")),"Terminal usage-limit code is recognized");
        expect(!ChatGPTResearchStatus.isQuotaFailure(new IOException("Connection failed"))&&!ChatGPTResearchStatus.isQuotaFailure(new IllegalStateException("HTTP 403 access_denied")),"Transport and access failures do not become quota diagnostics");
        JSONObject account=new JSONObject().put("client_id",client).put("subject","fixture-subject").put("access_token","fictional-test-token");
        vault=new JSONObject().put("active",client).put("accounts",new org.json.JSONArray().put(account));
        expect(!ChatGPTResearchStatus.update(vault,client,"fixture-subject",true,ChatGPTResearchStatus.QUOTA_MESSAGE)&&!account.has("lastResearchError"),"Cancelled research cannot change account diagnostics");
        expect(!ChatGPTResearchStatus.update(vault,"other-client","fixture-subject",false,ChatGPTResearchStatus.QUOTA_MESSAGE)&&!account.has("lastResearchError"),"An account switch blocks an older request diagnostic");
        expect(!ChatGPTResearchStatus.update(vault,client,"other-subject",false,ChatGPTResearchStatus.QUOTA_MESSAGE)&&!account.has("lastResearchError"),"A different identity cannot receive an older account diagnostic");
        expect(ChatGPTResearchStatus.update(vault,client,"fixture-subject",false,ChatGPTResearchStatus.QUOTA_MESSAGE)&&ChatGPTResearchStatus.QUOTA_MESSAGE.equals(account.getString("lastResearchError")),"Quota failure stores only the fixed friendly diagnostic");
        expect("fictional-test-token".equals(account.getString("access_token"))&&client.equals(vault.getString("active")),"Quota diagnostics leave active account and credentials intact");
        JSONObject renewed=new JSONObject().put("client_id",client).put("subject","fixture-subject").put("access_token","fictional-renewed-token");
        ChatGPTResearchStatus.retain(account,renewed);
        expect(ChatGPTResearchStatus.QUOTA_MESSAGE.equals(renewed.optString("lastResearchError")),"Successful reauthentication preserves the last failed research attempt");
        expect(!ChatGPTResearchStatus.update(vault,client,"fixture-subject",true,"")&&account.has("lastResearchError"),"Cancelled success cannot clear an earlier quota diagnostic");
        expect(ChatGPTResearchStatus.update(vault,client,"fixture-subject",false,"")&&!account.has("lastResearchError"),"Successful research clears its account diagnostic");
        expect(!ChatGPTResearchStatus.update(vault,client,"fixture-subject",false,""),"No diagnostic write is needed after an already clean success");
        account.remove("access_token");
        expect(!ChatGPTResearchStatus.update(vault,client,"fixture-subject",false,ChatGPTResearchStatus.QUOTA_MESSAGE),"Signed-out account cannot receive a late research diagnostic");
        System.out.println("ChatGPT DNS, registration and research-status checks passed: "+checks);
    }
}
