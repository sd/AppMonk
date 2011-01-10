package appmonk.tricks;

import java.util.HashMap;

import android.graphics.Bitmap;
import android.os.Handler;
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
}
