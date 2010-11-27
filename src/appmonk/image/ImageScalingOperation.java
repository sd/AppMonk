package appmonk.image;

import android.graphics.Bitmap;
import android.util.Log;
import appmonk.tricks.AppMonk;

public class ImageScalingOperation extends ImageRequest.Operation {
    protected static final String TAG = "AppMonk";

    public static final int PROPORTIONAL = 0;
    
    int widthInDips = -1;
    int heightInDips = -1;
    
    public ImageScalingOperation(ImageRequest request, int widthInDips, int heightInDips) {
        super(request);
        this.widthInDips = widthInDips;
        this.heightInDips = heightInDips;
    }

    @Override
    public Bitmap perform(Bitmap previousBitmap) {
        Bitmap bitmap = previousBitmap;
        
        if (widthInDips > 0) {
            int widthInPixels = (int) (widthInDips * AppMonk.screenDensity);
            if (previousBitmap.getWidth() != widthInPixels) {
                int heightInPixels = previousBitmap.getHeight() * widthInPixels / previousBitmap.getWidth();
                try {
                    bitmap = Bitmap.createScaledBitmap(bitmap, widthInPixels, heightInPixels, true);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Error scaling image to " + variationName(), e);
                }
            }
        }
        return bitmap;
    }

    @Override
    public String name(String previousName) {
        return variationName() + "-" + previousName;
    }
    
    public String variationName() {
        if ((heightInDips == PROPORTIONAL) && (widthInDips != PROPORTIONAL)) {
            return "w_" + widthInDips;
        }
        else if ((widthInDips == PROPORTIONAL) && (heightInDips != PROPORTIONAL)) {
            return "h_" + heightInDips;
        }
        else if ((widthInDips != PROPORTIONAL) && (heightInDips != PROPORTIONAL)) {
            return "w_" + widthInDips + "-h_" + heightInDips;
        }
        else {
            return "w_-h_";
        }
    }
}
