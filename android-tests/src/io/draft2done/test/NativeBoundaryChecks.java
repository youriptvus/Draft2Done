package io.draft2done.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Fault and byte-boundary checks use only disposable, app-owned cache fixtures. */
final class NativeBoundaryChecks {
    interface Checker { void check(boolean passed, String description) throws Exception; }
    interface Evaluator { String evaluate(String js) throws Exception; }

    /** Carry forward the actual earlier live-test quota result; never makes an API request. */
    static void recordObservedQuota(Activity activity) throws Exception {
        Field connectionField=activity.getClass().getDeclaredField("chatgpt");connectionField.setAccessible(true);
        Object connection=connectionField.get(activity);
        Method selected=connection.getClass().getDeclaredMethod("selected");selected.setAccessible(true);
        JSONObject account=(JSONObject)selected.invoke(connection);
        Class<?> status=Class.forName("io.draft2done.app.ChatGPTResearchStatus",true,activity.getClassLoader());
        Field message=status.getDeclaredField("QUOTA_MESSAGE");message.setAccessible(true);
        Method update=connection.getClass().getDeclaredMethod("updateResearchError",String.class,String.class,String.class,String.class);update.setAccessible(true);
        update.invoke(connection,"observed-live-quota",account.getString("client_id"),account.getString("subject"),message.get(null));
    }

    static void recovery(Activity activity, Checker check) throws Exception {
        File directory = fixtureDirectory(activity, "recovery");
        File primary = new File(directory, "workspace.json");
        File backup = new File(directory, "workspace.backup.json");
        Class<?> type = Class.forName("io.draft2done.app.WorkspaceStore", true, activity.getClassLoader());
        Constructor<?> constructor = type.getDeclaredConstructor(File.class, int.class); constructor.setAccessible(true);
        Object store = constructor.newInstance(directory, 4096);
        Method load = type.getDeclaredMethod("load"); load.setAccessible(true);
        Method save = type.getDeclaredMethod("save", String.class); save.setAccessible(true);
        String first = "{\"draft\":\"Fictional QA: caf\u00e9 and \u03b1\"}", second = "{\"draft\":\"Second fictional revision\"}";
        try {
            Object value = load.invoke(store);
            check.check("{}".equals(field(value, "json")) && !bool(value, "unreadable"), "A new workspace starts empty without a recovery error");
            save.invoke(store, first);
            check.check(first.equals(field(load.invoke(store), "json")), "Atomic workspace save preserves Unicode text");
            save.invoke(store, second);
            check.check(first.equals(read(backup)), "The previous valid save is retained for recovery");
            write(primary, "{broken");
            value = load.invoke(store);
            check.check(first.equals(field(value, "json")) && bool(value, "recovered"), "Corrupt primary workspace recovers the previous valid save");

            File blocked = blockNextWrite(directory);
            check.check(failsSave(save, store, second), "A blocked atomic write reports a failure");
            value = load.invoke(store);
            check.check(first.equals(read(backup)) && first.equals(field(value, "json")), "Failed save after recovery preserves the last good backup");
            removeFixture(blocked, directory);
            save.invoke(store, second);
            check.check(second.equals(field(load.invoke(store), "json")) && first.equals(read(backup)), "Saving a recovered workspace repairs primary without replacing backup with corrupt data");

            if (!primary.delete()) throw new IOException("Could not remove QA primary fixture");
            value = load.invoke(store);
            check.check(first.equals(field(value, "json")) && bool(value, "recovered"), "Missing primary workspace recovers an existing backup");
            save.invoke(store, second);
            check.check(second.equals(field(load.invoke(store), "json")), "A recovered missing primary can be saved again");

            try (OutputStream out = new FileOutputStream(primary)) { out.write(new byte[]{(byte)0xc3, 0x28}); }
            check.check(first.equals(field(load.invoke(store), "json")), "Invalid UTF-8 workspace bytes trigger valid-backup recovery");
            write(backup, "{broken too");
            value = load.invoke(store);
            check.check(bool(value, "unreadable") && field(value, "json") == null, "Two corrupt workspace files report unreadable data instead of an empty workspace");
            save.invoke(store, first);
            check.check(first.equals(field(load.invoke(store), "json")), "An explicit valid restore can replace unreadable files");
            check.check(failsSave(save, store, "{broken") && first.equals(field(load.invoke(store), "json")), "Invalid JSON is rejected without replacing saved work");
            StringBuilder oversized = new StringBuilder("{\"draft\":\"");
            for (int i=0; i<2100; i++) oversized.append('\u00e9'); oversized.append("\"}");
            check.check(failsSave(save, store, oversized.toString()) && first.equals(field(load.invoke(store), "json")), "UTF-8 byte limits protect saved work even when character count fits");

            blocked = blockNextWrite(directory);
            check.check(failsSave(save, store, second) && first.equals(field(load.invoke(store), "json")) && first.equals(read(backup)), "Failed normal overwrite preserves both current data and a valid recovery copy");
            removeFixture(blocked, directory);
            write(new File(directory, "workspace.json.new"), "{interrupted write");
            check.check(first.equals(field(load.invoke(store), "json")), "An unfinished atomic-write file does not replace the last complete save");
            File legacy = new File(directory, "workspace.json.bak");
            if (!primary.renameTo(legacy)) throw new IOException("Could not prepare legacy atomic fixture");
            check.check(first.equals(field(load.invoke(store), "json")), "Android AtomicFile legacy backup is restored before reading");
        } finally { removeFixture(directory, activity.getCacheDir()); }
    }

