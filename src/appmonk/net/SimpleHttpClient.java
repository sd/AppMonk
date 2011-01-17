package appmonk.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Build;
import android.util.Log;
import appmonk.tricks.AppMonk;
import appmonk.tricks.IOTricks;


public class SimpleHttpClient {
    protected static final String TAG = "AppMonk/HTTP";

    public static boolean debug = false;
    
    protected BasicHttpContext httpContext;

    public SimpleHttpClient() {
        httpContext = new BasicHttpContext();
    }

    protected static String defaultUserAgent = null;
    public static void setDefaultUserAgent(String ua) {
        defaultUserAgent = ua;
    }
    
    public static String getDefaultUserAgent() {
        if (defaultUserAgent == null) {
            defaultUserAgent = AppMonk.applicationName + " - " + AppMonk.packageName 
                        + AppMonk.versionName + "/" + AppMonk.versionCode 
                        + " (Android " + Build.VERSION.RELEASE  + " " + Build.FINGERPRINT
                        + " on " + Build.MODEL + "/" + Build.PRODUCT 
                        + " ; " + Build.TYPE + " " + Build.TAGS
                        + ")";
        }
        return defaultUserAgent;
    }
    
    public static String userAgent() {
        return getDefaultUserAgent();
    }

    public static int GET = 1;
    public static int POST = 2;
    public static int PUT = 3;
    public static int DELETE = 4;

    public static class Response implements Serializable {
        private static final long serialVersionUID = 1L;

        transient HttpResponse httpResponse = null;
        URI uri = null;
        String body = null;
        JSONObject jsonObject = null;
        JSONArray jsonArray = null;
        int status = -1;
        private boolean isRetry = false;

        public String body() {
            if (body == null) {
                try {
                    HttpEntity entity = httpResponse.getEntity();
                    body = SimpleHttpClient.streamToString(entity.getContent());
                    entity.consumeContent();
                }
                catch (OutOfMemoryError e) {
                    if (!isRetry) {
                        System.gc();
                        isRetry = true;
                        return body();
                    }
                    isRetry = false;
                    Log.e(TAG, "Out of memory converting stream to string.", e);
                }
                catch (IllegalStateException e) {
                    Log.e(TAG, "Error streaming response body", e);
                }
                catch (IOException e) {
                    Log.e(TAG, "Error streaming response body", e);
                }
            }
            return body;
        }

        public JSONObject asJSONObject() {
            if (jsonObject == null) {
                try {
                    jsonObject = new JSONObject(body());
                }
                catch (OutOfMemoryError e) {
                    if (!isRetry) {
                        System.gc();
                        isRetry = true;
                        return asJSONObject();
                    }
                    isRetry = false;
                    Log.e(TAG, "Out of memory creating JSONObject.", e);
                }
                catch (Exception e) {
                }
            }
            
            return jsonObject;
        }

        public JSONArray asJSONArray() {
            if (jsonArray == null) {
                try {
                    jsonArray = new JSONArray(body());
                }
                catch (OutOfMemoryError e) {
                    if (!isRetry) {
                        System.gc();
                        isRetry = true;
                        return asJSONArray();
                    }
                    isRetry = false;
                    Log.e(TAG, "Out of memory creating JSONArray.", e);
                }
                catch (Exception e) {
                }
            }
            return jsonArray;
        }

        public int status() {
            if (status == -1 && httpResponse != null) {
                status = httpResponse.getStatusLine().getStatusCode();
            }
            return status;
        }

        public boolean isNetworkGone() {
            return (httpResponse == null);
        }

        public boolean isNetworkOk() {
            return (httpResponse != null);
        }

        public boolean isOk() {
            return status() == 200;
        }

        public boolean isNotAuthorized() {
            return status() == 403;
        }

        public boolean isBadCredentials() {
            return status() == 401;
        }

        public boolean isNotFound() {
            return status() == 404;
        }

        public boolean isUnprocessable() {
            return status() == 422;
        }
    }
    
    protected static Response defaultResponse() {
        return new Response();
    }

