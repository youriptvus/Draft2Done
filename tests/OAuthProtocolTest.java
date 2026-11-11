package io.draft2done.app;
import org.json.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
public class OAuthProtocolTest {
 static int checks=0;
 static void expect(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 interface Checked{void run()throws Exception;}
 static void rejects(Checked action,String label)throws Exception{boolean rejected=false;try{action.run();}catch(Exception e){rejected=true;}expect(rejected,label);}
 static String enc(byte[] v){return Base64.getUrlEncoder().withoutPadding().encodeToString(v);}
 static String jwt(JSONObject claims,KeyPair pair,String algorithm)throws Exception{String header=enc(new JSONObject().put("alg",algorithm).put("kid","test-key").toString().getBytes(StandardCharsets.UTF_8));String body=enc(claims.toString().getBytes(StandardCharsets.UTF_8));Signature s=Signature.getInstance("SHA256withRSA");s.initSign(pair.getPrivate());s.update((header+"."+body).getBytes(StandardCharsets.US_ASCII));return header+"."+body+"."+enc(s.sign());}
 static JSONObject stream(String type,JSONObject response)throws Exception{return new JSONObject().put("type",type).put("response",response);}
 static JSONObject consume(String text)throws Exception{return OAuthProtocol.consumeStream(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));}
 static JSONObject citation(Object start,Object end)throws Exception{return new JSONObject().put("type","url_citation").put("url","https://example.org/paper").put("title","Paper").put("start_index",start).put("end_index",end);}
 static JSONObject answer(String text,JSONArray annotations)throws Exception{return new JSONObject().put("output",new JSONArray().put(new JSONObject().put("type","message").put("content",new JSONArray().put(new JSONObject().put("type","output_text").put("text",text).put("annotations",annotations)))));}
 static JSONObject complete(JSONObject answer)throws Exception{return consume("data: "+stream("response.completed",answer)+"\n\n");}
 public static void main(String[] args)throws Exception{
  expect(OAuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk").equals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"),"RFC7636 PKCE test vector");
  String url=OAuthProtocol.authorization("dynamic_agent_client","urn:uuid:stable","http://127.0.0.1:1455/auth/callback","state","nonce","verifier","");expect(url.contains("agent_name_hint=Draft2Done")&&url.contains("chatgpt.tokens.use.direct")&&url.contains("resource.invoke"),"Dynamic registration scopes and actual app name");
  expect(!OAuthProtocol.authorization("oaiapp_saved","urn:uuid:stable","http://127.0.0.1:1455/auth/callback","state","nonce","verifier","").contains("agent_name_hint"),"Returning registration is reused");
  expect(!OAuthProtocol.same("one","two")&&OAuthProtocol.same("same","same"),"State comparisons");
  expect(OAuthProtocol.callbackHTML(true).contains("draft2done://auth/return")&&!OAuthProtocol.callbackHTML(true).contains("connected successfully"),"Callback provides return button without prematurely claiming account connection");
  KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair pair=generator.generateKeyPair();RSAPublicKey pub=(RSAPublicKey)pair.getPublic();byte[] modulus=pub.getModulus().toByteArray();if(modulus[0]==0)modulus=java.util.Arrays.copyOfRange(modulus,1,modulus.length);
  JSONObject jwks=new JSONObject().put("keys",new JSONArray().put(new JSONObject().put("kty","RSA").put("kid","test-key").put("alg","RS256").put("use","sig").put("n",enc(modulus)).put("e",enc(pub.getPublicExponent().toByteArray()))));
  long now=System.currentTimeMillis()/1000;JSONObject claims=new JSONObject().put("iss",OAuthProtocol.ISSUER).put("sub","subject").put("aud","oaiapp_test").put("nonce","nonce").put("exp",now+60).put("iat",now);
  String valid=jwt(claims,pair,"RS256");expect(OAuthProtocol.verifyIdentity(valid,jwks,"oaiapp_test","nonce",now).getString("sub").equals("subject"),"Valid signed identity");
  rejects(()->OAuthProtocol.verifyIdentity(valid,jwks,"wrong-client","nonce",now),"Wrong audience rejected");
  rejects(()->OAuthProtocol.verifyIdentity(valid,jwks,"oaiapp_test","wrong-nonce",now),"Wrong nonce rejected");
  rejects(()->OAuthProtocol.verifyIdentity(valid,jwks,"oaiapp_test","nonce",now+100),"Expired identity rejected");
  rejects(()->OAuthProtocol.verifyIdentity(jwt(claims,pair,"none"),jwks,"oaiapp_test","nonce",now),"Unsigned algorithm rejected");
  rejects(()->OAuthProtocol.verifyIdentity(valid,new JSONObject().put("keys",new JSONArray()),"oaiapp_test","nonce",now),"Unknown key rejected");
  KeyPair attacker=generator.generateKeyPair();rejects(()->OAuthProtocol.verifyIdentity(jwt(claims,attacker,"RS256"),jwks,"oaiapp_test","nonce",now),"Forged signature rejected");
  JSONObject answer=new JSONObject().put("id","response-test").put("output",new JSONArray().put(new JSONObject().put("type","message").put("content",new JSONArray().put(new JSONObject().put("type","output_text").put("text","A supported answer").put("annotations",new JSONArray().put(new JSONObject().put("type","url_citation").put("url","https://example.org/paper").put("title","Paper").put("start_index",2).put("end_index",11)))))));
  JSONObject result=consume("data: "+stream("response.completed",answer)+"\n\n");expect(result.getBoolean("completed")&&result.getJSONArray("sources").length()==1,"Complete SSE and cited source retained");
  expect(!result.getBoolean("webSearchUsed"),"A citation without a completed search tool is not confirmed browsing");
  JSONObject searched=new JSONObject(answer.toString());searched.getJSONArray("output").put(new JSONObject().put("type","web_search_call").put("status","completed"));
  expect(complete(searched).getBoolean("webSearchUsed"),"Explicitly completed terminal search tool confirms web use");
  for(String status:new String[]{"failed","in_progress","searching",""}){
   JSONObject unfinished=new JSONObject(answer.toString()),tool=new JSONObject().put("type","web_search_call");if(!status.isEmpty())tool.put("status",status);unfinished.getJSONArray("output").put(tool);
   expect(!complete(unfinished).getBoolean("webSearchUsed"),"Search tool does not confirm web use with status "+(status.isEmpty()?"missing":status));
  }
  JSONObject prose=complete(answer("See [EPA](https://www.epa.gov/green-infrastructure).",new JSONArray()));
  expect(!prose.getBoolean("webSearchUsed")&&prose.getJSONArray("sources").length()==0&&prose.getJSONArray("annotations").length()==0,"Prose-only Markdown URL is not browsing or a provider citation");
  Object[][] invalidSpans={{-1,4},{0,19},{8,4},{4,4},{JSONObject.NULL,4},{0,JSONObject.NULL},{"0",4},{0,"4"},{0.5,4},{0,4.5},{0,Long.MAX_VALUE}};
  for(int index=0;index<invalidSpans.length;index++){
   JSONObject malformed=complete(answer("A supported answer",new JSONArray().put(citation(invalidSpans[index][0],invalidSpans[index][1]))));
   expect(malformed.getJSONArray("annotations").length()==0&&malformed.getJSONArray("sources").length()==0&&malformed.getString("text").equals("A supported answer"),"Invalid citation span "+index+" is ignored without losing the readable answer");
  }
  JSONObject boundary=complete(answer("A supported answer",new JSONArray().put(citation(0,18))));
  expect(boundary.getJSONArray("annotations").length()==1&&boundary.getJSONArray("annotations").getJSONObject(0).getInt("end")==18,"Citation spanning the exact text boundaries remains valid");
  JSONObject multiple=answer("First paragraph.",new JSONArray());multiple.getJSONArray("output").getJSONObject(0).getJSONArray("content").put(new JSONObject().put("type","output_text").put("text","A supported answer").put("annotations",new JSONArray().put(citation(2,11))));
  JSONObject joined=complete(multiple),joinedCitation=joined.getJSONArray("annotations").getJSONObject(0);
  expect(joinedCitation.getInt("start")==20&&joinedCitation.getInt("end")==29&&joinedCitation.getInt("end")<=joined.getString("text").length(),"Valid citation offsets include earlier content and the paragraph separator");
  final byte[] completed=("data: "+stream("response.completed",answer)+"\n\n").getBytes(StandardCharsets.UTF_8);
  java.io.InputStream terminal=new java.io.FilterInputStream(new ByteArrayInputStream(completed)){
   @Override public int read(byte[] b,int offset,int length)throws java.io.IOException{if(available()==0)throw new java.io.IOException("The server kept its connection open after completion");return super.read(b,offset,length);}
  };
  expect(OAuthProtocol.consumeStream(terminal).getBoolean("completed"),"Completed responses return without waiting for connection close");
  rejects(()->consume("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Partial\"}\n\n"),"Interrupted SSE never reported successful");
  rejects(()->consume("data: "+stream("response.failed",new JSONObject().put("error",new JSONObject().put("code","subscription_sharing_usage_limit_exceeded")))+"\n\n"),"Plan limit terminal failure reported");
  try{consume("data: "+stream("response.failed",new JSONObject().put("error",new JSONObject().put("code","subscription_sharing_usage_limit_exceeded")))+"\n\n");throw new AssertionError("Limit must fail");}catch(IllegalStateException expected){expect(expected.getMessage().contains("subscription_sharing_usage_limit_exceeded")&&expected.getMessage().contains("Usage")&&!expected.getMessage().contains("reset time"),"Usage limit preserves code and directs user to usage controls without inventing reset timing");}
  rejects(()->consume("data: "+stream("response.incomplete",answer)+"\n\n"),"Incomplete response rejected");
  System.out.println("OAuth protocol checks passed: "+checks);
 }
}
