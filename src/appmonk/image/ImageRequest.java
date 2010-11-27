package appmonk.image;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.text.TextUtils;
import appmonk.tricks.IOTricks;

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
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 * 
 */

/*============================================
 
SAMPLE USAGE:
ImageRequest imageReq = new ImageRequest("http://example.com/images/photo.jpg")
                                    .roundCorners(2)
                                    .scale(200, 200)
                                    .overlay(R.drawable.icon_star, -1, -1)
                                    .cacheWithSuffix("rounded");
if (imageReq.isCached()) {
    imageReq.getDrawable();
}
else {
}

==============================================*/

public class ImageRequest {
    protected static final String TAG = "AppMonk";

    protected List<Operation> operations = null;
    
    public ImageRequest() {
        operations = new ArrayList<Operation>();
    }

    public ImageRequest load(String url) {
        addOperation(new ImageLoaderOperation(this, url));
        return this;
    }
    
    public ImageRequest widthInDips(int w) {
        addOperation(new ImageScalingOperation(this, w, ImageScalingOperation.PROPORTIONAL));
        return this;
    }

    public ImageRequest heightInDips(int h) {
        addOperation(new ImageScalingOperation(this, ImageScalingOperation.PROPORTIONAL, h));
        return this;
    }

    public ImageRequest roundCorners(int r) {
        addOperation(new ImageRoundCornersOperation(this, r));
        return this;
    }

    public ImageRequest cache() {
        addOperation(new ImageCachingOperation(this));
        return this;
    }

    public ImageRequest cache(String variation) {
        addOperation(new ImageCachingOperation(this, variation));
        return this;
    }

    public ImageRequest cache(String name, String variation) {
        addOperation(new ImageCachingOperation(this, name, variation));
        return this;
    }

    public ImageRequest cache(URL url, String variation) {
        addOperation(new ImageCachingOperation(this, url.toString(), variation));
        return this;
    }

    public boolean addOperation(Operation operation) {
        if (operation.request != this)
            return false;

        operations.add(operation);
        return true;
    }
    
    public Bitmap getBitmap() {
        Bitmap bitmap = null;
        int pos;
        
        pos = operations.size() - 1;
        while (pos > 0) {
            if (operations.get(pos).isCached()) {
                break;
            }
            else {
                pos--;
            }
        }
        
        while (pos < operations.size()) {
            bitmap = operations.get(pos).perform(bitmap);
            pos++;
        }
        
        return bitmap;
    }
    
    public String name() {
        String name = null;
        int pos = 0;
        while (pos < operations.size()) {
            name = operations.get(pos).name(name);
            pos++;
        }
        return name;
    }
    
    public String cacheNameFor(String url) {
        return IOTricks.sanitizeFileName(url);
    }

    public String cacheNameFor(URL url) {
        return IOTricks.sanitizeFileName(url.toString());
    }

    public String cacheNameFor(String url, String variation) {
        if (TextUtils.isEmpty(variation))
            return IOTricks.sanitizeFileName(url);
        else
            return variation + "-" + IOTricks.sanitizeFileName(url);
    }

    public String cacheNameFor(URL url, String variation) {
        if (TextUtils.isEmpty(variation))
            return IOTricks.sanitizeFileName(url.toString());
        else
            return variation + "-" + IOTricks.sanitizeFileName(url.toString());
    }

    public File cacheFileFor(String cacheName) {
        return new File(ImageCache.cachePath() + cacheName);
    }
    
    public File cacheFileFor(String cacheName, String variation) {
        return new File(ImageCache.cachePath() + variation + "-" + cacheName);
    }

    public abstract static class Operation {
        ImageRequest request = null;
        
        Operation() {
            this.request = null;
        }
        Operation(ImageRequest request) {
            this.request = request;
        }

        public boolean isExpensive() { return true; }
        public boolean isCached() { return false; }
        public abstract Bitmap perform(Bitmap previousBitmap);
        public String name(String previousName) { return previousName; };
    }
}
