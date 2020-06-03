package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.openrefine.wikidata.editing.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Command for redirecting the user to the authorization page.
 */
public class AuthorizeCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ConnectionManager manager = ConnectionManager.getInstance();
        try {
            if (manager.supportOAuth()) {
                String remember = request.getParameter("remember-credentials");
                manager.setRememberCredentials("on".equals(remember));
                String authorizationUrl = manager.getAuthorizationUrl();
                response.sendRedirect(authorizationUrl);
            } else {
                throw new IllegalStateException("You must configure Wikidata OAuth client id/secret in refine.ini " +
                        "in order to use OAuth to login");
            }
        } catch (Exception e) {
            // TODO: use respondWithErrorPage after fixing its relative path bug
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            Writer writer = response.getWriter();
            writer.write("<h1>" + e.getMessage() + "<h1>");
            writer.close();
        }
    }
}
