package com.google.refine.rdf.commands;

import java.io.IOException; 
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.rdf.app.ApplicationContext;

public class DeleteVocabularyCommand extends RdfCommand{

    public DeleteVocabularyCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String projectId = request.getParameter("project");
        
        try {
        	this.getRdfSchema(request).removePrefix(name);
        	
            this.getRdfContext().getVocabularySearcher().deleteVocabulary(name, projectId);
            respondJSON(response, new Jsonizable() {
                
                @Override
                public void write(JSONWriter writer, Properties options)
                        throws JSONException {
                    writer.object();
                    writer.key("code"); writer.value("ok");
                    writer.endObject();
                }
            });
        } catch (JSONException e) {
            respondException(response, e);
        } catch (Exception e){
            respondException(response, e);
        }
    }

}
