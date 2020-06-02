package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.openrefine.wikidata.editing.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class AuthorizeCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if(!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }
        respond(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        respond(request, response);
    }

    /**
     * Redirect the user to authorization page to grant OpenRefine the essential rights.
     */
    protected void respond(HttpServletRequest request, HttpServletResponse response) {
        try {
            // TODO: respond error if the client id/secret are missing
            String authorizationUrl = ConnectionManager.getInstance().getAuthorizationUrl();
            response.sendRedirect(authorizationUrl);
        } catch (InterruptedException | ExecutionException | IOException e) {
            // TODO: respond error
            e.printStackTrace();
        }
    }
}
