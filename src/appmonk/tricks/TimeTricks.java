package appmonk.tricks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;
import appmonk.toolkit.R;

@SuppressWarnings("unused")
public class TimeTricks {
    
    protected static final SimpleDateFormat RFC_3339_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
    protected static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);

    protected static final SimpleDateFormat[] ISO_FORMATS = {ISO_8601_FORMAT, RFC_3339_FORMAT};
    
    protected static boolean initialized = false;
    
    protected static String str_less_than_a_minute = "";
    protected static String str_one_minute = "";
    protected static String str_n_minutes = "";
    protected static String str_one_hour = "";
    protected static String str_n_hours = "";
    protected static String str_one_day = "";
    protected static String str_n_days = "";
    
    protected static String str_less_than_a_minute_ago = "";
    protected static String str_one_minute_ago = "";
    protected static String str_n_minutes_ago = "";
    protected static String str_one_hour_ago = "";
    protected static String str_n_hours_ago = "";
    protected static String str_one_day_ago = "";
    protected static String str_n_days_ago = "";
    protected static String str_in_the_future = "";

    public static void initializeResources() {
        if (initialized)
            return;
        initialized = true;
        
        str_less_than_a_minute = AppMonk.getString(R.string.less_than_a_minute);
        str_one_minute = AppMonk.getString(R.string.one_minute);
        str_n_minutes = AppMonk.getString(R.string.n_minutes);
        str_one_hour = AppMonk.getString(R.string.one_hour);
        str_n_hours = AppMonk.getString(R.string.n_hours);
        str_one_day = AppMonk.getString(R.string.one_day);
        str_n_days = AppMonk.getString(R.string.n_days);

        str_less_than_a_minute_ago = AppMonk.getString(R.string.less_than_a_minute_ago);
        str_one_minute_ago = AppMonk.getString(R.string.less_than_a_minute_ago);
        str_n_minutes_ago = AppMonk.getString(R.string.n_minutes_ago);
        str_one_hour_ago = AppMonk.getString(R.string.one_hour_ago);
        str_n_hours_ago = AppMonk.getString(R.string.n_hours_ago);
        str_one_day_ago = AppMonk.getString(R.string.one_day_ago);
        str_n_days_ago = AppMonk.getString(R.string.one_day_ago);
        str_in_the_future = AppMonk.getString(R.string.in_the_future);
    }

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
                // Log.d("AppMonk", "failed to parse date " + dateStr + " as " + format.toPattern(), e);
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
    
    public static String timeAgoInWords(Calendar date) {
        if (date == null) { return ""; }
        return timeAgoInWords(date.getTimeInMillis());
    }

    public static String timeInWords(long millis) {
        if (millis < 0) {
            millis = -millis;
        }

        if (millis < (1000 * 60)) {
            return str_less_than_a_minute;
        }
        else if (millis < (1000 * 60)) {
            return str_one_minute;
        }
        else if (millis < (1000 * 60 * 60)) {
            return String.format(str_n_minutes, (millis / (1000 * 60)));
        }
        else if (millis < (1000 * 60 * 60 * 2)) {
            return str_one_hour;
        }
        else if (millis < (1000 * 60 * 60 * 36)) {
            return String.format(str_n_hours, (millis / (1000 * 60 * 60)));
        }
        else if (millis < (1000 * 60 * 60 * 24 * 2)) {
            return str_one_day;
        }
        else {
            return String.format(str_n_days, (millis / (1000 * 60 * 60 * 24)));
        }
    }

    public static String timeAgoInWords(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 0) {
            return str_in_the_future;
        }
        else if (diff < (60 * 1000)) {
            return str_less_than_a_minute_ago;
        }
        else if (diff < (60 * 1000 * 2)) {
            return str_one_minute_ago;
        }
        else if (diff < (60 * 1000 * 60)) {
            return String.format(str_n_minutes_ago, (diff / (60 * 1000)));
        }
        else if (diff < (60 * 1000 * 60 * 2)) {
            return str_one_hour_ago;
        }
        else if (diff < (60 * 1000 * 60 * 24)) {
            return String.format(str_n_hours_ago, (diff / (60 * 1000 * 60)));
        }
        else if (diff < (60 * 1000 * 60 * 24 * 2)) {
            return str_one_day_ago;
        }
        else {
            return String.format(str_n_days_ago, (diff / (60 * 1000 * 60 * 24)));
        }
    }
}
