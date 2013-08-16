/*

Copyright 2010,2013 Google Inc. and other contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.freebase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.freebase.Freebase;
import com.google.api.services.freebase.FreebaseRequestInitializer;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.oauth.Credentials;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

public class FreebaseUtils {

    private static final String FREEBASE_API_VERSION = "v1";
//    private static final String FREEBASE_SANDBOX_API_VERSION = "v1sandbox";

    private static final String GOOGLE_RPC_URL = "https://www.googleapis.com/rpc";

    private static final String FREEBASE_SERVICE_URL = "https://www.googleapis.com/freebase/" + FREEBASE_API_VERSION;

    private static final String GOOGLE_BATCH_URL = "https://www.googleapis.com/batch";

    static final Logger logger = LoggerFactory.getLogger("freebase");
    
    static final public String FREEBASE_HOST = "freebase.com";
    
    static final private String FREEQ_URL = "http://data.labs.freebase.com/freeq/refine";
    
    static final private String AGENT_ID = "/en/google_refine";
        
    static final private int SAMPLE_SIZE = 300;
    static final private int JUDGES = 4;
    
    public static final String API_KEY = "AIzaSyBAZ_EjMPKlOzyyZXv6JKXPPwJFISVji3M";

    public static String getApiKey() {
        PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
        String key = (String) ps.get("freebase.api.key");
        if (key == null) {
            key =  System.getProperty("refine.google_api_key");
        }
        return key == null ? API_KEY : key;
    }

    private static String getUserInfoURL(String host) {
        // TODO: Needs to be upgraded to new APIs sandbox-freebase.com as host becomes v1sandbox as version
        return "http://api." + host + "/api/service/user_info";
    }

    private static String getMQLWriteURL(String host) {
        return "http://api." + host + "/api/service/mqlwrite";
    }

    private static String getMQLReadURL(String host) {
        return "http://api." + host + "/api/service/mqlread";
    }
    
    private static String getUserAgent() {
        return RefineServlet.FULLNAME;        
    }
    
    public static String getUserInfo(Credentials credentials, Provider provider) 
        throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException {
        
        OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);
        
        HttpGet httpRequest = new HttpGet(getUserInfoURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        
        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");
        
        // sign the request with the oauth library
        consumer.sign(httpRequest);

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }

    public static String getUserBadges(Provider provider, String user_id)
    throws ClientProtocolException, IOException, JSONException {

        String query = "{" +
          "'id' : '" + user_id + "'," +
          "'!/type/usergroup/member' : [{" +
            "'id' : null," +
            "'key' : [{" +
              "'namespace' : null" +
            "}]" +
          "}]" +
        "}".replace("'", "\"");

        return mqlread(provider, query);
    }

    /**
     * Perform an MQLREAD operation using the credentials of the given OAuth provider
     * 
     * @deprecated This will go away when we switch to Google authentication.
     */
    @Deprecated
    public static String mqlread(Provider provider, String query) 
    throws ClientProtocolException, IOException, JSONException {
        
        JSONObject envelope = new JSONObject();
        envelope.put("query", new JSONObject(query));
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("query", envelope.toString()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

        HttpPost httpRequest = new HttpPost(getMQLReadURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        httpRequest.setEntity(entity);

        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
    
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }

    
    /**
     * Perform a single unauthenticated MQLread.
     * 
     * (wrapper method for a bunch of alternative implementations)
     */
    static public String mqlread(String query) 
            throws IOException, JSONException {
        // A bunch of implementations which don't work for MQLread, but do for other methods
        // String result = rpcCall(query);
        // String result = googleCall(query);
        // String result = batchCall1(query);

        String result =  mqlreadBatchMime(query);
        return result;
    }
    
    
    public static String mqlwrite(Credentials credentials, Provider provider, String query) 
    throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException, JSONException {
        OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);

        JSONObject envelope = new JSONObject();
        envelope.put("query", new JSONObject(query));
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("query", envelope.toString()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

        HttpPost httpRequest = new HttpPost(getMQLWriteURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        httpRequest.setEntity(entity);

        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");

        // sign the request with the oauth library
        consumer.sign(httpRequest);

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
    
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }

    private static String getTweezersParams(int sample_size, int judges) {
        String o = "{" +
            "'sample_size':" + sample_size + "," +
            "'votes':{" +
                "'reconciled':" + judges + "," +
                "'invalid':" + judges + "," +
                "'new':" + judges + "," +
                "'skip':" + (judges + 2) +
            "}" + 
        "}";
        return o.replace('\'', '"');
    }
    
    public static String uploadTriples(
        HttpServletRequest request,
        String qa,
        String source_name,
        String source_id,
        String mdo_id,
        String triples
    ) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, JSONException, IOException {
                
        Provider provider = OAuthUtilities.getProvider(FREEBASE_HOST);
        
        Credentials credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
        
        JSONObject mdo_info = new JSONObject();
        mdo_info.put("name", source_name);
        if (source_id != null) {
            mdo_info.put("info_source",source_id);
        }
        
        JSONObject user_info = new JSONObject(getUserInfo(credentials, provider));
        if (user_info.has("username")) {

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("user", user_info.getString("id")));
            formparams.add(new BasicNameValuePair("action_type", "LOAD_TRIPLE"));
            formparams.add(new BasicNameValuePair("operator", user_info.getString("id")));
            formparams.add(new BasicNameValuePair("software_tool_used", AGENT_ID));
            formparams.add(new BasicNameValuePair("mdo_info", mdo_info.toString()));
            formparams.add(new BasicNameValuePair("graphport", "sandbox"));
            formparams.add(new BasicNameValuePair("payload", triples));
            formparams.add(new BasicNameValuePair("check_params", "false"));
            if (mdo_id != null) {
                formparams.add(new BasicNameValuePair("mdo_guid", mdo_id));
            }
            if (Boolean.parseBoolean(qa)) {
                formparams.add(new BasicNameValuePair("rabj", getTweezersParams(SAMPLE_SIZE,JUDGES)));
            }

            String freeqKey = System.getProperty("freeq.key");
            if (freeqKey != null) {
                logger.warn("Found Freeq key, will bypass OAuth signature");
                formparams.add(new BasicNameValuePair("apikey", freeqKey));
            }

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

            HttpPost httpRequest = new HttpPost(getFreeQUrl());
            httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
            httpRequest.setEntity(entity);

            if (freeqKey == null) {
                logger.warn("Calculating OAuth signature");
                HttpPost surrogateRequest = new HttpPost(getUserInfoURL(FREEBASE_HOST));
                surrogateRequest.setEntity(entity);
                
                OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);
    
                // TODO(SM) This method uses a lot of memory and often results in OutOfMemoryErrors.
                // Is there something we can do to generate an oauth signature without consuming so much memory?
                consumer.sign(surrogateRequest);
                    
                Header[] h = surrogateRequest.getHeaders("Authorization");
                if (h.length > 0) {
                    httpRequest.setHeader("X-Freebase-Credentials", h[0].getValue());
                } else {
                    throw new RuntimeException("Couldn't find the oauth signature header in the surrogate request");
                }
            }
            
            // execute the request
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            
            // return the results
            return EntityUtils.toString(httpResponse.getEntity());
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
        
    static public String getFreeQUrl() {
        String url = (String) ProjectManager.singleton.getPreferenceStore().get("freebase.freeq");
        return url != null ? url : FREEQ_URL;
    }


    static final String BOUNDARY = "---theOpenRefineBoundary--=";
    
    /**
     * A hand rolled MIME multipart/mixed implementation for Google's Batch API
     */
    static private String mqlreadBatchMime(String query) throws JSONException, IOException {
        URL url = new URL(GOOGLE_BATCH_URL);
        String service_url = FREEBASE_SERVICE_URL+"/mqlread";
        
        // We could use the javax.mail package, but it's actually more trouble than it's worth
        String body = "--" + BOUNDARY + "\n" 
            + queryToMimeBodyPart("0", query, service_url, FreebaseUtils.getApiKey())
            + "\n--" + BOUNDARY + "\n" ;
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type","multipart/mixed; boundary="+ BOUNDARY); 
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
    
        Writer writer = new OutputStreamWriter(connection.getOutputStream());
        try {
            writer.write(body);
        } finally {
            writer.flush();
            writer.close();
        }
    
        connection.connect();
        String result = null;
        if (connection.getResponseCode() >= 400) {
            String responseMessage = connection.getResponseMessage();
            String errorStream = ParsingUtilities.inputStreamToString(connection.getErrorStream());
            LoggerFactory.getLogger("freebase").error(
                    "Error in mqlreadMime: " + connection.getResponseCode() + ":" + responseMessage + " : "
                            + errorStream);
        } else {
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                String boundary = s.substring(0,s.indexOf("\n"));
                boundary = boundary.split("\r")[0]; 
                String[] part = s.split(boundary); // part 0 is empty because of leading boundary
                String[] sections = part[1].split("\r\n\r\n");
                // Mime headers, followed by HTTP headers, followd by actual response
                result = sections[2];
            } finally {
                is.close();
            }
        }
        return result;        
    }

    static String queryToMimeBodyPart(String query_name, 
            String query, String service_url, String api_key) 
                    throws IOException {
        // We could use the javax.mail package, but it's actually more trouble than it's worth
        StringBuilder sb = new StringBuilder();
        sb.append("Content-Type: application/http\n"); 
        sb.append("Content-Transfer-Encoding: binary\n");
        sb.append("Content-ID: " + query_name + "\n");
        sb.append("\n");
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("query",query));
        params.add(new BasicNameValuePair("key", api_key));
        UrlEncodedFormEntity param_string = new UrlEncodedFormEntity(params, "UTF-8");
        
        String body = "GET " + service_url + "?" + ParsingUtilities.inputStreamToString(param_string.getContent()) + "\n";
        sb.append(body);
        sb.append("\n");
        
        return sb.toString();
    }

    //////////////////////// Unused methods for future use /////////////////////

    /**
     * This RPC call works for the Reconcile API, but MQLread is not supported over JSONRPC
     * 
     * NOTE: JSONRPC has been deprecated and replaced by HTTP Batch (which also
     * doesn't support MQLread, so perhaps we should just remove this))
     */
    @SuppressWarnings("unused")
    static private JSONObject mqlreadRpc(String query) throws JSONException, UnsupportedEncodingException, IOException {
        URL url = new URL(GOOGLE_RPC_URL);
    
        JSONObject params = new JSONObject();
        params.put("query",query);
        params.put("key", FreebaseUtils.getApiKey());
    
        JSONObject req1 = new JSONObject();
        req1.put("jsonrpc","2.0");    
        req1.put("id","q0");
        req1.put("method","freebase.mqlread");
        req1.put("apiVersion", FREEBASE_API_VERSION);
        req1.put("params",params);
    
        JSONArray body = new JSONArray();
        body.put(req1);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json"); //
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(),"utf-8");
        try {
            writer.write(body.toString());
        } finally {
            writer.flush();
            writer.close();
        }
        
        connection.connect();
        JSONArray result = null;
        if (connection.getResponseCode() >= 400) {
           String responseMessage = connection.getResponseMessage();
           String errorStream = ParsingUtilities.inputStreamToString(connection.getErrorStream());
           LoggerFactory.getLogger("freebase").error(
                   "Error in mqlreadMime: " + connection.getResponseCode() + ":" + responseMessage + " : "
                           + errorStream);
        } else {
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                result = ParsingUtilities.evaluateJsonStringToArray(s);
            } finally {
                is.close();
            }
        }
        return result.getJSONObject(0);
    }
    
    
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final FreebaseRequestInitializer REQUEST_INITIALIZER = 
        new FreebaseRequestInitializer(FreebaseUtils.getApiKey());
    
    /**
     * Submit a single MQL read query via the standard Google client library
     */
    @SuppressWarnings("unused")
    static private String mqlreadFreebaseClient(String query)
            throws IOException, JSONException {
        
        Freebase client = new Freebase.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
            .setApplicationName("OpenRefine")
            .setFreebaseRequestInitializer(REQUEST_INITIALIZER)
            .build();

        InputStream is = client.mqlread(query).executeAsInputStream();
        String result = ParsingUtilities.inputStreamToString(is);
        return result;
    }
    
    
    /**
     * Submit a single MQL query via the Batch endpoint
     * (not supported by Google's Java client)
     */
    @SuppressWarnings("unused")
    static private JSONObject mqlreadBatchFreebaseClient(String query) throws IOException, JSONException {
        JSONObject response = null;
        
        // FIXME: We really want JsonBatchCallback<Freebase> here, but it's not supported right now
        JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
            public void onSuccess(Void res, HttpHeaders responseHeaders) {
                System.out.println(res);
            }
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
              System.out.println("Error Message: " + e.getMessage());
            }
          };
    
          Freebase client = new Freebase.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
            .setApplicationName("OpenRefine")
            .setFreebaseRequestInitializer(REQUEST_INITIALIZER)
            .build();
          
          // FIXME: Batch doesn't work with MqlRead since it extends FreebaseRequest<Void>
          BatchRequest batch = client.batch();
          client.mqlread(query).queue(batch, callback); 
          batch.execute();
    
           return response;
    }

}
