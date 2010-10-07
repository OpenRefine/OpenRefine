package com.google.refine.rdf.vocab;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;

import com.google.refine.rdf.vocab.imp.Vocabulary;



public interface IVocabularySearcher {

	/**
	 * import the vocabulary from namespace and use the short name name for it
	 * this vocabulary is not limited to a specific project i.e. Global vocabulary
	 * @param name
	 * @param uri
	 * @throws VocabularyImportException
	 * @throws VocabularyIndexException
	 * @throws PrefixExistException
	 */
	public Vocabulary importAndIndexVocabulary(String name, String uri)throws VocabularyImportException, VocabularyIndexException, PrefixExistException ;
	
	/**
	 * import the vocabulary from namespace and use the short name name for it
	 * This vocabulary is to be used (searched...) only with project projectId
	 * @param name
	 * @param uri
	 * @param projectId
	 * @throws VocabularyImportException
	 * @throws VocabularyIndexException
	 * @throws PrefixExistException
	 */
	public Vocabulary importAndIndexVocabulary(String name, String uri,String projectId)throws VocabularyImportException, VocabularyIndexException, PrefixExistException ;
	
	public List<SearchResultItem> searchClasses(String str, String projectId) throws ParseException,IOException;
	
	public List<SearchResultItem> searchProperties(String str, String projectId) throws ParseException,IOException;

	public List<Vocabulary> getProjectVocabularies(String projectId)throws ParseException,IOException;
	
	public void addPredefinedVocabulariesToProject(long projectId)throws VocabularyIndexException, IOException;
	
	public void deleteVocabulary(String name,String projectId) throws ParseException,IOException;
	
	public void updateVocabulary(String name,String uri, String projectId) throws ParseException,IOException, VocabularyImportException, VocabularyIndexException;
	
	public void synchronizeIndex(Map<String, String> prefixes, String projectId)throws IOException, ParseException;
	
	public void dispose()throws CorruptIndexException, IOException;
	
	public void deleteProjectVocabularies(String projectId) throws ParseException,IOException;
}
