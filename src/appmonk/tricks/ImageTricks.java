package appmonk.tricks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import appmonk.image.ImageRequest;


@SuppressWarnings("unused")
public class ImageTricks {
    public static final String CAMERA_TEMP_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.tmp";
    public static final String CAMERA_TEMP_FILE_NAME = "camera.jpg";
    
    protected static class RequestInfo {
        String mName;
        List<WeakReference<ImageView>> mViews;
        boolean mIsNew = true;
        
        RequestInfo(String url) {
            mName = url;
            mViews = new ArrayList<WeakReference<ImageView>>();
            mIsNew = true;
        }
        
        static Map<String, RequestInfo> sInfos = new HashMap<String, RequestInfo>();
        static Map<String, String> sNameForView = new HashMap<String, String>();
        
        void addView(ImageView view) {
            synchronized (sInfos) {
                String viewHashCode = Integer.toString(view.hashCode());
                // First, if this widget is already included in another request, lets remove it before proceeding
                // We save it using the hashcode as index because using a weakreference as a hash key doesn't work well
                
                String existingUrl = sNameForView.remove(viewHashCode);
                if (existingUrl != null) {
                    RequestInfo existingInfo = sInfos.get(existingUrl);
                    if (existingInfo != null) {
                        for (WeakReference<ImageView> ref : existingInfo.mViews) {
                            ImageView v = ref.get();
                            if (v != null && v == view) {
                                existingInfo.mViews.remove(ref);
                                break;
                            }
                        }
                    }
                }

                sNameForView.put(viewHashCode, mName);
                
                for (WeakReference<ImageView> ref : mViews) {
                    ImageView v = ref.get();
                    if (v != null && v == view)
                        return; // This Request already points to this widget
                }
                mViews.add(new WeakReference<ImageView>(view));
            }
        }
        
        void completed() {
            synchronized (sInfos) {
                for (WeakReference<ImageView> ref : mViews) {
                    ImageView v = ref.get();
                    if (v != null)
                        sNameForView.remove(Integer.toString(v.hashCode()));
                }
                sInfos.remove(this);
            }
        }
        
        static RequestInfo get(String url) {
            RequestInfo info;
            
            synchronized (sInfos) {
                info = sInfos.get(url);
                if (info == null) {
                    info = new RequestInfo(url);
                    sInfos.put(url, info);
                }
            }
            
            return info;
        }
    }
    
    public static class AsyncImageRequest extends AsyncTricks.AsyncRequest {
        protected static Handler sUiThreadHandler = null;

        protected ImageRequest mImageRequest;
        protected RequestInfo mInfo;
        protected int mDefaultImageResource;
        protected Bitmap mBitmap = null;
        protected String mViewName = null;
        protected String mRequestName = null;
        
        public AsyncImageRequest(ImageRequest request, ImageView view, int defaultResource) {
            super(AsyncTricks.INTERACTIVE);
            
            if (sUiThreadHandler == null)
                sUiThreadHandler = new Handler();
            
            mImageRequest = request;
            mRequestName = mImageRequest.name();

            mDefaultImageResource = defaultResource;

            mInfo = RequestInfo.get(mRequestName);
            mInfo.addView(view);
        }
        
        public AsyncImageRequest(ImageRequest request, ImageView view) {
            this(request, view, 0);
        }
        
        public AsyncImageRequest(String url, ImageView view, int defaultResource) {
		this(new ImageRequest().load(url).cache(), view, defaultResource);
        }

        public AsyncImageRequest(String url, ImageView view) {
            this(new ImageRequest().load(url).cache(), view, 0);
        }
        
        public String label() {
            return "loading image " + mRequestName;
        }
        
        Handler handler() {
            return sUiThreadHandler;
        }
        
        public void displayImage(Bitmap bitmap) {
            for (WeakReference<ImageView> ref : mInfo.mViews) {
                ImageView v = ref.get();
                if (v != null)
                    v.setImageBitmap(bitmap);
            }
        }
        
        public void displayPlaceholder() {
            if (mDefaultImageResource != 0) {
                for (WeakReference<ImageView> ref : mInfo.mViews) {
                    ImageView v = ref.get();
                    if (v != null)
                        v.setImageResource(mDefaultImageResource);
                }
            }
            else {
                for (WeakReference<ImageView> ref : mInfo.mViews) {
                    ImageView v = ref.get();
                    if (v != null)
                        v.setImageBitmap(null);
                }
            }
        }
        
        public void displayFallback() {
            displayPlaceholder();
        }

        @Override
        public boolean before() {
            if (mImageRequest.isInMemory()) {
                mBitmap = mImageRequest.getBitmap();
                if (mBitmap != null) {
                    displayImage(mBitmap);
                    return false;
                }
                else { 
                    displayFallback();
                }
            }
            else {
                displayPlaceholder();
            }
            return true;
        }

        @Override
        public void request() {
            mBitmap = mImageRequest.getBitmap();
        }

        @Override
        public void interrupted() {
            displayFallback();
            mInfo.completed();
            mInfo = null;
        }

        @Override
        public void after() {
            if (mBitmap != null)
                displayImage(mBitmap);
            else
                displayFallback();
            mInfo.completed();
            mInfo = null;
        }

