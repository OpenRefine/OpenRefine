package com.metaweb.gridworks.oauth;

import java.io.IOException;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreebaseTimeCommonsHttpOAuthConsumer extends CommonsHttpOAuthConsumer {

    final static Logger logger = LoggerFactory.getLogger("oauth");
    
    private static final long serialVersionUID = -4139931605235255279L;

    private static final int SOCKET_TIMEOUT = 3000;
    private static final int CONNECTION_TIMEOUT = 3000;
    
    private static final String TIMER_URL = "http://gridworks-gadgets.freebaseapps.com/time";
    
    public FreebaseTimeCommonsHttpOAuthConsumer(String consumerKey, String consumerSecret) {
        super(consumerKey, consumerSecret);
    }

    /**
     * It might be that the user's computer's clock is not synchronized enough with the Freebase servers
     * and this might result in Freebase thinking that it was under a replay attack.
     * To avoid this problem we get the timestamp directly from acre that we know is synchronized.
     *
     * NOTE: this call is potentially vulnerable to a man-in-the-middle (MITM) attack, but the same
     * could be said if we used an NTP client.
     */
    protected String generateTimestamp() {
        
        long time = -1;
                
        try {
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
            HttpClient httpClient = new DefaultHttpClient(httpParams);
            HttpGet httpget = new HttpGet(TIMER_URL);
            HttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                time = Long.parseLong(EntityUtils.toString(entity),10);
                logger.info("Got remote timestamp {}", time);
            }
        } catch (IOException e) {
            logger.warn("Error obtaining the synchronized remote timestamp, defaulting to the local one",e);
        }
        
        if (time == -1) {
            time = System.currentTimeMillis();
        }
        
        return Long.toString(time / 1000L);
    }
    
}
