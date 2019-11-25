package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import com.google.refine.model.Project;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snaccooperative.connection.SNACConnector;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.google.gson.Gson;
import com.google.refine.model.Project;

public class SNACResourceCommand extends Command {
    private String attribute;
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //HashMap<String, String> attribute = new HashMap<>();
        String temp = request.getParameter("dict");
        if (temp != null){
          attribute = temp;
        }
        String resource = request.getParameter("project");
        resource = resource.replaceAll("\"\\{\\}\"","\\{\\}");
        Gson bruh = new Gson();
        Project reee = bruh.fromJson(resource,Project.class);
        System.out.println(reee);
        System.out.println(temp);
        System.out.println(attribute);
        // String apikey = request.getParameter("snackey");
        // SNACConnector manager = SNACConnector.getInstance();
        // if (apikey != null) {
        //     manager.saveKey(apikey);
        // } else if ("true".equals(request.getParameter("logout"))) {
        //     manager.removeKey();
        // }
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        writer.writeStartObject();
        writer.writeStringField("resource", attribute);
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