    protected static final int MAX_TOTAL_CONNECTIONS = 8;
    protected static final int CONNECTION_TIMEOUT = 10000;
    protected static final int SOCKET_TIMEOUT = 20000;
    private static DefaultHttpClient httpClient = null;

    protected DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpParams httpParams = new BasicHttpParams();
            HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
            HttpProtocolParams.setUseExpectContinue(httpParams, false); // some webservers have problems if this is set to true
            ConnManagerParams.setMaxTotalConnections(httpParams, MAX_TOTAL_CONNECTIONS);
            HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);
            
//            httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost("192.168.16.180", 8888, "http"));
            httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
            
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
            
            ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

            httpClient = new DefaultHttpClient(connectionManager, httpParams);
        }

        return httpClient;
    }

    public void setBasicAuthCredentials(String login, String password, String scope) {
        getHttpClient();

        httpClient.getCredentialsProvider().setCredentials(new AuthScope(scope, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(login, password));

        httpContext = new BasicHttpContext();

        // Generate BASIC scheme object and stick it to the local 
        // execution context
        BasicScheme basicAuth = new BasicScheme();
        httpContext.setAttribute("preemptive-auth", basicAuth);

        // Add as the first request interceptor
        httpClient.addRequestInterceptor(new ForcePreemptiveAuth(), 0);

    }

    protected HttpUriRequest prepareRequest(int method, String baseUri, String path, HttpEntity paramsEntity) {
        URI uri = null;
        HttpUriRequest request = null;

        try {
            StringBuffer sb = new StringBuffer();
            
            if (method == GET) {
                sb.append(baseUri);
                sb.append(path);
                
                if (paramsEntity != null) {
                    sb.append("?");
                    try {
                        IOTricks.copyStreamToStringBuffer(paramsEntity.getContent(), sb);
                    }
                    catch (IllegalStateException e) {
                        Log.e(TAG, "Error preparing url", e);
                        return null;
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Error preparing url", e);
                        return null;
                    }
                }
                uri = new URI(sb.toString());

                if (SimpleHttpClient.debug) Log.i(TAG, "GET " + uri.toString());
                if (SimpleHttpClient.debug && paramsEntity != null) Log.i(TAG, "-- " + IOTricks.StreamToString(paramsEntity.getContent()));

                request = new HttpGet(uri);
            }
            else if (method == POST) {
                sb.append(baseUri);
                sb.append(path);

                uri = new URI(sb.toString());

                if (SimpleHttpClient.debug) Log.i(TAG, "POST " + uri.toString());
                if (SimpleHttpClient.debug && paramsEntity != null) try {Log.i(TAG, "-- " + IOTricks.StreamToString(paramsEntity.getContent()));} catch (Exception e) {}

                request = new HttpPost(uri);

                request.getParams().setBooleanParameter("http.protocol.expect-continue", false);

                if (paramsEntity != null) {
                    request.addHeader(paramsEntity.getContentType());
                    ((HttpPost) request).setEntity(paramsEntity);
                }
                else {
                    request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                }
            }
            else if (method == PUT) {
                sb.append(baseUri);
                sb.append(path);

                uri = new URI(sb.toString());

                if (SimpleHttpClient.debug) Log.i(TAG, "PUT " + uri.toString());
                if (SimpleHttpClient.debug && paramsEntity != null) Log.i(TAG, "-- " + IOTricks.StreamToString(paramsEntity.getContent()));
                
                request = new HttpPut(uri);

                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                request.getParams().setBooleanParameter("http.protocol.expect-continue", false);


                if (paramsEntity != null) {
                    request.addHeader(paramsEntity.getContentType());
                    ((HttpPut) request).setEntity(paramsEntity);
                }
                else {
                    request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                }
            }
            else if (method == DELETE) {
                sb.append(baseUri);
                sb.append(path);

                if (paramsEntity != null) {
                    sb.append("?");
                    try {
                        IOTricks.copyStreamToStringBuffer(paramsEntity.getContent(), sb);
                    }
                    catch (IllegalStateException e) {
                        Log.e(TAG, "Error preparing url", e);
                        return null;
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Error preparing url", e);
                        return null;
                    }
                }
                uri = new URI(sb.toString());

                if (SimpleHttpClient.debug) Log.i(TAG, "DELETE " + uri.toString());
                if (SimpleHttpClient.debug && paramsEntity != null) Log.i(TAG, "-- " + IOTricks.StreamToString(paramsEntity.getContent()));

                request = new HttpDelete(uri);
            }

            request.addHeader("User-Agent", userAgent());
        }
        catch (URISyntaxException e) {
            Log.e(TAG, "Invalid URI for " + baseUri + " / " + path + " ...", e);
        }
        catch (IllegalStateException e) {
            Log.e(TAG, "Illegal State for " + baseUri + " / " + path + " ...", e);
        }
        catch (IOException e) {
            Log.e(TAG, "IOException for " + baseUri + " / " + path + " ...", e);
        }

        return request;
    }

    protected Response performRequest(HttpUriRequest request, Response response) {
        HttpClient httpClient = getHttpClient();

        if (response == null)
            response = defaultResponse();
        
        try {
            response.uri = request.getURI();
            response.httpResponse = httpClient.execute(request, httpContext);
        }
        catch (ClientProtocolException e) {
            request.abort();
            Log.e(TAG, "Client Protocol Exception for " + response.uri, e);
        }
        catch (SocketException e) {
            request.abort();
            Log.e(TAG, "Socket Exception for " + response.uri, e);
        }
        catch (UnknownHostException e) {
            request.abort();
            Log.e(TAG, "Unknown Host Exception for " + response.uri, e);
        }
        catch (IOException e) {
            request.abort();
            Log.e(TAG, "IO Exception for " + response.uri, e);
        }
        catch (Exception e) {
            request.abort();
            Log.e(TAG, "Exception for " + response.uri, e);
        }

        if (response.httpResponse != null) {
            if (SimpleHttpClient.debug) {
                Log.d(TAG, "RESPONSE: " + response.httpResponse.getStatusLine() + " for " + response.uri.toString());
                Log.d(TAG, response.body());
            }
            else {
                response.body();
            }
        }

        return response;
    }

    protected HttpEntity mapToEntity(Map<String, String> params) throws UnsupportedEncodingException {
        if (params != null && !params.isEmpty()) {
            if (params.containsKey("")) {
                return new StringEntity(params.get(""), HTTP.UTF_8);
            }
            else {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                for (String key : params.keySet()) {
                    if (SimpleHttpClient.debug) Log.e(TAG, "-- " + key + ": " + params.get(key));
                    nvps.add(new BasicNameValuePair(key, params.get(key)));
                }
                return new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
            }
        }
        return null;
    }

    protected HttpEntity mapToUploadEntity(Map<String, String> params, String uploadName, Uri uploadData, String uploadType) throws UnsupportedEncodingException {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                if (SimpleHttpClient.debug) Log.d(TAG, "-- " + key + ": " + params.get(key));
                nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        if (SimpleHttpClient.debug) Log.d(TAG, "-- Upload " + uploadName + " " + uploadData);
        return new MIMEFileUploadEntity(nvps, uploadName, uploadData, uploadType);
    }
    
    protected Response request(int method, String baseUri, String path, List<NameValuePair> params) {
        return request(method, baseUri, path, params, null);
    }

    protected Response request(int method, String baseUri, String path, Map<String, String> params) {
        return request(method, baseUri, path, params, null);
    }
    
    protected Response request(int method, String baseUri, String path, HttpEntity params) {
        return request(method, baseUri, path, params, null);
    }

    protected Response request(int method, String baseUri, String path, List<NameValuePair> params, Response response) {
        try {
            return request(method, baseUri, path, new UrlEncodedFormEntity(params, HTTP.UTF_8), response);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding parameters", e);
            return null;
        }

    }

    protected Response request(int method, String baseUri, String path, Map<String, String> params, Response response) {
        try {
            return request(method, baseUri, path, mapToEntity(params), response);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding parameters", e);
            return null;
        }
    }

    public Response requestWithUpload(int method, String baseUri, String path, Map<String, String> params, String attachmentName, Uri attachmentData, String attachmentType) {
        return request(method, baseUri, path, params, null);
    }
    
    public Response requestWithUpload(int method, String baseUri, String path, Map<String, String> params, String attachmentName, Uri attachmentData, String attachmentType, Response response) {
        try {
            return request(method, baseUri, path, mapToUploadEntity(params, attachmentName, attachmentData, attachmentType), response);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding parameters", e);
            return null;
        }
    }
    
    protected Response request(int method, String baseUri, String path, HttpEntity params, Response response) {
        HttpUriRequest request = prepareRequest(method, baseUri, path, params);
        if (request != null) {
            return performRequest(request, response);
        }
        else {
            return null;
        }
    }

    public static String streamToString(InputStream is) {
        InputStreamReader reader = new InputStreamReader(is);

        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[32 * 1024];
        int total = 0;
        int len;
        try {
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
                total += len;
            }
        }
        catch (IOException e) {
            Log.e(TAG, "Error in streamToString", e);
        }
        return sb.toString();
    }

    private static File internalCacheDirectory = null;

    public void setInternalCacheDirectory(File newCacheDirectory) {
        internalCacheDirectory = newCacheDirectory;
        if (!internalCacheDirectory.exists()) internalCacheDirectory.mkdir();
    }

    public static void purgeInternalCache(long maxAge) {
        File files[] = internalCacheDirectory.listFiles();
        long now = System.currentTimeMillis();
        if (files != null) {
            final int length = files.length;
            for (int i = 0; i < length; i++) {
                if (now - files[i].lastModified() > maxAge) {
                    files[i].delete();
                }
            }
        }
    }

    public Response get(String baseUri, String path, Map<String, String> params) {
        return request(GET, baseUri, path, params);
    }

    public Response post(String baseUri, String path, Map<String, String> params) {
        return request(POST, baseUri, path, params);
    }

    public Response put(String baseUri, String path, Map<String, String> params) {
        return request(PUT, baseUri, path, params);
    }

    public Response delete(String baseUri, String path, Map<String, String> params) {
        return request(DELETE, baseUri, path, params);
    }

    public String baseUri() {
        return "";
    }

    public Response get(String path, Map<String, String> params) {
        return request(GET, baseUri(), path, params);
    }

    public Response get(String path) {
        Map<String, String> params = new HashMap<String, String>();
        return request(GET, baseUri(), path, params);
    }

    public Response get(String path, String name1, String value1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        return request(GET, baseUri(), path, params);
    }

    public Response get(String path, String name1, String value1, String name2, String value2) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        return request(GET, baseUri(), path, params);
    }

    public Response get(String path, String name1, String value1, String name2, String value2, String name3,
            String value3) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return request(GET, baseUri(), path, params);
    }

    public Response post(String path, Map<String, String> params) {
        return request(POST, baseUri(), path, params);
    }

    public Response post(String path) {
        Map<String, String> params = new HashMap<String, String>();
        return request(POST, baseUri(), path, params);
    }

    public Response post(String path, String name1, String value1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        return request(POST, baseUri(), path, params);
    }

    public Response post(String path, String name1, String value1, String name2, String value2) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        return request(POST, baseUri(), path, params);
    }

    public Response post(String path, String name1, String value1, String name2, String value2, String name3,
            String value3) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return request(POST, baseUri(), path, params);
    }

    public Response put(String path, Map<String, String> params) {
        return request(PUT, baseUri(), path, params);
    }

    public Response put(String path) {
        Map<String, String> params = new HashMap<String, String>();
        return request(PUT, baseUri(), path, params);
    }

    public Response put(String path, String name1, String value1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        return request(PUT, baseUri(), path, params);
    }

    public Response put(String path, String name1, String value1, String name2, String value2) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        return request(PUT, baseUri(), path, params);
    }

    public Response put(String path, String name1, String value1, String name2, String value2, String name3,
            String value3) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return request(PUT, baseUri(), path, params);
    }

    public Response delete(String path, Map<String, String> params) {
        return request(DELETE, baseUri(), path, params);
    }

    public Response delete(String path) {
        Map<String, String> params = new HashMap<String, String>();
        return request(DELETE, baseUri(), path, params);
    }

    public Response delete(String path, String name1, String value1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        return request(DELETE, baseUri(), path, params);
    }

    public Response delete(String path, String name1, String value1, String name2, String value2) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        return request(DELETE, baseUri(), path, params);
    }

    public Response delete(String path, String name1, String value1, String name2, String value2, String name3,
            String value3) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return request(DELETE, baseUri(), path, params);
    }

    /*
     * == Base64Encoder ======================================================= Copyright (c) 2007-2009, Yusuke Yamamoto
     * All rights reserved.
     * 
     * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
     * following conditions are met: Redistributions of source code must retain the above copyright notice, this list of
     * conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright
     * notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
     * with the distribution. Neither the name of the Yusuke Yamamoto nor the names of its contributors may be used to
     * endorse or promote products derived from this software without specific prior written permission.
     * 
     * THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
     * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
     * EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
     * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
     * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
     * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
     * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     */

    public static class Base64Encoder {
        private static final char last2byte = (char) Integer.parseInt("00000011", 2);
        private static final char last4byte = (char) Integer.parseInt("00001111", 2);
        private static final char last6byte = (char) Integer.parseInt("00111111", 2);
        private static final char lead6byte = (char) Integer.parseInt("11111100", 2);
        private static final char lead4byte = (char) Integer.parseInt("11110000", 2);
        private static final char lead2byte = (char) Integer.parseInt("11000000", 2);
        private static final char[] encodeTable = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
                'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e',
                'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
                'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

        public static String encode(String from) {
            // TODO: Confirm which encoding is used by browsers when Base64'ing passwords for Basic Auth
            // We're currently defaulting to ISO8859, but it might be UTF-8
            return encode(from.getBytes());
        }

        public static String encode(byte[] from) {
            StringBuffer to = new StringBuffer((int) (from.length * 1.34) + 3);
            int num = 0;
            char currentByte = 0;
            final int length = from.length;
            for (int i = 0; i < length; i++) {
                num = num % 8;
                while (num < 8) {
                    switch (num) {
                    case 0:
                        currentByte = (char) (from[i] & lead6byte);
                        currentByte = (char) (currentByte >>> 2);
                        break;
                    case 2:
                        currentByte = (char) (from[i] & last6byte);
                        break;
                    case 4:
                        currentByte = (char) (from[i] & last4byte);
                        currentByte = (char) (currentByte << 2);
                        if ((i + 1) < from.length) {
                            currentByte |= (from[i + 1] & lead2byte) >>> 6;
                        }
                        break;
                    case 6:
                        currentByte = (char) (from[i] & last2byte);
                        currentByte = (char) (currentByte << 4);
                        if ((i + 1) < from.length) {
                            currentByte |= (from[i + 1] & lead4byte) >>> 4;
                        }
                        break;
                    }
                    to.append(encodeTable[currentByte]);
                    num += 6;
                }
            }
            if (to.length() % 4 != 0) {
                for (int i = 4 - to.length() % 4; i > 0; i--) {
                    to.append("=");
                }
            }
            return to.toString();
        }
    }

    static class ForcePreemptiveAuth implements HttpRequestInterceptor {
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            if (request.getFirstHeader("Authorization") != null) {
                // Using OAuth, leave as is
            }
            else if (request.getFirstHeader("X-No-Auth") != null) {
                // No auth required, leave as is
            }
            else {
                // If no auth scheme avaialble yet, try to initialize it preemptively
                if (authState.getAuthScheme() == null) {
                    AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                    CredentialsProvider credsProvider = (CredentialsProvider) context
                            .getAttribute(ClientContext.CREDS_PROVIDER);
                    HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                    if (authScheme != null) {
                        Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(),
                                targetHost.getPort()));
                        if (creds == null) {
                            throw new HttpException("No credentials for preemptive authentication");
                        }
                        else {
                            authState.setAuthScheme(authScheme);
                            authState.setCredentials(creds);
                        }
                    }
                }
            }
        }

    }
    
    public static class MIMEFileUploadEntity implements HttpEntity {
        static String lineEnd = "\r\n";
        static String twoHyphens = "--";

        protected byte[] topBytes;
        protected Uri media;
        protected byte[] bottomBytes;
        protected String contentType;

        public MIMEFileUploadEntity(List<NameValuePair> params, String fileParamName, Uri media, String mediaType) {
            StringBuffer sb = new StringBuffer();

            sb.append("----------MIMEFileUploadEntityBoundary-");
            sb.append(Double.toString(Math.random()).substring(2)).append(Double.toString(Math.random()).substring(2))
                    .append("--");

            String boundary = sb.toString();

            this.media = media;
            this.contentType = "multipart/form-data; boundary=" + boundary;
            String fileName;

            if (mediaType == null) {
                mediaType = AppMonk.getContentResolver().getType(media);
            }
            
            if (mediaType == null) {
                if (media.toString().endsWith(".jpg")) {
                    mediaType = "image/jpeg";
                } else if (media.toString().endsWith(".png")) {
                    mediaType = "image/png";
                }
            }

            if (mediaType.startsWith("image/jpeg")) {
                fileName = "Mobile-Upload.jpg";
            } else if (mediaType.startsWith("image/png")) {
                fileName = "Mobile-Upload.png";
            } else if (mediaType.startsWith("video")) {
                fileName = "Mobile-Upload.3gp";
            } else {
                fileName = "Mobile-Upload";
            }

            sb.setLength(0);
            for (NameValuePair pair : params) {
                sb.append(twoHyphens).append(boundary).append(lineEnd);
                sb.append("Content-Disposition: form-data; name=\"").append(pair.getName()).append("\"").append(lineEnd);
                sb.append(lineEnd);
                sb.append(pair.getValue().trim());
                sb.append(lineEnd);
            }

            sb.append(twoHyphens).append(boundary).append(lineEnd);
            sb.append("Content-Disposition: form-data; name=\"").append(fileParamName).append("\"; filename=\"").append(
                    fileName).append("\"").append(lineEnd);
            sb.append("Content-Type: ").append(mediaType).append(lineEnd);
            sb.append("Content-Transfer-Encoding: binary").append(lineEnd);
            sb.append(lineEnd);

            this.topBytes = sb.toString().getBytes();

            try {
                if (SimpleHttpClient.debug) {
                    String headers = sb.toString();
                    for (NameValuePair pair : params) {
                        if (pair.getName().contains("ass")) { // "Password" or
                            // "PassWord" or
                            // "password"...
                            headers = headers.replace(pair.getValue(), "xxxxxx");
                        }
                    }
                    Log.d("MIME", "MIME Headers: " + headers);
                }
            } catch (Exception e) {
            }

            sb.setLength(0);
            sb.append(lineEnd);
            sb.append(twoHyphens);
            sb.append(boundary);
            sb.append(twoHyphens);
            sb.append(lineEnd);
            this.bottomBytes = sb.toString().getBytes();

        }

        public void consumeContent() throws IOException {
        }

        public InputStream getContent() throws IOException, IllegalStateException {
            throw new IOException();
        }

        public Header getContentEncoding() {
            return null;
        }

        public long getContentLength() {
            int size = 0;

            size += topBytes.length;

            try {
                InputStream mediaStream = AppMonk.getContentResolver().openInputStream(media);
                size += IOTricks.countBytesInStream(mediaStream);
                mediaStream.close();
            } catch (Exception e) {
                return 0;
            }

            size += bottomBytes.length;

            return size;
        }

        public Header getContentType() {
            return new BasicHeader("Content-Type", contentType);
        }

        public boolean isChunked() {
            return false;
        }

        public boolean isRepeatable() {
            return true;
        }

        public boolean isStreaming() {
            return true;
        }

        public void writeTo(OutputStream outstream) throws IOException {
            outstream.write(topBytes);
            try {
                InputStream mediaStream = AppMonk.getContentResolver().openInputStream(media);
                IOTricks.copyStreamToStream(mediaStream, outstream, 32 * 1024);
                mediaStream.close();
            } catch (Exception e) {
            }

            outstream.write(bottomBytes);
        }
    }

}
