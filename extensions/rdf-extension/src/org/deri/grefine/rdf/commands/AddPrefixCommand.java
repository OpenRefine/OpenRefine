package org.deri.grefine.rdf.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.PrefixExistException;
import org.deri.grefine.rdf.vocab.VocabularyImportException;
import org.deri.grefine.rdf.vocab.VocabularyImporter;
import org.json.JSONException;

public class AddPrefixCommand extends RdfCommand{

    public AddPrefixCommand(ApplicationContext ctxt) {
        super(ctxt);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name").trim();
        String uri = request.getParameter("uri").trim();
        String projectId = request.getParameter("project");
        String fetchOption = request.getParameter("fetch");
        Boolean forceImport = Boolean.valueOf(request.getParameter("force-import"));
                
        try {    
            
            if(fetchOption.equals("web")){
                String fetchUrl = request.getParameter("fetch-url");
                if(fetchUrl==null || fetchOption.trim().isEmpty()){
                    fetchUrl = uri;
                }
                getRdfContext().getVocabularySearcher().importAndIndexVocabulary(name, uri, fetchUrl, projectId,new VocabularyImporter());
            }
            
            getRdfSchema(request).addPrefix(name, uri);
            respond(response,"ok","Vocabulary loaded.");
            
            
        } catch (PrefixExistException e) {
            //in case something went wrong importing the prefix, remove it from manager            
            getRdfSchema(request).removePrefix(name);
            respondException(response, e);
            
        } catch (VocabularyImportException e) {
            //logger.warning("Vocabulary import exception: " + e.getLocalizedMessage());
            //TODO: if force import was checked, respond with OK, do not remove it from manager
           
            if(forceImport) {
                try {
                    respond(response,"ok", "Vocabulary added, but not imported.");
                } catch (JSONException e1) {
                    respondException(response,e1);
                }
                
            }
            else {
                getRdfSchema(request).removePrefix(name);
                respondException(response,e);
            }
        } 
        catch (Exception e1){
            logger.warn("General exception");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            respond(response,"{\"code\":\"ok\"}");
        }
    }
}
