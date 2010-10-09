package com.google.refine.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;

public class SetDefaultVocabulariesCommand extends RdfCommand{

	public SetDefaultVocabulariesCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String projectId = request.getParameter("project");
		RdfSchema schema = getRdfSchema(request);
		try{
			getRdfContext().getVocabularyManager().setDefaultPrefixes(getRdfContext().getVocabularySearcher(),
					getRdfContext().getPredefinedVocabularyManager(),schema, projectId);
			respondJSON(response, new Jsonizable() {
				
				@Override
				public void write(JSONWriter writer, Properties options)
						throws JSONException {
					writer.object();
					writer.key("code"); writer.value("ok");
					writer.endObject();
				}
			});
		}catch(Exception e) {
            respondException(response, e);
        }
	}

	
}
