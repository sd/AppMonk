package appmonk.net;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import appmonk.tricks.IOTricks;

public class SimpleFileCache {
    protected static final String TAG = "AppMonk/Cache";
    
    public boolean debug = false;
    
    protected Context mContext = null;
    protected File mCacheDir = null;

    public SimpleFileCache(Context context, String cacheName) {
        mContext = context;
        
        String sdcard = android.os.Environment.getExternalStorageDirectory().toString();
        mCacheDir = new File(sdcard + "/Android/data/" + context.getPackageName() + "/cache/" + cacheName + "/");

        try {
            if (!mCacheDir.exists()) {
            	if (debug)
            		Log.d(TAG, "Creating cache dir " + mCacheDir.getPath());
                mCacheDir.mkdirs();
            }

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
    
    public void touch(String name) {
        if (!cacheEnabled())
            return;
        
        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);

        if (debug)
        	Log.d(TAG, "Touching " + name + " (" + cacheFile.getPath() + ")");
        
        cacheFile.setLastModified(System.currentTimeMillis()); // Touch file for cache cleanup purposes
    }

    public boolean remove(String name) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        
        if (cacheFile.canRead()) {
            if (debug)
            	Log.d(TAG, "Removed " + name + " (" + cacheFile.getPath() + ")");
            
            cacheFile.delete();
            return true;
        }
        else {
            if (debug)
            	Log.d(TAG, "Tried to remove but couldn't find " + name + " (" + cacheFile.getPath() + ")");

            return false;
        }
    }
    
    public Object fetch(String name, int maxAge) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);

        if (!cacheFile.canRead()) {
            if (debug)
                Log.d(TAG, "Cache file not found for " + name + " (" + cacheFile.getPath() + ")");
            return null;
        }
        
        try {
            if (maxAge <= 0 || (System.currentTimeMillis() - cacheFile.lastModified() < maxAge)) {
                if (debug)
                	Log.d(TAG, "Loading " + name + " (" + cacheFile.getPath() + ")");

                return IOTricks.loadObject(cacheFile);
            }
            else {
                if (debug)
                	Log.d(TAG, "Did not load stale file " + name + " (" + cacheFile.getPath() + ")");
            }
        }
        catch (IOException e) {
            if (debug)
            	Log.e(TAG, "-- IOException " + e.getMessage());
        }
        catch (ClassNotFoundException e) {
            if (debug)
            	Log.e(TAG, "-- ClassNotFoundException " + e.getMessage());
        }

        return null;
    }
    
    public boolean store(String name, Object value) {
        if (!cacheEnabled())
            return false;

        String cleanName = IOTricks.sanitizeFileName(name);
        File cacheFile = new File(mCacheDir, cleanName);
        try {
            if (debug)
            	Log.d(TAG, "Storing " + name + " (" + cacheFile.getPath() + ")");

            IOTricks.saveObject(value, cacheFile);
            return true;
        }
        catch (IOException e) {
            if (debug)
            	Log.e(TAG, "-- IOException " + e.getMessage());
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
