package appmonk.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import appmonk.tricks.AppMonk;

public class ImageRoundCornersOperation extends ImageRequest.Operation {
    protected static final String TAG = "AppMonk";

    int radiusInDips = -1;
    
    public ImageRoundCornersOperation(ImageRequest request, int radiusInDips) {
        super(request);
        this.radiusInDips = radiusInDips;
    }

    @Override
    public Bitmap perform(Bitmap previousBitmap) {
        if (previousBitmap == null)
            return null;
        
        Bitmap output = null;
        int w = previousBitmap.getWidth();
        int h = previousBitmap.getHeight();
        try {
            output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Error rounding corners", e);
        }

        if (output != null) {
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, w, h);
            final RectF rectF = new RectF(rect);
            final float roundPx = radiusInDips * AppMonk.screenDensity;

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(previousBitmap, rect, rect, paint);
        }
        
        return output;
    }

    @Override
    public String name(String previousName) {
        return "corner_" + radiusInDips + "-" + previousName;
    }
}