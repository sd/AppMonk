package appmonk.tricks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import appmonk.image.ImageRequest;


@SuppressWarnings("unused")
public class ImageTricks {
    protected static Handler uiThreadHandler = null;
    
    public static class AsyncImageRequest extends AsyncTricks.AsyncRequest {
        protected ImageRequest imageRequest;
        protected ImageView imageView;
        protected int defaultImageResource;
        protected Bitmap bitmap = null;
        
        protected static HashMap<String, String> assignedRequests = new HashMap<String, String>();
        
        public AsyncImageRequest(ImageRequest request, ImageView view, int defaultResource) {
            super(AsyncTricks.INTERACTIVE);
            
            imageRequest = request;
            imageView = view;
            defaultImageResource = defaultResource;
            
            if (uiThreadHandler == null)
                uiThreadHandler = new Handler();
            
            synchronized (assignedRequests) {
                String viewName = Integer.toString(view.hashCode());
                String requestName = imageRequest.name();
                assignedRequests.put(viewName, requestName);
            }
        }
        
        public AsyncImageRequest(ImageRequest request, ImageView view) {
            this(request, view, 0);
        }
        
        public AsyncImageRequest(String url, ImageView view, int defaultResource) {
            this(new ImageRequest().load(url), view, defaultResource);
        }

        public AsyncImageRequest(String url, ImageView view) {
            this(new ImageRequest().load(url), view, 0);
        }
        
        public String label() {
            return "loading image " + imageRequest.name() + " for view " + imageView.hashCode();
        }
        
        Handler handler() {
            return uiThreadHandler;
        }
        
        @Override
        public boolean before() {
            if (imageRequest.isInMemory()) {
                bitmap = imageRequest.getBitmap();
                if (bitmap != null)
                    imageView.setImageBitmap(bitmap);
                else if (defaultImageResource != 0)
                    imageView.setImageResource(defaultImageResource);
                else
                    imageView.setImageBitmap(null);
                return false;
            }
            else {
                if (defaultImageResource != 0)
                    imageView.setImageResource(defaultImageResource);
                else
                    imageView.setImageBitmap(null);
    
                return true;
            }
        }

        @Override
        public void request() {
            bitmap = imageRequest.getBitmap();
        }

        @Override
        public void interrupted() {
            boolean stillMatchesRequest = false;

            synchronized (assignedRequests) {
                String viewName = Integer.toString(imageView.hashCode());
                String requestName = imageRequest.name();
                String assignedName = assignedRequests.remove(viewName);
                if (assignedName != null && assignedName.equals(requestName)) {
                    stillMatchesRequest = true;
                }
            }

            if (stillMatchesRequest) {
                if (defaultImageResource != 0)
                    imageView.setImageResource(defaultImageResource);
                else
                    imageView.setImageBitmap(null);
            }
        }

        @Override
        public void after() {
            boolean stillMatchesRequest = false;
            
            synchronized (assignedRequests) {
                String viewName = Integer.toString(imageView.hashCode());
                String requestName = imageRequest.name();
                String assignedName = assignedRequests.remove(viewName);
                if (assignedName != null && assignedName.equals(requestName)) {
                    stillMatchesRequest = true;
                }
            }

            if (stillMatchesRequest) {
                if (bitmap != null) {
                    // Log.d("XXX", "Image " + imageRequest.name() + " loaded onto " + imageView);
                    imageView.setImageBitmap(bitmap);
                }
                else {
                    if (defaultImageResource != 0)
                        imageView.setImageResource(defaultImageResource);
                    else
                        imageView.setImageBitmap(null);
                }
            }
        }

        public void queue() {
            AsyncTricks.queueRequest(AsyncTricks.INTERACTIVE, this);
        }
    }
    
    
    public static Bitmap scaleDownBitmap(Bitmap original, int minDimension, boolean recycleOriginal) {
    	int origWidth = original.getWidth();
    	int origHeight = original.getHeight();
    	
    	if (origWidth <= minDimension && origHeight <= minDimension) {
    		Bitmap b = Bitmap.createBitmap(original);
    		if (recycleOriginal && (original != b))
    			original.recycle();
    		return b;
    	}
    	
    	int newWidth = 0;
    	int newHeight = 0;
    	
    	float ratio = (float)origHeight / (float)origWidth;
    	
    	if (origWidth > origHeight) {
    		newWidth = minDimension;
    		newHeight = (int)((float)newWidth * ratio);
    	} else {
    		newHeight = minDimension;
    		newWidth = (int)((float)newHeight / ratio);
    	}
    	
    	Bitmap rtr = Bitmap.createScaledBitmap(original, newWidth, newHeight, false);
    	if (recycleOriginal && original != rtr)
    		original.recycle();
    	return rtr;
    }
    
    public static void scaleDownImageFile(File originalImageFile, int minDimension, CompressFormat format, int quality) {
    	Bitmap b = BitmapFactory.decodeFile(originalImageFile.getAbsolutePath());
    	if (b == null)
    		throw new RuntimeException("Original image could not be decoded.");
    	
    	try {
	    	b = scaleDownBitmap(b, minDimension, true);
	    	originalImageFile.delete();
	    	originalImageFile.createNewFile();
	    	BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(originalImageFile));
	    	b.compress(format, quality, outputStream);
	    	outputStream.close();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    public static Uri scaleDownImageUri(Uri imageUri, int minDimension, CompressFormat format, int quality) {
    	try {
	    	InputStream mediaStream = AppMonk.getContentResolver().openInputStream(imageUri);
	    	Bitmap b = BitmapFactory.decodeStream(mediaStream);
	    	mediaStream.close();
	    	b = scaleDownBitmap(b, minDimension, true);
	    	
	    	File tmpFile = new File(Environment.getExternalStorageDirectory(), "scaledImage." + (format == CompressFormat.JPEG ? "jpg" : "png"));
	    	tmpFile.createNewFile();
	    	BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
	    	b.compress(format, quality, outputStream);
	    	
	    	outputStream.close();
	    	b.recycle();
	    	
	    	Uri rtr = Uri.parse(MediaStore.Images.Media.insertImage(AppMonk.getContentResolver(), tmpFile.getAbsolutePath(), null, null));
	    	tmpFile.delete();
	    	return rtr;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

}
