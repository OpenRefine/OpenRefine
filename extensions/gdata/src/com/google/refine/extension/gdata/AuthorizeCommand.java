package com.google.refine.extension.gdata;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;
import com.google.refine.commands.Command;

/**
 * Command to allow user to authenticate themselves.
 * 
 */
public class AuthorizeCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestUrl = AuthSubUtil.getRequestUrl(request.getRequestURL()
                + "2", // execution continues at authorize2 on redirect
                // Scope must be http, not https
                "http://spreadsheets.google.com/feeds", false, true);
        response.sendRedirect(requestUrl);
    }

}
