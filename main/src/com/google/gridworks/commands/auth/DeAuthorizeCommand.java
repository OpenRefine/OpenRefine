package com.google.gridworks.commands.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.commands.Command;
import com.google.gridworks.oauth.Credentials;
import com.google.gridworks.oauth.OAuthUtilities;
import com.google.gridworks.oauth.Provider;

public class DeAuthorizeCommand extends Command {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Provider provider = OAuthUtilities.getProvider(request);
            
            Credentials.deleteCredentials(request, response, provider, Credentials.Type.ACCESS);
            
            respond(response, "200 OK", "");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
