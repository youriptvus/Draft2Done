package io.draft2done.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Real native idle-client regression; blocks browser authorization and never supplies a code. */
final class SignInListenerChecks {
    interface Checker { void check(boolean condition,String label)throws Exception; }
    private static final int VAULT_LIMIT=1024*1024;
    private SignInListenerChecks() {}

    static void run(Instrumentation instrumentation,Activity activity,Checker check)throws Exception {
        run(instrumentation,activity,check,true);
    }

    /** false reproduces the old failure; true requires a listener that survives the idle client. */
    static void run(Instrumentation instrumentation,Activity activity,Checker check,boolean expectedResilient)throws Exception {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IOException("Listener QA requires the instrumentation worker thread.");
        Object connection=field(activity.getClass(),"chatgpt").get(activity);
        if(connection==null)throw new IOException("The native account connection is unavailable.");
        Class<?> type=connection.getClass();
        Object refreshLock=field(type,"refreshLock").get(connection);
        synchronized(refreshLock){runExclusive(instrumentation,activity,check,expectedResilient,connection,type);}
    }

    private static void runExclusive(Instrumentation instrumentation,Activity activity,Checker check,
                                     boolean expectedResilient,Object connection,Class<?> type)throws Exception {
        Field vaultField=field(type,"vault"),errorField=field(type,"vaultError"),listenerField=field(type,"listener");
        Method enqueue=method(type,"enqueue",String.class),handle=method(type,"handle",String.class,String.class,JSONObject.class),cancel=method(type,"cancel",String.class);
        // Old installed releases must not resolve a class that did not exist yet.
        Method activeService=expectedResilient?method(Class.forName("io.draft2done.app.SignInKeepAliveService",true,activity.getClassLoader()),"isActive",String.class):null;
        Object registry=field(type,"requests").get(connection);
        Method active=method(registry.getClass(),"activeExcept",String.class);
        Map<?,?> connections=(Map<?,?>)field(type,"connections").get(connection);
        synchronized(connection){
            if(listenerField.get(connection)!=null||!((Set<?>)active.invoke(registry,"")).isEmpty()||!connections.isEmpty())
                throw new IOException("Finish active account requests before isolated listener QA.");
        }
        File base=((AtomicFile)field(type,"file").get(connection)).getBaseFile().getCanonicalFile();
        if(!base.getParentFile().equals(activity.getFilesDir().getCanonicalFile())||!"chatgpt.enc".equals(base.getName()))
            throw new IOException("Refused an unexpected account-storage path.");
        File[] files={base,new File(base.getPath()+".bak"),new File(base.getPath()+".new")};
        byte[][] encrypted=new byte[files.length][];
        for(int i=0;i<files.length;i++)encrypted[i]=readOptional(files[i],VAULT_LIMIT);
        File[] workspace={new File(activity.getFilesDir(),"workspace.json"),new File(activity.getFilesDir(),"workspace.backup.json")};
        byte[][] workspaceHashes={fingerprint(workspace[0]),fingerprint(workspace[1])};
        SharedPreferences preferences=activity.getSharedPreferences("chatgpt-status",0);
        Map<String,Object> preferenceValues=copyPreferences(preferences);
        final JSONObject originalVault,testVault;
        final Object originalError;
        synchronized(connection){
            originalVault=(JSONObject)vaultField.get(connection);
            testVault=new JSONObject(originalVault.toString());
            originalError=errorField.get(connection);
        }
        AtomicInteger blocked=new AtomicInteger();CountDownLatch intercepted=new CountDownLatch(1);
        Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor(){
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
                Uri uri=intent.getData();
                if(Intent.ACTION_VIEW.equals(intent.getAction())&&uri!=null&&
                    "https".equalsIgnoreCase(uri.getScheme())&&"auth.openai.com".equalsIgnoreCase(uri.getHost())){
                    blocked.incrementAndGet();intercepted.countDown();
                    return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
                }
                return null;
            }
        };
        String request="qa-listener-"+UUID.randomUUID().toString();
        AtomicBoolean cancelRequested=new AtomicBoolean();
        ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"Draft2Done isolated listener QA");thread.setDaemon(true);return thread;});
        Future<String> future=null;Socket idle=null;ServerSocket observedListener=null;
        boolean installed=false,enqueued=false,joined=false,oldTimeout=false,resilient=false,serviceStarted=false;
        long idleElapsed=0;
        try {
            instrumentation.addMonitor(monitor);installed=true;
            synchronized(connection){vaultField.set(connection,testVault);}
            enqueue.invoke(connection,request);enqueued=true;
            future=worker.submit(()->{
                try{handle.invoke(connection,"signin",request,new JSONObject().put("account",""));return "UNEXPECTED_SUCCESS";}
                catch(Throwable failure){
                    if(cancelRequested.get())return "CANCELLED";
                    for(Throwable cause=failure;cause!=null;cause=cause.getCause())
                        if(cause instanceof SocketTimeoutException)return "SOCKET_TIMEOUT";
                    return "OTHER_FAILURE";
                }
            });
            if(!intercepted.await(8,TimeUnit.SECONDS))throw new IOException("Authorization launch interception was not observed; isolated listener check stopped.");
            long deadline=SystemClock.elapsedRealtime()+3000;
            while(SystemClock.elapsedRealtime()<deadline){
                Object value=listenerField.get(connection);
                if(value instanceof ServerSocket){observedListener=(ServerSocket)value;break;}
                if(future.isDone())break;
                Thread.sleep(25);
            }
            if(observedListener==null||observedListener.isClosed()||!observedListener.getInetAddress().isLoopbackAddress())
                throw new IOException("The isolated loopback listener was not available.");
            if(expectedResilient){
                long serviceDeadline=SystemClock.elapsedRealtime()+3000;
                do{
                    serviceStarted=Boolean.TRUE.equals(activeService.invoke(null,request));
                    if(serviceStarted||future.isDone())break;
                    Thread.sleep(50);
                }while(SystemClock.elapsedRealtime()<serviceDeadline);
            }
            idle=new Socket();
            idle.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),observedListener.getLocalPort()),1500);
            // Send no bytes. In the old implementation the first request-line
            // read times out after five seconds and escapes the entire sign-in.
            long started=SystemClock.elapsedRealtime(),until=started+7000;
            while(SystemClock.elapsedRealtime()<until&&!future.isDone())Thread.sleep(50);
            idleElapsed=SystemClock.elapsedRealtime()-started;
            oldTimeout=future.isDone()&&"SOCKET_TIMEOUT".equals(future.get(1,TimeUnit.SECONDS))&&
                idleElapsed>=4500&&(observedListener.isClosed()||listenerField.get(connection)==null);
            resilient=!future.isDone()&&idleElapsed>=6500&&!observedListener.isClosed()&&
                listenerField.get(connection)==observedListener;
        } finally {
            // Stop and join production code before restoring any account state.
            cancelRequested.set(true);
            if(idle!=null)try{idle.close();}catch(IOException ignored){}
            if(enqueued)try{cancel.invoke(connection,request);}catch(Exception ignored){}
            try{
                if(future!=null)future.get(10,TimeUnit.SECONDS);
                worker.shutdown();joined=worker.awaitTermination(3,TimeUnit.SECONDS);
            }catch(Exception ignored){
                if(observedListener!=null)try{observedListener.close();}catch(IOException ignoredAgain){}
                worker.shutdownNow();joined=worker.awaitTermination(5,TimeUnit.SECONDS);
            }
            if(!joined)throw new IOException("Isolated listener worker did not stop; account restoration cannot race a live worker.");
            // A cancelled worker may already have posted startActivity to the
            // main queue. Drain that queue while the strict monitor still blocks
            // authorization; never let a late browser launch escape cleanup.
            try{
                instrumentation.runOnMainSync(()->{});
                if(installed)instrumentation.removeMonitor(monitor);
            }finally{
                // Even a service/browser start or monitor failure must restore
                // account state after the production worker has finished. Keep
                // the monitor if draining failed, so queued auth stays blocked.
                synchronized(connection){
                    vaultField.set(connection,originalVault);errorField.set(connection,originalError);
                    try{for(int i=0;i<files.length;i++)restoreFile(files[i],encrypted[i]);}
                    finally{restorePreferences(preferences,preferenceValues);}
                }
            }
        }
        check.check(blocked.get()==1,"Exactly one authorization browser launch was intercepted before external navigation; no OAuth request was sent");
        check.check(idleElapsed>=4500,"A real idle loopback client remained connected across the old five-second read timeout");
        check.check(expectedResilient?resilient:oldTimeout,expectedResilient?
            "Fixed native sign-in remains pending with its listener active after the idle client timeout":
            "Old native sign-in reproduces the idle-client socket timeout and closes its listener");
        if(expectedResilient){
            boolean serviceStopped=false;long deadline=SystemClock.elapsedRealtime()+3000;
            do{
                serviceStopped=!Boolean.TRUE.equals(activeService.invoke(null,request));
                if(serviceStopped)break;
                Thread.sleep(50);
            }while(SystemClock.elapsedRealtime()<deadline);
            check.check(serviceStarted,"Actual sign-in foreground service is active for this exact pending attempt");
            check.check(serviceStopped,"Cancelling the isolated sign-in removes its foreground-service lease before return");
        }
        check.check(joined&&listenerField.get(connection)==null&&!((Set<?>)active.invoke(registry,"")).contains(request)&&!connections.containsKey(request),
            "The isolated sign-in request was cancelled or completed and its production registry/listener were cleaned");
        boolean exact=true;for(int i=0;i<files.length;i++)exact&=Arrays.equals(encrypted[i],readOptional(files[i],VAULT_LIMIT));
        check.check(exact,"Encrypted account file and atomic sidecars exactly match their original bytes or absence");
        check.check(vaultField.get(connection)==originalVault&&java.util.Objects.equals(originalError,errorField.get(connection)),
            "The untouched original in-memory account vault and prior error were restored");
        check.check(preferenceValues.equals(copyPreferences(preferences)),"Account-status preferences were restored after the production worker and pending apply writes finished");
        check.check(Arrays.equals(workspaceHashes[0],fingerprint(workspace[0]))&&Arrays.equals(workspaceHashes[1],fingerprint(workspace[1])),
            "The listener fixture leaves real workspace and previous-save backup bytes unchanged");
    }

    private static Field field(Class<?> type,String name)throws Exception{Field value=type.getDeclaredField(name);value.setAccessible(true);return value;}
    private static Method method(Class<?> type,String name,Class<?>... parameters)throws Exception{Method value=type.getDeclaredMethod(name,parameters);value.setAccessible(true);return value;}
    private static byte[] readOptional(File file,int maximum)throws IOException {
        if(!file.exists())return null;
        if(!file.isFile())throw new IOException("Account fixture snapshot requires an ordinary private file.");
        try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(out.size()+count>maximum)throw new IOException("Private account snapshot exceeds its bound.");out.write(buffer,0,count);}return out.toByteArray();
        }
    }
    private static void restoreFile(File target,byte[] original)throws Exception {
        if(Arrays.equals(original,readOptional(target,VAULT_LIMIT)))return;
        if(original==null){if(target.exists()&&!target.delete())throw new IOException("Could not restore private account-file absence.");return;}
        File temporary=new File(target.getParentFile(),"qa-signin-restore-"+UUID.randomUUID().toString()+".tmp");
        try{
            try(FileOutputStream output=new FileOutputStream(temporary)){output.write(original);output.getFD().sync();}
            if(!temporary.renameTo(target))throw new IOException("Could not restore the exact private account file.");
        }finally{if(temporary.exists()&&!temporary.delete())throw new IOException("Could not remove the private account restoration temporary file.");}
    }
    private static Map<String,Object> copyPreferences(SharedPreferences preferences)throws IOException {
        Map<String,Object> result=new HashMap<>();
        for(Map.Entry<String,?> entry:preferences.getAll().entrySet()){
            Object value=entry.getValue();
            if(value instanceof Set){Set<String> copy=new HashSet<>();for(Object item:(Set<?>)value){if(!(item instanceof String))throw new IOException("Unsupported private preference value.");copy.add((String)item);}value=copy;}
            else if(!(value instanceof String)&&!(value instanceof Boolean)&&!(value instanceof Integer)&&!(value instanceof Long)&&!(value instanceof Float))throw new IOException("Unsupported private preference value.");
            result.put(entry.getKey(),value);
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    private static void restorePreferences(SharedPreferences preferences,Map<String,Object> original)throws IOException {
        SharedPreferences.Editor editor=preferences.edit().clear();
        for(Map.Entry<String,Object> entry:original.entrySet()){
            String key=entry.getKey();Object value=entry.getValue();
            if(value instanceof String)editor.putString(key,(String)value);
            else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)editor.putInt(key,(Integer)value);
            else if(value instanceof Long)editor.putLong(key,(Long)value);
            else if(value instanceof Float)editor.putFloat(key,(Float)value);
            else if(value instanceof Set)editor.putStringSet(key,(Set<String>)value);
        }
        if(!editor.commit())throw new IOException("Could not restore private account-status preferences.");
    }
    private static byte[] fingerprint(File file)throws Exception {
        if(!file.exists())return null;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(FileInputStream input=new FileInputStream(file)){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);}return digest.digest();
    }
}
