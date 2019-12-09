package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Cell;
import com.google.refine.ProjectManager;

import org.snaccooperative.exporters.SNACResourceCreator;
import org.snaccooperative.connection.SNACConnector;

public class SNACUploadCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String API_key = request.getParameter("apikey");
        SNACResourceCreator manager = SNACResourceCreator.getInstance();
        SNACConnector key_manager = SNACConnector.getInstance();
        API_key = key_manager.getKey();
        System.out.println("Key: "+ API_key);
        manager.uploadResources(API_key);


        // Project p = getProject(request);
        // SNACResourceCreator.setProject(p);
        // List<Row> rows = p.rows;

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        writer.writeStartObject();
        writer.writeStringField("done", manager.getColumnMatchesJSONString());
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
