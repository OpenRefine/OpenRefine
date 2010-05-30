package com.metaweb.gridworks.oauth;

public class FreebaseProvider extends Provider {

    public FreebaseProvider(String host) {
        super(host);
    }
    
    public String getRequestTokenServiceURL() {
        return "https://" + host + "/api/oauth/request_token";
    }

    public String getAccessTokenServiceURL() {
        return "https://" + host + "/api/oauth/access_token";
    }

    public String getUserAuthorizationURL() {
        return "https://" + host + "/signin/app";
    }
    
}
