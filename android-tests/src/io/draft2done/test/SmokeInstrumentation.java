package io.draft2done.test;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import org.json.*;
import java.io.*;
import java.util.concurrent.*;

/** Signed local test harness; never bundled into the release application. */
public final class SmokeInstrumentation extends Instrumentation {
 private Activity activity;private WebView web;private final JSONArray checks=new JSONArray();private Bundle options;
 @Override public void onCreate(Bundle args){options=args==null?new Bundle():args;start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Intent intent=new Intent().setClassName("io.draft2done.app","io.draft2done.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
  activity=startActivitySync(intent);runOnMainSync(()->web=find(activity.getWindow().getDecorView()));
  waitJS("!!window.Draft2Done",20000);check(true,"Release UI loaded on Samsung");
  if("standalone".equals(options.getString("mode"))){StandaloneChecks.run(this::evaluate,this::check,this::click,this::capture);writeReport();result.putString("stream","Standalone phone UI and consent passed with simulated assist replies; no provider inference sent.");finish(Activity.RESULT_OK,result);return;}
  if("chatgpt-status".equals(options.getString("mode"))){result.putString("stream","ChatGPT connection status: "+evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,accounts:(s.accounts||[]).map(a=>({signedIn:!!a.signedIn,planEnabled:!!a.planEnabled})),error:s.error||'',signingIn:!!s.signingIn};"));finish(Activity.RESULT_OK,result);return;}
  if("models-audit".equals(options.getString("mode"))){
    click("[data-action=page][data-id=settings]");click("[data-action=chatgpt-models]");waitJS("!window.Draft2Done.info().busy",90000);
    check("true".equals(evaluate("!window.Draft2Done.info().lastError&&!!document.querySelector('#chatgpt-model')?.value")),"Live eligible account model catalog loaded without inference");
    result.putString("stream","Eligible model catalog: "+evaluate("return Array.from(document.querySelector('#chatgpt-model').options).filter(o=>o.value).map(o=>({slug:o.value,name:o.textContent}));"));writeReport();finish(Activity.RESULT_OK,result);return;
  }
  if("chatgpt-verify-limit".equals(options.getString("mode"))){
    check("true".equals(evaluate("JSON.parse(window.DraftNative.chatgptStatus()).connected===true")),"Previously authenticated ChatGPT account survives process restart");
    check("true".equals(evaluate("JSON.parse(window.DraftNative.chatgptStatus()).error===''")),"Successful connection clears earlier sign-in error across process restart");
    check("true".equals(evaluate("return (()=>{const p=window.Draft2Done.getState().projects.filter(p=>p.title==='[QA] ChatGPT research check');return p.length>0&&p.every(p=>p.workingText===''&&p.researchBriefs.length===0);})();")),"Previously usage-limited requests did not save partial research or change a paper");
    if("subscription_sharing_usage_limit_exceeded".equals(options.getString("observedCode"))){
      NativeBoundaryChecks.recordObservedQuota(activity);
      click("[data-action=page][data-id=settings]");
      check("true".equals(evaluate("let s=JSON.parse(window.DraftNative.chatgptStatus());return s.connected&&s.error===''&&s.researchError.includes('usage limit')&&document.querySelector('.chatgpt-card').textContent.includes('Last AI request');")),"Previously observed quota failure is recorded locally and shown separately from sign-in status");
    }
    writeReport();result.putString("stream","Connected-session persistence and prior quota-failure safety verified; no new plan request sent.");finish(Activity.RESULT_OK,result);return;
  }
  if("recovery".equals(options.getString("mode"))){NativeBoundaryChecks.recovery(activity,this::check);writeReport();result.putString("stream","Native atomic save/recovery fault checks passed");finish(Activity.RESULT_OK,result);return;}
  if("documents".equals(options.getString("mode"))){NativeBoundaryChecks.documents(this,activity,this::evaluate,this::check);writeReport();result.putString("stream","Native document byte roundtrips and limits passed");finish(Activity.RESULT_OK,result);return;}
  if("restore-home".equals(options.getString("mode"))){click("[data-action=page][data-id=settings]");evaluate("let el=document.querySelector('[data-setting=theme]');el.value='dark';el.dispatchEvent(new Event('change',{bubbles:true}));true");click("[data-action=page][data-id=home]");Thread.sleep(1200);check(evaluate("document.documentElement.dataset.theme==='dark'&&!!document.querySelector('.welcome')").equals("true"),"Elegant dark welcome screen selected");writeReport();result.putString("stream","Dark welcome screen selected");finish(Activity.RESULT_OK,result);return;}
  if("screens".equals(options.getString("mode"))){
    runOnMainSync(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));Thread.sleep(1200);
    click("[data-action=page][data-id=home]");capture("dark-home.png");
    click("[data-action=page][data-id=write]");capture("dark-editor.png");
    String height=evaluate("window.innerHeight");evaluate("document.querySelector('#draft-editor').focus();true");
    runOnMainSync(()->{web.requestFocus();((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(web,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});Thread.sleep(1200);
    boolean[] visible={false};runOnMainSync(()->{if(android.os.Build.VERSION.SDK_INT>=30)visible[0]=web.getRootWindowInsets().isVisible(WindowInsets.Type.ime());});
    check(visible[0],"Samsung keyboard opens for the document editor");check(evaluate("document.querySelector('#draft-editor').getBoundingClientRect().top>=0 && document.querySelector('#draft-editor').getBoundingClientRect().top<window.innerHeight-50").equals("true"),"Writing area stays visible above the Samsung keyboard");capture("keyboard-editor.png");
    runOnMainSync(()->((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(web.getWindowToken(),0));evaluate("document.querySelector('#draft-editor').blur();true");Thread.sleep(700);
    click("[data-action=page][data-id=home]");runOnMainSync(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));writeReport();result.putString("stream","Phone screenshots and keyboard check completed");finish(Activity.RESULT_OK,result);return;
  }
  if("pickers".equals(options.getString("mode"))){
    evaluate("window.DraftNative.importDocument();true");waitExternal();check(true,"Android import document picker opens");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);Thread.sleep(800);
    evaluate("window.DraftNative.exportDocument('Draft2Done-QA.txt','text/plain','RHJhZnQyRG9uZSBRQSBleHBvcnQu');true");waitExternal();check(true,"Android export document picker opens");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);Thread.sleep(800);
    evaluate("window.DraftNative.printDocument('<html><body><h1>Draft2Done QA</h1><p>Fictional print preview.</p></body></html>');true");waitExternal();check(true,"Android PDF print interface opens");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);Thread.sleep(800);writeReport();result.putString("stream","Native document picker and print checks passed");finish(Activity.RESULT_OK,result);return;
  }
  if("verify-reopen".equals(options.getString("mode"))){String checkpoint=options.getString("checkpoint","Samsung save check");check(evaluate("window.Draft2Done.getState().projects.some(p=>p.title==='[QA] Samsung draft'&&p.workingText.includes("+JSONObject.quote(checkpoint)+"))").equals("true"),"Accepted working text survived process restart");writeReport();result.putString("stream","Reopen check passed");finish(Activity.RESULT_OK,result);return;}
  if("research".equals(options.getString("mode"))){click("[data-action=page][data-id=research]");evaluate("let el=document.querySelector('#research-online');el.checked=true;el.dispatchEvent(new Event('change',{bubbles:true}));true");evaluate("document.querySelector('#research-query').value='urban green space wellbeing';true");click("[data-action=search]");waitJS("!window.Draft2Done.info().busy",90000);check(evaluate("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).sources.length>0").equals("true"),"Live scholarly sources retrieved from Samsung native transport");evaluate("let el=document.querySelector('#research-online');el.checked=false;el.dispatchEvent(new Event('change',{bubbles:true}));true");Thread.sleep(1000);writeReport();result.putString("stream","Device research check passed");finish(Activity.RESULT_OK,result);return;}
  if("signin".equals(options.getString("mode"))){
    click("[data-action=page][data-id=settings]");click("[data-action=chatgpt-signin]");
    long deadline=System.currentTimeMillis()+300000;boolean connected=false;
    while(System.currentTimeMillis()<deadline){
      connected="true".equals(evaluate("JSON.parse(window.DraftNative.chatgptStatus()).connected===true"));if(connected)break;
      if("true".equals(evaluate("!window.Draft2Done.info().busy&&!!window.Draft2Done.info().lastError")))throw new IOException("ChatGPT sign-in ended without connecting. Inspect the app connection message.");
      Thread.sleep(500);
    }
    check(connected,"Personal ChatGPT consent completed with plan permission");writeReport();result.putString("stream","Personal ChatGPT connected; no account credentials included in report.");finish(Activity.RESULT_OK,result);return;
  }
  if("chatgpt-live".equals(options.getString("mode"))){
    check("true".equals(evaluate("JSON.parse(window.DraftNative.chatgptStatus()).connected===true")),"ChatGPT plan connection is active");
    click("[data-action=page][data-id=settings]");click("[data-action=chatgpt-models]");waitJS("!window.Draft2Done.info().busy",90000);
    check("true".equals(evaluate("!window.Draft2Done.info().lastError&&!!document.querySelector('#chatgpt-model')?.value")),"Eligible ChatGPT model catalog loads and selects an available model");
    click("[data-action=page][data-id=home]");click("[data-action=new]");
    click(".paper-title");evaluate("document.querySelector('#rename-input').value='[QA] ChatGPT research check';return true;");click("[data-action=rename-save]");
    click("[data-action=page][data-id=research]");click("[data-action=chatgpt-research]");
    evaluate("document.querySelector('#chatgpt-query').value='In a fictional classroom, 4 of 10 students chose blue. Based only on this scenario, what proportion chose blue?';document.querySelector('#chatgpt-web').checked=false;document.querySelectorAll('[data-chatgpt-source]').forEach(el=>el.checked=false);return true;");
    click("[data-action=chatgpt-research-send]");waitJS("!window.Draft2Done.info().busy",190000);
    check("true".equals(evaluate("!window.Draft2Done.info().lastError&&document.querySelector('.research-answer')?.textContent.length>10")),"A minimal fictional request succeeds using the connected ChatGPT plan; diagnostic="+evaluate("window.Draft2Done.info().lastError"));
    check("true".equals(evaluate("return (()=>{let s=window.Draft2Done.getState();let p=s.projects.find(p=>p.id===s.activeId);return p.workingText===''&&p.researchBriefs?.length===1&&p.researchBriefs[0].webSearchUsed===false;})();")),"Research brief is saved separately without uploading or changing a paper");
    writeReport();result.putString("stream","Live ChatGPT model and minimal research request checks passed; no account details or credentials included.");finish(Activity.RESULT_OK,result);return;
  }
  click("[data-action=page][data-id=settings]");
  evaluate("document.querySelector('[data-setting=theme]').value='dark';document.querySelector('[data-setting=theme]').dispatchEvent(new Event('change',{bubbles:true}));true");check(evaluate("document.documentElement.dataset.theme").contains("dark"),"Dark theme applied on device");
  click("[data-action=page][data-id=home]");
  String fixture="Urban green spaces give people places to meet, walk, and rest. This fictional paper asks how access to those spaces is related to wellbeing. A park can be nearby on a map and still feel difficult to reach. The argument should consider the route, opening hours, and the needs of different residents.\n\nI want to compare access with distance rather than treating them as the same idea. I also need to discuss the limits of the available evidence. My next section should explain how a student could evaluate those questions without inventing results or assuming that an association proves a cause.";
  evaluate("window.NativeEvents.imported("+new JSONObject().put("name","Samsung-QA.txt").put("mime","text/plain").put("base64",android.util.Base64.encodeToString(fixture.getBytes("UTF-8"),android.util.Base64.NO_WRAP)).toString()+");return true;");
  waitJS("!!document.querySelector('#draft-editor')",5000);
  evaluate("document.querySelector('.paper-title').click();true");waitJS("!!document.querySelector('#rename-input')",3000);evaluate("document.querySelector('#rename-input').value='[QA] Samsung draft';true");click("[data-action=rename-save]");
  String original=evaluate("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).originalText");
  String checkpoint=options.getString("checkpoint","Samsung save check");
  evaluate("let el=document.querySelector('#draft-editor');el.value+=String.fromCharCode(10,10)+"+JSONObject.quote("Samsung save check: "+checkpoint+". Access means listening to the community.")+";el.dispatchEvent(new Event('input',{bubbles:true}));true");click("[data-action=save]");Thread.sleep(1100);
  check(original.equals(evaluate("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).originalText")),"Editing leaves original unchanged");
  click("[data-action=page][data-id=dna]");check(evaluate("!!document.querySelector('.metric-grid')").equals("true"),"Writer DNA visible on device");
  click("[data-action=page][data-id=settings]");evaluate("document.querySelector('summary.advanced-title').click();true");
  evaluate("for(let [key,value] of Object.entries({backend:'ollama',endpoint:'http://127.0.0.1:11435',model:'llama3:8b-instruct-q4_K_M',context:8192,maxTokens:1200})){let el=document.querySelector('[data-setting='+key+']');el.value=value;el.dispatchEvent(new Event('change',{bubbles:true}));}true");
  click("[data-action=test-ai]");waitJS("!window.Draft2Done.info().busy",90000);check(evaluate("window.Draft2Done.info().lastError").equals("\"\""),"Real Ollama inference from phone over USB reverse");
  click("[data-action=page][data-id=write]");click("[data-action=tab][data-id=helper]");click("[data-action=analyze]");waitJS("!window.Draft2Done.info().busy",90000);check(evaluate("document.querySelector('.response-card')?.textContent.length>100").equals("true"),"Real Helper Mode returned suggestions on phone");
  click("[data-action=tab][data-id=complete]");evaluate("let el=document.querySelector('#minimum-input');el.value=Number(document.querySelector('#current-count').textContent.replaceAll(',',''))+55;el.dispatchEvent(new Event('change',{bubbles:true}));true");
  click("[data-action=generate]");waitJS("!!document.querySelector('[data-action=generate-confirm]')",5000);click("[data-action=generate-confirm]");waitJS("!window.Draft2Done.info().busy",90000);check(evaluate("!!document.querySelector('.proposal-editor')").equals("true"),"Real Complete Mode produced separate proposal on phone");
  click("[data-action=accept]");check(evaluate("window.Draft2Done.getState().projects.find(p=>p.id===window.Draft2Done.getState().activeId).contributions.length>0").equals("true"),"Explicit proposal acceptance recorded contribution");
  click("[data-action=tab][data-id=draft]");click("[data-action=save]");Thread.sleep(1100);
  click("[data-action=page][data-id=research]");check(evaluate("document.querySelector('#research-online').checked===false").equals("true"),"Research remains optional and off by default");
  click("[data-action=page][data-id=settings]");check(evaluate("document.querySelector('[data-setting=autoUpdates]').checked").equals("true"),"Automatic update checks enabled by default");
  click("[data-action=updates-check]");waitJS("!window.Draft2Done.info().busy",30000);check(evaluate("!window.Draft2Done.info().lastError&&typeof window.Draft2Done.getState().updateInfo?.available==='boolean'&&!!window.Draft2Done.getState().updateInfo.message").equals("true"),"GitHub update check returns a release status");
  click("[data-action=page][data-id=write]");Thread.sleep(800);writeReport();result.putString("stream","Device checks passed: "+checks.length());finish(Activity.RESULT_OK,result);
 }catch(Throwable error){try{writeReport();}catch(Exception ignored){}result.putString("stream","Device test failed: "+error.getClass().getSimpleName()+": "+error.getMessage());finish(Activity.RESULT_CANCELED,result);}}
 private WebView find(View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){WebView found=find(group.getChildAt(i));if(found!=null)return found;}}return null;}
 private String evaluate(String js)throws Exception{if(web==null)throw new IllegalStateException("WebView not found");CountDownLatch latch=new CountDownLatch(1);String[] out={null};activity.runOnUiThread(()->web.evaluateJavascript("(()=>{"+(js.contains(";")?js:"return ("+js+");")+"})()",value->{out[0]=value;latch.countDown();}));if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("JavaScript action timed out");return out[0]==null?"null":out[0];}
 private void waitJS(String js,int timeout)throws Exception{long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){if("true".equals(evaluate(js)))return;Thread.sleep(200);}throw new IllegalStateException("Timed out waiting for "+js);}
 private void click(String selector)throws Exception{String quoted=JSONObject.quote(selector);String result=evaluate("let el=document.querySelector("+quoted+");if(!el)throw new Error('Missing action');el.click();return true;");if(!"true".equals(result))throw new IllegalStateException("Action unavailable: "+selector);Thread.sleep(200);}
 private void check(boolean condition,String label)throws Exception{checks.put(new JSONObject().put("check",label).put("passed",condition));if(!condition)throw new AssertionError(label);}
 private void capture(String name)throws Exception{Thread.sleep(600);android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();if(screenshot==null)throw new IOException("Could not capture test screen");File directory=getTargetContext().getExternalFilesDir("qa");directory.mkdirs();try(OutputStream out=new FileOutputStream(new File(directory,name))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screenshot.recycle();}}
 private void waitExternal()throws Exception{long end=System.currentTimeMillis()+15000;while(System.currentTimeMillis()<end){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null&&!"io.draft2done.app".contentEquals(root.getPackageName())&&!"com.android.systemui".contentEquals(root.getPackageName()))return;Thread.sleep(200);}throw new IllegalStateException("Android document/print interface did not open.");}
 private void writeReport()throws Exception{File directory=getTargetContext().getExternalFilesDir("qa");if(directory==null)throw new IOException("QA storage unavailable");directory.mkdirs();String mode=options.getString("mode","smoke");if("verify-reopen".equals(mode))mode="reopen";try(OutputStream out=new FileOutputStream(new File(directory,mode+".json"))){out.write(new JSONObject().put("checks",checks).toString(2).getBytes("UTF-8"));}}
}
