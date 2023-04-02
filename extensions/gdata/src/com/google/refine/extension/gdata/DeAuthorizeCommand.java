/*
 * Copyright (c) 2011, Thomas F. Morris
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.refine.commands.Command;
import com.google.api.client.http.ByteArrayContent;

public class DeAuthorizeCommand extends Command {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String sessionToken = TokenCookie.getToken(request);
            if (sessionToken != null) {

                // No method to do this in Google's client lib, so roll our own
                HttpRequestFactory factory = HTTP_TRANSPORT.createRequestFactory();
                GenericUrl url = new GenericUrl("https://accounts.google.com/o/oauth2/revoke?token=" + sessionToken);
                HttpRequest rqst = factory.buildGetRequest(url);
                HttpResponse resp = rqst.execute();
                if (resp.getStatusCode() != 200) {
                    respond(response, String.valueOf(resp.getStatusCode()), resp.getStatusMessage());
                }

                TokenCookie.deleteToken(request, response);
            }
            respond(response, "200 OK", "");
        } catch (Exception e) {
            respondException(response, e);
        }
        try {
            // Make the API request to update the Wikidata item
            String apiUrl = "https://www.wikidata.org/w/api.php?action=wbcreateclaim&format=json";
            String claim = "{ \"mainsnak\": { \"snaktype\": \"value\", \"property\": \"P31\", \"datavalue\": { \"value\": { \"entity-type\": \"item\", \"id\": \"Q5\" }, \"type\": \"wikibase-entityid\" } }, \"type\": \"statement\", \"rank\": \"normal\" }";
            String csrfToken = "1234567890abcdef"; // Replace with actual CSRF token

            HttpRequestFactory factory = HTTP_TRANSPORT.createRequestFactory();
            GenericUrl url = new GenericUrl(apiUrl);

            HttpRequest rqst = factory.buildPostRequest(url, ByteArrayContent.fromString("application/json", claim));

            // HttpRequest rqst = factory.buildPostRequest(url,
            // ByteArrayClaim.fromString("application/json",claim);
            rqst.getHeaders().setCookie("wikidatawikiSession=" + csrfToken);
            HttpResponse resp = rqst.execute();

            // Check if the API response contains an error
            if (resp.getStatusCode() != 200) {
                // Log the API request and response for debugging purposes
                System.out.println("Wikidata API request: " + rqst.toString());
                System.out.println("Wikidata API response: " + resp.parseAsString());
                throw new RuntimeException(
                        "Wikidata API error: " + resp.getStatusCode() + " - " + resp.getStatusMessage());
            }

            // Handle the successful response
            // ...
        } catch (Exception e) {
            // Log the error for debugging purposes
            e.printStackTrace();

            // Rethrow the error with a more detailed error message
            throw new RuntimeException("Failed to update Wikidata item: " + e.getMessage());
        }
    }
}
