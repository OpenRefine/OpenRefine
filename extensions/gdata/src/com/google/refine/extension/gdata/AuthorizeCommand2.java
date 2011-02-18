package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;

/**
 * Second half of authentication sequence after Google has redirected back to us
 * 
 */
public class AuthorizeCommand2 extends Command {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String queryString = request.getQueryString();
            if (queryString == null) {
                HttpUtilities.respond(response, "401 Unauthorized", "Authentication failed.");
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

            HttpUtilities.respond(response, "200 OK", "");
            // } catch (AuthenticationException e) {
            // } catch (GeneralSecurityException e) {
        } catch (Exception e) { // AuthenticationException
                                // GeneralSecurityException
            e.printStackTrace();
            TokenCookie.deleteToken(request, response);
            HttpUtilities.respondException(response, e);
        }
    }

}
