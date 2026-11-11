package io.draft2done.test;

import android.app.Activity;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

/** Isolated native staging-store checks. Never opens the user's draft-input file. */
final class DraftInputStoreChecks {
    interface Checker { void check(boolean condition,String label)throws Exception; }
    private DraftInputStoreChecks() {}

    static void run(Activity activity,Checker check)throws Exception {
        File cache=activity.getCacheDir().getCanonicalFile();
        File directory=new File(cache,"qa-draft-input-"+UUID.randomUUID().toString()).getCanonicalFile();
        if(!cache.equals(directory.getParentFile())||!directory.mkdir())throw new IOException("Could not create isolated draft-input fixture.");
        String workspaceBefore=fingerprint(new File(activity.getFilesDir(),"workspace.json"));
        String backupBefore=fingerprint(new File(activity.getFilesDir(),"workspace.backup.json"));
        try {
            Class<?> type=Class.forName("io.draft2done.app.DraftInputStore",true,activity.getClassLoader());
            Constructor<?> constructor=type.getDeclaredConstructor(File.class);constructor.setAccessible(true);
            Method load=method(type,"load"),save=method(type,"save",String.class),history=method(type,"hasHistory");
            File target=new File(directory,"staging.json");
            Object store=constructor.newInstance(target);
            check.check("null".equals(load.invoke(store))&&!Boolean.TRUE.equals(history.invoke(store)),"Fresh isolated staging is empty and has no migration history");

            String text="\n\n  Fictional caf\u00e9 and \u03b1 with \ud83d\udcdd.\n\nLeading blanks, paragraphs and punctuation stay exact.\n";
            String first="\n  "+record(text,2,text.length()-1,24.5).toString()+"\n";
            check.check("ok".equals(save.invoke(store,first)),"Native staging save acknowledges a synchronous write");
            check.check(first.equals(read(target)),"Acknowledged staging is immediately present as exact UTF-8 file bytes");
            Object reopened=constructor.newInstance(target);
            check.check(first.equals(load.invoke(reopened)),"A new native store instance immediately reloads exact leading blank lines, Unicode and selection JSON");
            check.check(Boolean.TRUE.equals(history.invoke(reopened)),"Committed staging history survives creating a new store instance");
            String next=record("Second fictional paragraph.\n\nAnother line.",0,6,0).toString();
            save.invoke(store,next);
            check.check(next.equals(load.invoke(constructor.newInstance(target))),"A second acknowledged save replaces the staging record on disk");

            String[] invalid={
                "{broken","[]","{\"text\":42}","{\"text\":null}",
                record("abc",-1,1,0).toString(),record("abc",0,4,0).toString(),record("abc",2,1,0).toString(),
                record("abc",0.5,1,0).toString(),record("abc",0,1.5,0).toString(),
                record("abc",0,1,0).put("start","0").toString(),
                record("abc",0,1,-1).toString(),record("abc",0,1,0).put("scroll","0").toString(),
                record("abc",0,1,((long)Integer.MAX_VALUE)+1).toString(),
                "{\"text\":\"abc\",\"scroll\":1e309}"
            };
            String[] labels={"Malformed JSON","Non-object JSON","Non-string text","Null text","Negative selection","Selection beyond text","Reversed selection","Fractional selection start","Fractional selection end","String selection","Negative scroll","String scroll","Excessive scroll","Non-finite scroll"};
            for(int index=0;index<invalid.length;index++){
                check.check(rejected(save,store,invalid[index])&&next.equals(load.invoke(constructor.newInstance(target))),labels[index]+" is rejected without replacing the previously committed paste");
            }
            // One meaningful multibyte boundary case; no 24 MiB JSON allocation.
            check.check(rejectOversizedUtf8(save,store)&&next.equals(load.invoke(constructor.newInstance(target))),"Text below ten million characters but over 10 MiB UTF-8 is refused without losing the previous paste");

            File pending=new File(target.getPath()+".new");
            write(pending,"{interrupted");
            check.check(next.equals(load.invoke(constructor.newInstance(target))),"An unfinished AtomicFile new record cannot replace the last committed paste");
            File legacy=new File(target.getPath()+".bak");
            if(!target.renameTo(legacy))throw new IOException("Could not prepare isolated legacy backup.");
            write(target,"{corrupt primary");
            check.check(Boolean.TRUE.equals(history.invoke(constructor.newInstance(target))),"Legacy AtomicFile backup counts as prior staging history");
            check.check(next.equals(load.invoke(constructor.newInstance(target))),"AtomicFile restores a valid legacy backup over a corrupt primary");

            File blocked=block(pending);
            boolean failedSave=rejected(save,store,first),failedClear=rejected(save,store,"");
            check.check(failedSave&&failedClear&&next.equals(read(target)),"Blocked atomic-write directory reports save and clear failure while retaining committed bytes");
            unblock(blocked);
            check.check(next.equals(load.invoke(constructor.newInstance(target))),"A new instance still reads the committed paste after write failures");

            if(!target.renameTo(legacy))throw new IOException("Could not prepare isolated recovery write failure.");
            blocked=block(pending);
            boolean failedAfterBackup=rejected(save,store,first);
            unblock(blocked);
            check.check(failedAfterBackup&&next.equals(load.invoke(constructor.newInstance(target))),"A failed new write preserves recoverable data from the previous AtomicFile backup");

            check.check("ok".equals(save.invoke(store,""))&&"null".equals(load.invoke(constructor.newInstance(target))),"Explicit clear commits an empty tombstone rather than old paste contents");
            check.check("null".equals(read(target))&&Boolean.TRUE.equals(history.invoke(constructor.newInstance(target))),"Cleared tombstone and history persist on disk so stale legacy input cannot migrate again");
            write(pending,"{interrupted after clear");
            check.check("null".equals(load.invoke(constructor.newInstance(target))),"An interrupted later write cannot resurrect content over a committed clear");
            File historyOnly=new File(directory,"history-only.json");
            write(new File(historyOnly.getPath()+".new"),"{interrupted first save");
            check.check(Boolean.TRUE.equals(history.invoke(constructor.newInstance(historyOnly))),"An unfinished first write records history without pretending it was committed");

            // A nonempty directory at the exact base path can make AtomicFile's
            // rename fail. The API must not acknowledge an uncommitted save.
            File badTarget=new File(directory,"blocked-target");
            if(!badTarget.mkdir())throw new IOException("Could not prepare isolated blocked target.");
            write(new File(badTarget,"blocker"),"Fictional directory blocker");
            File retainedBackup=new File(badTarget.getPath()+".bak");
            write(retainedBackup,next);
            Object failingStore=constructor.newInstance(badTarget);
            check.check(rejected(save,failingStore,first)&&next.equals(read(retainedBackup)),"A blocked base-directory commit reports failure and leaves the previous backup intact");

            check.check(workspaceBefore.equals(fingerprint(new File(activity.getFilesDir(),"workspace.json")))&&backupBefore.equals(fingerprint(new File(activity.getFilesDir(),"workspace.backup.json"))),"Isolated draft-input checks leave real workspace and backup bytes unchanged");
        } finally { cleanup(directory,cache); }
    }

