package org.deri.grefine.rdf.vocab;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.openrdf.repository.Repository;




public interface IVocabularySearcher {

	/**
	 * import the vocabulary from namespace and use the short name name for it
	 * this vocabulary is not limited to a specific project i.e. Global vocabulary
	 */
	public void importAndIndexVocabulary(String name, String uri, String fetchUrl,VocabularyImporter importer) throws VocabularyImportException, VocabularyIndexException, PrefixExistException, CorruptIndexException, IOException;
	
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
	public void importAndIndexVocabulary(String name, String uri, String fetchUrl,String projectId,VocabularyImporter importer) throws VocabularyImportException, VocabularyIndexException, PrefixExistException, CorruptIndexException, IOException;
	public void importAndIndexVocabulary(String name, String uri, Repository repository, String projectId,VocabularyImporter importer) throws VocabularyImportException, VocabularyIndexException, PrefixExistException, CorruptIndexException, IOException;
	
	public List<SearchResultItem> searchClasses(String str, String projectId) throws IOException;
	
	public List<SearchResultItem> searchProperties(String str, String projectId) throws IOException;
	
	public void deleteTermsOfVocabs(Set<Vocabulary> toRemove,String projectId) throws CorruptIndexException, IOException;
	public void deleteTermsOfVocab(String vocabName, String projectId) throws CorruptIndexException, IOException;
	
	public void addPredefinedVocabulariesToProject(long projectId)throws VocabularyIndexException, IOException;
	
	public void update() throws CorruptIndexException, IOException;

	public void synchronize(String projectId, Set<String> prefixes) throws IOException;
}
