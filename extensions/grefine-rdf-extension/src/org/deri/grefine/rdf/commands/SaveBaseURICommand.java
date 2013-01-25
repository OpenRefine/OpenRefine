package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.Util;
import org.deri.grefine.rdf.app.ApplicationContext;

import com.google.refine.model.Project;

public class SaveBaseURICommand extends RdfCommand {

    public SaveBaseURICommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            String base = request.getParameter("baseURI");
            URI baseUri;
            try{
            	baseUri = Util.buildURI(base);
            }catch(RuntimeException re){
            	respondException(response, re);
            	return;
            }
            Util.getProjectSchema(getRdfContext(),project).setBaseUri(baseUri);
            
            project.getMetadata().updateModified();
            
            respond(response,"OK","Base URI saved");
            
        }catch(Exception e){
            respondException(response, e);
        }
    }
}
