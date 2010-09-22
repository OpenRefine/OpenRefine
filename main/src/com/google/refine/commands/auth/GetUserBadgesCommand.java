package com.google.refine.commands.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;
import com.google.refine.util.FreebaseUtils;

public class GetUserBadgesCommand extends Command {
    
    final static Logger logger = LoggerFactory.getLogger("check-authorization_command");
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        try {
            Provider provider = OAuthUtilities.getProvider(request);
            String user_id = request.getParameter("user_id");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            String user_badges = FreebaseUtils.getUserBadges(provider, user_id);
            response.getWriter().write(user_badges);
        } catch (Exception e) {
            logger.info("error",e);
            respondException(response, e);
        }
    }
    
}