    private static JSONObject record(String text,Number start,Number end,Number scroll)throws Exception {
        return new JSONObject().put("text",text).put("start",start).put("end",end).put("scroll",scroll);
    }
    private static boolean rejected(Method save,Object store,String value)throws Exception {
        try{save.invoke(store,value);return false;}
        catch(InvocationTargetException failure){if(failure.getCause() instanceof Exception)return true;throw failure;}
    }
    private static boolean rejectOversizedUtf8(Method save,Object store)throws Exception {
        char[] text=new char[(10*1024*1024)/3+1];Arrays.fill(text,'\u4e2d');
        String raw="{\"text\":\""+new String(text)+"\"}";
        return rejected(save,store,raw);
    }
    private static Method method(Class<?> type,String name,Class<?>... arguments)throws Exception {
        Method value=type.getDeclaredMethod(name,arguments);value.setAccessible(true);return value;
    }
    private static String read(File file)throws Exception {
        byte[] bytes=new byte[(int)file.length()];
        try(FileInputStream input=new FileInputStream(file)){
            int offset=0,count;while(offset<bytes.length&&(count=input.read(bytes,offset,bytes.length-offset))!=-1)offset+=count;
            if(offset!=bytes.length)throw new IOException("Isolated fixture read was incomplete.");
        }
        return new String(bytes,StandardCharsets.UTF_8);
    }
    private static void write(File file,String value)throws Exception {
        try(FileOutputStream out=new FileOutputStream(file)){out.write(value.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
    }
    private static File block(File directory)throws Exception {
        if(!directory.mkdir())throw new IOException("Could not prepare isolated atomic-write blocker.");
        write(new File(directory,"blocker"),"Fictional write blocker");return directory;
    }
    private static void unblock(File directory)throws IOException {
        if(!new File(directory,"blocker").delete()||!directory.delete())throw new IOException("Could not remove isolated write blocker.");
    }
    private static String fingerprint(File file)throws Exception {
        if(!file.exists())return "absent";
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(FileInputStream input=new FileInputStream(file)){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);}
        StringBuilder value=new StringBuilder();for(byte item:digest.digest())value.append(String.format(java.util.Locale.ROOT,"%02x",item&255));return value.toString();
    }
    private static void cleanup(File directory,File cache)throws IOException {
        if(!directory.getCanonicalFile().getParentFile().equals(cache.getCanonicalFile())||!directory.getName().startsWith("qa-draft-input-"))throw new IOException("Refused cleanup outside isolated draft-input cache.");
        File[] children=directory.listFiles();if(children==null)throw new IOException("Could not list isolated fixtures.");
        for(File child:children){
            if(!child.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile()))throw new IOException("Refused linked fixture cleanup.");
            if(child.isDirectory()){
                if(!child.getName().equals("blocked-target")&&!child.getName().endsWith(".new"))throw new IOException("Refused unknown fixture directory cleanup.");
                File[] entries=child.listFiles();if(entries==null)throw new IOException("Could not list isolated blocker.");
                for(File entry:entries)if(!entry.getName().equals("blocker")||!entry.getCanonicalFile().getParentFile().equals(child.getCanonicalFile())||!entry.isFile()||!entry.delete())throw new IOException("Refused unknown blocker cleanup.");
            }
            if(!child.delete())throw new IOException("Could not delete isolated fixture.");
        }
        if(!directory.delete())throw new IOException("Could not remove isolated draft-input cache.");
    }
}
