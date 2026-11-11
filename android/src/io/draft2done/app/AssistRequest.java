package io.draft2done.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Strict translation of the reviewed writing payload into the ChatGPT plan contract. */
final class AssistRequest {
    static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
    static final int MAX_MESSAGE_CHARS = 200000;
    private AssistRequest() {}
    static JSONObject build(JSONObject payload) throws Exception {
        if(payload==null)throw new IllegalArgumentException("The writing request is missing.");
        fields(payload,"model","messages");
        if(payload.toString().getBytes(StandardCharsets.UTF_8).length>MAX_PAYLOAD_BYTES)throw new IllegalArgumentException("The writing request exceeds 1 MB. Nothing was sent or truncated.");
        Object rawModel=payload.opt("model");
        if(!(rawModel instanceof String)||!((String)rawModel).matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,149}"))throw new IllegalArgumentException("Choose an available ChatGPT model first.");
        JSONArray messages=payload.optJSONArray("messages");
        if(messages==null||messages.length()!=2)throw new IllegalArgumentException("Writing requests require one trusted system message followed by one user data message.");
        String instructions=message(messages,0,"system"),data=message(messages,1,"user");
        if(instructions.length()>24000||instructions.length()+data.length()>MAX_MESSAGE_CHARS)throw new IllegalArgumentException("The full writing request exceeds 200,000 characters. Nothing was sent or truncated.");
        // The app preview includes these trusted instructions. Do not append hidden
        // prompt text after the user's approval; only translate the wire shape.
        JSONObject body=new JSONObject().put("model",rawModel).put("instructions",instructions)
            .put("input",new JSONArray().put(new JSONObject().put("role","user").put("content",data)))
            .put("store",false).put("stream",true);
        if(body.toString().getBytes(StandardCharsets.UTF_8).length>MAX_PAYLOAD_BYTES)throw new IllegalArgumentException("The writing request exceeds 1 MB. Nothing was sent or truncated.");
        return body;
    }
    private static String message(JSONArray messages,int index,String role)throws Exception {
        Object raw=messages.get(index);
        if(!(raw instanceof JSONObject))throw new IllegalArgumentException("Writing messages must be objects.");
        JSONObject message=(JSONObject)raw;fields(message,"role","content");
        if(!role.equals(message.opt("role"))||!(message.opt("content") instanceof String))throw new IllegalArgumentException("Writing messages must contain the expected role and plain text content.");
        String content=message.getString("content");
        if(content.trim().isEmpty())throw new IllegalArgumentException("Writing messages cannot be empty.");
        return content;
    }
    private static void fields(JSONObject object,String... allowed)throws Exception {
        Set<String> names=new HashSet<>(Arrays.asList(allowed));Iterator<String> keys=object.keys();
        while(keys.hasNext())if(!names.contains(keys.next()))throw new IllegalArgumentException("Unsupported writing request field.");
    }
}
