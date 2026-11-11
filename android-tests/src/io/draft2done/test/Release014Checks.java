package io.draft2done.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.os.ParcelFileDescriptor;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.webkit.WebView;
import android.view.View;
import android.view.ViewGroup;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Actual release UI + existing public research APIs; never sends ChatGPT inference. */
final class Release014Checks {
    private static final String STAGING="draft2done-paste-draft";
    private static final String DRAFT="Photosynthesis converts light energy into chemical energy. My fictional classroom paper asks how a plant's leaves contribute to this process without inventing experimental findings. I want to distinguish an explanation from a measurement and learn which sources support each claim.\n\nThe French Revolution provides a different historical subject for comparison. Research should identify relevant material while keeping my wording intact. This example also preserves café, α, punctuation—and paragraph breaks.";
    private Release014Checks() {}

    static void run(Instrumentation instrumentation,Activity activity,NativeDocumentWorkflowChecks.Evaluator js,
                    NativeDocumentWorkflowChecks.Clicker click,NativeDocumentWorkflowChecks.Checker check,boolean live)throws Exception {
        Thread.sleep(1900);
        String prefix="Draft2Done-QA-"+UUID.randomUUID().toString().replace("-","");
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved=new NativeDocumentWorkflowChecks.WorkspaceSnapshot(activity,js,prefix);
        String staging=decode(js.evaluate("localStorage.getItem("+JSONObject.quote(STAGING)+")"));
        String nativeStaging=decode(js.evaluate("window.DraftNative.loadDraftInput()"));
        String account=js.evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};");
        int orientation=activity.getRequestedOrientation();
        try {
            click.click("[data-action=page][data-id=home]");
            check.check("true".equals(js.evaluate("const e=document.querySelector('#paste-draft');return !!e&&e.compareDocumentPosition(document.querySelector('.start-actions'))===Node.DOCUMENT_POSITION_FOLLOWING;")),"Paste Your Draft is above the existing import options on the real Samsung");
            tap(instrumentation,activity,js,"#paste-draft");Thread.sleep(900);
            check.check("true".equals(js.evaluate("document.activeElement===document.querySelector('#paste-draft')&&document.body.classList.contains('keyboard-open')")),"Physical Samsung touchscreen focuses Paste Your Draft and opens the Android keyboard");
            js.evaluate("const s=window.Draft2Done.getState();s.settings.autoUpdates=false;s.settings.autosave=true;const e=document.querySelector('#paste-draft');e.focus();e.value="+JSONObject.quote(DRAFT)+";e.setSelectionRange(4,19);e.dispatchEvent(new Event('input',{bubbles:true}));return true;");
            check.check("true".equals(js.evaluate("const e=document.querySelector('#paste-draft');return e.selectionStart===4&&e.selectionEnd===19&&JSON.parse(localStorage.getItem('draft2done-paste-draft')).text==="+JSONObject.quote(DRAFT)+";")),"Native paste uses standard selection and durably stages exact Unicode and paragraph text");
            activity.runOnUiThread(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));Thread.sleep(1100);
            check.check("true".equals(js.evaluate("document.querySelector('#paste-draft').value==="+JSONObject.quote(DRAFT))),"Physical landscape rotation preserves the staged draft");
            activity.runOnUiThread(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));Thread.sleep(1100);
            click.click("[data-action=page][data-id=projects]");click.click("[data-action=page][data-id=home]");
            check.check("true".equals(js.evaluate("document.querySelector('#paste-draft').value==="+JSONObject.quote(DRAFT))),"Navigation preserves the staged draft");
            reload(js);
            check.check("true".equals(js.evaluate("document.querySelector('#paste-draft').value==="+JSONObject.quote(DRAFT))),"A real bundled WebView reload restores staged paste before project creation");
            int before=Integer.parseInt(js.evaluate("window.Draft2Done.getState().projects.length"));
            tap(instrumentation,activity,js,"[data-action=paste-use]");
            waitFor(js,"window.Draft2Done.info().page==='write'",10000);
            check.check("true".equals(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return s.projects.length==="+(before+1)+"&&p.originalText==="+JSONObject.quote(DRAFT)+"&&p.workingText==="+JSONObject.quote(DRAFT)+";")),"Use This Draft creates a separate protected project without replacing another paper");
            js.evaluate("const s=window.Draft2Done.getState();s.projects.find(p=>p.id===s.activeId).title="+JSONObject.quote(prefix)+";return true;");
            click.click("[data-action=tab][data-id=helper]");
            check.check("true".equals(js.evaluate("document.querySelector('.analysis-summary').textContent.includes('Draft observations')")),"Pasted draft is available to real on-device Helper analysis");
            click.click("[data-action=tab][data-id=complete]");
            check.check("true".equals(js.evaluate("!!document.querySelector('#minimum-input')&&!!document.querySelector('#voice-select')")),"Pasted draft enters Complete with word goals and voice selection");
            js.evaluate("const e=document.querySelector('#information-links-enabled');e.checked=true;e.dispatchEvent(new Event('change',{bubbles:true}));return true;");
            check.check("true".equals(js.evaluate("document.querySelector('.information-copy').textContent==="+JSONObject.quote(DRAFT)+"&&document.querySelectorAll('[data-action=information-concept]').length>0")),"Real Complete reading view highlights concepts without changing any draft text");
            click.click("[data-action=information-concept]");
            check.check("true".equals(js.evaluate("!!document.querySelector('#information-query')&&!document.querySelector('[data-action=information-open]')")),"An offline concept sheet opens without fabricated destinations or automatic web requests");
            click.click("[data-action=modal-close]");
            click.click("[data-action=tab][data-id=helper]");
            check.check("true".equals(js.evaluate("!!document.querySelector('.information-copy')&&document.querySelector('#information-links-enabled').checked")),"Information-link preference and reading view are shared with Helper");
            click.click("[data-action=page][data-id=settings]");click.click("[data-action=about]");
            check.check("true".equals(js.evaluate("const m=document.querySelector('.modal');return m.textContent.includes('0.1.4')&&document.querySelector('.about-dedication').textContent==="+JSONObject.quote("Dedicated to Victoria Downs, whose inspiration helped bring Draft2Done to life.")+";")),"About shows version 0.1.4 and the exact permanent Victoria Downs dedication");
            click.click("[data-action=modal-close]");
            if(live) {liveResearch(js,click,check);openRetrievedSource(instrumentation,js,click,check);offlineRecovery(instrumentation,js,click,check);}
            click.click("[data-action=page][data-id=write]");click.click("[data-action=tab][data-id=draft]");click.click("[data-action=save]");
            reload(js);
            check.check("true".equals(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p.title==="+JSONObject.quote(prefix)+"&&p.originalText==="+JSONObject.quote(DRAFT)+"&&p.workingText==="+JSONObject.quote(DRAFT)+"&&Array.isArray(p.revisions);")),"Saving and reopening retains the pasted original, working text and revision structure");
        } finally {
            activity.runOnUiThread(()->activity.setRequestedOrientation(orientation));Thread.sleep(600);
            saved.restore(activity,js,click);
            // Suppress only this old QA page's flush while replacing its staged
            // input with the exact original raw record, then restore normal app code.
            js.evaluate("window.NativeEvents.pause=()=>{};window.addEventListener('pagehide',e=>e.stopImmediatePropagation(),true);document.addEventListener('visibilitychange',e=>e.stopImmediatePropagation(),true);const restored=window.DraftNative.saveDraftInput("+JSONObject.quote("null".equals(nativeStaging)?"":nativeStaging)+");if(restored!=='ok')throw Error('Native staging restoration failed');"+(staging==null?"localStorage.removeItem('draft2done-paste-draft');":"localStorage.setItem('draft2done-paste-draft',"+JSONObject.quote(staging)+");")+"setTimeout(()=>location.reload(),80);return true;");
            Thread.sleep(1000);waitFor(js,"!!window.Draft2Done",20000);
            check.check(saved.matchesDisk(activity),"Release QA restores exact pre-test workspace and backup bytes");
            check.check(saved.matchesState(js),"Release QA restores every project, profile, preference and active paper");
            check.check((staging==null?"null":JSONObject.quote(staging)).equals(js.evaluate("localStorage.getItem('draft2done-paste-draft')")),"Release QA restores the user's exact pending paste record");
            check.check(nativeStaging.equals(decode(js.evaluate("window.DraftNative.loadDraftInput()"))),"Release QA restores the exact native durable paste record or empty tombstone");
            check.check(account.equals(js.evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};")),"Release QA preserves the connected account without sending AI inference");
            saved.clear(activity);
        }
    }

    private static void liveResearch(NativeDocumentWorkflowChecks.Evaluator js,NativeDocumentWorkflowChecks.Clicker click,NativeDocumentWorkflowChecks.Checker check)throws Exception {
        String[][] queries={{"photosynthesis","wikipedia"},{"French Revolution","wikipedia"},{"urban green space wellbeing","crossref"},{"photosynthesis chlorophyll","pubmed"}};
        js.evaluate("window.__qa014Sources=[];return true;");
        for(String[] entry:queries){
            click.click("[data-action=page][data-id=research]");
            js.evaluate("const s=window.Draft2Done.getState();s.projects.find(p=>p.id===s.activeId).sources=[];s.settings.providers=["+JSONObject.quote(entry[1])+"];s.settings.online=true;s.lastQuery="+JSONObject.quote(entry[0])+";window.Draft2Done.render();return true;");
            click.click("[data-action=search]");waitFor(js,"!window.Draft2Done.info().busy",90000);
            check.check("true".equals(js.evaluate("!window.Draft2Done.info().lastError&&!document.querySelector('.modal-backdrop')")),"Actual live "+entry[1]+" request completes for query: "+entry[0]);
            String evidence=decode(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return JSON.stringify(p.sources.filter(x=>x.provider==="+JSONObject.quote(entry[1])+"&&x.verification!=='unverified').map(x=>({title:x.title,url:x.url,verification:x.verification,excerpt:(x.abstract||'').slice(0,220)})));"));
            JSONArray results=new JSONArray(evidence);
            check.check(results.length()>0,"Genuine live network results (no mock/cache): "+entry[1]+" / "+entry[0]+" / "+results.toString());
            boolean relevant=false;
            for(int i=0;i<results.length();i++){
                JSONObject result=results.getJSONObject(i);java.net.URI url=new java.net.URI(result.getString("url"));
                check.check(("https".equals(url.getScheme())||"http".equals(url.getScheme()))&&url.getHost()!=null,"Live retrieved result has an absolute source URL: "+result.getString("url"));
                String words=(result.optString("title")+" "+result.optString("excerpt")).toLowerCase(java.util.Locale.ROOT);
                for(String term:entry[0].toLowerCase(java.util.Locale.ROOT).split(" "))if(term.length()>3&&words.contains(term))relevant=true;
            }
            check.check(relevant,"Retrieved titles/excerpts overlap the distinct public query: "+entry[0]);
            js.evaluate("const s=window.Draft2Done.getState();window.__qa014Sources.push(...s.projects.find(p=>p.id===s.activeId).sources);return true;");
        }
        js.evaluate("const s=window.Draft2Done.getState();s.projects.find(p=>p.id===s.activeId).sources=window.__qa014Sources;delete window.__qa014Sources;return true;");
    }
    private static void openRetrievedSource(Instrumentation instrumentation,NativeDocumentWorkflowChecks.Evaluator js,NativeDocumentWorkflowChecks.Clicker click,NativeDocumentWorkflowChecks.Checker check)throws Exception {
        click.click("[data-action=page][data-id=write]");click.click("[data-action=tab][data-id=helper]");
        js.evaluate("const b=Array.from(document.querySelectorAll('[data-action=information-concept]')).find(x=>x.textContent.toLowerCase()==='photosynthesis');if(!b)throw Error('Photosynthesis concept unavailable');b.click();return true;");
        check.check("true".equals(js.evaluate("Array.from(document.querySelectorAll('[data-action=information-open]')).some(b=>new URL(b.dataset.url).hostname==='en.wikipedia.org')")),"Contextual information links reuse an actually retrieved relevant Wikipedia source");
        click.click("[data-action=information-open]");
        boolean external=false;
        for(int i=0;i<40;i++){
            android.view.accessibility.AccessibilityNodeInfo view=instrumentation.getUiAutomation().getRootInActiveWindow();
            String pkg=view==null||view.getPackageName()==null?"":view.getPackageName().toString();
            if(!pkg.isEmpty()&&!"io.draft2done.app".equals(pkg)&&!"com.android.systemui".equals(pkg)&&!pkg.contains("inputmethod")){external=true;break;}
            Thread.sleep(250);
        }
        check.check(external,"Explicit source tap opens the Android browser for the retrieved public page");
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(700);
        check.check("true".equals(js.evaluate("const s=window.Draft2Done.getState();return s.projects.find(p=>p.id===s.activeId).originalText==="+JSONObject.quote(DRAFT))),"Returning from the public source keeps the protected original intact");
        if("true".equals(js.evaluate("!!document.querySelector('[data-action=modal-close]')")))click.click("[data-action=modal-close]");
    }
    private static void offlineRecovery(Instrumentation instrumentation,NativeDocumentWorkflowChecks.Evaluator js,NativeDocumentWorkflowChecks.Clicker click,NativeDocumentWorkflowChecks.Checker check)throws Exception {
        String wifi=shell(instrumentation,"settings get global wifi_on").trim(),data=shell(instrumentation,"settings get global mobile_data").trim();
        if(!wifi.matches("[0-3]")||!data.matches("[01]"))throw new IllegalStateException("Cannot restore unknown pre-test Wi-Fi/mobile-data states; radios left unchanged.");
        try {
            shell(instrumentation,"svc wifi disable");shell(instrumentation,"svc data disable");Thread.sleep(3500);
            click.click("[data-action=page][data-id=research]");
            js.evaluate("const s=window.Draft2Done.getState();s.settings.providers=['wikipedia'];s.settings.online=true;s.lastQuery='photosynthesis';window.Draft2Done.render();return true;");
            String before=js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return JSON.stringify({original:p.originalText,text:p.workingText,sources:p.sources});");
            click.click("[data-action=search]");waitFor(js,"!window.Draft2Done.info().busy",45000);
            check.check("true".equals(js.evaluate("!!document.querySelector('.modal-backdrop')&&/retried|connection|network|resolve|internet|timed out/i.test(document.querySelector('.modal-backdrop').textContent)")),"Actual disabled-network search shows an understandable failure and retry guidance");
            check.check(before.equals(js.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return JSON.stringify({original:p.originalText,text:p.workingText,sources:p.sources});")),"Actual offline failure preserves original, working draft and all saved sources");
            click.click("[data-action=modal-close]");
        } finally {
            shell(instrumentation,"svc data "+("1".equals(data)?"enable":"disable"));
            shell(instrumentation,"svc wifi "+("0".equals(wifi)?"disable":"enable"));
        }
        Thread.sleep(6500);
        click.click("[data-action=search]");waitFor(js,"!window.Draft2Done.info().busy",60000);
        check.check("true".equals(js.evaluate("!document.querySelector('.modal-backdrop')&&!window.Draft2Done.info().lastError")),"Restoring the original radios allows an explicit real Wikipedia search retry");
        check.check(wifi.equals(shell(instrumentation,"settings get global wifi_on").trim())&&data.equals(shell(instrumentation,"settings get global mobile_data").trim()),"Network QA restores the original Wi-Fi and mobile-data preferences");
    }
    private static String shell(Instrumentation instrumentation,String command)throws Exception {
        try(ParcelFileDescriptor descriptor=instrumentation.getUiAutomation().executeShellCommand(command);InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>8192)throw new IllegalStateException("Unexpectedly large QA shell result");out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static void tap(Instrumentation instrumentation,Activity activity,NativeDocumentWorkflowChecks.Evaluator js,String selector)throws Exception {
        js.evaluate("document.querySelector("+JSONObject.quote(selector)+").scrollIntoView({block:'center'});return true;");Thread.sleep(200);
        JSONArray rect=new JSONArray(decode(js.evaluate("const r=document.querySelector("+JSONObject.quote(selector)+").getBoundingClientRect();return JSON.stringify([r.left+r.width/2,r.top+r.height/2,innerWidth]);")));
        WebView web=find(activity.getWindow().getDecorView());int[] location=new int[2];web.getLocationOnScreen(location);
        float scale=web.getWidth()/(float)rect.getDouble(2),x=location[0]+(float)rect.getDouble(0)*scale,y=location[1]+(float)rect.getDouble(1)*scale;
        long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_UP,x,y,0);
        try{instrumentation.sendPointerSync(down);instrumentation.sendPointerSync(up);}finally{down.recycle();up.recycle();}Thread.sleep(300);
    }
    private static WebView find(View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){WebView web=find(group.getChildAt(i));if(web!=null)return web;}}return null;}
    private static void reload(NativeDocumentWorkflowChecks.Evaluator js)throws Exception {js.evaluate("setTimeout(()=>location.reload(),80);return true;");Thread.sleep(1000);waitFor(js,"!!window.Draft2Done",20000);}
    private static void waitFor(NativeDocumentWorkflowChecks.Evaluator js,String expression,int timeout)throws Exception {long until=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<until){if("true".equals(js.evaluate(expression)))return;Thread.sleep(150);}throw new IllegalStateException("Timed out: "+expression);}
    private static String decode(String json)throws Exception {Object value=new JSONTokener(json).nextValue();return value==JSONObject.NULL?null:String.valueOf(value);}
}
