package com.metaweb.gridworks.oauth;


public abstract class Provider {

    protected String host;
    
    public Provider(String host) {
        this.host = host;
    }
    
    public String getHost() {
        return host;
    }
    
    abstract public String getRequestTokenServiceURL();
    abstract public String getAccessTokenServiceURL();
    abstract public String getUserAuthorizationURL();
}
