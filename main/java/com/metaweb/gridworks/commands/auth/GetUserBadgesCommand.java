package com.metaweb.gridworks.commands.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.oauth.OAuthUtilities;
import com.metaweb.gridworks.oauth.Provider;
import com.metaweb.gridworks.util.FreebaseUtils;

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
