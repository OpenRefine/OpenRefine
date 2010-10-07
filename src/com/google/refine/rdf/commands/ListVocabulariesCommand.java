package com.google.refine.rdf.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.imp.Vocabulary;

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
            //FIXME will this is a very defnsive strategy... we pay the cost of checking for synchronization per call
            //going out-synchronized is very rare
            RdfSchema schema = getRdfSchema(request);
            getRdfContext().getVocabularySearcher().synchronizeIndex(schema.getPrefixesMap(),projectId);
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("vocabularies");
            writer.array();
            List<Vocabulary> importedVocabs = this.getRdfContext().getVocabularySearcher().getProjectVocabularies(projectId);
            List<DescribedVocabulary> allVocabs = new ArrayList<DescribedVocabulary>();
            
            Set<String> namesOfimportedVocabularies = new HashSet<String>();
            for(Vocabulary v:importedVocabs){
            	namesOfimportedVocabularies.add(v.getName());
            	allVocabs.add(new DescribedVocabulary(v,true));
            }
            	
            Map<String, String> allPrefixes = schema.getPrefixesMap();
            for(Entry<String, String> entry:allPrefixes.entrySet()){
            	if(!namesOfimportedVocabularies.contains(entry.getKey())){
            		//add unimported vocabulary to the result
            		allVocabs.add(new DescribedVocabulary(new Vocabulary(entry.getKey(), entry.getValue()), false));
            	}
            }
            
            Collections.sort(allVocabs, new Comparator<DescribedVocabulary>() {

				@Override
				public int compare(DescribedVocabulary o1,
						DescribedVocabulary o2) {
					return o1.vocabulary.getName().compareTo(o2.vocabulary.getName());
				}
			});
            
            for(DescribedVocabulary v:allVocabs){
                v.write(writer);
            }
            writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
	
	private static final class DescribedVocabulary {
		Vocabulary vocabulary;
		boolean imported;
		public DescribedVocabulary(Vocabulary v,boolean imported){
			this.vocabulary = v;
			this.imported = imported;
		}
		
		public void write(JSONWriter writer) throws JSONException{
			this.vocabulary.write(writer, this.imported);
		}
	}
}
