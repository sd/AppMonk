package appmonk.tricks;

import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;
import appmonk.image.ImageRequest;


public class ImageTricks {
    protected static Handler uiThreadHandler = null;
    
    public static class AsyncImageRequest extends AsyncTricks.AsyncRequest {
        protected ImageRequest imageRequest;
        protected ImageView imageView;
        protected int defaultImageResource;
        protected Bitmap bitmap = null;
        
        public AsyncImageRequest(ImageRequest request, ImageView view, int defaultResource) {
            super(AsyncTricks.INTERACTIVE);
            
            imageRequest = request;
            imageView = view;
            defaultImageResource = defaultResource;
            
            if (uiThreadHandler == null)
                uiThreadHandler = new Handler();
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
            return "loading image " + imageRequest.name();
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
            bitmap = imageRequest.getBitmap();
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
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
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
