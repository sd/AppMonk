package appmonk.net;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.util.Log;
import appmonk.tricks.AppMonk;

// Wraps SimpleHttpClient and provides very simple, non-http-standard, file-based caching of responses
//
// NOTE: NOT HTTP STANDARD CACHING. This code ignores http cache headers
//
public class CachingHttpClient extends SimpleHttpClient {
    public static final int DEFAULT_TTL = 60 * 30 * 1000; // 30 minutes
    
    protected SimpleFileCache mCache = null;
    protected int mDefaultMaxAge = DEFAULT_TTL;
    
    public CachingHttpClient() {
        this(AppMonk.getContext());
    }

    public CachingHttpClient(Context context) {
        super();
        
        mCache = new SimpleFileCache(context, "http");
    }
    
    public void setDefaultMaxAge(int maxAge) {
        mDefaultMaxAge = maxAge;
    }
    
    @Override
    protected Response performRequest(HttpUriRequest request, Response response) {
        return performRequest(request, response, mDefaultMaxAge);
    }
    
    protected Response performRequest(HttpUriRequest request, Response response, int maxAge) {
        Response cachedResponse = null;
        String cacheUri = null;
        
        if (request.getMethod().equals("GET") && maxAge > 0) {
            cacheUri = request.getURI().toString();
            try {
                cachedResponse = (Response) mCache.fetch(cacheUri, maxAge);
                if (cachedResponse != null) {
                    return cachedResponse;
                }
            }
            catch (ClassCastException e) {
            }
        }
        
        response = super.performRequest(request, response);
        
        if (cacheUri != null) {
            response.getAllData();
            if (response.isOk())
                mCache.store(cacheUri, response);
        }
        
        return response;
    }
    
    public boolean isCachedAndFresh(String uri, int ttl) {
        return mCache.isFresh(uri, ttl);
    }

    public boolean clearFromCache(URI uri) {
        return mCache.remove(uri.toString());
    }

    protected boolean clearFromCache(String baseUri, String path, List<NameValuePair> params) {
        try {
            return clearFromCache(baseUri, path, new UrlEncodedFormEntity(params, HTTP.UTF_8));
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding parameters", e);
            return false;
        }

    }
    protected boolean clearFromCache(String baseUri, String path, Map<String, String> params) {
        try {
            return clearFromCache(baseUri, path, mapToEntity(params));
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding parameters", e);
            return false;
        }
    }

    public boolean clearFromCache(String baseUri, String path, HttpEntity params) {
        HttpUriRequest request = prepareRequest(GET, baseUri, path, params);
        
        return clearFromCache(request.getURI());
    }
    
    public boolean clearFromCache(String path, Map<String, String> params) {
        return clearFromCache(baseUri(), path, params);
    }

    public boolean clearFromCache(String path) {
        Map<String, String> params = new HashMap<String, String>();
        return clearFromCache(baseUri(), path, params);
    }

    public boolean clearFromCache(String path, String name1, String value1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        return clearFromCache(baseUri(), path, params);
    }

    public boolean clearFromCache(String path, String name1, String value1, String name2, String value2) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        return clearFromCache(baseUri(), path, params);
    }

    public boolean clearFromCache(String path, String name1, String value1, String name2, String value2, String name3,
            String value3) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return clearFromCache(baseUri(), path, params);
    }

}
