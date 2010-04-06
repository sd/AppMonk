package appmonk.tricks;

import org.json.JSONObject;

public class JsonTricks {
    public static String getString(JSONObject json, String... path) {
        json = getJSONObject(json, path, 0, path.length - 1);
        if (json != null) {
            return json.optString(path[path.length - 1]);
        }
        return null;
    }
    
    public static JSONObject getJSONObject(JSONObject json, String... path) {
        return getJSONObject(json, path, 0, path.length - 1);
    }
    
    protected static JSONObject getJSONObject(JSONObject json, String[] path, int start, int length) {
        if (start >= length)
            return json;
       
        while (start < length) {
            if (json == null)
                break;
            else
                json = json.optJSONObject(path[start]);
            start++;
        }
        return json;
    }
}
