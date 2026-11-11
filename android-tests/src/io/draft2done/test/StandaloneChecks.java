package io.draft2done.test;

import org.json.JSONObject;

/** Phone UI/bridge integration fixtures. No assist request reaches a provider. */
final class StandaloneChecks {
    interface JS { String run(String source) throws Exception; }
    interface Check { void run(boolean condition,String label) throws Exception; }
    interface Click { void run(String selector) throws Exception; }
    interface Capture { void run(String name) throws Exception; }
    private StandaloneChecks() {}
    static void run(JS js,Check check,Click click,Capture capture) throws Exception {
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
    }
    private static void waitFor(JS js,String predicate,int timeout)throws Exception {
        long deadline=System.currentTimeMillis()+timeout;
        while(System.currentTimeMillis()<deadline){if("true".equals(js.run(predicate)))return;Thread.sleep(150);}
        throw new IllegalStateException("Standalone UI timed out: "+predicate);
    }
}
