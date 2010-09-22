package com.google.refine.commands.freebase;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.oauth.Credentials;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;
import com.google.refine.util.FreebaseUtils;

public class MQLWriteCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Provider provider = OAuthUtilities.getProvider(request);
            
            Credentials access_credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            if (access_credentials != null) {
                String query = request.getParameter("query");
                String result = FreebaseUtils.mqlwrite(access_credentials, provider, query);
                response.getWriter().write(result);
            } else {    
                respond(response, "401 Unauthorized", "You don't have the right credentials");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
