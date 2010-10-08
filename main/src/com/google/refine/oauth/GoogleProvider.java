package com.google.refine.oauth;

public class GoogleProvider extends Provider {
    
    public String getRequestTokenServiceURL() {
        return "https://www.google.com/accounts/OAuthGetRequestToken";
    }

    public String getAccessTokenServiceURL() {
        return "https://www.google.com/accounts/OAuthGetAccessToken";
    }

    public String getUserAuthorizationURL() {
        return "https://www.google.com/accounts/OAuthAuthorizeToken";
    }
}
