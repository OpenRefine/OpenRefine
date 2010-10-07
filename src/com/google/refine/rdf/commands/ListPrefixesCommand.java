package com.google.refine.rdf.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;

public class ListPrefixesCommand extends RdfCommand{

	public ListPrefixesCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        try{
            JSONWriter writer = new JSONWriter(response.getWriter());
            RdfSchema schema = getRdfSchema(request);
            
            writer.object();
            writer.key("prefixes");
            Util.writePrefixes(schema.getPrefixesMap(),writer);
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
	}

	
}
