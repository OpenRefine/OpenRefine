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
import com.google.refine.rdf.vocab.PrefixExistException;

public class AddPrefixCommand extends RdfCommand{

    public AddPrefixCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name").trim();
        String uri = request.getParameter("uri").trim();
        String projectId = request.getParameter("project");
        try {    
        	getRdfSchema(request).addPrefix(name, uri);
        	getRdfContext().getVocabularySearcher().importAndIndexVocabulary(name, uri, projectId);
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
        } catch (PrefixExistException e) {
            respondException(response, e);
        } catch (Exception e){
        	response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
        	respond(response,"{\"code\":\"ok\"}");
        }
    }

    
}
