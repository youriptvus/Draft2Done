package io.draft2done.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PageRange;
import android.print.PrintManager;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.FrameLayout;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline app host. Only packaged, pinned-origin content can reach DraftNative. */
public final class MainActivity extends Activity {
    private static final String ORIGIN = "https://app.draft2done.local";
    private static final int IMPORT_FILE = 101, EXPORT_FILE = 102;
    private static final int STATE_LIMIT = 4 * 1024 * 1024;
    private static final int FILE_LIMIT = 10 * 1024 * 1024;
    private final ExecutorService workers = Executors.newFixedThreadPool(3);
    private final Map<String, NetworkTask> requests = new ConcurrentHashMap<>();
    private WorkspaceStore workspace;
    private ChatGPTConnection chatgpt;
    private AppUpdates updates;
    private WebView webView, printView;
    private PrintDocumentAdapter printAdapter;
    private FrameLayout hostView;
    private volatile boolean destroyed;
    private byte[] pendingExport;
    private boolean importing;

    @Override @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workspace = new WorkspaceStore(getFilesDir(), STATE_LIMIT);
        chatgpt = new ChatGPTConnection(this);
        updates = new AppUpdates(this);
        WebView.setWebContentsDebuggingEnabled(false);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF4F2EB);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.addJavascriptInterface(new NativeBridge(), "DraftNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (isAppUri(request.getUrl())) return false;
                if (request.isForMainFrame() && request.hasGesture()) openBrowser(request.getUrl().toString());
                return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!isAppUri(request.getUrl()) || !"GET".equals(request.getMethod())) return rejected();
                String path = request.getUrl().getPath();
                if (path == null || "/".equals(path)) path = "/index.html";
                if (path.contains("..") || path.contains("\\") || path.indexOf('\u0000') >= 0) return rejected();
                try {
                    String mime = mimeFor(path);
                    Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Cache-Control", "no-store");
                    headers.put("X-Content-Type-Options", "nosniff");
                    headers.put("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'none'; worker-src 'self' blob:; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'");
                    return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, getAssets().open("web" + path));
                } catch (Exception ignored) { return rejected(); }
            }
        });
        FrameLayout host=new FrameLayout(this);
        hostView=host;
        host.addView(webView,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(host);
        // Android 15+ uses edge-to-edge. Explicitly pad our full-screen host for system bars/IME.
        host.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout() | android.view.WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                event("keyboard",insets.isVisible(android.view.WindowInsets.Type.ime())?"true":"false");
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        webView.loadUrl(ORIGIN + "/index.html");
    }

    private static boolean isAppUri(Uri uri) {
        return "https".equals(uri.getScheme()) && "app.draft2done.local".equals(uri.getHost()) && uri.getPort() == -1 && uri.getUserInfo() == null;
    }
    private static WebResourceResponse rejected() {
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
    }
    private static String mimeFor(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".wasm")) return "application/wasm";
        return "application/octet-stream";
    }
    private void event(String name, String arguments) {
        if (destroyed) return;
        runOnUiThread(() -> {
            if (!destroyed && webView != null && isAppUri(Uri.parse(webView.getUrl() == null ? "" : webView.getUrl()))) {
                webView.evaluateJavascript("window.NativeEvents && window.NativeEvents." + name + " && window.NativeEvents." + name + "(" + arguments + ");", null);
            }
        });
    }
    private void notice(String message) {
        event("notice", JSONObject.quote(message));
        runOnUiThread(() -> { if (!destroyed) Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); });
    }
    private void openBrowser(String value) {
        try {
            Uri uri = Uri.parse(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("Only web links can be opened.");
            startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (Exception exception) { notice("Could not open the link in a browser."); }
    }

    public final class NativeBridge {
        @JavascriptInterface public String loadState() {
            WorkspaceStore.Loaded loaded = workspace.load();
            if (loaded.unreadable) { notice("Saved workspace could not be read. Import a backup to recover it."); return "DRAFT2DONE_STATE_READ_ERROR"; }
            if (loaded.recovered) notice("Recovered the previous saved workspace after a storage error.");
            return loaded.json;
        }
        @JavascriptInterface public String saveState(String json) {
            try { workspace.save(json); return "ok"; }
            catch (Exception exception) { return "error: " + exception.getMessage(); }
        }
        @JavascriptInterface public String appInfo() {
            return "{\"name\":\"Draft2Done\",\"version\":"+JSONObject.quote(AppVersion.name(MainActivity.this))+",\"platform\":\"android\",\"packageId\":"+JSONObject.quote(getPackageName())+",\"maxImportBytes\":"+FILE_LIMIT+",\"maxStateBytes\":"+STATE_LIMIT+"}";
        }
        @JavascriptInterface public void setAppearance(String theme){
            final boolean dark="dark".equals(theme);
            runOnUiThread(()->{
                int color=dark?0xFF151E20:0xFFF5F5EF;
                getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(color));
                if(hostView!=null)hostView.setBackgroundColor(color);
                getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);
                if(android.os.Build.VERSION.SDK_INT>=29){getWindow().setNavigationBarContrastEnforced(false);getWindow().setStatusBarContrastEnforced(false);}
                int flags=getWindow().getDecorView().getSystemUiVisibility();
                if(dark)flags&=~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                else flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);webView.setBackgroundColor(color);
            });
        }
        @JavascriptInterface public String chatgptStatus(){return chatgpt.status();}
        @JavascriptInterface public void chatgpt(String action,String id,String payload){
            if(id==null||id.isEmpty()||id.length()>120)return;
            if(action==null||payload==null){event("addon",JSONObject.quote(id)+",{},\"Invalid request.\"");return;}
            int limit="assist".equals(action)?AssistRequest.MAX_PAYLOAD_BYTES:40000;
            if(payload.length()>limit){event("addon",JSONObject.quote(id)+",{},"+JSONObject.quote("The request exceeds the "+("assist".equals(action)?"1 MB writing":"40 KB add-on")+" limit. Nothing was sent or truncated."));return;}
            try{if(action.startsWith("update-"))updates.enqueue(id);else chatgpt.enqueue(id);}
            catch(Exception error){event("addon",JSONObject.quote(id)+",{},"+JSONObject.quote(error.getMessage()));return;}
            workers.execute(()->{try{JSONObject result=action.startsWith("update-")?updates.handle(action,id):chatgpt.handle(action,id,new JSONObject(payload));event("addon",JSONObject.quote(id)+","+result.toString()+",\"\"");}catch(Exception error){event("addon",JSONObject.quote(id)+",{},"+JSONObject.quote(error.getMessage()==null?"Connection failed.":error.getMessage()));}});
        }
        @JavascriptInterface public void chatgptCancel(String id){chatgpt.cancel(id);updates.cancel(id);}
        @JavascriptInterface public void importDocument() {
            runOnUiThread(() -> {
                if (importing) { notice("A document picker is already open."); return; }
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*","application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/json","application/octet-stream"});
                    importing = true;
                    startActivityForResult(intent, IMPORT_FILE);
                } catch (ActivityNotFoundException exception) { importing = false; notice("No Android document picker is available."); }
            });
        }
        @JavascriptInterface public void exportDocument(String name, String mime, String base64) {
            workers.execute(() -> {
                try {
                    if (base64 == null || base64.length() > ((FILE_LIMIT + 2) / 3) * 4) throw new IllegalArgumentException("Export exceeds 10 MB.");
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    if (bytes.length > FILE_LIMIT) throw new IllegalArgumentException("Export exceeds 10 MB.");
                    String safeName = (name == null ? "Draft2Done.txt" : name).replaceAll("[\\\\/\\p{Cntrl}]", "_");
                    if (safeName.length() > 160) safeName = safeName.substring(0,160);
                    final String fileName = safeName;
                    runOnUiThread(() -> {
                        if (pendingExport != null) { notice("Finish the current export first."); return; }
                        try {
                            pendingExport = bytes;
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime == null ? "application/octet-stream" : mime);
                            intent.putExtra(Intent.EXTRA_TITLE, fileName);
                            startActivityForResult(intent, EXPORT_FILE);
                        } catch (Exception exception) { pendingExport = null; notice("Could not open the save dialog."); }
                    });
                } catch (Exception exception) { notice("Export failed: " + exception.getMessage()); }
            });
        }
        @JavascriptInterface public void openExternal(String url) { runOnUiThread(() -> openBrowser(url)); }
        @JavascriptInterface public void request(String id, String url, String method, String headersJson, String body) {
            if (id == null || id.length() > 120) return;
            NetworkTask task = new NetworkTask(id, url, method, headersJson, body);
            if (requests.size() >= 12 || requests.putIfAbsent(id, task) != null) {
                event("response", JSONObject.quote(id) + ",0,\"\",\"Too many active requests or a duplicate request ID.\"");
                return;
            }
            workers.execute(task);
        }
        @JavascriptInterface public void cancelRequest(String id) {
            NetworkTask task = id == null ? null : requests.get(id);
            if (task != null) { task.cancelled = true; if (task.connection != null) task.connection.disconnect(); }
        }
        @JavascriptInterface public void printDocument(String html) {
            if (html == null || html.length() > STATE_LIMIT) { notice("Document is too large to print."); return; }
            runOnUiThread(() -> {
                if (printView != null) { notice("Finish or cancel the current PDF print job before starting another."); return; }
                printView = new WebView(MainActivity.this);
                printView.getSettings().setJavaScriptEnabled(false);
                printView.getSettings().setAllowFileAccess(false);
                printView.getSettings().setAllowContentAccess(false);
                final String printUrl=ORIGIN+"/print/"+java.util.UUID.randomUUID()+".html";
                final byte[] document=html.getBytes(StandardCharsets.UTF_8);
                printView.setWebViewClient(new WebViewClient() {
                    private boolean handedOff;
                    @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                        if(request.isForMainFrame()&&"GET".equals(request.getMethod())&&printUrl.equals(request.getUrl().toString())){
                            WebResourceResponse response=new WebResourceResponse("text/html","UTF-8",new ByteArrayInputStream(document));
                            response.setResponseHeaders(Collections.singletonMap("Cache-Control","no-store"));
                            return response;
                        }
                        return rejected();
                    }
                    @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                        if(request.isForMainFrame()&&view==printView&&printAdapter==null){releasePrintView(view);notice("Could not prepare this document for PDF export. Try again.");}
                    }
                    @Override public void onPageFinished(WebView view, String url) {
                        // WebView can report page completion again after print rendering.
                        // A single offscreen WebView must own exactly one print job.
                        if (handedOff || view != printView || !printUrl.equals(url)) return;
                        handedOff = true;
                        if (destroyed) { releasePrintView(view); return; }
                        PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                        if (manager == null) { releasePrintView(view); notice("Android printing is unavailable."); return; }
                        String title=view.getTitle();
                        if(title==null)title="";
                        title=title.replaceAll("[\\\\/\\p{Cntrl}]", "_").trim();
                        if(title.isEmpty())title="Draft2Done paper";
                        final String printName=title.substring(0,Math.min(title.length(),120));
                        final PrintDocumentAdapter delegate=view.createPrintDocumentAdapter(printName);
                        PrintDocumentAdapter adapter=new PrintDocumentAdapter(){
                            private boolean finished;
                            @Override public void onStart(){delegate.onStart();}
                            @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes newAttributes,CancellationSignal cancellation,LayoutResultCallback callback,Bundle extras){delegate.onLayout(oldAttributes,newAttributes,cancellation,callback,extras);}
                            @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,CancellationSignal cancellation,WriteResultCallback callback){delegate.onWrite(pages,destination,cancellation,callback);}
                            @Override public void onFinish(){if(finished)return;finished=true;try{delegate.onFinish();}finally{runOnUiThread(()->releasePrintView(view));}}
                        };
                        printAdapter=adapter;
                        try{manager.print(printName,adapter,new PrintAttributes.Builder().build());}
                        catch(Exception error){adapter.onFinish();notice("Could not start Android printing. Try again.");}
                    }
                });
                printView.loadUrl(printUrl);
            });
        }
    }

    private void releasePrintView(WebView view){
        if(view!=printView)return;
        printAdapter=null;printView=null;view.setWebViewClient(null);view.destroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_FILE) importing = false;
        if (requestCode == EXPORT_FILE) {
            final byte[] bytes = pendingExport;
            pendingExport = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null || bytes == null) return;
            Uri destination = data.getData();
            workers.execute(() -> {
                try (OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
                    if (output == null) throw new IllegalStateException("The destination is unavailable.");
                    output.write(bytes);
                    notice("Document exported successfully.");
                } catch (Exception exception) { notice("Export failed: " + exception.getMessage()); }
            });
        }
        if (requestCode == IMPORT_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri source = data.getData();
            workers.execute(() -> {
                try {
                    String name = "Imported document";
                    try (Cursor cursor = getContentResolver().query(source, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn);
                            int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                            if (sizeColumn >= 0 && !cursor.isNull(sizeColumn) && cursor.getLong(sizeColumn) > FILE_LIMIT) throw new IllegalArgumentException("Choose a document smaller than 10 MB.");
                        }
                    }
                    byte[] bytes;
                    try (InputStream input = getContentResolver().openInputStream(source)) {
                        if (input == null) throw new IllegalStateException("The document is unavailable.");
                        bytes = readLimited(input, FILE_LIMIT);
                    }
                    JSONObject payload = new JSONObject();
                    payload.put("name",name);
                    payload.put("mime", getContentResolver().getType(source));
                    payload.put("base64",Base64.encodeToString(bytes,Base64.NO_WRAP));
                    event("imported",payload.toString());
                } catch (Exception exception) { notice("Import failed: " + exception.getMessage()); }
            });
        }
    }

    private final class NetworkTask implements Runnable {
        private final String id, url, method, headersJson, body;
        private volatile boolean cancelled;
        private volatile HttpURLConnection connection;
        NetworkTask(String id, String url, String method, String headersJson, String body) {
            this.id=id; this.url=url; this.method=method; this.headersJson=headersJson; this.body=body;
        }
        @Override public void run() {
            int status = 0;
            String response = "", error = "";
            try {
                if (cancelled) throw new IllegalStateException("Request cancelled.");
                if (url == null || url.length() > 4096) throw new IllegalArgumentException("Invalid request URL.");
                URL target = validatedUrl(url);
                String verb = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
                if (!"GET".equals(verb) && !"POST".equals(verb)) throw new IllegalArgumentException("Only GET and POST are supported.");
                if (body != null && body.length() > STATE_LIMIT) throw new IllegalArgumentException("Request body is too large.");
                JSONObject headers = new JSONObject(headersJson == null || headersJson.isEmpty() ? "{}" : headersJson);
                if (headers.length() > 24) throw new IllegalArgumentException("Too many request headers.");
                connection = (HttpURLConnection) target.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(90000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod(verb);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", AppVersion.userAgent(MainActivity.this)+" (Android)");
                Iterator<String> names = headers.keys();
                while (names.hasNext()) {
                    String name = names.next(), value = headers.getString(name);
                    if (!name.matches("[A-Za-z0-9-]{1,80}") || value.length() > 4096 || value.contains("\r") || value.contains("\n")) throw new IllegalArgumentException("Invalid request header.");
                    if ("host".equalsIgnoreCase(name) || "connection".equalsIgnoreCase(name) || "content-length".equalsIgnoreCase(name) || "cookie".equalsIgnoreCase(name)) continue;
                    connection.setRequestProperty(name, value);
                }
                if (cancelled) throw new IllegalStateException("Request cancelled.");
                if ("POST".equals(verb)) {
                    connection.setDoOutput(true);
                    byte[] bytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
                    if (bytes.length > STATE_LIMIT) throw new IllegalArgumentException("Request body exceeds 4 MB.");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
                }
                status = connection.getResponseCode();
                if (status >= 300 && status < 400) throw new IllegalStateException("Redirect blocked. Use the endpoint's final URL in Settings.");
                if (connection.getContentLengthLong() > STATE_LIMIT) throw new IllegalStateException("Response exceeds 4 MB. Try a smaller request.");
                InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (stream != null) try (InputStream input = stream) { response = new String(readLimited(input, STATE_LIMIT), StandardCharsets.UTF_8); }
                if (cancelled) throw new IllegalStateException("Request cancelled.");
            } catch (Exception exception) {
                error = cancelled ? "Request cancelled." : (exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
                requests.remove(id, this);
                event("response",JSONObject.quote(id) + "," + status + "," + JSONObject.quote(response) + "," + JSONObject.quote(error));
            }
        }
    }
    static URL validatedUrl(String value) throws Exception {
        URL parsed = new URL(value);
        if (parsed.getUserInfo() != null || parsed.getRef() != null || parsed.getHost().isEmpty() || parsed.getPort() == 0 || parsed.getPort() > 65535) throw new IllegalArgumentException("Invalid endpoint URL.");
        String scheme = parsed.getProtocol().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) return parsed;
        if (!"http".equals(scheme) || !isLocalHost(parsed.getHost())) throw new IllegalArgumentException("Internet endpoints require HTTPS. HTTP is allowed only for localhost and private LAN AI servers.");
        return parsed;
    }
    static boolean isLocalHost(String value) {
        String host = value.toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || "[::1]".equals(host) || "::1".equals(host)) return true;
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return false;
        int[] octets = new int[4];
        try { for (int i=0;i<4;i++) { if (!parts[i].matches("[0-9]{1,3}")) return false; octets[i]=Integer.parseInt(parts[i]); if(octets[i]>255) return false; } }
        catch (NumberFormatException ignored) { return false; }
        return octets[0] == 10 || octets[0] == 127 || (octets[0] == 192 && octets[1] == 168) || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31);
    }
    private static byte[] readLimited(InputStream input, int limit) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read=input.read(buffer)) != -1) { if(output.size() + read > limit) throw new IllegalArgumentException("Data exceeds the " + (limit/(1024*1024)) + " MB limit."); output.write(buffer,0,read); }
        return output.toByteArray();
    }
    @Override protected void onPause() {
        // Flush the pending edit debounce before Android backgrounds the activity.
        event("pause", "");
        super.onPause();
    }
    @Override public void onBackPressed() {
        // Stay on the packaged page and let the application close its active pane first.
        webView.evaluateJavascript("window.NativeEvents && window.NativeEvents.back ? window.NativeEvents.back() : false", result -> { if (!"true".equals(result)) MainActivity.super.onBackPressed(); });
    }
    @Override protected void onDestroy() {
        destroyed = true;
        if(chatgpt!=null)chatgpt.destroy();
        if(updates!=null)updates.destroy();
        for (NetworkTask request : requests.values()) { request.cancelled=true; if (request.connection != null) request.connection.disconnect(); }
        workers.shutdownNow();
        // Android owns an active print adapter until onFinish; keep its WebView alive.
        if (printView != null && printAdapter == null) releasePrintView(printView);
        if (webView != null) { webView.removeJavascriptInterface("DraftNative"); webView.destroy(); }
        super.onDestroy();
    }
}
