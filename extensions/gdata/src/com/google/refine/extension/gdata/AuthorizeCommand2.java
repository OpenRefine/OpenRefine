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
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;

import com.google.refine.commands.Command;

/**
 * Second half of authentication sequence after Google has redirected back to us
 * 
 */
public class AuthorizeCommand2 extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String queryString = request.getQueryString();
            if (queryString == null) {
                respond(response, "401 Unauthorized", "Authentication failed.");
                return;
            }
            
            String onetimeUseToken = AuthSubUtil.getTokenFromReply(URLDecoder.decode(queryString,"UTF-8"));
            // FIXME(SM): can we safely assume UTF-8 encoding here?

            String sessionToken = AuthSubUtil.exchangeForSessionToken(onetimeUseToken, null);
            TokenCookie.setToken(request, response, sessionToken);

//            FeedURLFactory factory = FeedURLFactory.getDefault();
//            SpreadsheetService service = new SpreadsheetService(
//                    "Google-Refine-GData-Extension-0.2");
//            service.setAuthSubToken(sessionToken);
//
//            SpreadsheetFeed feed = service.getFeed(
//                    factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
//            List<SpreadsheetEntry> spreadsheets = feed.getEntries();
//            for (int i = 0; i < spreadsheets.size(); i++) {
//                SpreadsheetEntry entry = spreadsheets.get(i);
//                System.out.println("\t" + entry.getTitle().getPlainText());
//            }

            respond(response, "200 OK", "");
            // } catch (AuthenticationException e) {
            // } catch (GeneralSecurityException e) {
        } catch (Exception e) { // AuthenticationException
                                // GeneralSecurityException
            e.printStackTrace();
            TokenCookie.deleteToken(request, response);
            respondException(response, e);
        }
    }

}
