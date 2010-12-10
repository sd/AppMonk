package appmonk.image;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import appmonk.tricks.IOTricks;

public class ImageLoaderOperation extends ImageRequest.Operation {
    protected static final String TAG = "AppMonk";

    protected static final int BUFFER_SIZE = 64 * 1024; // 64KB

    URL url = null;
    String cacheName = null;
    File cacheFile = null;
    
    public ImageLoaderOperation(ImageRequest request, String url) {
        super(request);
        try {
            this.url = new URL(url);
        }
        catch (MalformedURLException e) {
            this.url = null;
        }
        if (url != null) {
            cacheName = request.cacheNameFor(url);
            cacheFile = request.cacheFileFor(cacheName);
        }
    }

    @Override
    public Bitmap perform(Bitmap previousBitmap) {
        if (ImageCache.cacheEnabled()) {
            if (isCached() || downloadToCache())
                return loadFromCache();
        }
        else {
            return loadFromSource();
        }
        return null;
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

    public Bitmap loadFromSource() {
        Bitmap bitmap = null;
        try {
            if (url != null) {
                InputStream is = (InputStream) url.openStream();
                if (is != null) {
                    bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                }
                return bitmap;
            }
        } 
        catch (IOException ioe) {
            Log.e(TAG, "Could not download image from " + url, ioe);
        } 
        catch (Exception e) {
            Log.e(TAG, "Could not download image from " + url, e);
        } 
        return bitmap;
    }

    public boolean downloadToCache() {
        try {
            if (url != null) {
                File tempFile = new File(cacheFile + "-tmp");

                URLConnection conn = url.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
                FileOutputStream cacheOut = new FileOutputStream(tempFile);
                IOTricks.copyStreamToStream(bis, cacheOut, BUFFER_SIZE);
                cacheOut.getFD().sync();
                cacheOut.close();
                bis.close();
                is.close();

                if (cacheFile.canRead())
                    cacheFile.delete();
                tempFile.renameTo(cacheFile);
                
                return true;
            }
        } 
        catch (IOException ioe) {
            Log.e(TAG, "Could not download image file from " + url, ioe);
        } 
        catch (Exception e) {
            Log.e(TAG, "Could not download image file from " + url, e);
        } 
        return false;
    }

    public Bitmap loadFromCache() {
        Bitmap bitmap = null;
        if (cacheFile.canRead()) {
            try {
                bitmap = BitmapFactory.decodeFile(cacheFile.toString());
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