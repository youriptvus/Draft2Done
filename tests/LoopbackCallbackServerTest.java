package io.draft2done.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Real loopback TCP only. Synthetic callbacks never contact an identity provider. */
public final class LoopbackCallbackServerTest {
    private static int checks;
    private static final String STATE="synthetic-current-state";
    private static void expect(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private interface Checked {void run()throws Exception;}
    private static void rejects(Checked action,Class<? extends Throwable> type,String label)throws Exception {
        Throwable failure=null;try{action.run();}catch(Throwable caught){failure=caught;}
        expect(failure!=null&&type.isInstance(failure),label);
    }
    private static final class Cancelled extends Exception {}
    private static final class Harness implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,32,InetAddress.getByName("127.0.0.1"));
        final AtomicBoolean cancelled=new AtomicBoolean();
        final CountDownLatch accepted=new CountDownLatch(1);
        final ExecutorService executor=Executors.newSingleThreadExecutor(task->{Thread t=new Thread(task,"callback-test-master");t.setDaemon(true);return t;});
        final Future<URI> result;
        Harness(long total,long peer)throws Exception{this(total,peer,STATE);}
        Harness(long total,long peer,String state)throws Exception {
            result=executor.submit(()->LoopbackCallbackServer.await(server,state,total,peer,()->{if(cancelled.get())throw new Cancelled();},accepted::countDown));
        }
        int port(){return server.getLocalPort();}
        Socket connect()throws IOException {Socket socket=new Socket("127.0.0.1",port());socket.setSoTimeout(2000);return socket;}
        URI finish()throws Exception {
            try{return result.get(3,TimeUnit.SECONDS);}catch(ExecutionException error){Throwable cause=error.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new AssertionError(cause);}
        }
        public void close()throws Exception{cancelled.set(true);server.close();executor.shutdownNow();expect(executor.awaitTermination(2,TimeUnit.SECONDS),"Master executor terminates after owned listener cleanup");}
    }
    private static String valid(){return "/auth/callback?state="+STATE+"&code=synthetic-code";}
    private static String request(Harness h,String target){return "GET "+target+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\n\r\n";}
    private static String exchange(Harness h,String raw)throws Exception{return exchange(h,raw,2000);}
    private static String exchange(Harness h,String raw,int timeout)throws Exception {
        try(Socket socket=h.connect()){socket.setSoTimeout(timeout);socket.getOutputStream().write(raw.getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();return response(socket.getInputStream());}
    }
    private static String response(InputStream input)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int count;
        while((count=input.read(buffer))!=-1){if(bytes.size()+count>16000)throw new AssertionError("Unbounded callback response");bytes.write(buffer,0,count);}
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
    private static void successful(Harness h,String response)throws Exception {
        expect(response.startsWith("HTTP/1.1 200 OK\r\n"),"Valid callback receives HTTP 200");
        expect(response.contains("Cache-Control: no-store\r\n")&&response.contains("Referrer-Policy: no-referrer\r\n"),"Callback response prohibits caching and referrer disclosure");
        int split=response.indexOf("\r\n\r\n");String body=response.substring(split+4),length=null;
        for(String line:response.substring(0,split).split("\r\n"))if(line.startsWith("Content-Length: "))length=line.substring(16);
        expect(length!=null&&Integer.parseInt(length)==body.getBytes(StandardCharsets.UTF_8).length,"Content-Length counts actual UTF-8 callback bytes");
        expect(body.contains("draft2done://auth/return")&&!body.contains(STATE)&&!body.contains("synthetic-code"),"Generic response offers app return without echoing callback secrets");
        URI uri=h.finish();expect("/auth/callback".equals(uri.getPath())&&uri.getRawQuery().contains("code=synthetic-code"),"Verified callback is returned privately to the OAuth owner");
        expect(!h.server.isClosed(),"Successful callback leaves caller-owned listener cleanup to its owner");
    }

    public static void main(String[] args)throws Exception {
        idleThenValid();concurrentIdle();malformedThenValid();drainHeaders();absolutePeerDeadline();totalDeadline();cancelAccept();cancelPeer();externalClose();freshState();denial();resetPeer();
        System.out.println("Loopback callback TCP tests passed: "+checks+" (synthetic loopback only; no provider traffic)");
    }

    private static void idleThenValid()throws Exception {
        try(Harness h=new Harness(3000,120);Socket idle=h.connect()){
            expect(h.accepted.await(1,TimeUnit.SECONDS),"Idle TCP peer is actually accepted before the callback test");
            expect(idle.getInputStream().read()==-1,"Blank browser preconnect expires and closes only its own socket");
            expect(!h.result.isDone()&&!h.server.isClosed(),"Idle peer timeout does not abort the pending login listener");
            successful(h,exchange(h,request(h,valid())));
        }
    }
    private static void concurrentIdle()throws Exception {
        List<Socket> idle=new ArrayList<>();
        try(Harness h=new Harness(5000,3000)){
            try {
                for(int i=0;i<8;i++)idle.add(h.connect());
                expect(h.accepted.await(1,TimeUnit.SECONDS),"Concurrent browser preconnections reach the production listener");
                long began=System.nanoTime();String reply=exchange(h,request(h,valid()),1500);
                expect(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<1500,"Eight idle preconnections do not serialize or starve a valid callback");
                successful(h,reply);
                for(Socket socket:idle)expect(socket.getInputStream().read()==-1,"Returning a valid callback closes every other accepted peer");
            } finally {for(Socket socket:idle)socket.close();}
        }
    }
    private static void malformedThenValid()throws Exception {
        try(Harness h=new Harness(10000,500)){
            String[] bad={
                "\r\n",
                "POST "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\n\r\n",
                "HEAD "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\n\r\n",
                request(h,"/favicon.ico"),request(h,"/auth/callback?code=synthetic-code"),
                request(h,"/auth/callback?state=wrong&code=synthetic-code"),
                request(h,valid()+"&state=duplicate"),request(h,"/auth/callback?state="+STATE),
                request(h,valid()+"&error=access_denied"),request(h,"http://example.org"+valid()),
                request(h,"//example.org"+valid()),request(h,valid()+"#fragment"),
                request(h,"/auth/callback?state="+STATE+"&code=%Q1"),
                request(h,"/auth/callback?state="+STATE+"&code=%00"),
                "GET "+valid()+" HTTP/1.1\r\nHost: example.org\r\n\r\n",
                "GET "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\nHost: 127.0.0.1:"+h.port()+"\r\n\r\n",
                "GET "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\nTransfer-Encoding: chunked\r\n\r\n",
                "GET /"+repeat('x',8200)+" HTTP/1.1\r\n\r\n",
                "GET "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\nX-Fill: "+repeat('x',16400)+"\r\n\r\n"
            };
            for(String raw:bad){
                String reply=exchange(h,raw);
                expect(reply.startsWith("HTTP/1.1 4"),"Malformed, mismatched or over-bound request is rejected per peer");
                expect(!h.result.isDone()&&!h.server.isClosed(),"Invalid peer cannot terminate or consume the pending authorization attempt");
            }
            successful(h,exchange(h,request(h,valid())));
        }
    }
    private static void drainHeaders()throws Exception {
        try(Harness h=new Harness(3000,1000);Socket socket=h.connect()){
            socket.getOutputStream().write(("GET "+valid()+" HTTP/1.1\r\nHost: 127.0.0.1:"+h.port()+"\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
            socket.setSoTimeout(120);
            rejects(()->socket.getInputStream().read(),SocketTimeoutException.class,"Callback cannot complete before the terminating header blank line");
            expect(!h.result.isDone(),"Reading the request line alone does not consume authorization");
            socket.getOutputStream().write("User-Agent: synthetic-test\r\n\r\n".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();socket.setSoTimeout(2000);
            successful(h,response(socket.getInputStream()));
        }
    }
    private static void absolutePeerDeadline()throws Exception {
        try(Harness h=new Harness(3000,180);Socket socket=h.connect()){
            ScheduledExecutorService trickle=Executors.newSingleThreadScheduledExecutor(task->{Thread thread=new Thread(task,"callback-test-trickle");thread.setDaemon(true);return thread;});AtomicInteger writes=new AtomicInteger();
            try {
                trickle.scheduleAtFixedRate(()->{try{socket.getOutputStream().write('G');socket.getOutputStream().flush();writes.incrementAndGet();}catch(IOException ignored){}},0,20,TimeUnit.MILLISECONDS);
                long began=System.nanoTime();expect(socket.getInputStream().read()==-1,"A byte-trickling peer is closed by its absolute deadline");
                expect(writes.get()>=3&&TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<1000,"Progressing socket reads cannot renew the peer deadline");
            } finally {trickle.shutdownNow();}
            successful(h,exchange(h,request(h,valid())));
        }
    }
    private static void totalDeadline()throws Exception {
        try(Harness h=new Harness(120,1000)){
            rejects(h::finish,SocketTimeoutException.class,"Whole-login timeout expires with no callback");
            expect(!h.server.isClosed(),"Timeout retains caller ownership of the master listener");
        }
        try(Harness h=new Harness(350,1000)){
            int invalid=0;long began=System.nanoTime();
            while(!h.result.isDone())try{exchange(h,request(h,"/auth/callback?state=wrong&code=synthetic-code"),100);invalid++;}catch(IOException ended){break;}
            rejects(h::finish,SocketTimeoutException.class,"Repeated invalid requests do not restart the total deadline");
            expect(invalid>=2&&TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<1500,"Invalid traffic remains bounded by the original whole-login deadline");
        }
    }
    private static void cancelAccept()throws Exception {
        try(Harness h=new Harness(5000,5000)){
            long began=System.nanoTime();h.cancelled.set(true);rejects(h::finish,Cancelled.class,"Cancellation interrupts a pending accept through the actual checker");
            expect(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<700,"Cancellation polling does not wait for the login timeout");
            expect(!h.server.isClosed(),"Checker cancellation leaves master socket ownership with caller");
        }
    }
    private static void cancelPeer()throws Exception {
        try(Harness h=new Harness(5000,5000);Socket peer=h.connect()){
            expect(h.accepted.await(1,TimeUnit.SECONDS),"Cancellation test holds an accepted idle peer rather than a queued connection");
            long began=System.nanoTime();h.cancelled.set(true);rejects(h::finish,Cancelled.class,"Cancellation also interrupts an accepted idle peer");
            expect(peer.getInputStream().read()==-1,"Cancellation closes the accepted peer socket");
            expect(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<700,"Accepted-peer cancellation does not wait for its five-second read timeout");
        }
    }
    private static void externalClose()throws Exception {
        try(Harness h=new Harness(5000,5000);Socket peer=h.connect()){
            expect(h.accepted.await(1,TimeUnit.SECONDS),"Caller-close test holds an accepted peer");
            h.server.close();rejects(h::finish,IOException.class,"External master close ends the listener promptly");
            expect(peer.getInputStream().read()==-1,"External master close also cleans up accepted peers");
        }
    }
    private static void freshState()throws Exception {
        try(Harness old=new Harness(3000,1000)){old.cancelled.set(true);rejects(old::finish,Cancelled.class,"Previous authorization attempt is canceled before retry");}
        try(Harness h=new Harness(3000,500,"synthetic-new-state")){
            expect(exchange(h,request(h,valid())).startsWith("HTTP/1.1 400"),"Old-state callback cannot complete a fresh authorization attempt");
            expect(!h.result.isDone(),"Stale callback leaves the new attempt active");
            String reply=exchange(h,request(h,"/auth/callback?state=synthetic-new-state&code=synthetic-code"));
            expect(reply.startsWith("HTTP/1.1 200")&&h.finish().getRawQuery().contains("state=synthetic-new-state"),"Fresh state succeeds after rejecting a stale callback");
        }
    }
    private static void denial()throws Exception {
        try(Harness h=new Harness(3000,500)){
            String reply=exchange(h,request(h,"/auth/callback?state="+STATE+"&error=access_denied"));
            expect(reply.startsWith("HTTP/1.1 200")&&!reply.contains("access_denied"),"Verified denial receives only generic callback HTML");
            expect(h.finish().getRawQuery().contains("error=access_denied"),"Verified denial is returned to the OAuth owner without any token exchange");
        }
    }
    private static void resetPeer()throws Exception {
        try(Harness h=new Harness(3000,500)){
            Socket reset=h.connect();expect(h.accepted.await(1,TimeUnit.SECONDS),"Reset test reaches an accepted production peer");reset.setSoLinger(true,0);reset.close();
            successful(h,exchange(h,request(h,valid())));
        }
    }
    private static String repeat(char value,int count){char[] chars=new char[count];java.util.Arrays.fill(chars,value);return new String(chars);}
}
