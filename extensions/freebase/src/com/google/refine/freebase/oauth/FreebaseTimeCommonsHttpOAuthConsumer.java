/*

Copyright 2010, Google Inc.
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

package com.google.refine.freebase.oauth;

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
    
    private static final String TIMER_URL = "http://refinery.freebaseapps.com/time";
    
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
    @Override
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
                logger.debug("Got remote timestamp {}", time);
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
