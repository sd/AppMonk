package appmonk.tricks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

public class AppMonk {
    protected static Context applicationContext = null;

    public static String packageName = null;
    public static String versionName = null;
    public static int versionCode = 0;
    
    public static float screenDensity;
    public static float textScale;

    
    public static void setContext(Context context) {
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();

            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

                packageName = context.getPackageName();
                versionName = packageInfo.versionName;
                versionCode = packageInfo.versionCode;
            } catch (NameNotFoundException e) {
                packageName = "?";
                versionName = "?";
                versionCode = 0;
            }

            screenDensity = applicationContext.getResources().getDisplayMetrics().density;
            textScale = applicationContext.getResources().getDisplayMetrics().scaledDensity;
        }
    }
    
    public static Context getContext() {
        return applicationContext;
    }
    
    public static String getString(int resource) {
        return applicationContext.getString(resource);
    }

    public static String getString(int resource, Object... formatArgs) {
        return applicationContext.getString(resource, formatArgs);
    }

    public static int getResourceId(String name) {
        return applicationContext.getResources().getIdentifier(name, null, packageName);
    }

    public static Drawable getDrawable(int resource) {
        return applicationContext.getResources().getDrawable(resource);
    }

    public static Bitmap getBitmap(int resource) {
        Drawable drawable = applicationContext.getResources().getDrawable(resource);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            return null;
        }
    }
}