        public void queue() {
            String existingRequest = null;
            
            if (mInfo.mIsNew) {
                mInfo.mIsNew = false;
                
                if (existingRequest == null) {
                    AsyncTricks.queueRequest(AsyncTricks.INTERACTIVE, this);
                }
                else {
                    AsyncTricks.replaceRequest(AsyncTricks.INTERACTIVE, this);
                }
            }
        }
    }
    
    
    public static Bitmap scaleDownBitmap(Bitmap original, int maxDimension, boolean recycleOriginal) {
    	int origWidth = original.getWidth();
    	int origHeight = original.getHeight();
    	
    	if (origWidth <= maxDimension && origHeight <= maxDimension) {
    		Bitmap b = Bitmap.createBitmap(original);
    		if (recycleOriginal && (original != b))
    			original.recycle();
    		return b;
    	}
    	
    	int newWidth = 0;
    	int newHeight = 0;
    	
    	float ratio = (float)origHeight / (float)origWidth;
    	
    	if (origWidth > origHeight) {
    		newWidth = maxDimension;
    		newHeight = (int)((float)newWidth * ratio);
    	} else {
    		newHeight = maxDimension;
    		newWidth = (int)((float)newHeight / ratio);
    	}
    	
    	Bitmap rtr = Bitmap.createScaledBitmap(original, newWidth, newHeight, false);
    	if (recycleOriginal && original != rtr)
    		original.recycle();
    	return rtr;
    }
    
    public static void scaleDownImageFile(File originalImageFile, int maxDimension, CompressFormat format, int quality) {
    	Bitmap b = BitmapFactory.decodeFile(originalImageFile.getAbsolutePath());
    	if (b == null)
    		throw new RuntimeException("Original image could not be decoded.");
    	
    	try {
	    	b = scaleDownBitmap(b, maxDimension, true);
	    	originalImageFile.delete();
	    	originalImageFile.createNewFile();
	    	BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(originalImageFile));
	    	b.compress(format, quality, outputStream);
	    	outputStream.close();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    public static Bitmap scaleDownImageUriToBitmap(Uri imageUri, int maxDimension, boolean deleteOriginal) {
    	try {
    		InputStream mediaStream = AppMonk.getContentResolver().openInputStream(imageUri);
    		BitmapFactory.Options opts = new BitmapFactory.Options();
    		opts.inJustDecodeBounds = true;
        	BitmapFactory.decodeStream(mediaStream, null, opts);
        	mediaStream.close();
            int outWidth = opts.outWidth;
        	
        	mediaStream = AppMonk.getContentResolver().openInputStream(imageUri);

        	opts = new BitmapFactory.Options();
        	opts.inSampleSize = outWidth / maxDimension;
        	
            Bitmap bitmap = BitmapFactory.decodeStream(mediaStream, null, opts);
        	
        	mediaStream.close();

//        	bitmap = scaleDownBitmap(bitmap, maxDimension, true);
        	
        	if (deleteOriginal)
        		AppMonk.getContentResolver().delete(imageUri, null, null);
        	return bitmap;
        	
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    
    public static File scaleDownImageUriToFile(Uri imageUri, int maxDimension, CompressFormat format, int quality, boolean deleteOriginal) {
    	if (!ImageTricks.checkTempCameraDir())
    		return null;

    	Bitmap b = scaleDownImageUriToBitmap(imageUri, maxDimension, deleteOriginal);
    	if (b == null) 
    		return null;
    	
    	try {
        	
        	File tmpFile = new File(ImageTricks.CAMERA_TEMP_DIR, "scaledImage." + (format == CompressFormat.JPEG ? "jpg" : "png"));
        	tmpFile.createNewFile();
        	BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
        	b.compress(format, quality, outputStream);
        	
        	outputStream.close();
        	b.recycle();
        	
        	
        	return tmpFile;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    	
    }
    
    public static Uri scaleDownImageUri(Uri imageUri, int maxDimension, CompressFormat format, int quality, boolean deleteOriginal) {
    	try {
	    	File tmpFile = scaleDownImageUriToFile(imageUri, maxDimension, format, quality, deleteOriginal);
	    	
	    	if (tmpFile == null)
	    		return null;
	    	
	    	Uri rtr = Uri.parse(MediaStore.Images.Media.insertImage(AppMonk.getContentResolver(), tmpFile.getAbsolutePath(), null, null));
	    	tmpFile.delete();
	    	return rtr;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

    public static boolean checkTempCameraDir() {
        File dir = new File(CAMERA_TEMP_DIR);
        if (!dir.exists()) {
            try {
                if (!dir.mkdirs())
                    return false;
            } 
            catch (Exception e) {
                return false;
            }
        }
        
        if (!dir.canWrite())
            return false;
        
        File noMedia = new File(dir, ".nomedia");
        try {
            noMedia.createNewFile();
        } 
        catch (Exception e) {
            return false;
        }
        return true;
    }

//    public static File _tempCameraFile = null;
    
//    public static File tempCameraFile() {
//        if (_tempCameraFile == null)
//            _tempCameraFile = new File(ImageTricks.CAMERA_TEMP_DIR, ImageTricks.CAMERA_TEMP_FILE_NAME);
//        return _tempCameraFile;
//    }

    public static Uri putImageFileIntoGalleryAndGetUri(Context c, File imageFile, boolean deleteImageFileAfter) {
        if (imageFile.exists() && imageFile.isFile()) {
            try {
                Uri dataUri = Uri.parse(MediaStore.Images.Media.insertImage(c.getContentResolver(), imageFile.getAbsolutePath(), null, null));
                if (deleteImageFileAfter)
                    imageFile.delete();
                return dataUri;
            } 
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }   
        }
        return null;
    }
    
    public static Uri putBitmapIntoGalleryAndGetUri(Context c, Bitmap image, boolean recycleOriginal) {
        if (image != null) {
            Uri dataUri = Uri.parse(MediaStore.Images.Media.insertImage(c.getContentResolver(), image, null, null));
            if (recycleOriginal)
                image.recycle();
            return dataUri; 
        }
        return null;
    }
}
