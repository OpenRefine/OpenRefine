package com.metaweb.gridworks.oauth;

import java.util.HashMap;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

public class OAuthUtilities {
    
    static final private Map<String,Provider> providers = new HashMap<String,Provider>();
    static final private Map<String,String[]> infos = new HashMap<String,String[]>();
   
    static private final String[] FREEBASE_OAUTH_INFO = { "#9202a8c04000641f80000000150979b7" , "8ded7babfad2f94f4c77e39bbd6c90f31939999b"};
    static private final String[] SANDBOX_OAUTH_INFO  = { "#9202a8c04000641f800000001505964a" , "a816b13991bf1d191ad1c371f3c230feca6a11e2"};

    static {
        providers.put("freebase", new FreebaseProvider("www.freebase.com"));
        providers.put("sandbox", new FreebaseProvider("www.sandbox-freebase.com"));
        
        infos.put("www.freebase.com", FREEBASE_OAUTH_INFO);
        infos.put("www.sandbox-freebase.com", SANDBOX_OAUTH_INFO);
    }
    
    public static Provider getProvider(String name) {
        return (name == null) ? null : providers.get(name);
    }

    public static OAuthConsumer getConsumer(Provider provider) {
        if (provider == null) throw new RuntimeException("Provider can't be null");
        String[] consumer_info = infos.get(provider.getHost());
        if (consumer_info == null) throw new RuntimeException("Can't find secrets for provider '" + provider.getHost() + "'");
        return new CommonsHttpOAuthConsumer(consumer_info[0],consumer_info[1]);
    }

    public static OAuthConsumer getConsumer(Credentials credentials, Provider provider) {
        OAuthConsumer consumer = getConsumer(provider);
        consumer.setTokenWithSecret(credentials.getToken(), credentials.getSecret());
        return consumer;
    }
    
    public static OAuthProvider getOAuthProvider(Provider p) {
        return new CommonsHttpOAuthProvider(p.getRequestTokenServiceURL(), p.getAccessTokenServiceURL(), p.getUserAuthorizationURL());        
    }

}
