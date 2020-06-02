package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.openrefine.wikidata.editing.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;

public class AuthorizedCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }
        respond(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        respond(request, response);
    }

    protected void respond(HttpServletRequest request, HttpServletResponse response) {
        String verifier = request.getParameter("oauth_verifier");
        // String oauthToken = request.getParameter("oauth_token");
        try {
            ConnectionManager.getInstance().login(verifier);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            Writer writer = response.getWriter();
            writer.write("<script>window.close()</script>");
            writer.flush();
            writer.close();

        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }
}
