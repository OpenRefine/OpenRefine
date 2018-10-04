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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.util.CookiesUtilities;

import oauth.signpost.OAuth;
import oauth.signpost.http.HttpParameters;

public class Credentials {

    private static final String TOKEN = "oauth_token";
    private static final String SECRET = "oauth_token_secret";
    
    public enum Type { 
        REQUEST("request"), 
        ACCESS("access");
        
        private final String postfix;
        
        Type(String postfix) {
            this.postfix = postfix;
        }
        
        public String getCookieName(Provider provider) {
            if (provider == null) {
                throw new RuntimeException("Provider can't be null");
            }
            return provider.getHost() + "_" + postfix;
        }
    };
    
    public static Credentials getCredentials(HttpServletRequest request, Provider provider, Type type) {
        Cookie cookie = CookiesUtilities.getCookie(request, type.getCookieName(provider));
        return (cookie == null) ? null : makeCredentials(cookie.getValue(), provider);
    }

    public static void setCredentials(HttpServletRequest request, HttpServletResponse response, Credentials credentials, Type type, int max_age) {
        String name = type.getCookieName(credentials.getProvider());
        String value = credentials.toString();
        CookiesUtilities.setCookie(request, response, name, value, max_age);
    }
    
    public static void deleteCredentials(HttpServletRequest request, HttpServletResponse response, Provider provider, Type type) {
        CookiesUtilities.deleteCookie(request, response, type.getCookieName(provider));
    }

    public static Credentials makeCredentials(String str, Provider provider) {
        HttpParameters p = OAuth.decodeForm(str);
        return new Credentials(p.getFirst(TOKEN), p.getFirst(SECRET), provider);
    }
    
    private Provider provider;
    private String token;
    private String secret;
    
    public Credentials(String token, String secret, Provider provider) {
        this.token = token;
        if (token == null) {
            throw new RuntimeException("Could not find " + TOKEN + " in auth credentials");
        }
        this.secret = secret;
        if (secret == null) {
            throw new RuntimeException("Could not find " + SECRET + " in auth credentials");
        }
        this.provider = provider;
        if (provider == null) {
            throw new RuntimeException("Provider can't be null");
        }
    }

    public String getToken() {
        return token;
    }

    public String getSecret() {
        return secret;
    }
    
    public Provider getProvider() {
        return provider;
    }
    
    @Override
    public String toString() {
        return TOKEN + "=" + OAuth.percentEncode(token) + "&" + SECRET + "=" + OAuth.percentEncode(secret);
    }
    
}
