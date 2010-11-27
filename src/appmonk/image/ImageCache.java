package appmonk.image;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import appmonk.tricks.AppMonk;

public class ImageCache {
    protected static final String TAG = "AppMonk";

    protected static Context appContext = null;
    public static void setContext(Context context) {
        if (appContext == null)
            appContext = context;
    }

    protected static String cachePath = null;
    protected static boolean cachePathHasBeenVerified = false;

    public static String cachePath() {
        if (cachePath == null) {
            setCachePath(defaultCachePath());
        }
        if (!cachePathHasBeenVerified) {
            try {
                File nomediaFile = new File(cachePath + ".nomedia");
                if (!nomediaFile.exists()) {
                    new File(cachePath).mkdirs();
                    nomediaFile.createNewFile();
                }
                cachePathHasBeenVerified = true;
            } catch (IOException e) {
                Log.e(TAG, "Error creating image cache directories", e);
            }
            cachePathHasBeenVerified = true;
        }
        return cachePath;
    }
    public static void setCachePath(String path) {
        if (path.endsWith("/"))
            cachePath = path;
        else
            cachePath = path + "/";
        
        cachePathHasBeenVerified = false;
    }
    public static String defaultCachePath() {
        String sdcard = android.os.Environment.getExternalStorageDirectory().toString();
        return sdcard + "/Android/data/" + AppMonk.packageName + "/images";
    }
    public static boolean cacheEnabled() {
        return cachePathHasBeenVerified && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    
    public static void touch(File cacheFile) {
        cacheFile.setLastModified(System.currentTimeMillis()); // Touch file for cache cleanup purposes
    }
}
