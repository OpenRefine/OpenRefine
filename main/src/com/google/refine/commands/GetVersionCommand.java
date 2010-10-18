package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.RefineServlet;

public class GetVersionCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JSONObject o = new JSONObject();
            o.put("version", RefineServlet.VERSION);
            o.put("revision", RefineServlet.REVISION);
            o.put("full_version", RefineServlet.FULL_VERSION);
            o.put("full_name", RefineServlet.FULLNAME);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            respond(response, o.toString());
        } catch (JSONException e) {
            e.printStackTrace(response.getWriter());
        }
    }
}
