package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.openrefine.wikidata.editing.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command for checking if OAuth is supported,
 * i.e., whether the OAuth client id and client secret have been configured or not.
 */
public class SupportOAuthCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("support_oauth", ConnectionManager.getInstance().supportOAuth());
        respondJSON(response, jsonResponse);
    }
}
