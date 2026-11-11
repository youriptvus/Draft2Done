package io.draft2done.app;

import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Synchronously committed private staging, independent of workspace Autosave. */
final class DraftInputStore {
    static final int TEXT_LIMIT=10*1024*1024;
    static final int RECORD_LIMIT=24*1024*1024;
    private final AtomicFile file;
    DraftInputStore(File target){file=new AtomicFile(target);}

    synchronized boolean hasHistory(){
        File base=file.getBaseFile();
        return base.exists()||new File(base.getPath()+".bak").exists()||new File(base.getPath()+".new").exists();
    }

    synchronized String load()throws Exception {
        if(!hasHistory())return "null";
        checkFiles();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream input=file.openRead()){
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1){
                if(bytes.size()+count>RECORD_LIMIT)throw new IOException("Saved paste exceeds the private staging limit. Keep a backup before replacing it.");
                bytes.write(buffer,0,count);
            }
        }
        String raw=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        validate(raw);
        return raw;
    }

    synchronized String save(String input)throws Exception {
        // A committed empty tombstone prevents asynchronous legacy WebView data
        // from resurrecting a paste after successful project creation or clear.
        String raw=input==null||input.isEmpty()?"null":input;
        validate(raw);
        checkFiles();
        FileOutputStream output=null;
        try{
            output=file.startWrite();output.write(raw.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
            file.finishWrite(output);
            output=null;
            // AtomicFile can log a rename failure without throwing. Never call
            // the input saved until the committed base contains the exact record.
            if(!file.getBaseFile().isFile()||!raw.equals(load()))throw new IOException("The paste could not be committed. Keep a copy before leaving the app.");
        }catch(Exception failure){if(output!=null)file.failWrite(output);throw failure;}
        return "ok";
    }

    private void checkFiles()throws IOException {
        File base=file.getBaseFile();
        for(String suffix:new String[]{"",".bak",".new"}){
            File candidate=new File(base.getPath()+suffix);
            if(candidate.exists()&&!candidate.isFile())throw new IOException("Private draft storage is unavailable. Its existing contents were retained.");
        }
    }

    private static void validate(String raw)throws Exception {
        if(raw.getBytes(StandardCharsets.UTF_8).length>RECORD_LIMIT)throw new IOException("This paste is too large to stage safely. Keep an external copy and split the paper.");
        if("null".equals(raw))return;
        JSONObject value=new JSONObject(raw);
        Object text=value.opt("text");
        if(!(text instanceof String))throw new IOException("Saved paste is not a valid text record. Keep a copy before replacing it.");
        String draft=(String)text;
        if(draft.getBytes(StandardCharsets.UTF_8).length>TEXT_LIMIT)throw new IOException("This paste exceeds 10 MB. Split it into smaller papers.");
        double start=number(value,"start",0,draft.length(),true),end=number(value,"end",0,draft.length(),true);
        if(end<start)throw new IOException("Saved paste selection is invalid.");
        number(value,"scroll",0,Integer.MAX_VALUE,false);
    }

    private static double number(JSONObject record,String name,double minimum,double maximum,boolean integer)throws IOException {
        if(!record.has(name))return minimum;
        Object raw=record.opt(name);
        if(!(raw instanceof Number))throw new IOException("Saved paste "+name+" is invalid.");
        double value=((Number)raw).doubleValue();
        if(Double.isNaN(value)||Double.isInfinite(value)||value<minimum||value>maximum||(integer&&value!=Math.floor(value)))
            throw new IOException("Saved paste "+name+" is invalid.");
        return value;
    }
}
