package io.draft2done.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded, cancellation-aware browser callback transport. Never exchanges tokens. */
public final class LoopbackCallbackServer {
    public interface Checker { void check() throws Exception; }
    private static final int MAX_PEERS=12;
    private static final int REQUEST_LINE_LIMIT=8192;
    private static final int HEADER_LIMIT=16*1024;
    private static final int POLL_MILLIS=100;
    private LoopbackCallbackServer() {}

    public static URI await(ServerSocket server,String expectedState,long timeoutMillis,Checker check)throws Exception {
        return await(server,expectedState,timeoutMillis,5000,check);
    }

    /** Short peer deadlines allow actual TCP regression tests without long sleeps. */
    public static URI await(ServerSocket server,String expectedState,long timeoutMillis,long peerTimeoutMillis,Checker check)throws Exception {
        return await(server,expectedState,timeoutMillis,peerTimeoutMillis,check,null);
    }

    /** Package-only acceptance signal makes cancellation tests deterministic. */
    static URI await(ServerSocket server,String expectedState,long timeoutMillis,long peerTimeoutMillis,Checker check,Runnable peerAccepted)throws Exception {
        if(server==null||!server.isBound()||server.isClosed()||server.getInetAddress()==null||!server.getInetAddress().isLoopbackAddress())
            throw new IllegalArgumentException("Sign-in requires a bound loopback listener.");
        if(expectedState==null||expectedState.isEmpty()||expectedState.length()>1024||check==null)
            throw new IllegalArgumentException("Sign-in callback state is unavailable.");
        if(timeoutMillis<1||timeoutMillis>3600000||peerTimeoutMillis<1||peerTimeoutMillis>60000)
            throw new IllegalArgumentException("Sign-in callback timeout is invalid.");
        final long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        final AtomicReference<URI> result=new AtomicReference<>();
        final AtomicBoolean claimed=new AtomicBoolean();
        final Set<Socket> peers=ConcurrentHashMap.newKeySet();
        final ThreadPoolExecutor workers=new ThreadPoolExecutor(0,MAX_PEERS,0L,TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),task->{Thread thread=new Thread(task,"Draft2Done-OAuth-callback");thread.setDaemon(true);return thread;});
        int previousTimeout=server.getSoTimeout();
        try {
            while(true){
                active(check);
                URI completed=result.get();
                if(completed!=null)return completed;
                long remaining=remainingMillis(deadline);
                if(remaining<=0)throw timeout();
                server.setSoTimeout((int)Math.min(POLL_MILLIS,remaining));
                Socket peer;
                try{peer=server.accept();}
                catch(SocketTimeoutException idle){continue;}
                catch(SocketException closed){active(check);throw closed;}
                if(peers.size()>=MAX_PEERS){close(peer);continue;}
                peers.add(peer);
                final Socket accepted=peer;
                final long peerDeadline=Math.min(deadline,System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(peerTimeoutMillis));
                try{
                    workers.execute(()->{
                        try {
                            if(peerAccepted!=null)peerAccepted.run();
                            URI callback=readCallback(accepted,server.getLocalPort(),expectedState,peerDeadline);
                            if(callback==null){respond(accepted,400,false);return;}
                            if(!claimed.compareAndSet(false,true)){respond(accepted,400,false);return;}
                            // A browser closing its tab must not discard a verified
                            // callback. Publish only after the response attempt so
                            // normal responses are not cut short by worker cleanup.
                            try{respond(accepted,200,true);}finally{result.set(callback);}
                        }catch(BadRequest invalid){try{respond(accepted,invalid.status,false);}catch(IOException ignored){}}
                        catch(Exception ignored){/* One broken/preconnected peer cannot end the login. */}
                        finally{peers.remove(accepted);close(accepted);}
                    });
                }catch(RejectedExecutionException full){peers.remove(peer);close(peer);}
            }
        } finally {
            for(Socket peer:peers)close(peer);
            workers.shutdownNow();
            // ServerSocket belongs to the caller, including on cancellation.
            if(!server.isClosed())try{server.setSoTimeout(previousTimeout);}catch(SocketException ignored){}
        }
    }

    private static void active(Checker check)throws Exception {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("ChatGPT sign-in was cancelled.");
        check.check();
    }

    private static URI readCallback(Socket socket,int port,String expectedState,long deadline)throws Exception {
        DeadlineReader reader=new DeadlineReader(socket,deadline);
        String line=reader.line(REQUEST_LINE_LIMIT,414);
        if(line==null||line.isEmpty())throw new BadRequest(400);
        String[] first=line.split(" ",-1);
        if(first.length!=3||!("HTTP/1.1".equals(first[2])||"HTTP/1.0".equals(first[2])))throw new BadRequest(400);
        if(!"GET".equals(first[0]))throw new BadRequest(405);
        int used=0;String host=null;
        while(true){
            int allowance=HEADER_LIMIT-used;
            if(allowance<=0)throw new BadRequest(431);
            String header=reader.line(allowance,431);
            if(header==null)throw new BadRequest(400);
            used+=header.length()+2;
            if(used>HEADER_LIMIT)throw new BadRequest(431);
            if(header.isEmpty())break;
            int colon=header.indexOf(':');
            if(colon<1||header.charAt(0)==' '||header.charAt(0)=='\t')throw new BadRequest(400);
            String name=header.substring(0,colon),value=header.substring(colon+1).trim();
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))throw new BadRequest(400);
            name=name.toLowerCase(Locale.ROOT);
            if("host".equals(name)){if(host!=null)throw new BadRequest(400);host=value.toLowerCase(Locale.ROOT);}
            if("transfer-encoding".equals(name)||("content-length".equals(name)&&!"0".equals(value)))throw new BadRequest(400);
        }
        if(!("127.0.0.1:"+port).equals(host)&&!("localhost:"+port).equals(host))throw new BadRequest(400);
        String target=first[1];
        if(!target.startsWith("/")||target.startsWith("//"))throw new BadRequest(400);
        URI uri;
        try{uri=new URI(target);}catch(Exception malformed){throw new BadRequest(400);}
        if(uri.isAbsolute()||uri.getRawAuthority()!=null||uri.getRawFragment()!=null||!"/auth/callback".equals(uri.getRawPath()))throw new BadRequest(400);
        Map<String,String> query=new HashMap<>();
        String raw=uri.getRawQuery();
        if(raw==null)throw new BadRequest(400);
        for(String part:raw.split("&",-1)){
            if(part.isEmpty())continue;
            int equal=part.indexOf('=');
            String key,value;
            try{key=URLDecoder.decode(equal<0?part:part.substring(0,equal),"UTF-8");value=URLDecoder.decode(equal<0?"":part.substring(equal+1),"UTF-8");}
            catch(Exception malformed){throw new BadRequest(400);}
            if(key.isEmpty()||key.length()>200||key.matches(".*[\\x00-\\x1f\\x7f].*")||value.matches(".*[\\x00-\\x1f\\x7f].*")||query.size()>=32||query.put(key,value)!=null)throw new BadRequest(400);
        }
        if(!OAuthProtocol.same(expectedState,query.get("state")))throw new BadRequest(400);
        String code=query.get("code"),error=query.get("error");
        boolean hasCode=code!=null&&!code.isEmpty()&&code.length()<=8192;
        boolean hasError=error!=null&&!error.isEmpty()&&error.length()<=1024;
        if(hasCode==hasError||code!=null&&error!=null)throw new BadRequest(400);
        return new URI("http://127.0.0.1:"+port+target);
    }

    private static void respond(Socket socket,int status,boolean valid)throws IOException {
        byte[] body=OAuthProtocol.callbackHTML(valid).getBytes(StandardCharsets.UTF_8);
        String reason=status==200?"OK":status==405?"Method Not Allowed":status==414?"URI Too Long":status==431?"Request Header Fields Too Large":"Bad Request";
        String headers="HTTP/1.1 "+status+" "+reason+"\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\r\nConnection: close\r\nContent-Length: "+body.length+"\r\n\r\n";
        OutputStream out=socket.getOutputStream();out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();
    }

    private static final class DeadlineReader {
        private final Socket socket;private final InputStream input;private final long deadline;private int lastTimeout=-1;
        DeadlineReader(Socket socket,long deadline)throws IOException{this.socket=socket;this.deadline=deadline;input=new BufferedInputStream(socket.getInputStream(),4096);}
        private int read()throws IOException {
            while(true){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Callback peer was cancelled.");
                long remaining=remainingMillis(deadline);if(remaining<=0)throw new SocketTimeoutException("Callback peer deadline reached.");
                int timeout=(int)Math.min(POLL_MILLIS,remaining);if(timeout!=lastTimeout){socket.setSoTimeout(timeout);lastTimeout=timeout;}
                try{return input.read();}catch(SocketTimeoutException waiting){if(remainingMillis(deadline)<=0)throw waiting;}
            }
        }
        String line(int maximum,int status)throws IOException {
            StringBuilder out=new StringBuilder();
            while(true){
                int value=read();
                if(value==-1)return out.length()==0?null:out.toString();
                if(value=='\n')return out.toString();
                if(value=='\r'){if(read()!='\n')throw new BadRequest(400);return out.toString();}
                if(value<32&&value!='\t'||value>126)throw new BadRequest(400);
                if(out.length()>=maximum)throw new BadRequest(status);
                out.append((char)value);
            }
        }
    }
    private static final class BadRequest extends IOException{final int status;BadRequest(int status){super("Invalid loopback callback request.");this.status=status;}}
    private static long remainingMillis(long deadline){long left=deadline-System.nanoTime();return left<=0?0:TimeUnit.NANOSECONDS.toMillis(left)+1;}
    private static SocketTimeoutException timeout(){return new SocketTimeoutException("ChatGPT sign-in timed out. Return to Settings and try again.");}
    private static void close(Socket socket){try{socket.close();}catch(IOException ignored){}}
}
