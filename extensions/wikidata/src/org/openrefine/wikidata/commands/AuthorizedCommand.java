package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.openrefine.wikidata.editing.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Command for OAuth callback.
 */
public class AuthorizedCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String verifier = request.getParameter("oauth_verifier");
        ConnectionManager.getInstance().login(verifier);
        // Simply close the current page here.
        // The frontend is able to tell whether the current page is closed or not,
        // when it's closed, a callback will be triggered.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        Writer writer = response.getWriter();
        writer.write("<script>window.close()</script>");
        writer.close();
    }
}
