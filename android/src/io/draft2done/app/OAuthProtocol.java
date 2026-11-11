package io.draft2done.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Public OAuth and SSE protocol logic, separate from Android credential storage. */
public final class OAuthProtocol {
    public static final String ISSUER="https://auth.openai.com";
    public static final String RESOURCE="https://api.openai.com/v1";
    public static final String SCOPES="openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    private OAuthProtocol() {}
    public static String callbackHTML(boolean valid){
        return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Draft2Done sign-in</title></head><body style=\"font:18px system-ui;padding:32px;line-height:1.7;background:#151f21;color:#e4ebe5\"><h1>Draft2Done</h1><p>"+(valid?"Sign-in received. Return to the app to see whether your account connected.":"This callback could not be verified. Return to the app and try again.")+"</p><p><a href=\"draft2done://auth/return\" style=\"display:inline-block;padding:14px 20px;background:#85bea8;color:#132223;border-radius:12px;text-decoration:none;font-weight:bold\">Return to Draft2Done</a></p></body></html>";
    }
    public static String random(int bytes){byte[] value=new byte[bytes];new SecureRandom().nextBytes(value);return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    public static String challenge(String verifier)throws Exception{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));}
    public static boolean same(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    public static String form(Map<String,String> values)throws Exception{StringBuilder out=new StringBuilder();for(Map.Entry<String,String> e:values.entrySet()){if(out.length()>0)out.append('&');out.append(URLEncoder.encode(e.getKey(),"UTF-8")).append('=').append(URLEncoder.encode(e.getValue(),"UTF-8"));}return out.toString();}
    public static Map<String,String> fields(String... values){Map<String,String> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put(values[i],values[i+1]);return out;}
    public static String authorization(String client,String host,String redirect,String state,String nonce,String verifier,String hint)throws Exception{
        Map<String,String> p=fields("client_id",client,"ext_agent_host_id",host,"response_type","code","redirect_uri",redirect,"scope",SCOPES,"resource",RESOURCE,"state",state,"nonce",nonce,"code_challenge_method","S256","code_challenge",challenge(verifier));
        if("dynamic_agent_client".equals(client))p.put("agent_name_hint","Draft2Done");
        else if(hint!=null&&!hint.isEmpty())p.put("id_token_hint",hint);
        return ISSUER+"/api/accounts/authorize?"+form(p);
    }
    public static JSONObject verifyIdentity(String token,JSONObject jwks,String client,String nonce,long epochSeconds)throws Exception{
        String[] parts=token.split("\\.",-1);if(parts.length!=3)throw new SecurityException("Invalid identity token format.");
        JSONObject header=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8));
        if(!"RS256".equals(header.optString("alg"))||header.has("crit"))throw new SecurityException("Unsupported identity signature algorithm.");
        String kid=header.getString("kid");JSONObject key=null;JSONArray keys=jwks.getJSONArray("keys");
        for(int i=0;i<keys.length();i++){JSONObject candidate=keys.getJSONObject(i);if(kid.equals(candidate.optString("kid"))&&"RSA".equals(candidate.optString("kty"))&&(!candidate.has("use")||"sig".equals(candidate.optString("use")))&&(!candidate.has("alg")||"RS256".equals(candidate.optString("alg")))){key=candidate;break;}}
        if(key==null)throw new SecurityException("No matching identity verification key.");
        BigInteger n=new BigInteger(1,Base64.getUrlDecoder().decode(key.getString("n"))),e=new BigInteger(1,Base64.getUrlDecoder().decode(key.getString("e")));
        if(n.bitLength()<2048)throw new SecurityException("Identity signing key is too small.");
        Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n,e)));verifier.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
        if(!verifier.verify(Base64.getUrlDecoder().decode(parts[2])))throw new SecurityException("Identity signature could not be verified.");
        JSONObject claims=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8));
        if(!ISSUER.equals(claims.getString("iss")))throw new SecurityException("Unexpected identity issuer.");
        Object audience=claims.get("aud");boolean match=client.equals(audience);if(audience instanceof JSONArray){JSONArray a=(JSONArray)audience;for(int i=0;i<a.length();i++)if(client.equals(a.optString(i)))match=true;if(a.length()>1&&!client.equals(claims.optString("azp")))throw new SecurityException("Unexpected authorized party.");}
        if(!match)throw new SecurityException("Identity was issued for a different application.");
        if(claims.getLong("exp")<epochSeconds-5||claims.getLong("iat")>epochSeconds+5)throw new SecurityException("Identity token has expired or its time is invalid.");
        if(!same(nonce,claims.optString("nonce"))||claims.optString("sub").isEmpty())throw new SecurityException("Identity nonce or subject is invalid.");
        return claims;
    }
    public static JSONObject consumeStream(InputStream stream)throws Exception{
        BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));String line;StringBuilder data=new StringBuilder(),text=new StringBuilder();JSONObject response=null;int size=0;
        while((line=reader.readLine())!=null){size+=line.length();if(size>4*1024*1024)throw new IllegalStateException("Research response is too large.");if(line.isEmpty()){if(data.length()==0)continue;String payload=data.toString();data.setLength(0);if("[DONE]".equals(payload))continue;JSONObject event=new JSONObject(payload);String type=event.optString("type");if("response.output_text.delta".equals(type))text.append(event.optString("delta"));if("response.completed".equals(type)){response=event.getJSONObject("response");break;}else if("response.failed".equals(type)||"response.incomplete".equals(type)||"error".equals(type))throw new IllegalStateException("ChatGPT did not complete this research request. "+safeError(event));}
            else if(line.startsWith("data:")){if(data.length()>0)data.append('\n');data.append(line.substring(5).trim());}}
        if(response==null)throw new IllegalStateException("Connection ended before ChatGPT confirmed completion. No result was saved.");
        StringBuilder finalText=new StringBuilder();JSONArray sources=new JSONArray(),annotations=new JSONArray();boolean web=false;JSONArray output=response.optJSONArray("output");
        if(output!=null)for(int i=0;i<output.length();i++){JSONObject item=output.getJSONObject(i);if("web_search_call".equals(item.optString("type")))web=true;JSONArray content=item.optJSONArray("content");if(content==null)continue;for(int j=0;j<content.length();j++){JSONObject part=content.getJSONObject(j);if(!"output_text".equals(part.optString("type")))continue;int offset=finalText.length();if(offset>0){finalText.append("\n\n");offset+=2;}finalText.append(part.optString("text"));JSONArray a=part.optJSONArray("annotations");if(a!=null)for(int k=0;k<a.length();k++){JSONObject citation=a.getJSONObject(k);if(!"url_citation".equals(citation.optString("type")))continue;String url=citation.optString("url");if(!url.startsWith("https://")&&!url.startsWith("http://"))continue;JSONObject s=new JSONObject().put("url",url).put("title",citation.optString("title",url));sources.put(s);annotations.put(new JSONObject().put("start",offset+citation.optInt("start_index",0)).put("end",offset+citation.optInt("end_index",0)).put("url",url).put("title",s.getString("title")));}}}
        String result=finalText.length()>0?finalText.toString():text.toString();if(result.trim().isEmpty())throw new IllegalStateException("ChatGPT completed without a readable research answer.");
        return new JSONObject().put("text",result).put("sources",sources).put("annotations",annotations).put("webSearchUsed",web).put("responseId",response.optString("id")).put("completed",true);
    }
    private static String safeError(JSONObject event){
        JSONObject response=event.optJSONObject("response");
        JSONObject error=response==null?event.optJSONObject("error"):response.optJSONObject("error");
        String code=error==null?event.optString("code"):error.optString("code");
        if("subscription_sharing_usage_limit_exceeded".equals(code))return "Code: "+code+". A ChatGPT usage limit was reached. Check ChatGPT Settings > Usage for account or app limits. Your account is still connected. Free source search and local AI remain available.";
        if("subscription_sharing_user_not_eligible".equals(code))return "Code: "+code+". ChatGPT plan usage is unavailable for this account or workspace. Free source search and local AI remain available.";
        if(code.matches("[a-zA-Z0-9_]{1,100}"))return "Code: "+code+". Check account access, usage controls and model support.";
        return "Check account access and usage controls.";
    }

}
