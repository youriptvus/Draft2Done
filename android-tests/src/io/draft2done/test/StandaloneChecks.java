package io.draft2done.test;

import android.app.Activity;
import android.util.AtomicFile;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/** Phone UI/bridge integration fixtures. No assist request reaches a provider. */
final class StandaloneChecks {
    interface JS { String run(String source) throws Exception; }
    interface Check { void run(boolean condition,String label) throws Exception; }
    interface Click { void run(String selector) throws Exception; }
    interface Capture { void run(String name) throws Exception; }
    private static final String METADATA="qa-standalone-preservation.json";
    private StandaloneChecks() {}
    static void run(Activity activity,JS js,Check check,Click click,Capture capture) throws Exception {
        File metadataFile=new File(activity.getCacheDir(),METADATA);
        if(metadataFile.exists())throw new IOException("Standalone preservation needs recovery before another test.");
        String prefix="Draft2Done-QA-"+UUID.randomUUID().toString().replace("-","");
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved=new NativeDocumentWorkflowChecks.WorkspaceSnapshot(activity,js::run,prefix);
        JSONObject baseline=new JSONObject().put("prefix",prefix);
        boolean preserved=false;
        try {
            String raw=decode(js.run("localStorage.getItem('draft2done-paste-draft')"));
            String nativeRaw=nativeStage(js);
            if(nativeRaw!=null){JSONObject nativeValue;try{nativeValue=new JSONObject(nativeRaw);}catch(Exception invalid){throw new IOException("Unreadable native pending input was left untouched.");}if(!(nativeValue.opt("text") instanceof String))throw new IOException("Unreadable native pending input was left untouched.");}
            baseline.put("localPresent",raw!=null).put("localRaw",raw==null?JSONObject.NULL:raw)
                .put("nativePresent",nativeRaw!=null).put("nativeRaw",nativeRaw==null?JSONObject.NULL:nativeRaw)
                .put("displayText",decode(js.run("document.querySelector('#paste-draft')?.value||''")))
                .put("account",accountFingerprint(js));
            writeMetadata(metadataFile,baseline);preserved=true;
            js.run("const s=window.Draft2Done.getState();s.settings.autoUpdates=false;s.settings.autosave=true;s.settings.defaultMode='draft';return true;");
        check.run("true".equals(js.run("JSON.parse(window.DraftNative.chatgptStatus()).connected===true")),"Existing encrypted ChatGPT session is connected; no sign-in needed");
        click.run("[data-action=page][data-id=settings]");
        if("true".equals(js.run("!!document.querySelector('[data-action=chatgpt-use-writing]')")))click.run("[data-action=chatgpt-use-writing]");
        click.run("[data-action=chatgpt-models]");
        waitFor(js,"!window.Draft2Done.info().busy",90000);
        check.run("true".equals(js.run("!window.Draft2Done.info().lastError&&!!document.querySelector('#chatgpt-model')?.value")),"Eligible model catalog loads directly on phone without inference");
        click.run("[data-action=chatgpt-recommend-model]");
        check.run("true".equals(js.run("window.Draft2Done.getState().settings.backend==='chatgpt'&&document.querySelector('#chatgpt-model').value==='gpt-6-astra'")),"Strongest recommended model is selected from this account's returned catalog");
        capture.run("standalone-settings.png");

        // Interception is asserted before any writing action. Unrecognized AI calls fail closed.
        check.run("true".equals(js.run("const real=window.DraftNative;window.__standaloneQA={real,calls:[],httpCalls:0,fail:false};const qa=window.__standaloneQA;const stub=new Proxy({}, {get(_target,key){if(key==='chatgpt')return qa.assist;if(key==='request')return function(id){qa.httpCalls++;setTimeout(()=>window.NativeEvents.response(id,0,'','QA forbids PC/HTTP transport.'),0);};const value=real[key];return typeof value==='function'?(...args)=>real[key](...args):value;}});qa.assist=function(action,id,raw){if(action!=='assist'){setTimeout(()=>window.NativeEvents.addon(id,null,'QA forbids unexpected AI action.'),0);return;}const payload=JSON.parse(raw);qa.calls.push(payload);const record=JSON.parse(payload.messages[1].content);const text=record.task==='helper'?'Which practical obstacle should the next paragraph explain, and what evidence would support that explanation?':'A useful next step is to distinguish a nearby place from a place that residents can actually use. Distance describes one part of access, while opening hours and the condition of a route can affect the same journey. The paper can develop this distinction as a question for investigation. It should identify the evidence needed to compare these explanations, then state the limits of any conclusion drawn from that evidence.';setTimeout(()=>window.NativeEvents.addon(id,qa.fail?null:{completed:true,model:payload.model,text:JSON.stringify({[record.task==='helper'?'suggestions':'paragraphs']:[{text}],warnings:['Simulated QA response; no provider inference.']})},qa.fail?'ChatGPT AI request reached a usage limit (subscription_sharing_usage_limit_exceeded). No automatic retry.':''),20);};window.DraftNative=stub;return window.DraftNative===stub&&window.DraftNative.chatgpt===qa.assist;")),"Simulated assist boundary is installed and verified before any Send action");
        try {
            click.run("[data-action=page][data-id=home]");
            String draft="Urban green spaces give people places to meet, walk, and rest. This fictional paper asks how access to those spaces is related to wellbeing. A park can be nearby on a map and still feel difficult to reach. The argument should consider the route, opening hours, and the needs of different residents.\n\nI want to compare access with distance rather than treating them as the same idea. I also need to discuss the limits of the available evidence. My next section should explain how a student could evaluate those questions without inventing results or assuming that an association proves a cause.";
            String base64=android.util.Base64.encodeToString(draft.getBytes("UTF-8"),android.util.Base64.NO_WRAP);
            js.run("window.NativeEvents.imported("+new JSONObject().put("name","Standalone-QA.txt").put("mime","text/plain").put("base64",base64)+");return true;");
            waitFor(js,"!!document.querySelector('#draft-editor')",10000);
            js.run("window.__standaloneQA.fixtureId=window.Draft2Done.getState().activeId;return true;");
            String original=js.run("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).originalText");
            click.run("[data-action=tab][data-id=helper]");
            click.run("[data-action=analyze]");
            waitFor(js,"!!document.querySelector('[data-action=remote-send]')",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===0&&document.querySelector('#remote-draft-preview').value===window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).workingText")),"Full draft preview appears before any transmission");
            check.run("true".equals(js.run("document.activeElement.id!=='remote-draft-preview'")),"Readonly consent preview does not focus the phone keyboard");
            capture.run("standalone-consent.png");
            click.run("[data-action=remote-cancel]");
            waitFor(js,"!window.Draft2Done.info().busy",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===0")),"Keeping draft on phone cancels without a provider request");
            click.run("[data-action=analyze]");
            waitFor(js,"!!document.querySelector('[data-action=remote-send]')",10000);
            js.run("window.__standaloneQA.preview=JSON.parse(document.querySelector('#remote-payload-preview').textContent);return true;");
            click.run("[data-action=remote-send]");
            waitFor(js,"!window.Draft2Done.info().busy",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===1&&JSON.stringify(window.__standaloneQA.calls[0])===JSON.stringify(window.__standaloneQA.preview)&&!!document.querySelector('.response-card')")),"Approved exact payload reaches direct native adapter and renders simulated Helper reply");
            click.run("[data-action=tab][data-id=complete]");
            js.run("let el=document.querySelector('#minimum-input');el.value=Number(document.querySelector('#current-count').textContent.replaceAll(',',''))+30;el.dispatchEvent(new Event('change',{bubbles:true}));return true;");
            click.run("[data-action=generate]");
            waitFor(js,"!!document.querySelector('[data-action=generate-confirm]')",5000);
            click.run("[data-action=generate-confirm]");
            waitFor(js,"!!document.querySelector('[data-action=remote-send]')",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===1")),"Complete requires fresh sharing consent after generation confirmation");
            click.run("[data-action=remote-send]");
            waitFor(js,"!window.Draft2Done.info().busy",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===2&&!!document.querySelector('.proposal-editor')")),"Complete renders a separate simulated proposal through direct adapter");
            click.run("[data-action=regenerate]");
            waitFor(js,"!!document.querySelector('[data-action=remote-send]')",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===2&&JSON.parse(JSON.parse(document.querySelector('#remote-payload-preview').textContent).messages[1].content).sectionToRevise.length>20")),"Regeneration previews the proposed section with fresh consent");
            click.run("[data-action=remote-send]");
            waitFor(js,"!window.Draft2Done.info().busy",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===3&&!!document.querySelector('.proposal-editor')")),"Regeneration succeeds using a simulated completed response");
            click.run("[data-action=accept]");
            check.run(original.equals(js.run("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).originalText")),"Explicit acceptance preserves the immutable original");
            check.run("true".equals(js.run("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).contributions.length>0&&window.__standaloneQA.httpCalls===0")),"Accepted contribution is recorded without any PC/HTTP transport");
            click.run("[data-action=tab][data-id=helper]");
            js.run("window.__standaloneQA.fail=true;window.__standaloneQA.notes=window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).helperNotes;return true;");
            click.run("[data-action=analyze]");
            waitFor(js,"!!document.querySelector('[data-action=remote-send]')",10000);
            click.run("[data-action=remote-send]");
            waitFor(js,"!window.Draft2Done.info().busy",10000);
            check.run("true".equals(js.run("window.__standaloneQA.calls.length===4&&window.__standaloneQA.httpCalls===0&&window.Draft2Done.info().lastError.includes('usage limit')")),"Simulated usage limit stops without retries or PC/API fallback");
            check.run("true".equals(js.run("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).helperNotes===window.__standaloneQA.notes")),"Failed simulated request leaves previous Helper notes intact");
            click.run("[data-action=modal-close]");
            click.run("[data-action=tab][data-id=draft]");
            check.run("true".equals(js.run("window.Draft2Done.getState().activeId===window.__standaloneQA.fixtureId")),"Cleanup is scoped to this run's fictional test paper");
            click.run("[data-action=paper-menu]");
            click.run("[data-action=delete]");
            click.run("[data-action=delete-confirm]");
            check.run("true".equals(js.run("!window.Draft2Done.getState().projects.some(p=>p.id===window.__standaloneQA.fixtureId)")),"Fictional test paper is removed after validation");
            click.run("[data-action=page][data-id=home]");
        } finally {
            js.run("if(window.__standaloneQA){window.DraftNative=window.__standaloneQA.real;delete window.__standaloneQA;}return true;");
        }
        } finally {
            if(preserved)restore(activity,js,check,click,saved,baseline);
            else {saved.clear(activity);new AtomicFile(metadataFile).delete();}
        }
    }

    /** Recovery remains available if a device interruption stops the finally block. */
    static void recover(Activity activity,JS js,Check check,Click click) throws Exception {
        JSONObject original=new JSONObject(new String(read(new File(activity.getCacheDir(),METADATA),32*1024*1024),StandardCharsets.UTF_8));
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved=boundCheckpoint(activity,original);
        restore(activity,js,check,click,saved,original);
    }

    private static void restore(Activity activity,JS js,Check check,Click click,
                                NativeDocumentWorkflowChecks.WorkspaceSnapshot saved,JSONObject original)throws Exception {
        boundCheckpoint(activity,original);
        js.run("if(window.__standaloneQA){window.DraftNative=window.__standaloneQA.real;delete window.__standaloneQA;}return true;");
        if("true".equals(js.run("window.Draft2Done.info().busy&&!!document.querySelector('[data-action=stop]')"))){click.run("[data-action=stop]");waitFor(js,"!window.Draft2Done.info().busy",15000);}
        saved.restore(activity,js::run,click::run);
        boolean localPresent=original.getBoolean("localPresent"),nativePresent=original.getBoolean("nativePresent");
        String localRaw=localPresent?original.getString("localRaw"):null,nativeRaw=nativePresent?original.getString("nativeRaw"):null;
        String authority=nativePresent?nativeRaw:localRaw;
        js.run("window.NativeEvents.pause=()=>{};window.addEventListener('pagehide',e=>e.stopImmediatePropagation(),true);document.addEventListener('visibilitychange',e=>e.stopImmediatePropagation(),true);const e=document.querySelector('#paste-draft');if(e){e.value="+JSONObject.quote(original.getString("displayText"))+";let p={};try{p=JSON.parse("+JSONObject.quote(authority==null?"null":authority)+")||{};}catch(_){}const a=Number.isInteger(p.start)?Math.max(0,Math.min(e.value.length,p.start)):0,b=Number.isInteger(p.end)?Math.max(a,Math.min(e.value.length,p.end)):a;e.setSelectionRange(a,b);e.scrollTop=Number.isFinite(p.scroll)?Math.max(0,p.scroll):0;e.dispatchEvent(new Event('input',{bubbles:true}));}if(window.DraftNative.saveDraftInput("+JSONObject.quote(nativePresent?nativeRaw:"")+")!=='ok')throw Error('Native input restoration failed');"+(localPresent?"localStorage.setItem('draft2done-paste-draft',"+JSONObject.quote(localRaw)+");":"localStorage.removeItem('draft2done-paste-draft');")+"setTimeout(()=>location.reload(),80);return true;");
        Thread.sleep(1000);waitFor(js,"!!window.Draft2Done&&!!document.querySelector('#paste-draft')",20000);
        check.run(saved.matchesDisk(activity),"Standalone QA restores exact original workspace and backup bytes");
        check.run(saved.matchesState(js::run),"Standalone QA restores all original projects, active selection, settings, profiles and notes");
        String actualLocal=decode(js.run("localStorage.getItem('draft2done-paste-draft')")),actualNative=nativeStage(js);
        check.run((localPresent?localRaw.equals(actualLocal):actualLocal==null)&&(nativePresent?nativeRaw.equals(actualNative):actualNative==null),"Standalone QA restores exact native and browser pending input, including absence");
        check.run("true".equals(js.run("return document.querySelector('#paste-draft').value==="+JSONObject.quote(original.getString("displayText"))+";")),"Standalone QA reloads the user's original pending input into normal app memory");
        check.run(original.getString("account").equals(accountFingerprint(js)),"Standalone QA preserves the connected account while restoring the user's original model preferences");
        saved.clear(activity);new AtomicFile(new File(activity.getCacheDir(),METADATA)).delete();
    }

    private static NativeDocumentWorkflowChecks.WorkspaceSnapshot boundCheckpoint(Activity activity,JSONObject original)throws Exception {
        String prefix=original.getString("prefix");if(!prefix.matches("Draft2Done-QA-[a-f0-9]{32}"))throw new IOException("Unknown standalone preservation prefix; data left untouched.");
        JSONObject binding=new JSONObject(new String(read(new File(new File(activity.getCacheDir(),"qa-document-checkpoint"),"pending.json"),8192),StandardCharsets.UTF_8));
        if(!prefix.equals(binding.getString("prefix")))throw new IOException("Standalone preservation does not own this workspace checkpoint; data left untouched.");
        NativeDocumentWorkflowChecks.WorkspaceSnapshot saved=NativeDocumentWorkflowChecks.WorkspaceSnapshot.pending(activity);if(saved==null)throw new IOException("Standalone preservation checkpoint is unavailable.");return saved;
    }
    private static String nativeStage(JS js)throws Exception {
        if(!"true".equals(js.run("typeof window.DraftNative.loadDraftInput==='function'&&typeof window.DraftNative.saveDraftInput==='function'")))throw new IOException("Standalone preservation requires native durable-input support.");
        String raw=decode(js.run("window.DraftNative.loadDraftInput()"));return raw==null||"null".equals(raw)?null:raw;
    }
    private static String accountFingerprint(JS js)throws Exception {
        String value=js.run("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};");
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    private static void writeMetadata(File file,JSONObject value)throws Exception {
        byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>32*1024*1024)throw new IOException("Standalone preservation metadata exceeds its private bound; input left untouched.");
        AtomicFile atomic=new AtomicFile(file);FileOutputStream output=null;try{output=atomic.startWrite();output.write(bytes);atomic.finishWrite(output);}catch(Exception error){if(output!=null)atomic.failWrite(output);throw error;}
    }
    private static byte[] read(File file,int limit)throws Exception {
        try(InputStream input=new AtomicFile(file).openRead()){ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] bytes=new byte[8192];int count;while((count=input.read(bytes))!=-1){if(output.size()+count>limit)throw new IOException("Standalone preservation metadata exceeds its private bound.");output.write(bytes,0,count);}return output.toByteArray();}
    }
    private static String decode(String value)throws Exception {Object parsed=new JSONTokener(value).nextValue();return parsed==JSONObject.NULL?null:String.valueOf(parsed);}
    private static void waitFor(JS js,String predicate,int timeout)throws Exception {
        long deadline=System.currentTimeMillis()+timeout;
        while(System.currentTimeMillis()<deadline){if("true".equals(js.run(predicate)))return;Thread.sleep(150);}
        throw new IllegalStateException("Standalone UI timed out: "+predicate);
    }
}
