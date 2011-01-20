package appmonk.tricks;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

public class StorageTricks {
	public static final String CAMERA_TEMP_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.tmp";
	public static final File CAMERA_TEMP_FILE = new File(CAMERA_TEMP_DIR, "camera.jpg");
	
	
	public static boolean checkExtStorage() {
		File dir = new File(CAMERA_TEMP_DIR);
		if (!dir.exists()) {
			try {
				if (!dir.mkdirs())
					return false;
			} catch (Exception e) {
				return false;
			}
		}
		
		if (!dir.canWrite())
			return false;
		
		File noMedia = new File(dir, ".nomedia");
		try {
			noMedia.createNewFile();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static Uri putImageFileIntoGalleryAndGetUri(Context c, File imageFile, boolean deleteImageFileAfter) {
		if (imageFile.exists() && imageFile.isFile()) {
			try {
				Uri dataUri = Uri.parse(MediaStore.Images.Media.insertImage(c.getContentResolver(), imageFile.getAbsolutePath(), null, null));
				if (deleteImageFileAfter)
					imageFile.delete();
				return dataUri;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}	
		}
		return null;
	}
}
