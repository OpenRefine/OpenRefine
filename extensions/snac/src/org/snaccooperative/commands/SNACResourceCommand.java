package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snaccooperative.connection.SNACConnector;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class SNACResourceCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String random = request.getParameter("data");

        // Create API request call
        // Store the returning value (id) based on the insertResource command
        // System.out.println("ID Found to be: " + id); // Can be seen on the terminal 

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://api.snaccooperative.org");
        // post.setEntity(new StringEntity("{\"command\": \"search\",\"term\": \"Mozart\",\"count\": 10,\"start\": 0,\"entity_type\": \"person\"}" , "UTF-8"));
        // post.setEntity(new StringEntity("{\n    \"command\": \"insert_resource\",\n    \"resource\": {\n        \"dataType\": \"Resource\",\n        \"title\": \"My Title\",\n        \"link\": \"https:\\/\\/mylink.com\",\n        \"abstract\": \"This is a full abstract\",\n        \"source\": \"My full citation\",\n        \"documentType\": {\n            \"id\": \"696\",\n            \"term\": \"ArchivalResource\",\n            \"type\": \"document_type\"\n        },\n        \"extent\": \"20 pages\",\n        \"repository\": {\n            \"ark\": \"http:\\/\\/n2t.net\\/ark:\\/99166\\/w6kq8qkp\",\n            \"dataType\": \"Constellation\",\n            \"id\": 76763300\n        },\n        \"operation\": \"insert\"\n    },\n    \"apikey\": \"NmZjMTY3Yjc4ZjgxZGRmMzM5YTI0YzZhMDVhMGJhNjE3MTU2ZTg5Mw\"\n}" , "UTF-8"));
        post.setEntity(new StringEntity("{\n    \"command\": \"insert_resource\",\n    \"resource\": {\n        \"dataType\": \"Resource\",\n        \"title\": \"My Title\",\n        \"link\": \"https:\\/\\/mylink.com\",\n        \"abstract\": \"This is a full abstract\",\n        \"source\": \"My full citation\",\n        \"documentType\": {\n            \"id\": \"696\",\n            \"term\": \"ArchivalResource\",\n            \"type\": \"document_type\"\n        },\n        \"extent\": \"20 pages\",\n        \"repository\": {\n            \"ark\": \"http:\\/\\/n2t.net\\/ark:\\/99166\\/w6kq8qkp\",\n            \"dataType\": \"Constellation\",\n            \"id\": 76763300\n        },\n        \"operation\": \"insert\"\n    },\n    \"apikey\": \"...\"\n}", "UTF-8"));
        HttpResponse response2 = client.execute(post);
        String id = EntityUtils.toString(response2.getEntity());
        System.out.println("ID FOUND TO BE: " + id);

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
        writer.writeStringField("resource", "Hmm test");
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
