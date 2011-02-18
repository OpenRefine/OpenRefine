package com.google.refine.extension.gdata;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;

public class DeAuthorizeCommand extends Command {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String sessionToken = TokenCookie.getToken(request);
            if (sessionToken != null) {
                AuthSubUtil.revokeToken(sessionToken, null);
                TokenCookie.deleteToken(request, response);
            }
            HttpUtilities.respond(response, "200 OK", "");
        } catch (Exception e) {
            HttpUtilities.respondException(response, e);
        }
    }
}
