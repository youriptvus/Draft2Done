package io.draft2done.app;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
public final class UpdateProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{if(!"content".equals(uri.getScheme())||!(getContext().getPackageName()+".updates").equals(uri.getAuthority())||uri.getEncodedPath()==null||!uri.getEncodedPath().matches("/release-[a-f0-9]{64}\\.apk")||uri.getQuery()!=null||uri.getFragment()!=null)throw new FileNotFoundException("Unknown update file.");File file=new File(new File(getContext().getCacheDir(),"updates"),uri.getLastPathSegment());if(!file.isFile())throw new FileNotFoundException("Update file is unavailable.");return file;}
 @Override public String getType(Uri uri){try{file(uri);return "application/vnd.android.package-archive";}catch(FileNotFoundException invalid){return null;}}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only.");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{File f=file(uri);MatrixCursor c=new MatrixCursor(new String[]{"_display_name","_size"});c.addRow(new Object[]{"Draft2Done-update.apk",f.length()});return c;}catch(Exception e){return null;}}
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}
