package appmonk.net;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import appmonk.tricks.IOTricks;

public class SimpleFileCache {
    protected static final String TAG = "AppMonk/Cache";
    
    protected Context mContext = null;
    protected File mCacheDir = null;

    public SimpleFileCache(Context context, String cacheName) {
        mContext = context;
        
        String sdcard = android.os.Environment.getExternalStorageDirectory().toString();
        mCacheDir = new File(sdcard + "/Android/data/" + context.getPackageName() + "/cache/" + cacheName + "/");

        try {
            if (!mCacheDir.exists())
                mCacheDir.mkdirs();

            File nomediaFile;
            
            nomediaFile = new File(sdcard + "/Android/data/" + context.getPackageName() + "/.nomedia");
            if (!nomediaFile.exists())
                nomediaFile.createNewFile();
            
            nomediaFile = new File(sdcard + "/Android/data/" + context.getPackageName() + "/cache/.nomedia");
            if (!nomediaFile.exists())
                nomediaFile.createNewFile();

            nomediaFile = new File(sdcard + "/Android/data/" + context.getPackageName() + "/cache/" + cacheName + "/.nomedia");
            if (!nomediaFile.exists())
                nomediaFile.createNewFile();
        } 
        catch (IOException e) {
            Log.e(TAG, "Error creating cache directory " + mCacheDir.getPath(), e);
            mCacheDir = null;
        }
    }
    
    public File cacheBase() {
        return mCacheDir;
    }
    
    public boolean cacheEnabled() {
        return mCacheDir != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    
    public void touch(File cacheFile) {
        if (!cacheEnabled())
            return;
        
        cacheFile.setLastModified(System.currentTimeMillis()); // Touch file for cache cleanup purposes
    }

    public boolean remove(String name) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        
        if (cacheFile.canRead()) {
            cacheFile.delete();
            return true;
        }
        else {
            return false;
        }
    }
    
    public Object fetch(String name, int maxAge) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);

        try {
            if (System.currentTimeMillis() - cacheFile.lastModified() < maxAge)
                return IOTricks.loadObject(cacheFile);
        }
        catch (IOException e) {
        }
        catch (ClassNotFoundException e) {
        }

        return null;
    }
    
    public boolean store(String name, Object value) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        try {
            IOTricks.saveObject(value, cacheFile);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
    
    public boolean isCached(String name) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        return cacheFile.canRead();
    }
    
    public boolean isFresh(String name, int maxAge) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        return cacheFile.canRead() && (System.currentTimeMillis() - cacheFile.lastModified() < maxAge);
    }
    
    public long age(String name) {
        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        return (System.currentTimeMillis() - cacheFile.lastModified());
    }

    public boolean purge(String name, int maxAge) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        
        if (cacheFile.canRead() && (System.currentTimeMillis() - cacheFile.lastModified() >= maxAge)) {
            cacheFile.delete();
            return true;
        }
        else {
            return false;
        }
    }
    
    public void purgeAll(int maxAge) {
        if (!cacheEnabled())
            return;

        File files[] = mCacheDir.listFiles();
        long now = System.currentTimeMillis();
        if (files != null) {
            final int length = files.length;
            for (int i = 0; i < length; i++) {
                if (now - files[i].lastModified() > maxAge) {
                    files[i].delete();
                }
            }
        }
    }
}
