package com.google.refine.rdf.vocab;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.vocab.imp.Vocabulary;

public interface IVocabularyManager {

	/**
	 * returns all vocabularies of a project. This includes vocabularies that are indexed (which can be retrieved from VocabularySearcher)
	 * but also ones that are added to the project but unknown to the VocabularySearcher i.e. not indexed. the non-indexed 
	 * vocabularies usually result from importing a project, extract/apply schema, changing the default vocabularies...
	 * @param searcher
	 * @param schema
	 * @param projectId
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @see IVocabularySearcher#getProjectVocabularies(String)
	 */
	public List<DescribedVocabulary> getVocabulariesOfProject(IVocabularySearcher searcher, RdfSchema schema, String projectId) throws ParseException, IOException;
	
	public void setDefaultPrefixes(IVocabularySearcher searcher, IPredefinedVocabularyManager predefinedVocabularyManager, RdfSchema schema, String projectId) throws CorruptIndexException, IOException, ParseException;
	
	public static final class DescribedVocabulary {
		Vocabulary vocabulary;
		boolean imported;
		
		public DescribedVocabulary(Vocabulary v,boolean imported){
			this.vocabulary = v;
			this.imported = imported;
		}
		
		public void write(JSONWriter writer) throws JSONException{
			this.vocabulary.write(writer, this.imported);
		}
		
		public Vocabulary getVocabulary() {
			return vocabulary;
		}
	}
}
