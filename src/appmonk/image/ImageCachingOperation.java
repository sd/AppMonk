package appmonk.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

public class ImageCachingOperation extends ImageRequest.Operation {
    protected static final String TAG = "AppMonk";

    protected static final int BUFFER_SIZE = 64 * 1024; // 64KB

    String cacheName = null;
    File cacheFile = null;
    
    public ImageCachingOperation(ImageRequest request) {
        this(request, null);
    }
    
    public ImageCachingOperation(ImageRequest request, String variation) {
        super(request);
        String name = request.name(); // the name for this request, before this operation is added to the list
        
        cacheName = request.cacheNameFor(name, variation);
        cacheFile = request.cacheFileFor(cacheName);
    }

    public ImageCachingOperation(ImageRequest request, String name, String variation) {
        super(request);
        cacheName = request.cacheNameFor(name, variation);
        cacheFile = request.cacheFileFor(cacheName);
    }
    
    @Override
    public Bitmap perform(Bitmap previousBitmap) {
        if (previousBitmap == null) {
            return loadFromCache();
        }
        else {
            saveToCache(previousBitmap);
            return previousBitmap;
        }
    }

    @Override
    public String name(String previousName) {
        return cacheName;
    }
    
    @Override
    public boolean isCached() {
        if (cacheName == null)
            return false;
        
        if (cacheFile == null)
            cacheFile = request.cacheFileFor(cacheName);
        return cacheFile.canRead();
    }

    public boolean saveToCache(Bitmap bitmap) {
        try {
            if (bitmap != null) {
                File tempFile = new File(cacheFile + "-tmp");

                FileOutputStream cacheOut = new FileOutputStream(tempFile);
                bitmap.compress(CompressFormat.PNG, 100, cacheOut);
                cacheOut.close();
                
                if (cacheFile.canRead())
                    cacheFile.delete();
                tempFile.renameTo(cacheFile);
                
                Log.d(TAG, "Saved to cache " + cacheName);
                return true;
            }
        } 
        catch (IOException ioe) {
            Log.e(TAG, "Could save to cache " + cacheName, ioe);
        } 
        catch (Exception e) {
            Log.e(TAG, "Could save to cache " + cacheName, e);
        } 
        return false;
    }

    public Bitmap loadFromCache() {
        Bitmap bitmap = null;
        if (cacheFile.canRead()) {
            try {
                bitmap = BitmapFactory.decodeFile(cacheFile.toString());
                Log.d(TAG, "Loaded cached image for " + cacheName);
                ImageCache.touch(cacheFile);
            } 
            catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory loading image " + cacheName + " from file cache", e);
            } 
            catch (Exception e) {
                Log.e(TAG, "Error loading image " + cacheName + " from file cache", e);
            }
        }
        return bitmap;
    }
}