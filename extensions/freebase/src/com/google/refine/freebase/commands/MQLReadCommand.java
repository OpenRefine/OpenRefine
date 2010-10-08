package com.google.refine.freebase.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.freebase.util.FreebaseUtils;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;

public class MQLReadCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Provider provider = OAuthUtilities.getProvider(request);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            String query = request.getParameter("query");
            String result = FreebaseUtils.mqlread(provider,query);
            response.getWriter().write(result);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
