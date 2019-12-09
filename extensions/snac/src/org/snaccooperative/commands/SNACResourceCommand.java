package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Cell;
import com.google.refine.ProjectManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import org.snaccooperative.exporters.SNACResourceCreator;

public class SNACResourceCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
              // HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
              // HttpPost post_t = new HttpPost("https://snac-dev.iath.virginia.edu/api/");
              // String query = "{\"command\": \"vocabulary\",\"query_string\": \"eng\",\"type\": \"language_code\",\"entity_type\": null}";
              // System.out.println(query);
              // post_t.addHeader("content-type", "application/x-www-form-urlencoded");
              // post_t.setEntity(new StringEntity(query,"UTF-8"));
              // HttpResponse response_t = httpClient.execute(post_t);
              // String result = EntityUtils.toString(response_t.getEntity());
              // System.out.println(result);
        String dict = request.getParameter("dict");
        SNACResourceCreator manager = SNACResourceCreator.getInstance();
        if (dict != null){
            Project p = getProject(request);
            manager.setUp(p, dict);
        }

        // Project p = getProject(request);
        // SNACResourceCreator.setProject(p);
        // List<Row> rows = p.rows;

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        writer.writeStartObject();
        writer.writeStringField("resource", manager.getColumnMatchesJSONString());
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // doPost(request, response);
        SNACResourceCreator manager = SNACResourceCreator.getInstance();

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        writer.writeStartObject();
        writer.writeStringField("resource", manager.exportResourcesJSON());
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }
}
