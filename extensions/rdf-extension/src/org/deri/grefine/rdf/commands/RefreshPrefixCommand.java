package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.PrefixExistException;
import org.deri.grefine.rdf.vocab.VocabularyImporter;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public class RefreshPrefixCommand extends RdfCommand{

	public RefreshPrefixCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String name = request.getParameter("name");
		String uri = request.getParameter("uri");
		String projectId = request.getParameter("project");
		getRdfSchema(request).removePrefix(name);
		
		getRdfContext().getVocabularySearcher().deleteTermsOfVocab(name, projectId);
		try{
			getRdfContext().getVocabularySearcher().importAndIndexVocabulary(name, uri, uri, projectId,new VocabularyImporter());
        } catch (PrefixExistException e) {
            respondException(response, e);
            return;
        } catch (Exception e){
        	response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
        	respond(response,"{\"code\":\"ok\"}");
        	return;
        }
			
		try{
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
		} 
	}
}