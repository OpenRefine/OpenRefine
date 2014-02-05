package org.deri.grefine.rdf.vocab.imp;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.deri.grefine.rdf.vocab.IVocabularySearcher;
import org.deri.grefine.rdf.vocab.PrefixExistException;
import org.deri.grefine.rdf.vocab.RDFNode;
import org.deri.grefine.rdf.vocab.RDFSClass;
import org.deri.grefine.rdf.vocab.RDFSProperty;
import org.deri.grefine.rdf.vocab.SearchResultItem;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyImportException;
import org.deri.grefine.rdf.vocab.VocabularyImporter;
import org.deri.grefine.rdf.vocab.VocabularyIndexException;
import org.openrdf.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VocabularySearcher implements IVocabularySearcher {

	final static Logger logger = LoggerFactory.getLogger("vocabulary_searcher");

	private static final String CLASS_TYPE = "class";
	private static final String PROPERTY_TYPE = "property";
	// project id is always a number. it is safe to use this placeholder
	private static final String GLOBAL_VOCABULARY_PLACE_HOLDER = "g";

	private IndexWriter writer;
	private IndexSearcher searcher;
	private IndexReader r;

	private Directory _directory;
	
	public VocabularySearcher(File dir) throws IOException {
                _directory = new SimpleFSDirectory(new File(dir, "luceneIndex"));
                Analyzer a = new SimpleAnalyzer(Version.LUCENE_43);
                IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_43,a);                

                writer = new IndexWriter(_directory,conf);
                writer.commit();
                r = DirectoryReader.open(_directory);
                searcher = new IndexSearcher(r);
        }

	@Override
	public void importAndIndexVocabulary(String name, String uri, String fetchUrl,VocabularyImporter importer)throws VocabularyImportException, VocabularyIndexException,PrefixExistException, CorruptIndexException, IOException {
		importAndIndexVocabulary(name, uri, fetchUrl, GLOBAL_VOCABULARY_PLACE_HOLDER,importer);
	}

	@Override
	public void importAndIndexVocabulary(String name, String uri, String fetchUrl, String projectId,VocabularyImporter importer) throws VocabularyImportException,VocabularyIndexException, PrefixExistException,
			CorruptIndexException, IOException {
		List<RDFSClass> classes = new ArrayList<RDFSClass>();
		List<RDFSProperty> properties = new ArrayList<RDFSProperty>();
		importer.importVocabulary(name, uri, fetchUrl,classes, properties);
		indexTerms(name, uri, projectId, classes, properties);
	}

	
	@Override
	public void importAndIndexVocabulary(String name, String uri, Repository repository, String projectId,VocabularyImporter importer) throws VocabularyImportException, VocabularyIndexException,
			PrefixExistException, CorruptIndexException, IOException {
		List<RDFSClass> classes = new ArrayList<RDFSClass>();
		List<RDFSProperty> properties = new ArrayList<RDFSProperty>();
		importer.importVocabulary(name, uri, repository, classes, properties);
		indexTerms(name, uri, projectId, classes, properties);
	}

	@Override
	public List<SearchResultItem> searchClasses(String str, String projectId)
			throws IOException {
		Query query = prepareQuery(str, CLASS_TYPE, projectId);
		TopDocs docs = searcher.search(query, getMaxDoc());		
		return prepareSearchResults(docs);
	}

	@Override
	public List<SearchResultItem> searchProperties(String str, String projectId)
			throws IOException {
		Query query = prepareQuery(str, PROPERTY_TYPE, projectId);
		TopDocs docs = searcher.search(query, getMaxDoc());
		return prepareSearchResults(docs);
	}

	@Override
	public void deleteTermsOfVocabs(Set<Vocabulary> toRemove, String projectId)
			throws CorruptIndexException, IOException {
		for (Vocabulary v : toRemove) {
			deleteTerms(v.getName(), projectId);
		}
		this.update();
	}

	@Override
	public void addPredefinedVocabulariesToProject(long projectId)throws VocabularyIndexException, IOException{
		//get all documents of the global scope
		TopDocs docs = getDocumentsOfProjectId(GLOBAL_VOCABULARY_PLACE_HOLDER);
		//add all of them to project projectId
		addDocumentsToProject(docs,String.valueOf(projectId));
		
		this.update();
	}
	
	@Override
	public void update() throws CorruptIndexException, IOException {
		writer.commit();
		// TODO this shouldn't be required but it is not working without it...
		// check
		r.close();
		r = IndexReader.open(_directory);
		searcher = new IndexSearcher(r);
	}
	
	@Override
	public void synchronize(String projectId, Set<String> prefixes) throws IOException{
		Set<String> allPrefixes = getPrefixesOfProjectId(projectId);
		allPrefixes.removeAll(prefixes);
		if(!allPrefixes.isEmpty()){
			deletePrefixesOfProjectId(projectId,allPrefixes);
		}
		this.update();
	}
	
	@Override
	public void deleteTermsOfVocab(String vocabName, String projectId) throws CorruptIndexException, IOException {
		deleteTerms(vocabName, projectId);
		this.update();
	}

	/*
	 * Private methods
	 */
	private void deleteTerms(String prefix, String projectId)
			throws CorruptIndexException, IOException {
		if (projectId == null || projectId.isEmpty()) {
			throw new RuntimeException("projectId is null");
		}
		// "type":vocabulary AND "projectId":projectId AND "name":name
		// ("type": (class OR property) ) AND "projectId":projectId AND
		// "prefix":name

		BooleanQuery termsQuery = new BooleanQuery();
		BooleanQuery typeQuery = new BooleanQuery();
		typeQuery
				.add(new TermQuery(new Term("type", CLASS_TYPE)), Occur.SHOULD);
		typeQuery.add(new TermQuery(new Term("type", PROPERTY_TYPE)),
				Occur.SHOULD);

		termsQuery.add(typeQuery, Occur.MUST);
		termsQuery.add(new TermQuery(new Term("projectId", projectId)),
				Occur.MUST);
		termsQuery.add(new TermQuery(new Term("prefix", prefix)), Occur.MUST);

		writer.deleteDocuments(termsQuery);
	}

	private void indexTerms(String name, String uri, String projectId,
			List<RDFSClass> classes, List<RDFSProperty> properties)
			throws CorruptIndexException, IOException {
		for (RDFSClass c : classes) {
			indexRdfNode(c, CLASS_TYPE, projectId);
		}
		for (RDFSProperty p : properties) {
			indexRdfNode(p, PROPERTY_TYPE, projectId);
		}

		this.update();
	}

	private void indexRdfNode(RDFNode node, String type, String projectId)
			throws CorruptIndexException, IOException {
		Document doc = new Document();
		doc.add(new Field("type", type, Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		doc.add(new Field("prefix", node.getVocabularyPrefix(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		String l = node.getLabel() == null ? "" : node.getLabel();
		doc.add(new Field("label", l, Field.Store.YES, Field.Index.ANALYZED));
		String d = node.getDescription() == null ? "" : node.getDescription();
		doc.add(new Field("description", d, Field.Store.YES,
				Field.Index.ANALYZED));
		doc.add(new Field("uri", node.getURI(), Field.Store.YES, Field.Index.NO));
		doc.add(new Field("localPart", node.getLocalPart(), Field.Store.YES,
				Field.Index.ANALYZED));
		doc.add(new Field("namespace", node.getVocabularyUri(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("projectId", String.valueOf(projectId),
				Field.Store.YES, Field.Index.NOT_ANALYZED));

		writer.addDocument(doc);
	}

	private Query prepareQuery(String s, String type, String projectId)
			throws IOException {
		BooleanQuery q1 = new BooleanQuery();
		// q1.add(new TermQuery(new
		// Term("projectId",GLOBAL_VOCABULARY_PLACE_HOLDER)), Occur.SHOULD);
		q1.add(new TermQuery(new Term("projectId", projectId)), Occur.MUST);

		BooleanQuery q2 = new BooleanQuery();
		q2.add(new TermQuery(new Term("type", type)), Occur.MUST);

		BooleanQuery q = new BooleanQuery();
		q.add(q1, Occur.MUST);
		q.add(q2, Occur.MUST);

		if (s != null && s.trim().length() > 0) {
			SimpleAnalyzer analyzer = new SimpleAnalyzer(Version.LUCENE_36);
			if (s.indexOf(":") == -1) {
				// the query we need:
				// "projectId":projectId AND "type":type AND ("prefix":s* OR
				// "localPart":s* OR "label":s* OR "description":s*)
				BooleanQuery q3 = new BooleanQuery();
				q3.add(new WildcardQuery(new Term("prefix", s + "*")),
						Occur.SHOULD);

				TokenStream stream = analyzer.tokenStream("localPart",
						new StringReader(s));
				// get the TermAttribute from the TokenStream
				CharTermAttribute termAtt = (CharTermAttribute) stream
						.addAttribute(CharTermAttribute.class);

				stream.reset();
								
				while (stream.incrementToken()) {
					String tmp = termAtt.toString() + "*";
					q3.add(new WildcardQuery(new Term("localPart", tmp)),
							Occur.SHOULD);
				}
				stream.close();
				stream.end();

				stream = analyzer.tokenStream("description",
						new StringReader(s));
				// get the TermAttribute from the TokenStream
				termAtt = (CharTermAttribute) stream
						.addAttribute(CharTermAttribute.class);

				stream.reset();
				while (stream.incrementToken()) {
					String tmp = termAtt.toString() + "*";
					q3.add(new WildcardQuery(new Term("description", tmp)),
							Occur.SHOULD);
				}
				stream.close();
				stream.end();

				stream = analyzer.tokenStream("label", new StringReader(s));
				// get the TermAttribute from the TokenStream
				termAtt = (CharTermAttribute) stream
						.addAttribute(CharTermAttribute.class);

				stream.reset();
				while (stream.incrementToken()) {
					String tmp = termAtt.toString() + "*";
					q3.add(new WildcardQuery(new Term("label", tmp)),
							Occur.SHOULD);
				}
				stream.close();
				stream.end();

				q.add(q3, Occur.MUST);
				return q;
			} else {
				// the query we need:
				// "projectId":projectId AND "type":type AND ("prefix":p1 AND
				// "localPart":s*)
				String p1 = s.substring(0, s.indexOf(":"));
				String p2 = s.substring(s.indexOf(":") + 1);

				BooleanQuery q3 = new BooleanQuery();
				q3.add(new TermQuery(new Term("prefix", p1)), Occur.SHOULD);

				BooleanQuery q4 = new BooleanQuery();

				TokenStream stream = analyzer.tokenStream("localPart",
						new StringReader(p2));
				// get the TermAttribute from the TokenStream
				CharTermAttribute termAtt = (CharTermAttribute) stream
						.addAttribute(CharTermAttribute.class);

				stream.reset();
				if (!p2.isEmpty()) {
					while (stream.incrementToken()) {
						q4.add(new WildcardQuery(new Term("localPart", termAtt.toString()
								 + "*")), Occur.SHOULD);
					}
				}
				stream.close();
				stream.end();

				q.add(q3, Occur.MUST);
				if (!p2.isEmpty()) {
					q.add(q4, Occur.MUST);
				}
				return q;
			}
		} else {
			return q;
		}

	}

	private List<SearchResultItem> prepareSearchResults(TopDocs docs)
			throws CorruptIndexException, IOException {
		List<SearchResultItem> res = new ArrayList<SearchResultItem>();
		for (int i = 0; i < docs.totalHits; i++) {
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			String uri = doc.get("uri");
			String label = doc.get("label");
			String description = doc.get("description");
			String prefix = doc.get("prefix");
			String lPart = doc.get("localPart");

			SearchResultItem item = new SearchResultItem(uri, prefix, lPart,
					label, description);
			res.add(item);
		}

		return res;

	}
	
	private void addDocumentsToProject(TopDocs docs,String projectId) throws CorruptIndexException, IOException{
		for(int i=0;i<docs.totalHits;i++){
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			//TODO this needs to be changed into a more efficient impl
			Document newdoc = new Document();
			Iterator fieldsIter = doc.getFields().iterator();
			while(fieldsIter.hasNext()){
				newdoc.add((IndexableField)fieldsIter.next());
			}
			newdoc.removeField("projectId");
			newdoc.add(new Field("projectId",projectId,Field.Store.YES,Field.Index.NOT_ANALYZED));
			writer.addDocument(newdoc);
		}
	}
	
	private TopDocs getDocumentsOfProjectId(String projectId) throws IOException{
		//query for:
		// "projectId":projectId
		Query query = new TermQuery(new Term("projectId",projectId));
		return searcher.search(query, getMaxDoc());
	}
	
	private Set<String> getPrefixesOfProjectId(String projectId) throws IOException{
		//query for:
		// "projectId":projectId
		Set<String> prefixes = new HashSet<String>();
		Query query = new TermQuery(new Term("projectId",projectId));
		TopDocs docs =  searcher.search(query, getMaxDoc());
		for (int i = 0; i < docs.totalHits; i++) {
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			prefixes.add(doc.get("prefix"));
		}
		return prefixes;
	}
	private void deletePrefixesOfProjectId(String projectId, Set<String> toDelete) throws CorruptIndexException, IOException {
		if (projectId == null || projectId.isEmpty()) {
			throw new RuntimeException("projectId is null");
		}
		// "type":vocabulary AND "projectId":projectId AND ("prefix":prefix OR ...)
		BooleanQuery q = new BooleanQuery();
		Query query = new TermQuery(new Term("projectId",projectId));
		//TODO backward compatibility is broken here!!!!!!
//		Query typeQ = new TermQuery(new Term("type", "vocabulary"));
		
		BooleanQuery prefixQ = new BooleanQuery();
		for(String p:toDelete){
			Query pQ = new TermQuery(new Term("prefix",p));
			prefixQ.add(pQ,Occur.SHOULD);
		}
		q.add(query,Occur.MUST);
//		q.add(typeQ,Occur.MUST);
		q.add(prefixQ,Occur.MUST);
		
		writer.deleteDocuments(q);		
	}
	
	private int getMaxDoc() throws IOException {
		return r.maxDoc() > 0 ? r.maxDoc() : 100000;
	}
}
