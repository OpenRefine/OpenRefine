package com.google.refine.rdf.commands;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.IVocabularyManager.DescribedVocabulary;

public class ListVocabulariesCommand extends RdfCommand{
    public ListVocabulariesCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		
		String projectId = request.getParameter("project");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        try{
        	//synchronize with the index i.e. make sure that there are no left vocabularies in the index that are no longer in the schema
            //cases when the index goes out of order are very rare. an example might result from apply "Save Rdf Schema" in the history tab
            //to a project that already have Rdf Schema created
            //FIXME  this is a very defensive strategy... we pay the cost of checking for synchronization per call
            //going out-synchronized is very rare. might reconsider if performance issues are noticed or reported... 
            RdfSchema schema = getRdfSchema(request);
            getRdfContext().getVocabularySearcher().synchronizeIndex(schema.getPrefixesMap(),projectId);
            
            List<DescribedVocabulary> allVocabs = getRdfContext().getVocabularyManager().getVocabulariesOfProject(this.getRdfContext().getVocabularySearcher(), schema, projectId);
            Collections.sort(allVocabs, new Comparator<DescribedVocabulary>() {

    			@Override
    			public int compare(DescribedVocabulary o1,
    					DescribedVocabulary o2) {
    				return o1.getVocabulary().getName().compareTo(o2.getVocabulary().getName());
    			}
    		});
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("vocabularies");
            writer.array();
            for(DescribedVocabulary v:allVocabs){
                v.write(writer);
            }
            writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
