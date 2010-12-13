package appmonk.tricks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class TimeTricks {
    
    protected static final SimpleDateFormat RFC_3339_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
    protected static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);

    protected static final SimpleDateFormat[] ISO_FORMATS = {RFC_3339_FORMAT, ISO_8601_FORMAT};
    
    public static Calendar isoStringToCalendar(String dateStr) {
        return isoStringToCalendar(dateStr, null);
    }
    
    public static Calendar isoStringToCalendar(String dateStr, Calendar valueIfError) {
        Date date = null;
        
        for (SimpleDateFormat format : ISO_FORMATS) {
            try {
                date = format.parse(dateStr);
                break;
            }
            catch (ParseException e) {
                Log.d("AppMonk", "failed to parse date " + dateStr + " as " + format.toPattern(), e);
            }
        }
        
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        }
        else {
            return valueIfError;
        }
    }

    public static String millisToIso(long millis) {
        return RFC_3339_FORMAT.format(millis);
    }
}
