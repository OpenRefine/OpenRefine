package com.google.refine.freebase.oauth;

import oauth.signpost.OAuthConsumer;

import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;
import com.google.refine.freebase.util.FreebaseUtils;

public class FreebaseProvider extends Provider {
    static private final String[] FREEBASE_OAUTH_INFO = { "#9202a8c04000641f80000000185352db" , "4561ee02279e6f04ebd88a1557e4292489380adf"};

    static public void register() {
        OAuthUtilities.registerOAuthProvider(new FreebaseProvider(FreebaseUtils.FREEBASE_HOST), FREEBASE_OAUTH_INFO);
    }

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
    
    
    @Override
    public OAuthConsumer createConsumer(String consumerKey, String consumerSecret) {
        return new FreebaseTimeCommonsHttpOAuthConsumer(consumerKey, consumerSecret);
    }
}
