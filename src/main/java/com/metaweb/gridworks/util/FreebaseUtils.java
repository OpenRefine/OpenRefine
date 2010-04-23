package com.metaweb.gridworks.util;

import java.io.IOException;
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
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.oauth.Credentials;
import com.metaweb.gridworks.oauth.OAuthUtilities;
import com.metaweb.gridworks.oauth.Provider;

public class FreebaseUtils {

    static final public String FREEBASE_HOST = "www.freebase.com";
    static final public String FREEBASE_SANDBOX_HOST = "www.sandbox-freebase.com";
    
    static final private String FREEQ_URL = "http://data.labs.freebase.com/freeq/prod/";
    
    private static String getUserInfoURL(String host) {
        return "http://" + host + "/api/service/user_info";
    }

    public static String getUserInfo(Credentials credentials, Provider provider) 
        throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException {
        
        OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);
        
        HttpGet httpRequest = new HttpGet(getUserInfoURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Gridworks " + Gridworks.getVersion());
        
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
    
    public static String uploadTriples(HttpServletRequest request, String info, String triples) 
        throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, JSONException, IOException {
        
        Provider provider = OAuthUtilities.getProvider(FREEBASE_HOST);
        
        Credentials credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
        
        JSONObject user_info = new JSONObject(getUserInfo(credentials, provider));
        if (user_info.has("username")) {
            
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("user", user_info.getString("username")));
            formparams.add(new BasicNameValuePair("action_type", "LOAD_TRIPLE"));
            formparams.add(new BasicNameValuePair("operator", "gridworks"));
            formparams.add(new BasicNameValuePair("mdo_info", info));
            formparams.add(new BasicNameValuePair("graphport", provider.getHost().equals(FREEBASE_HOST) ? "otg" : "sandbox"));
            formparams.add(new BasicNameValuePair("payload", triples));
            formparams.add(new BasicNameValuePair("check_params", "false"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

            HttpPost httpRequest = new HttpPost(FREEQ_URL);
            httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Gridworks " + Gridworks.getVersion());
            httpRequest.setEntity(entity);
            
            HttpPost surrogateRequest = new HttpPost(getUserInfoURL(FREEBASE_HOST));
            surrogateRequest.setEntity(entity);
            
            OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);

            consumer.sign(surrogateRequest);

            Header[] h = surrogateRequest.getHeaders("Authorization");
            if (h.length > 0) {
                httpRequest.setHeader("X-Freebase-Credentials", h[0].getValue());
            } else {
                throw new RuntimeException("Couldn't find the oauth signature header in the surrogate request");
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

}
