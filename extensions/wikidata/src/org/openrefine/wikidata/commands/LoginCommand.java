package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrefine.wikidata.editing.ConnectionManager;

import com.google.refine.commands.Command;


public class LoginCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("wb-username");
        String password = request.getParameter("wb-password");
        ConnectionManager manager = ConnectionManager.getInstance();
        if (username != null && password != null) {
            manager.login(username, password);
        } else if ("true".equals(request.getParameter("logout"))) {
            manager.logout();
        }
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        StringWriter sb = new StringWriter(2048);
        JSONWriter writer = new JSONWriter(sb, 32);
        
        try {
            writer.object();
            writer.key("logged_in");
            writer.value(manager.isLoggedIn());
            writer.key("username");
            writer.value(manager.getUsername());
            writer.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        respond(response, sb.toString());
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