    static void documents(Instrumentation instrumentation, Activity activity, Evaluator evaluate, Checker check) throws Exception {
        File directory = fixtureDirectory(activity, "documents");
        File input = new File(directory, "fictional-input.txt"), output = new File(directory, "fictional-output.txt");
        byte[] bytes = "Draft2Done fictional native roundtrip.\nUnicode: caf\u00e9, \u03b1 and \ud83d\udcdd.\n".getBytes(StandardCharsets.UTF_8);
        Method callback = activity.getClass().getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class); callback.setAccessible(true);
        try {
            writeBytes(input, bytes);
            evaluate.evaluate("window.__qaImported=null;window.__qaNotice='';window.__qaOriginalImported=window.NativeEvents.imported;window.__qaOriginalNotice=window.NativeEvents.notice;window.NativeEvents.imported=p=>window.__qaImported=p;window.NativeEvents.notice=m=>window.__qaNotice=String(m);return true;");
            activityResult(instrumentation, activity, callback, 101, input);
            waitJS(evaluate, "!!window.__qaImported", 15000);
            String encoded = evaluate.evaluate("window.__qaImported.base64");
            String base64 = new org.json.JSONTokener(encoded).nextValue().toString();
            check.check(Arrays.equals(bytes, Base64.decode(base64, Base64.DEFAULT)), "Native import callback transfers exact Unicode document bytes");

            String exportBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            evaluate.evaluate("window.DraftNative.exportDocument('Draft2Done-fictional-QA.txt','text/plain'," + JSONObject.quote(exportBase64) + ");return true;");
            Field pending = activity.getClass().getDeclaredField("pendingExport"); pending.setAccessible(true);
            long deadline = System.currentTimeMillis()+15000;
            while (pending.get(activity)==null && System.currentTimeMillis()<deadline) Thread.sleep(50);
            if (pending.get(activity)==null) throw new IOException("Native export did not prepare bytes");
            // The picker UI was tested separately. Supply only our cache URI as its result.
            activityResult(instrumentation, activity, callback, 102, output);
            deadline=System.currentTimeMillis()+15000;
            while ((!output.exists() || output.length()!=bytes.length) && System.currentTimeMillis()<deadline) Thread.sleep(50);
            check.check(output.exists() && Arrays.equals(bytes, readBytes(output)), "Native export callback writes exact document bytes to the selected destination");
            instrumentation.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK); Thread.sleep(500);

