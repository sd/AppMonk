package appmonk.tricks;

/*
 * Copyright (C) 2009, 2010 Sebastian Delmont <sd@notso.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 * 
 */

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonTricks {
    public static JSONObject getJSONObject(JSONObject json, String... path) {
        return getJSONObject(json, path, 0, path.length - 1);
    }
    
    public static String getString(JSONObject json, String... path) {
        json = getJSONObject(json, path, 0, path.length - 1);
        if (json != null) {
            return json.optString(path[path.length - 1]);
        }
        return null;
    }

    public static long getLong(JSONObject json, long defaultValue, String... path) {
        json = getJSONObject(json, path, 0, path.length - 1);
        if (json != null) {
            return json.optLong(path[path.length - 1], defaultValue);
        }
        return defaultValue;
    }

    public static JSONArray getJSONArray(JSONObject json, String... path) {
        json = getJSONObject(json, path, 0, path.length - 1);
        if (json != null) {
            return json.optJSONArray(path[path.length - 1]);
        }
        return null;
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
