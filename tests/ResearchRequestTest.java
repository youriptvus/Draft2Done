package io.draft2done.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Executes the exact native research policy without Activity construction or HTTP. */
public final class ResearchRequestTest {
    private static int checks;
    private static void expect(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static JSONObject research()throws Exception{
        return new JSONObject().put("model","actual-catalog-slug").put("store",false).put("stream",true)
            .put("instructions","Fictional research app instructions.")
            .put("input",new JSONArray().put(new JSONObject().put("role","user")
                .put("content",new JSONObject().put("question","Fictional public research question?")
                    .put("source_excerpts",new JSONArray()).toString())));
    }
    private static JSONObject writing()throws Exception{
        return AssistRequest.build(new JSONObject().put("model","actual-catalog-slug")
            .put("messages",new JSONArray()
                .put(new JSONObject().put("role","system").put("content","Reviewed writing instructions."))
                .put(new JSONObject().put("role","user").put("content","Full fictional draft and requirements."))));
    }
    public static void main(String[] arguments)throws Exception{
        Method configure=ChatGPTConnection.class.getDeclaredMethod("configureResearchSearch",JSONObject.class,boolean.class);
        configure.setAccessible(true);
        JSONObject writingBefore=writing();
        JSONObject searched=research();
        String originalInput=searched.getJSONArray("input").toString();
        configure.invoke(null,searched,true);
        expect("required".equals(searched.getString("tool_choice")),"Selected web research requires tool use");
        expect(searched.getJSONArray("tools").length()==1&&"web_search".equals(searched.getJSONArray("tools").getJSONObject(0).getString("type")),"Web research offers only the selected search tool");
        expect(originalInput.equals(searched.getJSONArray("input").toString()),"Search policy preserves the approved question and evidence");
        expect(searched.getBoolean("stream")&&!searched.getBoolean("store"),"Web research remains streamed and nonstored");
        JSONObject queryOnly=research();
        String unchanged=queryOnly.toString();
        configure.invoke(null,queryOnly,false);
        expect(unchanged.equals(queryOnly.toString())&&!queryOnly.has("tools")&&!queryOnly.has("tool_choice"),"Query-only research is unchanged and exposes no search policy");
        JSONObject writingAfter=writing();
        Set<String> writingFields=new HashSet<>();writingAfter.keys().forEachRemaining(writingFields::add);
        expect(writingFields.equals(new HashSet<>(Arrays.asList("model","instructions","input","store","stream"))),"Writing does not gain tools or tool_choice");
        expect(writingBefore.toString().equals(writingAfter.toString()),"Research policy has no shared state that changes Helper/Complete/regeneration requests");
        expect(!queryOnly.has("tool_choice"),"Reusing research policy does not leak required search into another request");
        Method validate=ChatGPTConnection.class.getDeclaredMethod("validateResearchResult",JSONObject.class,boolean.class);
        validate.setAccessible(true);
        JSONObject unverified=new JSONObject().put("text","A URL in prose is not browsing evidence.").put("webSearchUsed",false);
        String unverifiedBefore=unverified.toString();validate.invoke(null,unverified,false);
        expect(unverifiedBefore.equals(unverified.toString()),"Query-only research retains its explicitly unverified completed result");
        boolean blocked=false;try{validate.invoke(null,unverified,true);}catch(java.lang.reflect.InvocationTargetException error){blocked=error.getCause() instanceof IllegalStateException;}
        expect(blocked,"Requested web research rejects a completed response without confirmed tool use before saving");
        blocked=false;try{validate.invoke(null,new JSONObject().put("text","No tool metadata"),true);}catch(java.lang.reflect.InvocationTargetException error){blocked=error.getCause() instanceof IllegalStateException;}
        expect(blocked,"Missing tool metadata cannot silently satisfy requested search");
        JSONObject confirmed=new JSONObject().put("webSearchUsed",true).put("text","Completed search result");String confirmedBefore=confirmed.toString();validate.invoke(null,confirmed,true);
        expect(confirmedBefore.equals(confirmed.toString()),"Confirmed tool evidence is accepted without inventing or modifying citations");
        System.out.println("Research request boundary checks passed: "+checks);
    }
}
