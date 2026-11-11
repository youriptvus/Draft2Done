package io.draft2done.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Real Android chooser/print workflows. All shared files contain fictional QA text. */
final class NativeDocumentWorkflowChecks {
    interface Evaluator { String evaluate(String js)throws Exception; }
    interface Clicker { void click(String selector)throws Exception; }
    interface Checker { void check(boolean passed,String label)throws Exception; }
    private static final String DOWNLOADS="/sdcard/Download/";
    private static final String MARKER="Draft2Done fictional document workflow sentinel";
    private final Instrumentation instrumentation;
    private final Activity activity;
    private final Evaluator evaluate;
    private final Clicker click;
    private final Checker check;
    private final UiAutomation automation;
    private final String prefix="Draft2Done-QA-"+UUID.randomUUID().toString().replace("-","");
    private final String[] names={prefix+".txt",prefix+".docx",prefix+".PDF"};

    private NativeDocumentWorkflowChecks(Instrumentation instrumentation,Activity activity,Evaluator evaluate,Clicker click,Checker check){
        this.instrumentation=instrumentation;this.activity=activity;this.evaluate=evaluate;this.click=click;this.check=check;automation=instrumentation.getUiAutomation();
    }
    static void run(Instrumentation instrumentation,Activity activity,Evaluator evaluate,Clicker click,Checker check)throws Exception {
        new NativeDocumentWorkflowChecks(instrumentation,activity,evaluate,click,check).run(true);
    }
    static void quickImport(Instrumentation instrumentation,Activity activity,Evaluator evaluate,Clicker click,Checker check)throws Exception {
        new NativeDocumentWorkflowChecks(instrumentation,activity,evaluate,click,check).run(false);
    }
    static void recover(Instrumentation instrumentation,Activity activity,Evaluator evaluate,Clicker click,Checker check)throws Exception {
        WorkspaceSnapshot checkpoint=WorkspaceSnapshot.pending(activity);
        if(checkpoint!=null){
            checkpoint.restore(activity,evaluate,click);
            check.check(checkpoint.matchesDisk(activity),"Durable document checkpoint restores the exact original workspace and previous-save backup after interruption");
            check.check(checkpoint.matchesState(evaluate),"Durable recovery restores every project, setting and original active paper");
            deleteExactFiles(instrumentation,checkpoint.prefix);
            checkpoint.clear(activity);
            check.check(true,"Durable recovery removes only its three exact fictional QA files and clears its private checkpoint");
            return;
        }
        File manifest=new File(activity.getExternalFilesDir("qa"),"document-workflow-artifacts.json");
        JSONObject artifacts;try(InputStream in=new FileInputStream(manifest)){artifacts=new JSONObject(new String(bytes(in,4096),StandardCharsets.UTF_8));}
        JSONArray filenames=artifacts.getJSONArray("filenames");String first=filenames.getString(0),prefix=first.substring(0,first.length()-4);
        if(!prefix.matches("Draft2Done-QA-[a-f0-9]{32}"))throw new IOException("Refused an unknown document recovery prefix");
        File copies=new File(activity.getCacheDir(),"qa-document-pre-recovery");copies.mkdirs();
        for(String name:new String[]{"workspace.json","workspace.backup.json"}){
            File source=new File(activity.getFilesDir(),name);if(source.exists())try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(new File(copies,name))){out.write(bytes(in,4*1024*1024));}
        }
        String before=evaluate.evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects.filter(p=>p.title!=="+JSONObject.quote(prefix)+"),profiles:s.profiles,notes:s.notes,settings:s.settings});");
        String fixtureId=decode(evaluate.evaluate("window.Draft2Done.getState().projects.find(p=>p.title==="+JSONObject.quote(prefix)+")?.id||''"));
        check.check("true".equals(evaluate.evaluate("const s=window.Draft2Done.getState();return s.projects.filter(p=>p.title==="+JSONObject.quote(prefix)+").length<=1;")),"Crash recovery targets only the exact uniquely named fictional QA paper");
        evaluate.evaluate("const s=window.Draft2Done.getState();const qa=s.projects.find(p=>p.title==="+JSONObject.quote(prefix)+");if(qa){s.projects=s.projects.filter(p=>p.id!==qa.id);if(s.activeId===qa.id)s.activeId=s.projects[0]?.id||null;}window.Draft2Done.render();return true;");
        click.click("[data-action=page][data-id=home]");Thread.sleep(1000);
        check.check(before.equals(evaluate.evaluate("const s=window.Draft2Done.getState();return JSON.stringify({projects:s.projects,profiles:s.profiles,notes:s.notes,settings:s.settings});")),"Crash recovery preserves every other project, profile, note and setting; prior UI selection is not claimed");
        String clean=decode(evaluate.evaluate("JSON.stringify(window.Draft2Done.getState())"));
        byte[] cleanBackup=null;File backupCopy=new File(copies,"workspace.backup.json");
        if(backupCopy.exists()){
            JSONObject previous;try(InputStream in=new FileInputStream(backupCopy)){previous=new JSONObject(new String(bytes(in,4*1024*1024),StandardCharsets.UTF_8));}
            JSONArray old=previous.optJSONArray("projects"),kept=new JSONArray();String removed="";
            if(old!=null)for(int index=0;index<old.length();index++){JSONObject paper=old.getJSONObject(index);if(prefix.equals(paper.optString("title"))||(!fixtureId.isEmpty()&&fixtureId.equals(paper.optString("id"))))removed=paper.optString("id");else kept.put(paper);}
            previous.put("projects",kept);if(removed.equals(previous.optString("activeId")))previous.put("activeId",kept.length()==0?JSONObject.NULL:kept.getJSONObject(0).optString("id"));cleanBackup=previous.toString().getBytes(StandardCharsets.UTF_8);
        }
        Field field=activity.getClass().getDeclaredField("workspace");field.setAccessible(true);Object store=field.get(activity);
        synchronized(store){
            WorkspaceSnapshot.write(new File(activity.getFilesDir(),"workspace.json"),clean.getBytes(StandardCharsets.UTF_8));
            WorkspaceSnapshot.write(new File(activity.getFilesDir(),"workspace.backup.json"),cleanBackup);
        }
        for(int index=0;index<filenames.length();index++){
            String name=filenames.getString(index);if(!name.matches(java.util.regex.Pattern.quote(prefix)+"\\.(txt|docx|pdf|PDF)"))throw new IOException("Refused an unrelated cleanup file");
            try(ParcelFileDescriptor descriptor=instrumentation.getUiAutomation().executeShellCommand("rm -f "+DOWNLOADS+name);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){bytes(in,1024);}
        }
        check.check(true,"Removed only the three exact fictional export filenames and retained private pre-recovery copies");
    }
    private void run(boolean full)throws Exception {
        Thread.sleep(1800);
        WorkspaceSnapshot saved=new WorkspaceSnapshot(activity,evaluate,prefix);
        AccessibilityServiceInfo service=automation.getServiceInfo();int originalFlags=service.flags;
        service.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;automation.setServiceInfo(service);
        File artifactManifest=new File(activity.getExternalFilesDir("qa"),"document-workflow-artifacts.json");
        try(OutputStream out=new FileOutputStream(artifactManifest)){out.write(new JSONObject().put("directory",DOWNLOADS).put("filenames",new JSONArray(Arrays.asList(names))).toString(2).getBytes(StandardCharsets.UTF_8));}
        String session=evaluate.evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return JSON.stringify({connected:!!s.connected,active:s.active||''});");
        Instrumentation.ActivityMonitor location=new Instrumentation.ActivityMonitor(){
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
                if(Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())||Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction()))
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Download"));
                return null;
            }
        };
        instrumentation.addMonitor(location);
        try {
            click.click("[data-action=page][data-id=home]");click.click("[data-action=new]");click.click("[data-action=tab][data-id=draft]");
            click.click(".paper-title");evaluate.evaluate("document.querySelector('#rename-input').value="+JSONObject.quote(prefix)+";return true;");click.click("[data-action=rename-save]");
            String fictional="When people talk about cities, they often describe streets and buildings. This fictional classroom paragraph considers how shared green spaces fit into everyday city planning. It contains no observations about a real person or place and makes no claim to cite published research. Its purpose is to exercise document saving and importing on Android.\n\n"+MARKER+". Fictional Unicode fixture: caf\u00e9 and \u03b1.";
            evaluate.evaluate("const el=document.querySelector('#draft-editor');el.value="+JSONObject.quote(fictional)+";el.dispatchEvent(new Event('input',{bubbles:true}));return true;");
            String text=decode(evaluate.evaluate("document.querySelector('#draft-editor').value"));
            check.check(text.contains(MARKER),"Created a separate fictional paper for real Android document workflows");
            export("txt",names[0]);
            check.check(Arrays.equals(text.getBytes(StandardCharsets.UTF_8),waitFile(names[0])),"Android Save document writes exact UTF-8 TXT bytes in Downloads");
            if(full){export("docx",names[1]);
            check.check(docxContains(waitFile(names[1]),MARKER),"Android Save document writes a DOCX whose document XML contains the fictional draft");
            openPrintPreview();
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            waitUntil(()->"io.draft2done.app".equals(foreground()),15000,"Canceling print preview did not return to the app");waitPrintReleased();
            check.check(readOwnFile(names[2]).length==0,"Canceling Android print preview creates no PDF and releases the current print job");
            export("pdf",names[2]);
            byte[] pdf=waitFile(names[2]);
            check.check(pdf.length>1000&&new String(pdf,0,Math.min(5,pdf.length),StandardCharsets.US_ASCII).equals("%PDF-"),"Android Print Save as PDF creates a physical PDF file in Downloads");
            try(OutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir("qa"),"document-roundtrip.pdf"))){out.write(pdf);}}
            for(String name:full?names:new String[]{names[0]}){
                String before=evaluate.evaluate("window.Draft2Done.getState().activeId");
                evaluate.evaluate("window.__qaDocumentDelivery=null;const prior=window.NativeEvents.imported;window.NativeEvents.imported=function(payload){window.NativeEvents.imported=prior;let value=typeof payload==='string'?JSON.parse(payload):payload;window.__qaDocumentDelivery={nameMatches:value.name==="+JSONObject.quote(name)+",mime:value.mime||'',base64Length:(value.base64||'').length};return prior(payload);};return true;");
                evaluate.evaluate("window.DraftNative.importDocument();return true;");waitPicker();navigateDownloads();openOwnFile(name);
                try{waitJS("window.Draft2Done.getState().activeId!=="+before+"&&(()=>{const s=window.Draft2Done.getState();return s.projects.find(p=>p.id===s.activeId)?.originalText.replace(/\\s+/g,' ').includes("+JSONObject.quote(MARKER)+");})()",30000);}
                catch(Exception failure){
                    captureChooser("document-import-failure");
                    String diagnostic=evaluate.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);const toast=document.querySelector('.toast');return {delivery:window.__qaDocumentDelivery,activeChanged:s.activeId!=="+before+",activeTitleMatches:p?.title==="+JSONObject.quote(prefix)+",textHasMarker:!!p?.originalText.includes("+JSONObject.quote(MARKER)+"),importError:toast&&toast.textContent.startsWith('Import failed:')?toast.textContent.slice(0,300):'',lastError:window.Draft2Done.info().lastError?'JavaScript error recorded':''};");
                    check.check(false,"Native import diagnostic for "+name.substring(name.lastIndexOf('.')+1)+": "+diagnostic);throw failure;
                }
                evaluate.evaluate("delete window.__qaDocumentDelivery;return true;");
                check.check("true".equals(evaluate.evaluate("const s=window.Draft2Done.getState();const p=s.projects.find(p=>p.id===s.activeId);return p.originalText.replace(/\\s+/g,' ').includes('When people talk about cities')&&p.workingText.replace(/\\s+/g,' ').includes("+JSONObject.quote(MARKER)+");")),"Real Android Open document imports the saved "+name.substring(name.lastIndexOf('.')+1).toUpperCase()+" text into a new protected draft");
            }
        } catch(Exception failure) {
            try{captureChooser("document-workflow-failure");}catch(Exception ignored){}
            throw failure;
        } finally {
            instrumentation.removeMonitor(location);
            try {
                returnToApp();
                for(String name:names)deleteOwnFile(name);
            } finally {
                try{
                    saved.restore(activity,evaluate,click);
                    check.check(saved.matchesDisk(activity),"Original workspace and previous-save backup restored after document QA");
                    check.check(saved.matchesState(evaluate),"All original projects/settings and active paper restored after document QA");
                    check.check(session.equals(evaluate.evaluate("const s=JSON.parse(window.DraftNative.chatgptStatus());return JSON.stringify({connected:!!s.connected,active:s.active||''});")),"Document QA leaves the connected ChatGPT account unchanged");
                    saved.clear(activity);
                }finally{service.flags=originalFlags;automation.setServiceInfo(service);}
            }
        }
    }
    private void export(String format,String name)throws Exception {
        if("pdf".equals(format))openPrintPreview();
        else openExport(format);
        if("pdf".equals(format)){
            AccessibilityNodeInfo print=waitNode(n->id(n).endsWith("/print_button")&&n.isEnabled()&&n.isVisibleToUser(),20000,"PDF save button is not ready");press(print);
        }
        waitPicker();navigateDownloads();
        if("pdf".equals(format))captureChooser("document-location-before");
        AccessibilityNodeInfo filename=find(n->editable(n)&&n.isVisibleToUser());
        boolean samsungLocation=false;
        if(filename==null&&foreground().equals("com.sec.android.app.myfiles")){
            AccessibilityNodeInfo chooseLocation=find(n->text(n).equalsIgnoreCase("Save here")&&n.isEnabled()&&n.isVisibleToUser());
            if(chooseLocation==null){
                // Samsung labels its location-confirmation action "Save" on some versions.
                // Refuse to proceed if its suggested default name could replace an existing file.
                try(ParcelFileDescriptor descriptor=automation.executeShellCommand("if [ -e '/sdcard/Download/Draft2Done paper.pdf' ]; then echo exists; fi");InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){
                    if(bytes(input,1024).length!=0)throw new IOException("Refused Samsung location confirmation because the default PDF filename already exists");
                }
                chooseLocation=find(n->text(n).equalsIgnoreCase("Save")&&n.isEnabled()&&n.isVisibleToUser());
            }
            if(chooseLocation!=null){press(chooseLocation);samsungLocation=true;}
        }
        if("pdf".equals(format)&&samsungLocation){
            waitUntil(()->"io.draft2done.app".equals(foreground())||find(n->editable(n)&&n.isVisibleToUser())!=null,15000,"Samsung did not finish PDF folder selection");
            if("io.draft2done.app".equals(foreground())){waitFile(name);waitPrintReleased();return;}
        }
        try{filename=waitNode(n->editable(n)&&n.isVisibleToUser(),10000,"Save document filename field is unavailable");}
        catch(IOException error){chooserDiagnostic();throw error;}
        if("pdf".equals(format))captureChooser("document-filename-before");
        Bundle value=new Bundle();value.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name);
        if(!filename.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,value))throw new IOException("Could not set the fictional export filename");
        try{filename=waitNode(n->editable(n)&&n.isVisibleToUser()&&text(n).equals(name),5000,"The save dialog did not retain the exact fictional UUID filename");}
        catch(IOException error){if("pdf".equals(format))captureChooser("document-filename-entered");throw error;}
        AccessibilityNodeInfo confirm=find(n->id(n).equals("android:id/button1")&&n.isEnabled()&&n.isVisibleToUser());
        if(confirm==null)confirm=waitNode(n->text(n).equalsIgnoreCase("Save")&&n.isEnabled()&&n.isVisibleToUser(),10000,"Android Save document button is unavailable");
        press(confirm);
        if("pdf".equals(format)&&(foreground().equals("com.sec.android.app.myfiles")||foreground().contains("documentsui")))captureChooser("document-filename-after");
        waitFile(name);returnToApp();
        if("pdf".equals(format))waitPrintReleased();
    }
    private void openExport(String format)throws Exception {
        click.click("[data-action=page][data-id=write]");click.click("[data-action=tab][data-id=draft]");click.click("[data-action=paper-menu]");click.click("[data-action=export]");
        evaluate.evaluate("document.querySelector('#export-format').value="+JSONObject.quote(format)+";document.querySelector('#export-references').checked=false;return true;");click.click("[data-action=export-confirm]");
    }
    private void openPrintPreview()throws Exception {
            openExport("pdf");
            waitUntil(()->foreground().contains("printspooler"),20000,"Android print preview did not open");
            AccessibilityNodeInfo destination=find(n->id(n).endsWith("/destination_spinner"));
            if(destination!=null&&find(n->text(n).equalsIgnoreCase("Save as PDF")&&n.isVisibleToUser())==null){
                press(destination);press(waitNode(n->text(n).equalsIgnoreCase("Save as PDF"),10000,"Save as PDF destination is unavailable"));
            }
            waitNode(n->id(n).endsWith("/print_button")&&n.isEnabled()&&n.isVisibleToUser(),20000,"PDF save button is not ready");
            waitUntil(()->find(n->n.isVisibleToUser()&&((id(n).endsWith("/page_number")&&hasSelectedParent(n))||(id(n).endsWith("/preview_page")&&n.isSelected())))!=null,30000,"Android has not rendered and selected a PDF preview page");
            check.check(true,"Android PDF preview contains a rendered, selected fictional page before interaction");
    }
    private static boolean hasSelectedParent(AccessibilityNodeInfo node){for(int depth=0;node!=null&&depth<8;depth++,node=node.getParent())if(node.isSelected())return true;return false;}
    private static boolean editable(AccessibilityNodeInfo node){return node.isEditable()||node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);}
    private void chooserDiagnostic(){try{JSONArray controls=new JSONArray();chooserDiagnostic(automation.getRootInActiveWindow(),controls,0);try(OutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir("qa"),"document-picker-controls.json"))){out.write(new JSONObject().put("package",foreground()).put("controls",controls).toString(2).getBytes(StandardCharsets.UTF_8));}}catch(Exception ignored){}}
    private void captureChooser(String name)throws Exception {
        String current=foreground();
        if(!current.equals("io.draft2done.app")&&!current.equals("com.sec.android.app.myfiles")&&!current.contains("documentsui")&&!current.contains("printspooler")&&!current.equals("android")&&!current.equals("com.samsung.android.honeyboard"))throw new IOException("Refused to capture an unrelated application (foreground "+current+")");
        File directory=activity.getExternalFilesDir("qa");android.graphics.Bitmap screen=automation.takeScreenshot();
        if(screen!=null)try(OutputStream out=new FileOutputStream(new File(directory,name+".png"))){screen.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screen.recycle();}
        JSONArray nodes=new JSONArray();chooserTree(automation.getRootInActiveWindow(),nodes,0);try(OutputStream out=new FileOutputStream(new File(directory,name+".json"))){out.write(nodes.toString(2).getBytes(StandardCharsets.UTF_8));}
    }
    private void chooserTree(AccessibilityNodeInfo node,JSONArray nodes,int depth)throws Exception {
        if(node==null||depth>35||nodes.length()>=1000)return;
        android.graphics.Rect bounds=new android.graphics.Rect();node.getBoundsInScreen(bounds);
        nodes.put(new JSONObject().put("depth",depth).put("id",id(node)).put("class",String.valueOf(node.getClassName())).put("text",text(node)).put("description",description(node)).put("editable",editable(node)).put("visible",node.isVisibleToUser()).put("enabled",node.isEnabled()).put("clickable",node.isClickable()).put("bounds",bounds.toShortString()));
        for(int index=0;index<node.getChildCount();index++)chooserTree(node.getChild(index),nodes,depth+1);
    }
    private void chooserDiagnostic(AccessibilityNodeInfo node,JSONArray controls,int depth)throws Exception {
        if(node==null||depth>35||controls.length()>=150)return;
        String label=text(node),known="";for(String safe:new String[]{"Save here","Save","Cancel","Internal storage","Download","Downloads"})if(label.equalsIgnoreCase(safe))known=safe;
        if(node.isVisibleToUser()&&(!known.isEmpty()||editable(node)))controls.put(new JSONObject().put("id",id(node)).put("class",String.valueOf(node.getClassName())).put("label",known).put("editable",editable(node)).put("clickable",node.isClickable()).put("qaFilename",label.startsWith(prefix)?label:"").put("parentId",node.getParent()==null?"":id(node.getParent())));
        for(int index=0;index<node.getChildCount();index++)chooserDiagnostic(node.getChild(index),controls,depth+1);
    }
    private void waitPrintReleased()throws Exception {
        Field field=activity.getClass().getDeclaredField("printView");field.setAccessible(true);
        waitUntil(()->{boolean[] released={false};instrumentation.runOnMainSync(()->{try{released[0]=field.get(activity)==null;}catch(IllegalAccessException error){throw new IllegalStateException(error);}});return released[0];},15000,"Android did not finish the previous print job");
    }
    private void navigateDownloads()throws Exception {
        if(foreground().equals("com.sec.android.app.myfiles")){
            AccessibilityNodeInfo storage=find(n->text(n).equalsIgnoreCase("Internal storage")&&n.isVisibleToUser());
            if(storage!=null)press(storage);
            AccessibilityNodeInfo downloads=waitNode(n->(text(n).equalsIgnoreCase("Download")||text(n).equalsIgnoreCase("Downloads"))&&n.isVisibleToUser(),10000,"Samsung My Files Downloads folder is unavailable");
            press(downloads);
            Thread.sleep(450);
            waitNode(n->(text(n).equalsIgnoreCase("Download")||text(n).equalsIgnoreCase("Downloads"))&&n.isVisibleToUser(),10000,"Samsung My Files did not reach the Downloads folder");return;
        }
        AccessibilityNodeInfo drawer=waitNode(n->description(n).equalsIgnoreCase("Show roots")||description(n).equalsIgnoreCase("Open navigation drawer")||description(n).equalsIgnoreCase("Show navigation drawer"),10000,"Document picker navigation button is unavailable");
        press(drawer);press(waitNode(n->text(n).equalsIgnoreCase("Downloads")&&n.isVisibleToUser(),10000,"Downloads storage root is unavailable"));Thread.sleep(450);
    }
    private void openOwnFile(String name)throws Exception {
        ownName(name);
        AccessibilityNodeInfo file=find(n->fileLabel(n,name));
        if(file==null){
            press(waitNode(n->description(n).equalsIgnoreCase("Search")||id(n).endsWith("/option_menu_search"),10000,"Could not locate the QA file or picker search"));
            AccessibilityNodeInfo search=waitNode(n->n.isEditable()&&n.isVisibleToUser(),10000,"Picker search field is unavailable");
            Bundle query=new Bundle();query.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name);search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,query);search.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            automation.injectInputEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER),true);automation.injectInputEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER),true);
            file=waitNode(n->fileLabel(n,name),15000,"The exact QA file was not shown in Downloads");
        }
        press(file);Thread.sleep(700);
        if(foreground().equals("com.sec.android.app.myfiles")||foreground().contains("documentsui")){
            captureChooser("document-open-selection");
            AccessibilityNodeInfo confirm=find(n->n.isVisibleToUser()&&n.isEnabled()&&
                (text(n).equalsIgnoreCase("Open")||text(n).equalsIgnoreCase("Select")||text(n).equalsIgnoreCase("Done")||text(n).equalsIgnoreCase("Select file")));
            if(confirm!=null)press(confirm);
            else throw new IOException("The exact QA file was selected but the document picker has not completed the open action");
        }
        waitUntil(()->"io.draft2done.app".equals(foreground()),20000,"Selected document did not return to Draft2Done");Thread.sleep(600);
    }
    private static boolean fileLabel(AccessibilityNodeInfo node,String name){
        // Search contains the same UUID filename. It is not a file result.
        return text(node).equals(name)&&node.isVisibleToUser()&&!editable(node)
            &&!id(node).contains("search")&&!String.valueOf(node.getClassName()).contains("EditText");
    }
    private byte[] waitFile(String name)throws Exception {
        long deadline=System.currentTimeMillis()+20000;byte[] previous=new byte[0];int stable=0;
        while(System.currentTimeMillis()<deadline){byte[] current=readOwnFile(name);boolean pdf=name.endsWith(".pdf")||name.endsWith(".PDF");boolean complete=current.length>10&&(!pdf||new String(current,Math.max(0,current.length-1024),Math.min(1024,current.length),StandardCharsets.US_ASCII).contains("%%EOF"));if(complete&&Arrays.equals(previous,current)){if(++stable>=2)return current;}else stable=0;previous=current;Thread.sleep(250);}
        if(foreground().equals("com.sec.android.app.myfiles"))chooserDiagnostic();
        throw new IOException("The expected fictional QA export was not saved to Downloads: "+name);
    }
    private byte[] readOwnFile(String name)throws Exception {
        ownName(name);try(ParcelFileDescriptor descriptor=automation.executeShellCommand("cat "+DOWNLOADS+name);InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){return bytes(input,10*1024*1024);}
    }
    private void deleteOwnFile(String name)throws Exception {
        ownName(name);try(ParcelFileDescriptor descriptor=automation.executeShellCommand("rm -f "+DOWNLOADS+name);InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){bytes(input,1024);}
        check.check(readOwnFile(name).length==0,"Removed only the fictional QA "+name.substring(name.lastIndexOf('.')+1).toUpperCase()+" file from Downloads");
    }
    private void ownName(String name)throws IOException {
        if(!name.startsWith(prefix+".")||!name.matches("Draft2Done-QA-[a-f0-9]{32}\\.(txt|docx|pdf|PDF)"))throw new IOException("Refused to access a path outside this test's exact QA filenames");
    }
    private void returnToApp()throws Exception {
        long deadline=System.currentTimeMillis()+15000;
        long nextBack=System.currentTimeMillis()+1200;
        while(System.currentTimeMillis()<deadline){if("io.draft2done.app".equals(foreground())){Thread.sleep(600);return;}if(activity.isFinishing()||activity.isDestroyed())throw new IOException("The QA activity closed while returning from Android documents");if(System.currentTimeMillis()>=nextBack){automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);nextBack=System.currentTimeMillis()+2500;}Thread.sleep(150);}
        throw new IOException("Could not return from the document interface to Draft2Done");
    }
    private void waitPicker()throws Exception {
        try{waitUntil(()->foreground().contains("documentsui")||foreground().equals("com.sec.android.app.myfiles"),20000,"Android document picker did not open");}
        catch(IOException error){if(foreground().contains("printspooler")){android.graphics.Bitmap screenshot=automation.takeScreenshot();if(screenshot!=null){try(OutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir("qa"),"document-print-diagnostic.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screenshot.recycle();}}}throw error;}
    }
    private String foreground(){AccessibilityNodeInfo root=automation.getRootInActiveWindow();return root==null||root.getPackageName()==null?"":root.getPackageName().toString();}
    private interface Condition {boolean ready()throws Exception;}
    private void waitUntil(Condition condition,int timeout,String error)throws Exception {long until=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<until){if(condition.ready())return;Thread.sleep(100);}throw new IOException(error+" (foreground "+foreground()+")");}
    private void waitJS(String condition,int timeout)throws Exception {waitUntil(()->"true".equals(evaluate.evaluate("return ("+condition+");")),timeout,"Imported document did not reach the app");}
    private AccessibilityNodeInfo waitNode(Predicate<AccessibilityNodeInfo> predicate,int timeout,String error)throws Exception {long until=System.currentTimeMillis()+timeout;AccessibilityNodeInfo node;while(System.currentTimeMillis()<until){node=find(predicate);if(node!=null)return node;Thread.sleep(100);}throw new IOException(error+" (foreground "+foreground()+")");}
    private AccessibilityNodeInfo find(Predicate<AccessibilityNodeInfo> predicate){return find(automation.getRootInActiveWindow(),predicate,0);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,Predicate<AccessibilityNodeInfo> predicate,int depth){if(node==null||depth>35)return null;if(predicate.test(node))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo result=find(node.getChild(i),predicate,depth+1);if(result!=null)return result;}return null;}
    private static String text(AccessibilityNodeInfo node){return node.getText()==null?"":node.getText().toString();}
    private static String description(AccessibilityNodeInfo node){return node.getContentDescription()==null?"":node.getContentDescription().toString();}
    private static String id(AccessibilityNodeInfo node){return node.getViewIdResourceName()==null?"":node.getViewIdResourceName();}
    private static boolean contains(AccessibilityNodeInfo node,String value){if(text(node).equalsIgnoreCase(value)||description(node).equalsIgnoreCase(value))return true;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&contains(child,value))return true;}return false;}
    private static void press(AccessibilityNodeInfo node)throws IOException {for(AccessibilityNodeInfo current=node;current!=null;current=current.getParent())if(current.isEnabled()&&current.isVisibleToUser()&&current.isClickable()&&current.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;throw new IOException("The requested document control could not be activated: "+id(node)+" / "+String.valueOf(node.getClassName()));}
    private static String decode(String raw)throws Exception {return (String)new JSONTokener(raw).nextValue();}
    private static byte[] bytes(InputStream input,int limit)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(out.size()+count>limit)throw new IOException("QA data exceeded its size limit");out.write(buffer,0,count);}return out.toByteArray();}
    private static boolean docxContains(byte[] bytes,String marker)throws IOException {try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)if("word/document.xml".equals(entry.getName()))return new String(bytes(zip,2*1024*1024),StandardCharsets.UTF_8).contains(marker);}return false;}
    private static void deleteExactFiles(Instrumentation instrumentation,String prefix)throws Exception {
        if(!prefix.matches("Draft2Done-QA-[a-f0-9]{32}"))throw new IOException("Refused an unknown checkpoint filename prefix");
        for(String extension:new String[]{"txt","docx","pdf","PDF"})try(ParcelFileDescriptor descriptor=instrumentation.getUiAutomation().executeShellCommand("rm -f "+DOWNLOADS+prefix+"."+extension);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)){bytes(in,1024);}
    }

    static final class WorkspaceSnapshot {
        private final byte[] primary,backup;
        private final String state,prefix;
        WorkspaceSnapshot(Activity activity,Evaluator evaluate,String prefix)throws Exception {
            this.prefix=prefix;
            File directory=new File(activity.getCacheDir(),"qa-document-checkpoint");directory.mkdirs();
            if(new File(directory,"pending.json").exists())throw new IOException("A durable document checkpoint needs recovery before another run");
            primary=read(new File(activity.getFilesDir(),"workspace.json"));backup=read(new File(activity.getFilesDir(),"workspace.backup.json"));state=decode(evaluate.evaluate("JSON.stringify(window.Draft2Done.getState())"));
            if(primary!=null)write(new File(directory,"primary.json"),primary);if(backup!=null)write(new File(directory,"backup.json"),backup);
            write(new File(directory,"state.json"),state.getBytes(StandardCharsets.UTF_8));
            write(new File(directory,"pending.json"),new JSONObject().put("primary",primary!=null).put("backup",backup!=null).put("prefix",prefix).toString().getBytes(StandardCharsets.UTF_8));
        }
        private WorkspaceSnapshot(byte[] primary,byte[] backup,String state,String prefix){this.primary=primary;this.backup=backup;this.state=state;this.prefix=prefix;}
        static WorkspaceSnapshot pending(Activity activity)throws Exception {
            File directory=new File(activity.getCacheDir(),"qa-document-checkpoint");byte[] pending=read(new File(directory,"pending.json"));if(pending==null)return null;
            JSONObject metadata=new JSONObject(new String(pending,StandardCharsets.UTF_8));String prefix=metadata.getString("prefix");if(!prefix.matches("Draft2Done-QA-[a-f0-9]{32}"))throw new IOException("Refused an unknown durable recovery prefix");
            byte[] primary=metadata.getBoolean("primary")?read(new File(directory,"primary.json")):null,backup=metadata.getBoolean("backup")?read(new File(directory,"backup.json")):null,state=read(new File(directory,"state.json"));
            if(state==null||(metadata.getBoolean("primary")&&primary==null)||(metadata.getBoolean("backup")&&backup==null))throw new IOException("Document recovery checkpoint is incomplete; original files left untouched");
            String json=new String(state,StandardCharsets.UTF_8);new JSONObject(json);return new WorkspaceSnapshot(primary,backup,json,prefix);
        }
        void restore(Activity activity,Evaluator evaluate,Clicker click)throws Exception {
            evaluate.evaluate("const current=window.Draft2Done.getState();const automatic=current.settings.autosave;current.settings.autosave=true;try{window.NativeEvents.pause();}finally{current.settings.autosave=automatic;}const original=JSON.parse("+JSONObject.quote(state)+");for(const key of Object.keys(current))delete current[key];Object.assign(current,original);window.Draft2Done.render();return true;");
            click.click("[data-action=page][data-id=home]");
            Field field=activity.getClass().getDeclaredField("workspace");field.setAccessible(true);Object store=field.get(activity);
            synchronized(store){write(new File(activity.getFilesDir(),"workspace.json"),primary);write(new File(activity.getFilesDir(),"workspace.backup.json"),backup);}
        }
        boolean matchesDisk(Activity activity)throws Exception {return Arrays.equals(primary,read(new File(activity.getFilesDir(),"workspace.json")))&&Arrays.equals(backup,read(new File(activity.getFilesDir(),"workspace.backup.json")));}
        boolean matchesState(Evaluator evaluate)throws Exception{return state.equals(decode(evaluate.evaluate("JSON.stringify(window.Draft2Done.getState())")));}
        void clear(Activity activity){File directory=new File(activity.getCacheDir(),"qa-document-checkpoint");for(String name:new String[]{"pending.json","primary.json","backup.json","state.json"})new AtomicFile(new File(directory,name)).delete();directory.delete();}
        private static byte[] read(File file)throws Exception {if(!file.exists())return null;try(InputStream input=new AtomicFile(file).openRead()){return bytes(input,4*1024*1024);}}
        private static void write(File file,byte[] bytes)throws Exception {AtomicFile atomic=new AtomicFile(file);if(bytes==null){atomic.delete();return;}FileOutputStream out=null;try{out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);}catch(Exception error){if(out!=null)atomic.failWrite(out);throw error;}}
    }
}
