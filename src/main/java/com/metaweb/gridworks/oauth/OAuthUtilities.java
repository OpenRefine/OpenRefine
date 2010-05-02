package com.metaweb.gridworks.oauth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.http.HttpParameters;

import com.metaweb.gridworks.util.FreebaseUtils;

public class OAuthUtilities {
    
    static final private Map<String,Provider> providers = new HashMap<String,Provider>();
    static final private Map<String,String[]> infos = new HashMap<String,String[]>();
   
    static private final String[] FREEBASE_OAUTH_INFO = { "#9202a8c04000641f80000000150979b7" , "8ded7babfad2f94f4c77e39bbd6c90f31939999b"};

    static {
        Provider freebase = new FreebaseProvider(FreebaseUtils.FREEBASE_HOST);
        providers.put(freebase.getHost(), freebase);
        
        infos.put(freebase.getHost(), FREEBASE_OAUTH_INFO);
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
        OAuthConsumer oauthConsumer = new CommonsHttpOAuthConsumer(consumer_info[0],consumer_info[1]);
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