            evaluate.evaluate("window.__qaImported=null;window.__qaNotice='';return true;");
            File oversized = new File(directory,"oversized-input.txt");
            try (RandomAccessFile file=new RandomAccessFile(oversized,"rw")) { file.setLength(10*1024*1024+1); }
            activityResult(instrumentation, activity, callback, 101, oversized);
            waitJS(evaluate, "window.__qaNotice.startsWith('Import failed:')",15000);
            check.check("true".equals(evaluate.evaluate("window.__qaImported===null&&window.__qaNotice.includes('10 MB')")), "Oversized native import is rejected before passing document bytes to the UI");

            evaluate.evaluate("window.__qaNotice='';window.DraftNative.exportDocument('fictional.txt','text/plain','!invalid base64!');return true;");
            waitJS(evaluate,"window.__qaNotice.startsWith('Export failed:')",10000);
            check.check(pending.get(activity)==null, "Invalid native export encoding is rejected without opening a save operation");
        } finally {
            evaluate.evaluate("if(window.__qaOriginalImported)window.NativeEvents.imported=window.__qaOriginalImported;if(window.__qaOriginalNotice)window.NativeEvents.notice=window.__qaOriginalNotice;delete window.__qaImported;delete window.__qaNotice;delete window.__qaOriginalImported;delete window.__qaOriginalNotice;return true;");
            removeFixture(directory,activity.getCacheDir());
        }
    }

    private static boolean failsSave(Method save,Object store,String text)throws Exception {
        try { save.invoke(store,text);return false; } catch(InvocationTargetException expected) { return true; }
    }
    private static Object field(Object value,String name)throws Exception { Field field=value.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(value); }
    private static boolean bool(Object value,String name)throws Exception { return Boolean.TRUE.equals(field(value,name)); }
    private static File fixtureDirectory(Activity activity,String mode)throws IOException {
        File directory=new File(activity.getCacheDir(),"qa-native-"+mode+"-"+System.nanoTime());
        if(!directory.mkdirs())throw new IOException("Could not create disposable QA fixture");return directory;
    }
    private static File blockNextWrite(File directory)throws IOException {
        File blocked=new File(directory,"workspace.json.new");if(!blocked.mkdir())throw new IOException("Could not block QA write");write(blocked.toPath().resolve("blocker").toFile(),"QA fault injection");return blocked;
    }
    private static void activityResult(Instrumentation instrumentation,Activity activity,Method callback,int code,File file)throws Exception {
        Throwable[] error={null};instrumentation.runOnMainSync(()->{try{callback.invoke(activity,code,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file)));}catch(Throwable failure){error[0]=failure;}});
        if(error[0]!=null)throw new IOException("Native document callback failed",error[0]);
    }
    private static void waitJS(Evaluator evaluate,String condition,int timeout)throws Exception {
        long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){if("true".equals(evaluate.evaluate(condition)))return;Thread.sleep(100);}throw new IOException("Native QA callback timed out");
    }
    private static String read(File file)throws IOException { return new String(readBytes(file),StandardCharsets.UTF_8); }
    private static byte[] readBytes(File file)throws IOException {try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();} }
    private static void write(File file,String text)throws IOException {writeBytes(file,text.getBytes(StandardCharsets.UTF_8));}
    private static void writeBytes(File file,byte[] bytes)throws IOException {try(OutputStream out=new FileOutputStream(file)){out.write(bytes);} }
    private static void removeFixture(File file,File parent)throws IOException {
        String path=file.getCanonicalPath(), root=parent.getCanonicalPath()+File.separator;
        if(!path.startsWith(root))throw new IOException("Refused to remove a file outside the disposable QA directory");
        if(file.isDirectory()){File[] files=file.listFiles();if(files!=null)for(File child:files)removeFixture(child,file);}
        if(file.exists()&&!file.delete())throw new IOException("Could not remove disposable QA fixture");
    }
}
