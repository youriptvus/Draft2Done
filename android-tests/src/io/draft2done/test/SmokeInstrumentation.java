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
  waitJS("!!window.Draft2Done",20000);check(true,"Release UI loaded on the authorized Android test device");
  if("signin-listener-old".equals(options.getString("mode"))||"signin-listener-fixed".equals(options.getString("mode"))){SignInListenerChecks.run(this,activity,this::check,"signin-listener-fixed".equals(options.getString("mode")));writeReport();result.putString("stream","Isolated actual sign-in listener preconnect checks completed; browser authorization intercepted and original account files/preferences restored.");finish(Activity.RESULT_OK,result);return;}
  if("draft-input-store".equals(options.getString("mode"))){DraftInputStoreChecks.run(activity,this::check);writeReport();result.putString("stream","Isolated native durable draft-input store checks passed.");finish(Activity.RESULT_OK,result);return;}
  if("paste-restart-recover".equals(options.getString("mode"))){Release014RestartChecks.recover(activity,this::evaluate,this::click,this::check);writeReport();result.putString("stream","Private restart checkpoint and exact prior workspace/staging recovered.");finish(Activity.RESULT_OK,result);return;}
  if("paste-restart-start".equals(options.getString("mode"))||"paste-restart-verify".equals(options.getString("mode"))){if("paste-restart-start".equals(options.getString("mode")))Release014RestartChecks.start(activity,this::evaluate,this::click,this::check);else Release014RestartChecks.verify(activity,this::evaluate,this::click,this::check);writeReport();result.putString("stream","Process-restart staging phase completed; see exact restoration checks.");finish(Activity.RESULT_OK,result);return;}
  if("document-import-debug".equals(options.getString("mode"))){NativeDocumentWorkflowChecks.quickImport(this,activity,this::evaluate,this::click,this::check);writeReport();result.putString("stream","Focused real TXT picker import passed and workspace restored.");finish(Activity.RESULT_OK,result);return;}
  if("release014".equals(options.getString("mode"))||"research014-live".equals(options.getString("mode"))){Release014Checks.run(this,activity,this::evaluate,this::click,this::check,"research014-live".equals(options.getString("mode")));writeReport();result.putString("stream","Release0.1.4 UI and selected public API checks passed; workspace and staged paste restored. No ChatGPT inference sent.");finish(Activity.RESULT_OK,result);return;}
  if("document-workflow".equals(options.getString("mode"))){NativeDocumentWorkflowChecks.run(this,activity,this::evaluate,this::click,this::check);writeReport();result.putString("stream","Real Android save/open document and Save as PDF workflows passed; fictional QA files removed and workspace restored.");finish(Activity.RESULT_OK,result);return;}
  if("document-recover".equals(options.getString("mode"))){NativeDocumentWorkflowChecks.recover(this,activity,this::evaluate,this::click,this::check);writeReport();result.putString("stream","Document recovery completed; see per-check results for the recovered checkpoint and preserved data.");finish(Activity.RESULT_OK,result);return;}
  if("chatgpt-writing-live".equals(options.getString("mode"))){ChatGPTLiveChecks.run(this::evaluate,this::check,this::click,this::capture);writeReport();result.putString("stream","Direct ChatGPT writing checks completed; see per-check results for provider limits.");finish(Activity.RESULT_OK,result);return;}
  if("chatgpt-web-live".equals(options.getString("mode"))){ChatGPTLiveChecks.web(this::evaluate,this::check,this::click,this::capture);writeReport();result.putString("stream","Direct ChatGPT web-research checks completed; see per-check results for tool availability.");finish(Activity.RESULT_OK,result);return;}
  if("web-diagnostic".equals(options.getString("mode"))){String model=(String)new JSONTokener(evaluate("window.Draft2Done.getState().settings.chatgptModel")).nextValue();JSONObject metadata=ChatGPTWebDiagnosticChecks.run(activity,model,this::check);check(true,"Sanitized public-query stream structure: "+metadata.toString());writeReport();result.putString("stream","Single public web diagnostic finished; structural metadata only, no raw stream or account data saved.");finish(Activity.RESULT_OK,result);return;}
  if("chatgpt-refresh-live".equals(options.getString("mode"))){ChatGPTAccountChecks.run(activity,this::check);writeReport();result.putString("stream","Explicit renewable-session checks completed; see per-check results.");finish(Activity.RESULT_OK,result);return;}
  if("chatgpt-signout-live".equals(options.getString("mode"))){ChatGPTAccountChecks.signOut(activity,this::check);writeReport();result.putString("stream","Explicit sign-out/revocation checks completed; see per-check results.");finish(Activity.RESULT_OK,result);return;}
  if("preservation-diagnostic".equals(options.getString("mode"))){
    File baselineFile=new File(getTargetContext().getExternalFilesDir("qa"),"upgrade-baseline.json");
    JSONObject baseline;try(InputStream in=new FileInputStream(baselineFile)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);baseline=new JSONObject(new String(out.toByteArray(),"UTF-8"));}
    check(true,"Current structural metadata only: "+evaluate("const s=window.Draft2Done.getState();return {projects:s.projects.length,profiles:s.profiles.length,notes:Object.keys(s.notes).length,qaProjects:s.projects.map(p=>({knownQATitle:/^(?:Draft2Done-QA-|Standalone-QA|Samsung-QA|\\[QA\\])/.test(p.title),createdDuring014:String(p.createdAt)>='2026-11-11T04:50',originalLength:p.originalText.length,workingLength:p.workingText.length,revisions:p.revisions.length,sources:p.sources.length})),activePresent:!!s.activeId};"));
    int total=Integer.parseInt(evaluate("window.Draft2Done.getState().projects.length"));
    int matchingPrefix=-1;String matchingId=null;
    if(total<=30)for(int size=total;size>=0&&matchingPrefix<0;size--){
      JSONArray ids=new JSONArray(evaluate("const s=window.Draft2Done.getState();return [null,...s.projects.slice(0,"+size+").map(p=>p.id)];"));
      for(int i=0;i<ids.length();i++){Object candidate=ids.get(i);String encodedId=candidate==JSONObject.NULL?"null":JSONObject.quote((String)candidate);String value=evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects.slice(0,"+size+"),profiles:s.profiles,notes:s.notes,activeId:"+encodedId+"});");if(hash(value).equals(baseline.getString("workspaceHash"))){matchingPrefix=size;matchingId=encodedId;break;}}
    }
    check(true,"Read-only original-baseline prefix match: "+(matchingPrefix>=0)+"; original project count: "+matchingPrefix+"; possible added fictional tail: "+(matchingPrefix<0?-1:total-matchingPrefix));
    if("true".equals(options.getString("repairQaTail"))){
      check(matchingPrefix>=0,"Exact original workspace fingerprint is found before any QA cleanup mutation");
      String fixture="Urban green spaces give people places to meet, walk, and rest. This fictional paper asks how access to those spaces is related to wellbeing. A park can be nearby on a map and still feel difficult to reach. The argument should consider the route, opening hours, and the needs of different residents.\n\nI want to compare access with distance rather than treating them as the same idea. I also need to discuss the limits of the available evidence. My next section should explain how a student could evaluate those questions without inventing results or assuming that an association proves a cause.";
      check("true".equals(evaluate("const s=window.Draft2Done.getState();return s.projects.slice("+matchingPrefix+").every(p=>/^(?:Standalone-QA|\\[QA\\])/.test(p.title)&&String(p.createdAt)>='2026-11-11T04:50'&&p.originalText==="+JSONObject.quote(fixture)+");")),"Every removable tail record is a known recent exact fictional writing-test fixture");
      String before=(String)new JSONTokener(evaluate("JSON.stringify(window.Draft2Done.getState())")).nextValue();
      File recovery=new File(activity.getCacheDir(),"qa-tail-before-repair-"+java.util.UUID.randomUUID().toString()+".json");
      try(FileOutputStream out=new FileOutputStream(recovery)){out.write(before.getBytes("UTF-8"));out.getFD().sync();}
      evaluate("const s=window.Draft2Done.getState();s.projects=s.projects.slice(0,"+matchingPrefix+");s.activeId="+matchingId+";if(window.DraftNative.saveState(JSON.stringify(s))!=='ok')throw Error('Scoped QA cleanup save failed');return true;");
      check(hash(evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:s.activeId});")).equals(baseline.getString("workspaceHash")),"Scoped fictional-tail cleanup recovers the exact complete pre-upgrade workspace fingerprint");
      check(hash(evaluate("const s=JSON.parse(window.DraftNative.loadState());return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:s.activeId});")).equals(baseline.getString("workspaceHash")),"Scoped QA cleanup is committed to actual native workspace storage before leaving the harness");
      evaluate("setTimeout(()=>location.reload(),80);return true;");Thread.sleep(1000);waitJS("!!window.Draft2Done",20000);
      check(hash(evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:s.activeId});")).equals(baseline.getString("workspaceHash")),"A normal application reload retains the exact original pre-upgrade content and active selection");
    }
    for(String origin:new String[]{"workspace.json","workspace.backup.json"}){
      File inputFile=new File(activity.getFilesDir(),origin);if(!inputFile.isFile())continue;
      String raw;try(InputStream in=new FileInputStream(inputFile)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int count;while((count=in.read(bytes))!=-1){if(out.size()+count>4*1024*1024)throw new IOException("Workspace diagnostic bound");out.write(bytes,0,count);}raw=new String(out.toByteArray(),"UTF-8");}
      String encoded=JSONObject.quote(raw);JSONArray ids=new JSONArray(evaluate("const s=JSON.parse("+encoded+");return [s.activeId,null,...s.projects.map(p=>p.id)];"));boolean matched=false;
      for(int i=0;i<ids.length();i++){Object candidate=ids.get(i);String id=candidate==JSONObject.NULL?"null":JSONObject.quote((String)candidate);String value=evaluate("const s=JSON.parse("+encoded+");return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:"+id+"});");if(hash(value).equals(baseline.getString("workspaceHash"))){matched=true;break;}}
      check(true,"Read-only "+origin+" baseline match allowing active selection only: "+matched);
    }
    writeReport();result.putString("stream","true".equals(options.getString("repairQaTail"))?"Exact original baseline restored by removing only verified recent fictional QA records; original project text and account unchanged.":"Bounded read-only metadata diagnostic completed; no workspace content or account data changed.");finish(Activity.RESULT_OK,result);return;
  }
  if("upgrade-selection-recover".equals(options.getString("mode"))){
    File baselineFile=new File(getTargetContext().getExternalFilesDir("qa"),"upgrade-baseline.json");
    JSONObject baseline;try(InputStream in=new FileInputStream(baselineFile)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);baseline=new JSONObject(new String(out.toByteArray(),"UTF-8"));}
    String connection=evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};");
    check(hash(connection).equals(baseline.getString("sessionHash")),"Read-only preservation audit confirms the original connected account");
    JSONArray candidates=new JSONArray(evaluate("const s=window.Draft2Done.getState();return [null,...s.projects.map(p=>p.id)];"));
    Object match=null;boolean found=false;
    for(int i=0;i<candidates.length();i++){Object candidate=candidates.get(i);String encoded=candidate==JSONObject.NULL?"null":JSONObject.quote((String)candidate);String value=evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:"+encoded+"});");if(hash(value).equals(baseline.getString("workspaceHash"))){match=candidate;found=true;break;}}
    check(found,"Every original project, draft, revision, profile and note matches the pre-upgrade hash when only the active-paper selection is restored");
    String encoded=match==JSONObject.NULL?"null":JSONObject.quote((String)match);
    evaluate("window.Draft2Done.getState().activeId="+encoded+";window.NativeEvents.pause();return true;");
    String restored=hash(evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:s.activeId});"));
    check(restored.equals(baseline.getString("workspaceHash")),"Restoring only the test-changed selection exactly recovers the full original pre-upgrade workspace fingerprint");
    writeReport();result.putString("stream","Original pre-upgrade content/account fingerprint recovered by restoring only active-paper selection; no text changed.");finish(Activity.RESULT_OK,result);return;
  }
  if("upgrade-snapshot".equals(options.getString("mode"))||"upgrade-verify".equals(options.getString("mode"))){
    File baselineFile=new File(getTargetContext().getExternalFilesDir("qa"),"upgrade-baseline.json");baselineFile.getParentFile().mkdirs();
    String fingerprint=hash(evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,activeId:s.activeId});"));
    String connection=evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return {connected:!!s.connected,active:s.active||''};");
    if("upgrade-snapshot".equals(options.getString("mode"))){
      JSONObject baseline=new JSONObject().put("workspaceHash",fingerprint).put("sessionHash",hash(connection));
      try(OutputStream out=new FileOutputStream(baselineFile)){out.write(baseline.toString().getBytes("UTF-8"));}
      check(true,"Pre-upgrade workspace and connected-account fingerprints recorded without draft text or credentials");writeReport();result.putString("stream","Pre-upgrade fingerprints saved");finish(Activity.RESULT_OK,result);return;
    }
    String expected=options.getString("expectedVersion");long expectedCode=Long.parseLong(options.getString("expectedCode","0"));if(expected==null||expectedCode<1)throw new IllegalArgumentException("Expected release identity is required.");
    JSONObject baseline;try(InputStream in=new FileInputStream(baselineFile)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);baseline=new JSONObject(new String(out.toByteArray(),"UTF-8"));}
    check(fingerprint.equals(baseline.getString("workspaceHash")),"Same-key version upgrade preserves all projects, original/working drafts, profiles, notes and active paper");
    check(hash(connection).equals(baseline.getString("sessionHash")),"Version upgrade preserves the connected encrypted account session");
    android.content.pm.PackageInfo installed=getTargetContext().getPackageManager().getPackageInfo("io.draft2done.app",0);long code=android.os.Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
    check(expected.equals(installed.versionName)&&code==expectedCode,"Installed Android package has the requested version name and increased version code");
    check("true".equals(evaluate("JSON.parse(window.DraftNative.appInfo()).version==="+JSONObject.quote(expected))),"Native app information reports the installed release version");
    click("[data-action=page][data-id=home]");check("true".equals(evaluate("document.querySelector('.version-footer').textContent.includes("+JSONObject.quote(expected)+")")),"Welcome footer reports the current bundled version");
    click("[data-action=page][data-id=settings]");click("[data-action=about]");check("true".equals(evaluate("document.querySelector('.modal-heading').textContent.includes("+JSONObject.quote(expected)+")")),"About dialog agrees with the installed release version");capture("release-version.png");click("[data-action=modal-close]");click("[data-action=page][data-id=home]");
    writeReport();result.putString("stream","Installed upgrade, preserved data/session and all version displays verified");finish(Activity.RESULT_OK,result);return;
  }
  if("updates-live".equals(options.getString("mode"))){
    String expectedVersion=options.getString("expectedVersion"),expectedSha=options.getString("expectedSha");long expectedCode=Long.parseLong(options.getString("expectedCode","0"));
    if(expectedVersion==null||expectedSha==null||!expectedSha.matches("[a-f0-9]{64}")||expectedCode<1)throw new IllegalArgumentException("Supply independently verified expectedVersion, expectedCode and expectedSha.");
    click("[data-action=page][data-id=settings]");check("true".equals(evaluate("document.querySelector('[data-setting=autoUpdates]').checked===true")),"Automatic update checks remain enabled");
    click("[data-action=updates-check]");waitJS("!window.Draft2Done.info().busy",90000);
    check("true".equals(evaluate("const s=window.Draft2Done.getState();return !window.Draft2Done.info().lastError&&s.updateInfo?.repository==='https://github.com/youriptvus/Draft2Done'&&s.updateInfo.version==="+JSONObject.quote(expectedVersion)+"&&s.updateInfo.versionCode==="+expectedCode+"&&s.updateInfo.sha256==="+JSONObject.quote(expectedSha)+";")),"Installed release retrieves the actual public manifest with the independently verified version and hash");
    android.content.pm.PackageInfo installed=getTargetContext().getPackageManager().getPackageInfo("io.draft2done.app",0);long installedCode=android.os.Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;boolean updateExpected=expectedCode>installedCode;
    check("true".equals(evaluate("window.Draft2Done.getState().updateInfo.available==="+updateExpected)),"Published version is compared with the actual installed package version");
    check("true".equals(evaluate("window.Draft2Done.getState().updateInfo.message==="+JSONObject.quote(updateExpected?"A new Draft2Done version is ready.":"Draft2Done is up to date."))),"Public release status is displayed honestly");
    evaluate("document.querySelector('[data-action=updates-check]').closest('.card').scrollIntoView({block:'start'});return true;");capture("public-release-status.png");
    writeReport();result.putString("stream","Public GitHub update check passed: "+evaluate("window.Draft2Done.getState().updateInfo.message"));finish(Activity.RESULT_OK,result);return;
  }
  if("standalone".equals(options.getString("mode"))){StandaloneChecks.run(activity,this::evaluate,this::check,this::click,this::capture);writeReport();result.putString("stream","Standalone phone UI and consent passed with simulated assist replies; no provider inference sent.");finish(Activity.RESULT_OK,result);return;}
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
 private String hash(String value)throws Exception{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();}
 private void capture(String name)throws Exception{Thread.sleep(600);android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();if(screenshot==null)throw new IOException("Could not capture test screen");File directory=getTargetContext().getExternalFilesDir("qa");directory.mkdirs();try(OutputStream out=new FileOutputStream(new File(directory,name))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screenshot.recycle();}}
 private void waitExternal()throws Exception{long end=System.currentTimeMillis()+15000;while(System.currentTimeMillis()<end){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null&&!"io.draft2done.app".contentEquals(root.getPackageName())&&!"com.android.systemui".contentEquals(root.getPackageName()))return;Thread.sleep(200);}throw new IllegalStateException("Android document/print interface did not open.");}
 private void writeReport()throws Exception{File directory=getTargetContext().getExternalFilesDir("qa");if(directory==null)throw new IOException("QA storage unavailable");directory.mkdirs();String mode=options.getString("mode","smoke");if("verify-reopen".equals(mode))mode="reopen";try(OutputStream out=new FileOutputStream(new File(directory,mode+".json"))){out.write(new JSONObject().put("checks",checks).toString(2).getBytes("UTF-8"));}}
}
