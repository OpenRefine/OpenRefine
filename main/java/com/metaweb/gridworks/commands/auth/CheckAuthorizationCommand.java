package com.metaweb.gridworks.commands.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.oauth.Credentials;
import com.metaweb.gridworks.oauth.OAuthUtilities;
import com.metaweb.gridworks.oauth.Provider;
import com.metaweb.gridworks.util.FreebaseUtils;

public class CheckAuthorizationCommand extends Command {
    
    final static Logger logger = LoggerFactory.getLogger("check-authorization_command");
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        try {
            Provider provider = OAuthUtilities.getProvider(request);
                        
            // this cookie should not be there, but this is good hygiene practice
            Credentials.deleteCredentials(request, response, provider, Credentials.Type.REQUEST);
            
            Credentials access_credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            if (access_credentials != null) {
                String user_info = FreebaseUtils.getUserInfo(access_credentials, provider);
                response.getWriter().write(user_info);
            } else {    
                respond(response, "401 Unauthorized", "You don't have the right credentials");
            }
        } catch (Exception e) {
            logger.info("error",e);
            respondException(response, e);
        }
    }
    
}
