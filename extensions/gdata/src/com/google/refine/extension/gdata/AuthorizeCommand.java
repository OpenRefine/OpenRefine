package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.URL;

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

        char[] mountPointChars = this.servlet.getModule("gdata")
            .getMountPoint().getMountPoint().toCharArray();
        
        StringBuffer sb = new StringBuffer();
        sb.append(mountPointChars, 0, mountPointChars.length);
        sb.append("authorized");

        URL thisUrl = new URL(request.getRequestURL().toString());
        URL authorizedUrl = new URL(thisUrl, sb.toString());
        
        String requestUrl = AuthSubUtil.getRequestUrl(
            authorizedUrl.toExternalForm(), // execution continues at authorized on redirect
            "https://docs.google.com/feeds https://spreadsheets.google.com/feeds",
            false,
            true);
        response.sendRedirect(requestUrl);
    }

}
