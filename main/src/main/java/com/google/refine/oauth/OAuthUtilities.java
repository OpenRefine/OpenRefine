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

package com.google.refine.oauth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.OAuthConsumer;

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
        if (provider == null) {
            throw new RuntimeException("Can't find OAuth provider '" + provider_str + "'");
        }
        return provider;
    }
    
    public static OAuthConsumer getConsumer(Provider provider) {
        if (provider == null) {
            throw new RuntimeException("Provider can't be null");
        }
        String[] consumer_info = infos.get(provider.getHost());
        if (consumer_info == null) {
            throw new RuntimeException("Can't find secrets for provider '" + provider.getHost() + "'");
        }
        OAuthConsumer oauthConsumer = provider.createConsumer(consumer_info[0],consumer_info[1]);
        oauthConsumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy(provider.getRealm()));
        return oauthConsumer;
    }

    public static OAuthConsumer getConsumer(Credentials credentials, Provider provider) {
        OAuthConsumer consumer = getConsumer(provider);
        if (credentials != null) {
            consumer.setTokenWithSecret(credentials.getToken(), credentials.getSecret());
        }
        return consumer;
    }

}
