package appmonk.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import appmonk.tricks.AppMonk;

public class ImageOverlayOperation extends ImageRequest.Operation {
    protected static final String TAG = "AppMonk";

    int resourceId = 0;
    int top = 0;
    int left = 0;
    
    public ImageOverlayOperation(ImageRequest request, int resourceId, int top, int left) {
        super(request);
        this.resourceId = resourceId;
        this.top = top;
        this.left = left;
    }

    @Override
    public Bitmap perform(Bitmap previousBitmap) {
        Bitmap overlayBitmap = AppMonk.getBitmap(resourceId);

        synchronized (overlayBitmap) {
            if (overlayBitmap != null) {
                Bitmap combinedBitmap = null;
                try {
                    combinedBitmap = Bitmap.createBitmap(previousBitmap.getWidth(), previousBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                } 
                catch (OutOfMemoryError e) {
                    Log.e(TAG, "Error overlying image: ", e);
                }
    
                if (combinedBitmap != null) {
                    Canvas canvas = new Canvas(combinedBitmap);
                    canvas.drawBitmap(previousBitmap, 0, 0, null);
                    canvas.drawBitmap(overlayBitmap, left, top, null);
    
                    return combinedBitmap;
                }
            }
        }
        
        return previousBitmap;
    }

    @Override
    public String name(String previousName) {
        return "overlay_" + resourceId + "-" + previousName;
    }
}