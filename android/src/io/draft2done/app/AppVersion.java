package io.draft2done.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/** Runtime release metadata keeps bridge and network identification in sync. */
final class AppVersion {
    private AppVersion() {}
    static String name(Context context) {
        try {
            PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
            return info.versionName==null||info.versionName.isEmpty()?"unknown":info.versionName;
        } catch(PackageManager.NameNotFoundException unavailable) { return "unknown"; }
    }
    static String userAgent(Context context) {
        return "Draft2Done/"+name(context).replaceAll("[^A-Za-z0-9._+-]","_");
    }
}
