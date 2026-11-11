package io.draft2done.test;

import org.json.JSONObject;
import java.io.IOException;

/** Real provider checks using only a newly imported fictional paper. No retries/fallback. */
final class ChatGPTLiveChecks {
    interface JS { String run(String source)throws Exception; }
    interface Check { void run(boolean condition,String label)throws Exception; }
    interface Click { void run(String selector)throws Exception; }
    interface Capture { void run(String name)throws Exception; }
    private ChatGPTLiveChecks() {}
    static void run(JS js,Check check,Click click,Capture capture)throws Exception {
        JSONObject before=snapshot(js);
        String qaId=null,qaOriginal=null;
        Throwable failure=null;
        try {
        check.run("true".equals(js.run("JSON.parse(window.DraftNative.chatgptStatus()).connected===true")),"Real ChatGPT account is connected before live writing test");
        click.run("[data-action=page][data-id=settings]");click.run("[data-action=chatgpt-models]");waitFor(js,"!window.Draft2Done.info().busy",90000);
        requireSuccess(js,"Account model catalog");
        if("true".equals(js.run("!!document.querySelector('[data-action=chatgpt-use-writing]')")))click.run("[data-action=chatgpt-use-writing]");
        if("true".equals(js.run("!!document.querySelector('[data-action=chatgpt-recommend-model]:not([disabled])')")))click.run("[data-action=chatgpt-recommend-model]");
        check.run("true".equals(js.run("!!document.querySelector('#chatgpt-model')?.value")),"Live test selects a model actually returned for the account");
        click.run("[data-action=page][data-id=home]");
        String draft="This fictional classroom paper compares distance with practical access to a park. A nearby park may still be hard to use when the route is unsuitable or its opening hours do not fit a resident's schedule. I want to explain those possible barriers without claiming that they have already been measured.\n\nMy argument should distinguish a question from a finding. A student could compare routes and opening hours, then describe what the available evidence can and cannot support. I need a careful next paragraph that develops this distinction without inventing a study, a personal experience, or a causal result.";
        String encoded=android.util.Base64.encodeToString(draft.getBytes("UTF-8"),android.util.Base64.NO_WRAP);
        js.run("window.NativeEvents.imported("+new JSONObject().put("name","ChatGPT-live-012-QA.txt").put("mime","text/plain").put("base64",encoded)+");return true;");waitFor(js,"!!document.querySelector('[data-action=tab][data-id=draft]')",10000);
        qaOriginal=JSONObject.quote(draft);
        qaId=js.run("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p&&p.originalText==="+qaOriginal+"&&!"+before.getJSONArray("projectIds")+".includes(p.id)?p.id:null;");
        if("null".equals(qaId)) { qaId=null; throw new IOException("The newly imported fictional paper could not be identified safely; no other project will be removed."); }
        String original=js.run("const s=window.Draft2Done.getState();return s.projects.find(p=>p.id===s.activeId).originalText;");
        click.run("[data-action=tab][data-id=helper]");click.run("[data-action=analyze]");
        waitFor(js,"!!document.querySelector('[data-action=remote-send]')||!window.Draft2Done.info().busy",10000);
        check.run("true".equals(js.run("!!document.querySelector('#remote-draft-preview')&&document.querySelector('#remote-draft-preview').value==="+JSONObject.quote(draft))),"Actual Helper shows the entire fictional draft before transmission");
        approveAndWait(js,click,check,1);requireSuccess(js,"Actual ChatGPT Helper");
        check.run("true".equals(js.run("const s=window.Draft2Done.getState();return s.projects.find(p=>p.id===s.activeId).helperNotes.length>20;")),"Real completed ChatGPT Helper response is saved and rendered");
        click.run("[data-action=tab][data-id=complete]");
        js.run("const input=document.querySelector('#minimum-input');input.value=Number(document.querySelector('#current-count').textContent.replaceAll(',',''))+30;input.dispatchEvent(new Event('change',{bubbles:true}));return true;");
        click.run("[data-action=generate]");waitFor(js,"!!document.querySelector('[data-action=generate-confirm]')",5000);click.run("[data-action=generate-confirm]");
        approveAndWait(js,click,check,4);requireSuccess(js,"Actual ChatGPT Complete");
        check.run("true".equals(js.run("!!document.querySelector('.proposal-editor')&&document.querySelector('.proposal-editor').value.length>20")),"Real completed ChatGPT continuation is a separate editable proposal");
        click.run("[data-action=regenerate]");approveAndWait(js,click,check,1);requireSuccess(js,"Actual ChatGPT regeneration");
        check.run("true".equals(js.run("!!document.querySelector('.proposal-editor')&&document.querySelector('.proposal-editor').value.length>20")),"Real completed regeneration produces a reviewable section");
        click.run("[data-action=accept]");
        check.run(original.equals(js.run("const s=window.Draft2Done.getState();return s.projects.find(p=>p.id===s.activeId).originalText;")),"Live AI approval preserves the protected original");
        click.run("[data-action=page][data-id=research]");
        research(js,click,"In a fictional classroom, 4 of 10 students chose blue. Based only on this scenario, what proportion chose blue?",false);
        requireSuccess(js,"Actual ChatGPT query-only research");
        check.run("true".equals(js.run("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p.researchBriefs.at(-1)?.text.length>10&&p.researchBriefs.at(-1).webSearchUsed===false;")),"Real query-only research brief is stored separately from the paper");
        research(js,click,"Find the official Wikipedia page about urban green space. Return its canonical URL and one short statement supported by that page.",true);
        requireSuccess(js,"Actual ChatGPT web research");
        check.run("true".equals(js.run("const s=window.Draft2Done.getState();const b=s.projects.find(p=>p.id===s.activeId).researchBriefs.at(-1);return b?.webSearchUsed===true&&((b.sources||[]).length>0||(b.annotations||[]).length>0);")),"Actual account/model web search returns a brief with source links");
        capture.run("chatgpt-live-completed.png");
        } catch(Exception error) {
            failure=error;recordFailure(check,capture,error);throw error;
        } catch(AssertionError error) {
            failure=error;recordFailure(check,capture,error);throw error;
        } finally {
            try { cleanup(js,check,click,before,qaId,qaOriginal); }
            catch(Exception error) { if(failure!=null)failure.addSuppressed(error);else throw error; }
            catch(AssertionError error) { if(failure!=null)failure.addSuppressed(error);else throw error; }
        }
    }
    /** One opt-in web request, independent of the successful writing checks. */
    static void web(JS js,Check check,Click click,Capture capture)throws Exception {
        JSONObject before=snapshot(js);
        String qaId=null,qaOriginal=null;
        Throwable failure=null;
        try {
            check.run("true".equals(js.run("JSON.parse(window.DraftNative.chatgptStatus()).connected===true")),"Real ChatGPT account is connected before the single live web request");
            click.run("[data-action=page][data-id=settings]");click.run("[data-action=chatgpt-models]");waitFor(js,"!window.Draft2Done.info().busy",90000);
            requireSuccess(js,"Account model catalog");
            if("true".equals(js.run("!!document.querySelector('[data-action=chatgpt-recommend-model]:not([disabled])')")))click.run("[data-action=chatgpt-recommend-model]");
            check.run("true".equals(js.run("!!document.querySelector('#chatgpt-model')?.value")),"Web-only check uses a model actually returned for the account");
            String draft="This fictional classroom paper asks how green infrastructure might fit into city planning. It contains no measured findings, personal information, or real assignment. The separate research question will request one public government source without sending this paper.";
            String encoded=android.util.Base64.encodeToString(draft.getBytes("UTF-8"),android.util.Base64.NO_WRAP);
            click.run("[data-action=page][data-id=home]");
            js.run("window.NativeEvents.imported("+new JSONObject().put("name","ChatGPT-web-live-012-QA.txt").put("mime","text/plain").put("base64",encoded)+");return true;");
            waitFor(js,"!!document.querySelector('[data-action=tab][data-id=draft]')",10000);
            qaOriginal=JSONObject.quote(draft);
            qaId=js.run("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p&&p.originalText==="+qaOriginal+"&&!"+before.getJSONArray("projectIds")+".includes(p.id)?p.id:null;");
            if("null".equals(qaId)){qaId=null;throw new IOException("The new fictional web fixture could not be identified safely; no other project will be removed.");}
            click.run("[data-action=page][data-id=research]");
            research(js,click,"Find the official U.S. Environmental Protection Agency page about the benefits of green infrastructure. Return its canonical epa.gov URL and one short statement supported by that page.",true);
            // Report only bounded public-result metadata, never account/session state or another paper.
            String metadata=js.run("const s=window.Draft2Done.getState();const b=s.projects.find(p=>p.id==="+qaId+")?.researchBriefs?.at(-1);const clean=value=>String(value).replace(/Bearer\\s+\\S+/gi,'[redacted]').replace(/\\beyJ[A-Za-z0-9_.-]+/g,'[redacted]').replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}/gi,'[redacted]').replace(/[\\u0000-\\u001f]/g,' ').slice(0,240);return {webSearchUsed:b?.webSearchUsed===true,sourceCount:Array.isArray(b?.sources)?b.sources.length:0,annotationCount:Array.isArray(b?.annotations)?b.annotations.length:0,warnings:Array.isArray(b?.warnings)?b.warnings.slice(0,5).map(clean):[]};");
            check.run(true,"Single live web response metadata: "+metadata);
            js.run("const briefs=document.querySelectorAll('.chatgpt-brief');briefs[briefs.length-1]?.scrollIntoView({block:'start'});return true;");
            capture.run("chatgpt-web-live-result.png");
            requireSuccess(js,"Single actual ChatGPT web research");
            check.run("true".equals(js.run("const s=window.Draft2Done.getState();const b=s.projects.find(p=>p.id==="+qaId+")?.researchBriefs?.at(-1);return b?.webSearchUsed===true&&((b.sources||[]).length>0||(b.annotations||[]).length>0);")),"Single actual web request returns a completed brief with web search and public source links; "+metadata);
        } catch(Exception error) {
            failure=error;recordFailure(check,capture,error);throw error;
        } catch(AssertionError error) {
            failure=error;recordFailure(check,capture,error);throw error;
        } finally {
            try { cleanup(js,check,click,before,qaId,qaOriginal); }
            catch(Exception error) { if(failure!=null)failure.addSuppressed(error);else throw error; }
            catch(AssertionError error) { if(failure!=null)failure.addSuppressed(error);else throw error; }
        }
    }
    private static JSONObject snapshot(JS js)throws Exception {
        return new JSONObject(js.run("const s=window.Draft2Done.getState();const preferences={};for(const key of ['backend','model','chatgptModel','chatgptModels','providers','online','aiRouteChosen'])if(Object.prototype.hasOwnProperty.call(s.settings,key))preferences[key]=s.settings[key];return {activeId:s.activeId??null,projectIds:s.projects.map(p=>p.id),preferences};"));
    }
    private static void recordFailure(Check check,Capture capture,Throwable error) {
        String message=error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage());
        if(message.length()>1500)message=message.substring(0,1500);
        try { check.run(false,"Live test stopped before cleanup: "+message); }
        catch(AssertionError recorded) { /* Check records the provider error before throwing. */ }
        catch(Exception recording) { error.addSuppressed(recording); }
        try { capture.run("chatgpt-live-failed.png"); }
        catch(Exception recording) { error.addSuppressed(recording); }
    }
    private static void cleanup(JS js,Check check,Click click,JSONObject before,String qaId,String qaOriginal)throws Exception {
        if("true".equals(js.run("!!document.querySelector('[data-action=remote-cancel]')")))click.run("[data-action=remote-cancel]");
        if("true".equals(js.run("window.Draft2Done.info().busy&&!!document.querySelector('[data-action=stop]')")))click.run("[data-action=stop]");
        waitFor(js,"!window.Draft2Done.info().busy",10000);
        if("true".equals(js.run("!!document.querySelector('.modal-backdrop')")))click.run("[data-action=modal-close]");
        if(qaId!=null) {
            if(!"true".equals(js.run("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id==="+qaId+");if(!p)return true;if(p.originalText!=="+qaOriginal+"||"+before.getJSONArray("projectIds")+".includes(p.id))return false;s.activeId=p.id;return true;")))throw new IOException("Refusing cleanup because the fictional project's identity changed.");
            if("true".equals(js.run("window.Draft2Done.getState().projects.some(p=>p.id==="+qaId+")"))) {
                click.run("[data-action=page][data-id=write]");click.run("[data-action=paper-menu]");click.run("[data-action=delete]");
                if(!"true".equals(js.run("const s=window.Draft2Done.getState();return s.activeId==="+qaId+"&&s.projects.find(p=>p.id===s.activeId)?.originalText==="+qaOriginal+";")))throw new IOException("Refusing to delete an unrelated active project.");
                click.run("[data-action=delete-confirm]");
            }
        }
        final String removedId=qaId==null?"null":qaId;
        js.run("const s=window.Draft2Done.getState();const before="+before+";for(const key of ['backend','model','chatgptModel','chatgptModels','providers','online','aiRouteChosen']){if(Object.prototype.hasOwnProperty.call(before.preferences,key))s.settings[key]=before.preferences[key];else delete s.settings[key];}s.activeId=s.projects.some(p=>p.id===before.activeId)?before.activeId:null;for(const key of ['notes','chatgptBriefs'])if(s[key]&&"+removedId+"!==null)delete s[key]["+removedId+"];return true;");
        click.run("[data-action=page][data-id=home]");
        // Use the application's existing storage API; never add a checkpoint to a real paper.
        check.run("true".equals(js.run("const s=window.Draft2Done.getState();return window.DraftNative.saveState(JSON.stringify(s))==='ok';")),"Cleanup persists restored state through the normal app storage API");
        check.run("true".equals(js.run("const s=window.Draft2Done.getState();const before="+before+";return window.Draft2Done.info().page==='home'&&!s.projects.some(p=>p.id==="+removedId+")&&before.projectIds.every(id=>s.projects.some(p=>p.id===id))&&s.activeId===(s.projects.some(p=>p.id===before.activeId)?before.activeId:null)&&['backend','model','chatgptModel','chatgptModels','providers','online','aiRouteChosen'].every(key=>JSON.stringify(s.settings[key])===JSON.stringify(before.preferences[key]));")),"Live QA cleanup removes only its own new paper and restores prior selection/preferences");
    }
    private static void research(JS js,Click click,String query,boolean web)throws Exception {
        click.run("[data-action=chatgpt-research]");
        js.run("document.querySelector('#chatgpt-query').value="+JSONObject.quote(query)+";document.querySelector('#chatgpt-web').checked="+web+";document.querySelectorAll('[data-chatgpt-source]').forEach(el=>el.checked=false);return true;");
        click.run("[data-action=chatgpt-research-send]");waitFor(js,"!window.Draft2Done.info().busy",200000);
    }
    private static void approveAndWait(JS js,Click click,Check check,int maximum)throws Exception {
        int sent=0;long deadline=System.currentTimeMillis()+600000;
        while(System.currentTimeMillis()<deadline){
            if("true".equals(js.run("!!document.querySelector('[data-action=remote-send]')"))){
                if(sent>=maximum)throw new IOException("Live QA request budget exceeded; no additional request approved.");
                check.run("true".equals(js.run("!!document.querySelector('#remote-payload-preview')")),"Each real writing step presents its exact request for approval");
                click.run("[data-action=remote-send]");sent++;
            }
            if("true".equals(js.run("!window.Draft2Done.info().busy")))return;
            Thread.sleep(250);
        }
        throw new IOException("Actual ChatGPT writing timed out; no retry or fallback performed.");
    }
    private static void requireSuccess(JS js,String label)throws Exception {
        String error=js.run("window.Draft2Done.info().lastError");
        if(!"\"\"".equals(error)&&!"null".equals(error))throw new IOException(label+" failed: "+error);
    }
    private static void waitFor(JS js,String predicate,int timeout)throws Exception {
        long end=System.currentTimeMillis()+timeout;
        while(System.currentTimeMillis()<end){if("true".equals(js.run(predicate)))return;Thread.sleep(200);}
        throw new IOException("Live ChatGPT check timed out: "+predicate);
    }
}
