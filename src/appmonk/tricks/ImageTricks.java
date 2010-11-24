package appmonk.tricks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

/*
 * Copyright (C) 2009, 2010 Sebastian Delmont <sd@notso.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 * 
 */

/*============================================
 
SAMPLE USAGE:
ImageRequest imageReq = new ImageRequest("http://example.com/images/photo.jpg")
                                    .setCacheSuffix("rounded")
                                    .roundCorners(2)
                                    .scale(200, 200)
                                    .overlay(R.drawable.icon_star, -1, -1);
if (imageReq.isCached()) {
    imageReq.getDrawable();
}
else {
}

==============================================*/

public class ImageTricks {
    protected static final String TAG = "AppMonk";
    
    protected static final int BUFFER_SIZE = 64 * 1024; // 64KB
    
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
    
    public static class ImageRequest {
        String sourceUrl = null;
        String cacheName = null;
        File cacheFile = null;
        Bitmap bitmap = null;
        
        public ImageRequest(String url) {
            if (TextUtils.isEmpty(url)) {
                sourceUrl = null;
                cacheName = null;
            }
            else {
                sourceUrl = url;
                cacheName = IOTricks.sanitizeFileName(url);
            }
        }

        public ImageRequest setCacheSuffix(String suffix) {
            if (sourceUrl != null)
                cacheName = IOTricks.sanitizeFileName(sourceUrl) + "-" + suffix;
            return this;
        }
        
        public boolean loadBitmap() {
            if (ImageTricks.cacheEnabled()) {
                if (isCached() || downloadSourceToCache())
                    return loadFromCache();
            }
            else {
                return loadFromSource();
            }
            return false;
        }
        
        public boolean isCached() {
            if (cacheName == null)
                return false;
            
            if (cacheFile == null)
                cacheFile = new File(cachePath + cacheName);
            return cacheFile.canRead();
        }

        public boolean loadFromSource() {
            try {
                if (!TextUtils.isEmpty(sourceUrl)) {
                    URL url = new URL(sourceUrl);
                    InputStream is = (InputStream) url.openStream();
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                    }
                    return true;
                }
            } 
            catch (IOException ioe) {
                Log.e(TAG, "Could not download image from " + sourceUrl, ioe);
            } 
            catch (Exception e) {
                Log.e(TAG, "Could not download image from " + sourceUrl, e);
            } 
            return false;
        }

        public boolean downloadSourceToCache() {
            try {
                if (!TextUtils.isEmpty(sourceUrl)) {
                    URL url = new URL(sourceUrl);

                    cacheName = cacheName.toLowerCase();
                    File tempFile = new File(cacheFile + "-tmp");

                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
                    FileOutputStream cacheOut = new FileOutputStream(tempFile);
                    IOTricks.copyStreamToStream(bis, cacheOut, BUFFER_SIZE);
                    cacheOut.close();
                    bis.close();
                    is.close();

                    if (cacheFile.canRead())
                        cacheFile.delete();
                    tempFile.renameTo(cacheFile);
                }
            } 
            catch (IOException ioe) {
                Log.e(TAG, "Could not download image file from " + sourceUrl, ioe);
            } 
            catch (Exception e) {
                Log.e(TAG, "Could not download image file from " + sourceUrl, e);
            } 
            return false;
        }

        public boolean loadFromCache() {
            if (cacheFile.canRead()) {
                try {
                    bitmap = BitmapFactory.decodeFile(cacheFile.toString());
                    cacheFile.setLastModified(System.currentTimeMillis()); // Touch file for cache cleanup purposes
                    return true;
                } 
                catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory loading image " + cacheName + " from file cache", e);
                } 
                catch (Exception e) {
                    Log.e(TAG, "Error loading image " + cacheName + " from file cache", e);
                }
            }
            return false;
        }
    }
    
    protected static Handler uiThreadHandler = null;
    
    public static class AsyncImageRequest extends AsyncTricks.AsyncRequest {
        protected ImageRequest imageRequest;
        protected ImageView imageView;
        protected int defaultImageResource;
        protected boolean loadedOk;
        
        public AsyncImageRequest(ImageRequest request, ImageView view, int defaultResource) {
            super(AsyncTricks.INTERACTIVE);
            
            imageRequest = request;
            imageView = view;
            defaultImageResource = defaultResource;
            
            if (uiThreadHandler == null)
                uiThreadHandler = new Handler();
        }
        
        public AsyncImageRequest(String url, ImageView view) {
            this(new ImageRequest(url), view, 0);
        }
        
        public String label() {
            return "loading " + imageRequest.sourceUrl;
        }
        
        Handler handler() {
            return uiThreadHandler;
        }
        
        @Override
        public boolean before() {
            return true;
        }

        @Override
        public void request() {
            loadedOk = imageRequest.loadBitmap();
        }

        @Override
        public void interrupted() {
            if (defaultImageResource != 0)
                imageView.setImageResource(defaultImageResource);
            else
                imageView.setImageBitmap(null);
        }

        @Override
        public void after() {
            if (loadedOk) {
                imageView.setImageBitmap(imageRequest.bitmap);
            }
            else {
                if (defaultImageResource != 0)
                    imageView.setImageResource(defaultImageResource);
                else
                    imageView.setImageBitmap(null);
            }
        }

        public void queue() {
            AsyncTricks.queueRequest(AsyncTricks.INTERACTIVE, this);
        }
    }
}
