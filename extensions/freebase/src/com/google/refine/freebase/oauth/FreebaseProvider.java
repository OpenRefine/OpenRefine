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

import oauth.signpost.OAuthConsumer;

import com.google.refine.freebase.util.FreebaseUtils;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;

public class FreebaseProvider extends Provider {
    static private final String[] FREEBASE_OAUTH_INFO = { "#9202a8c04000641f80000000185352db" , "4561ee02279e6f04ebd88a1557e4292489380adf"};

    static public void register() {
        OAuthUtilities.registerOAuthProvider(new FreebaseProvider(FreebaseUtils.FREEBASE_HOST), FREEBASE_OAUTH_INFO);
    }

    public FreebaseProvider(String host) {
        super(host);
    }
    
    @Override
    public String getRequestTokenServiceURL() {
        return "http://api." + host + "/api/oauth/request_token";
    }

    @Override
    public String getAccessTokenServiceURL() {
        return "http://api." + host + "/api/oauth/access_token";
    }

    @Override
    public String getUserAuthorizationURL() {
        return "http://www." + host + "/signin/app";
    }
    
    @Override
    public String getRealm() {
        return "http://api" + host + "/";
    }
    
    @Override
    public OAuthConsumer createConsumer(String consumerKey, String consumerSecret) {
        return new FreebaseTimeCommonsHttpOAuthConsumer(consumerKey, consumerSecret);
    }
}
