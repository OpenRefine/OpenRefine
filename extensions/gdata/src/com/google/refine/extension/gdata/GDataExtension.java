/*
 * Copyright (c) 2010, Thomas F. Morris
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.spreadsheet.SpreadsheetService;

import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
abstract public class GDataExtension {
    static final String SERVICE_APP_NAME = "OpenRefine-GData-Extension";
    static final String CLIENT_ID = "647865400439.apps.googleusercontent.com";
    static final String CLIENT_SECRET = "0mW9OJji1yrgJk5AjJc5Pn6I"; // not really that secret, but the protocol accounts for that
    
    /** Global instance of the HTTP transport. */
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    static public String getAuthorizationUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        String authorizedUrl = makeRedirectUrl(module, request);
              
        // New Oauth2
        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
                CLIENT_ID, 
                authorizedUrl, // execution continues at authorized on redirect
                Arrays.asList("https://www.googleapis.com/auth/fusiontables", 
                        "https://www.googleapis.com/auth/drive", // create new spreadsheets
                        "https://spreadsheets.google.com/feeds"));
        
        return url.toString();

    }

    private static String makeRedirectUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        StringBuffer sb = new StringBuffer(module.getMountPoint().getMountPoint());
        sb.append("authorized?winname=");
        sb.append(ParsingUtilities.encode(request.getParameter("winname")));
        sb.append("&cb=");
        sb.append(ParsingUtilities.encode(request.getParameter("cb")));
    
        URL thisUrl = new URL(request.getRequestURL().toString());
        URL authorizedUrl = new URL(thisUrl, sb.toString());
        return authorizedUrl.toExternalForm();
    }

    static public String getTokenFromCode(ButterflyModule module, HttpServletRequest request) 
            throws MalformedURLException {
        String redirectUrl = makeRedirectUrl(module, request);
        StringBuffer fullUrlBuf = request.getRequestURL();
        if (request.getQueryString() != null) {
          fullUrlBuf.append('?').append(request.getQueryString());
        }
        AuthorizationCodeResponseUrl authResponse =
            new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
        // check for user-denied error
        if (authResponse.getError() != null) {
          // authorization denied...
        } else {
          // request access token using authResponse.getCode()...
            String code = authResponse.getCode();
            try {
                GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(HTTP_TRANSPORT,
                        JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, code, redirectUrl).execute();
                String token = response.getAccessToken();
                return token;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
      }


    static public DocsService getDocsService(String token) {
        DocsService service = new DocsService(SERVICE_APP_NAME);
        if (token != null) {
            service.setAuthSubToken(token);
        }
        return service;
    }
    
    static public SpreadsheetService getSpreadsheetService(String token) {
        SpreadsheetService service = new SpreadsheetService(SERVICE_APP_NAME);
        if (token != null) {
            service.setAuthSubToken(token);
        }
        return service;
    }
    
    static public Drive getDriveService(String token) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(token);
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(SERVICE_APP_NAME).build();
    }
    
    static boolean isSpreadsheetURL(String url) {
        // e.g. http://spreadsheets.google.com/ccc?key=tI36b9Fxk1lFBS83iR_3XQA&hl=en
        // TODO: The following should work, but the GData implementation is too limited
//        try {
//            FeedURLFactory.getSpreadsheetKeyFromUrl(url);
//            return true;
//        } catch (IllegalArgumentException e) {
//            return false;
//        }
        try {
            return url.contains("spreadsheet") && getSpreadsheetID(new URL(url)) != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    static String getSpreadsheetID(URL url) {
        return getParamValue(url,"key");
    }
    
    static private String getParamValue(URL url, String key) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith(key+"=")) {
                    int offset = key.length()+1;
                    String tableId = part.substring(offset);
                    return tableId;
                }
            }
        }
        return null; 
    }

}
