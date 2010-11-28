package appmonk.tricks;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlTricks {
    public static String appendParameter(String url, String name, String value) {
        int questionMark = url.indexOf("?");
        int hashMark = url.indexOf("#");
        
        char separator = (questionMark >= 0 ? '&' : '?');
        
        StringBuffer sb = new StringBuffer();
        if (hashMark >= 0) {
            sb.append(url.substring(0, hashMark));
            sb.append(separator);
            sb.append(urlEncode(name));
            sb.append("=");
            sb.append(urlEncode(value));
            sb.append(url.substring(hashMark));
        }
        else {
            sb.append(url);
            sb.append(separator);
            sb.append(urlEncode(name));
            sb.append("=");
            sb.append(urlEncode(value));
        }
        
        return sb.toString();
    }
    
    public static String urlEncode(String value) {
        try {
            if (value == null)
                return "";
            else
                return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
