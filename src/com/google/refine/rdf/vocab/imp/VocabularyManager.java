package com.google.refine.rdf.vocab.imp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.vocab.IPredefinedVocabularyManager;
import com.google.refine.rdf.vocab.IVocabularyManager;
import com.google.refine.rdf.vocab.IVocabularySearcher;

public class VocabularyManager implements IVocabularyManager{

	@Override
	public List<DescribedVocabulary> getVocabulariesOfProject(IVocabularySearcher searcher, RdfSchema schema, String projectId) throws ParseException, IOException {
		List<DescribedVocabulary> allVocabs = new ArrayList<DescribedVocabulary>();
		List<Vocabulary> importedVocabs = searcher.getProjectVocabularies(projectId);
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
        return allVocabs;
	}

	@Override
	public void setDefaultPrefixes(IVocabularySearcher searcher,IPredefinedVocabularyManager predefinedVocabularyManager,
				RdfSchema schema, String projectId) throws CorruptIndexException, IOException, ParseException {
		Map<String,String> importedPrefixes = searcher.setDefaultVocabularies(projectId);
		//make sure that other vocabularies which might not be imported are also added
		Map<String, String> allPrefixes = schema.getPrefixesMap();
        for(Entry<String, String> entry:allPrefixes.entrySet()){
        		searcher.addDefaultPrefixIfNotExist(entry.getKey(),entry.getValue());
        }
        
        allPrefixes.putAll(importedPrefixes);
        
        predefinedVocabularyManager.setPredefinedPrefixesMap(allPrefixes);
	}

}
