package com.google.refine.oauth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.http.HttpParameters;

public class OAuthUtilities {
    
    static final private Map<String,Provider> providers = new HashMap<String,Provider>();
    static final private Map<String,String[]> infos = new HashMap<String,String[]>();
    
    static final public void registerOAuthProvider(Provider provider, String[] oauthInfo) {
        providers.put(provider.getHost(), provider);
        infos.put(provider.getHost(), oauthInfo);
    }
    
    public static Provider getProvider(String name) {
        return (name == null) ? null : providers.get(name);
    }

    public static Provider getProvider(HttpServletRequest request) {
        String path = request.getPathInfo().substring(1);
        int slash = path.lastIndexOf('/');
        String provider_str = path.substring(slash + 1);
        Provider provider = getProvider(provider_str);
        if (provider == null) throw new RuntimeException("Can't find OAuth provider '" + provider_str + "'");
        return provider;
    }
    
    public static OAuthConsumer getConsumer(Provider provider) {
        if (provider == null) throw new RuntimeException("Provider can't be null");
        String[] consumer_info = infos.get(provider.getHost());
        if (consumer_info == null) throw new RuntimeException("Can't find secrets for provider '" + provider.getHost() + "'");
        OAuthConsumer oauthConsumer = provider.createConsumer(consumer_info[0],consumer_info[1]);
        HttpParameters params = new HttpParameters();
        params.put("realm", provider.getHost());
        oauthConsumer.setAdditionalParameters(params);
        return oauthConsumer;
    }

    public static OAuthConsumer getConsumer(Credentials credentials, Provider provider) {
        OAuthConsumer consumer = getConsumer(provider);
        if (credentials != null) {
            consumer.setTokenWithSecret(credentials.getToken(), credentials.getSecret());
        }
        return consumer;
    }
    
    public static OAuthProvider getOAuthProvider(Provider p) {
        return new CommonsHttpOAuthProvider(p.getRequestTokenServiceURL(), p.getAccessTokenServiceURL(), p.getUserAuthorizationURL());        
    }

}
