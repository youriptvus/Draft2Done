package io.draft2done.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import java.io.InterruptedIOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Temporary lifetime for a visible, user-started browser OAuth callback wait. */
public final class SignInKeepAliveService extends Service {
    private static final Object LOCK=new Object();
    private static final String CHANNEL="chatgpt-signin";
    private static final String START="io.draft2done.app.SIGNIN_START";
    private static final String CANCEL="io.draft2done.app.SIGNIN_CANCEL";
    private static final String GENERATION="generation";
    private static final int NOTIFICATION=14014;
    private static final long MAX_DURATION=10*60*1000L;
    private static Attempt current;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Attempt owned;

    private static final class Attempt {
        final String requestId,generation=UUID.randomUUID().toString();
        final long deadline=SystemClock.elapsedRealtime()+MAX_DURATION;
        final CountDownLatch ready=new CountDownLatch(1);
        Runnable cancel,expiry;
        SignInKeepAliveService owner;
        Exception failure;
        boolean foreground,closed;
        Attempt(String requestId,Runnable cancel){this.requestId=requestId;this.cancel=cancel;}
    }

    /** The sign-in worker owns this lease and closes it in its existing finally. */
    public static final class Lease implements AutoCloseable {
        private final Attempt attempt;
        private Lease(Attempt attempt){this.attempt=attempt;}
        @Override public void close(){finish(attempt,false,null);}
    }

    /** Start while the Activity is visible; never block the Android main thread. */
    public static Lease start(Activity activity,String requestId,Runnable cancel)throws Exception {
        if(activity==null||requestId==null||requestId.isEmpty()||requestId.length()>120||cancel==null)
            throw new IllegalArgumentException("ChatGPT sign-in could not be prepared.");
        if(Looper.myLooper()==Looper.getMainLooper())
            throw new IllegalStateException("ChatGPT sign-in must be prepared by its background worker.");
        final Attempt attempt=new Attempt(requestId,cancel);
        synchronized(LOCK){
            if(current!=null&&!current.closed)throw new IllegalStateException("A ChatGPT sign-in is already open.");
            current=attempt;
        }
        try {
            activity.runOnUiThread(()->{
                synchronized(LOCK){if(current!=attempt||attempt.closed)return;}
                try {
                    if(activity.isFinishing()||activity.isDestroyed())throw new IllegalStateException("The sign-in screen is no longer open.");
                    Intent intent=new Intent(activity,SignInKeepAliveService.class).setAction(START).putExtra(GENERATION,attempt.generation);
                    activity.startForegroundService(intent);
                }catch(Exception error){finish(attempt,false,error);}
            });
            if(!attempt.ready.await(10,TimeUnit.SECONDS))
                throw new IllegalStateException("ChatGPT sign-in could not stay active. Return to the app and try again.");
            synchronized(LOCK){
                if(attempt.failure!=null)throw new IllegalStateException("ChatGPT sign-in could not stay active. Return to the app and try again.",attempt.failure);
                if(current!=attempt||attempt.closed||!attempt.foreground)
                    throw new InterruptedIOException("ChatGPT sign-in was cancelled before the browser opened.");
            }
            return new Lease(attempt);
        } catch(Exception error) {
            finish(attempt,false,error);
            throw error;
        }
    }

    /** Sanitized per-attempt state for native regression checks. */
    static boolean isActive(String requestId){
        Attempt attempt;
        synchronized(LOCK){attempt=current;if(attempt==null||requestId==null||!requestId.equals(attempt.requestId)||attempt.closed||!attempt.foreground)return false;}
        if(SystemClock.elapsedRealtime()>=attempt.deadline){finish(attempt,true,null);return false;}
        return true;
    }

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?"":intent.getAction();
        String generation=intent==null?null:intent.getStringExtra(GENERATION);
        Attempt attempt;
        synchronized(LOCK){attempt=current;}
        if(attempt==null||attempt.closed||!attempt.generation.equals(generation)){
            if(owned==null)stopSelf(startId);
            return START_NOT_STICKY;
        }
        if(CANCEL.equals(action)){
            finish(attempt,true,null);
            return START_NOT_STICKY;
        }
        if(!START.equals(action)){
            if(owned==null)stopSelf(startId);
            return START_NOT_STICKY;
        }
        try {
            NotificationManager notifications=getSystemService(NotificationManager.class);
            NotificationChannel channel=new NotificationChannel(CHANNEL,"ChatGPT sign-in",NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps your current ChatGPT browser sign-in available until you finish or cancel.");
            notifications.createNotificationChannel(channel);
            Intent back=new Intent().setClassName(this,getPackageName()+".MainActivity").setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse("draft2done://auth/return"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent backAction=PendingIntent.getActivity(this,0,back,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            // Only an opaque service generation identifies the Cancel action.
            // OAuth state, codes, verifier, account and request IDs stay in memory.
            Intent stop=new Intent(this,SignInKeepAliveService.class).setAction(CANCEL)
                .setData(Uri.parse("draft2done-signin://cancel/"+attempt.generation))
                .putExtra(GENERATION,attempt.generation);
            PendingIntent cancelAction=PendingIntent.getService(this,0,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Connecting to ChatGPT")
                .setContentText("Complete sign-in in your browser, then return to Draft2Done.")
                .setContentIntent(backAction).setOngoing(true).setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Cancel",cancelAction).build());
            if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            Notification notification=builder.build();
            if(Build.VERSION.SDK_INT>=34)startForeground(NOTIFICATION,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTIFICATION,notification);
            synchronized(LOCK){
                if(current!=attempt||attempt.closed){stopUnowned(attempt);return START_NOT_STICKY;}
                owned=attempt;attempt.owner=this;attempt.foreground=true;
                attempt.expiry=()->finish(attempt,true,null);
                handler.postDelayed(attempt.expiry,Math.max(0,attempt.deadline-SystemClock.elapsedRealtime()));
                attempt.ready.countDown();
            }
        }catch(Exception error){finish(attempt,false,error);stopUnowned(attempt);}
        return START_NOT_STICKY;
    }

    private static void finish(Attempt attempt,boolean cancel,Exception failure){
        SignInKeepAliveService owner;Runnable callback;
        synchronized(LOCK){
            if(attempt.closed)return;
            attempt.closed=true;attempt.foreground=false;
            if(failure!=null)attempt.failure=failure;
            if(current==attempt)current=null;
            owner=attempt.owner;attempt.owner=null;
            callback=cancel?attempt.cancel:null;attempt.cancel=null;
            attempt.ready.countDown();
        }
        try{if(callback!=null)callback.run();}
        catch(RuntimeException ignored){/* Cleanup still runs if cancellation already completed. */}
        finally{if(owner!=null)owner.handler.post(()->owner.stopUnowned(attempt));}
    }

    /** Main-thread cleanup is identity-checked so an old lease cannot stop a retry. */
    private void stopUnowned(Attempt attempt){
        synchronized(LOCK){
            if(attempt.expiry!=null)handler.removeCallbacks(attempt.expiry);
            if(owned!=null&&owned!=attempt)return;
            if(owned==attempt)owned=null;
            if(current!=null&&!current.closed)return;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }

    @Override public void onTaskRemoved(Intent rootIntent){
        Attempt attempt=owned;if(attempt!=null)finish(attempt,true,null);
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy(){
        Attempt attempt=owned;owned=null;
        if(attempt!=null){if(attempt.expiry!=null)handler.removeCallbacks(attempt.expiry);finish(attempt,true,null);}
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
