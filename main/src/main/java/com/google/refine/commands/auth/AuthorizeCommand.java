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

package com.google.refine.commands.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.oauth.Credentials;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

public class AuthorizeCommand extends Command {
    
    private static final String OAUTH_VERIFIER_PARAM = "oauth_verifier";
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // get the provider from the request
        Provider provider = OAuthUtilities.getProvider(request);

        try {
            
            // see if the request comes with access credentials
            Credentials access_credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
                                    
            // prepare the continuation URL that the OAuth provider will redirect the user to
            // (we need to make sure this URL points back to this code or the dance will never complete)
            String callbackURL = getBaseURL(request,provider);
            
            if (access_credentials == null) {
                // access credentials are not available so we need to check 
                // to see at what stage of the OAuth dance we are
                
                // get the request token credentials
                Credentials request_credentials = Credentials.getCredentials(request, provider, Credentials.Type.REQUEST);

                OAuthConsumer consumer = OAuthUtilities.getConsumer(request_credentials, provider);
                OAuthProvider pp = provider.getProvider();
                
                if (request_credentials == null) {
                    // no credentials were found, so let's start the dance

                    // get the request token

                    String url = pp.retrieveRequestToken(consumer, callbackURL);
                    
                    request_credentials = new Credentials(consumer.getToken(), consumer.getTokenSecret(), provider);

                    // and set them to that we can retrieve them later in the second part of the dance
                    Credentials.setCredentials(request, response, request_credentials, Credentials.Type.REQUEST, 3600);
                    
                    // now redirect the user to the Authorize URL where she can authenticate against the
                    // service provider and authorize us. 
                    // The provider will bounce the user back here for us to continue the dance.
                    
                    response.sendRedirect(url);
                } else {
                    // we are at the second stage of the dance, so we need need to obtain the access credentials now
                    
                    // if we got here, it means that the user performed a valid authentication against the
                    // service provider and authorized us, so now we can request more permanent credentials
                    // to the service provider and save those as well for later use.

                    // this is set only for OAuth 1.0a  
                    String verificationCode = request.getParameter(OAUTH_VERIFIER_PARAM);
                    
                    pp.retrieveAccessToken(consumer, verificationCode);

                    access_credentials = new Credentials(consumer.getToken(), consumer.getTokenSecret(), provider);

                    // no matter the result, we need to remove the request token
                    Credentials.deleteCredentials(request, response, provider, Credentials.Type.REQUEST);
                    
                    Credentials.setCredentials(request, response, access_credentials, Credentials.Type.ACCESS, 30 * 24 * 3600);

                    finish(response);
                }
            } else {
                finish(response);
            }
        } catch (Exception e) {
            Credentials.deleteCredentials(request, response, provider, Credentials.Type.REQUEST);
            Credentials.deleteCredentials(request, response, provider, Credentials.Type.ACCESS);
            respondException(response, e);
        }
    }
    
    private void finish(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "text/html");

        PrintWriter writer = response.getWriter();
        writer.write(
            "<html>" + 
                "<body></body>" + 
                "<script type='text/javascript'>" +
                    "if (top.opener && top.opener.onauthorization) {" +
                    "   top.opener.onauthorization(window);" +
                    "}" +
                    "self.close();" +
                "</script>" +
            "</html>"
        );
        writer.flush();
    }
    
    private String getBaseURL(HttpServletRequest request, Provider provider) {
        String host = request.getHeader("host");
        if (host == null) {
            String referrer = request.getHeader("referer");
            if (referrer != null) {
                URI url;
                try {
                    url = new URI(referrer);
                    int port = url.getPort();
                    host = url.getHost() + ((port > -1) ? ":" + url.getPort() : "");
                } catch (URISyntaxException e) {
                    throw new RuntimeException("referrer '" + referrer + "' can't be parsed as a URL");
                }
            } else {
                throw new RuntimeException("neither the 'host' nor 'referer' headers were present in the HTTP response, I can't determine what URL OpenRefine is listening to.");
            }
        }
        return "http://" + host + "/command/core/authorize/" + provider.getHost();
    }
}
