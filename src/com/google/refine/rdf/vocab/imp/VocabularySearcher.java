package com.google.refine.rdf.vocab.imp;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.rdf.vocab.IVocabularySearcher;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.RDFNode;
import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.rdf.vocab.SearchResultItem;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.util.ParsingUtilities;

public class VocabularySearcher implements IVocabularySearcher {

	final static Logger logger = LoggerFactory.getLogger("vocabulary_searcher");

	private static final String CLASS_TYPE = "class";
	private static final String PROPERTY_TYPE = "property";
	private static final String VOCABULARY_TYPE = "vocabulary";
	//project id is always a number. it is safe to use this placeholder
	private static final String GLOBAL_VOCABULARY_PLACE_HOLDER = "g";

	private IndexWriter writer;
	private IndexSearcher searcher;

	private Directory _directory;

	public VocabularySearcher(File dir) throws IOException {
		_directory = new SimpleFSDirectory(new File(dir, "luceneIndex"));
		writer = new IndexWriter(_directory, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
		searcher = new IndexSearcher(_directory);
	}

	@Override
	public Vocabulary importAndIndexVocabulary(String name, String uri, String projectId)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException {
		if(projectId==null || projectId.isEmpty()){
			throw new RuntimeException("projectId is null");
		}
		try {
			if(prefixExists(name,projectId)){
				throw new PrefixExistException(name + " already defined");
			}
			Vocabulary v = new Vocabulary(name, uri);
			try{
				VocabularyImporter importer = new VocabularyImporter();
				v = importer.importVocabulary(name, uri);
			}finally{
				// we want the vocabulary document to be always added even if the import failed
				indexVocabulary(v,projectId);
			}

			indexTerms(v,projectId);
			return v;
		} catch (CorruptIndexException e) {
			throw new VocabularyIndexException(
					"Unable to index vocabulary retrieved from " + uri, e);
		} catch (IOException e) {
			throw new VocabularyIndexException(
					"Unable to index vocabulary retrieved from " + uri, e);
		}

	}

	@Override
	public Vocabulary importAndIndexVocabulary(String name, String uri)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException {
		return importAndIndexVocabulary(name, uri, GLOBAL_VOCABULARY_PLACE_HOLDER);

	}

	@Override
	public List<SearchResultItem> searchClasses(String str, String projectId)
			throws ParseException, IOException {
		org.apache.lucene.search.Query query = prepareQuery(str, CLASS_TYPE,
				projectId);
		TopDocs docs = searcher.search(query, getMaxDoc());
		return prepareSearchResults(docs);
	}
	
	@Override
	public List<SearchResultItem> searchProperties(String str, String projectId)
			throws ParseException, IOException {
		org.apache.lucene.search.Query query = prepareQuery(str, PROPERTY_TYPE,
				projectId);
		TopDocs docs = searcher.search(query, getMaxDoc());
		return prepareSearchResults(docs);
	}
	
	@Override
	public List<Vocabulary> getProjectVocabularies(String projectId)throws ParseException,IOException{
		if(projectId==null || projectId.isEmpty()){
			throw new RuntimeException("projectId is null");
		}
		org.apache.lucene.search.Query query = prepareVocabulariesQuery(projectId);
		TopDocs docs = searcher.search(query, getMaxDoc());
		return prepareVocabulariess(docs);
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
	public void deleteVocabulary(String name,String projectId) throws ParseException,IOException{
		if(projectId==null || projectId.isEmpty()){
			throw new RuntimeException("projectId is null");
		}
		// "type":vocabulary AND "projectId":projectId AND "name":name
		// ("type": (class OR property) ) AND "projectId":projectId AND "prefix":name
		
		BooleanQuery vocabQuery = new BooleanQuery();
		vocabQuery.add(new TermQuery(new Term("type", VOCABULARY_TYPE)), Occur.MUST);
		vocabQuery.add(new TermQuery(new Term("projectId", projectId)), Occur.MUST);
		vocabQuery.add(new TermQuery(new Term("name", name)), Occur.MUST);
		
		BooleanQuery termsQuery = new BooleanQuery();
		BooleanQuery typeQuery = new BooleanQuery();
		typeQuery.add(new TermQuery(new Term("type",CLASS_TYPE)),Occur.SHOULD);
		typeQuery.add(new TermQuery(new Term("type",PROPERTY_TYPE)),Occur.SHOULD);
		
		termsQuery.add(typeQuery, Occur.MUST);
		termsQuery.add(new TermQuery(new Term("projectId", projectId)), Occur.MUST);
		termsQuery.add(new TermQuery(new Term("prefix", name)), Occur.MUST);
		
		writer.deleteDocuments(vocabQuery);
		writer.deleteDocuments(termsQuery);
		this.update();
	}
	
	@Override
	public void updateVocabulary(String name, String uri, String projectId) throws ParseException,IOException,VocabularyImportException, VocabularyIndexException{
		if(projectId==null || projectId.isEmpty()){
			throw new RuntimeException("projectId is null");
		}
		VocabularyImporter importer = new VocabularyImporter();
		Vocabulary v = importer.importVocabulary(name, uri);
//		deleteVocabulary(name, projectId);
		try{
			indexVocabulary(v,projectId);
			indexTerms(v,projectId);
		} catch (CorruptIndexException e) {
			throw new VocabularyIndexException(
				"Unable to index vocabulary retrieved from " + uri, e);
		} catch (IOException e) {
			throw new VocabularyIndexException(
				"Unable to index vocabulary retrieved from " + uri, e);
		}
	}
	
	@Override
	public void synchronizeIndex(Map<String, String> prefixes, String projectId) throws IOException, ParseException{
		Map<String,String> indexed = getVocabulariesOfProjectId(projectId);
		if(!indexed.entrySet().equals(prefixes.entrySet())){
			//synchronization needed
			for(Entry<String, String> v:indexed.entrySet()){
				if(! prefixes.containsKey(v.getKey()) || ! prefixes.get(v.getKey()).equals(v.getValue())){
					//this is out of order
					deleteVocabulary(v.getKey(), projectId);
				}
			}
		}
		
	}
	
	@Override
	public void dispose() throws CorruptIndexException, IOException{
		this.writer.close();
		this.searcher.close();
	}
	
	@Override
	public void deleteProjectVocabularies(String projectId) throws ParseException,IOException{
		Query query = new TermQuery(new Term("projectId",projectId));
		this.writer.deleteDocuments(query);
//		this.writer.commit();
		this.update();
	}
	
	@Override
	public Map<String, String> setDefaultVocabularies(String projectId) throws CorruptIndexException, IOException, ParseException{
		//remove default vocabularies 
		this.deleteProjectVocabularies(GLOBAL_VOCABULARY_PLACE_HOLDER);
		//set the new defaults
		this.addDocumentsToProject(this.getDocumentsOfProjectId(projectId), GLOBAL_VOCABULARY_PLACE_HOLDER);
		return this.getVocabulariesOfProjectId(projectId);
	}
	
	@Override
	public void addDefaultPrefixIfNotExist(String name, String uri) throws IOException{
		if(prefixExists(name,GLOBAL_VOCABULARY_PLACE_HOLDER)){
			//already exists 
			return;
		}
		Vocabulary v = new Vocabulary(name, uri);
		indexVocabulary(v,GLOBAL_VOCABULARY_PLACE_HOLDER);
	}
	
	private void indexRdfNode(RDFNode node, String type, String projectId)
			throws CorruptIndexException, IOException {
		Document doc = new Document();
		doc.add(new Field("type", type, Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		doc.add(new Field("prefix", node.getVocabularyPrefix(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		String l = node.getLabel() == null ? "" : node.getLabel();
		Field labelField = new Field("label", l, Field.Store.YES,
				Field.Index.ANALYZED);
		doc.add(labelField);
		String d = node.getDescription() == null ? "" : node.getDescription();
		Field descriptionField = new Field("description", d, Field.Store.YES,
				Field.Index.ANALYZED);
		doc.add(descriptionField);
		doc.add(new Field("uri", node.getURI(), Field.Store.YES, Field.Index.NO));
		Field localPartField = new Field("localPart", node.getLocalPart(),
				Field.Store.YES, Field.Index.ANALYZED);
		doc.add(localPartField);
		Field namespaceField = new Field("namespace", node.getVocabularyUri(),
				Field.Store.YES, Field.Index.NOT_ANALYZED);
		doc.add(namespaceField);
		Field projectIdField = new Field("projectId",
				String.valueOf(projectId), Field.Store.YES,
				Field.Index.NOT_ANALYZED);
		doc.add(projectIdField);

		writer.addDocument(doc);
	}

	private Document buildVocabularyDoc(Vocabulary v, String projectId)
			throws CorruptIndexException, IOException {
		Document doc = new Document();
		doc.add(new Field("type", VOCABULARY_TYPE, Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		doc.add(new Field("name", v.getName(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		doc.add(new Field("uri", v.getUri(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		Field projectIdField = new Field("projectId",
				String.valueOf(projectId), Field.Store.YES,
				Field.Index.NOT_ANALYZED);
		doc.add(projectIdField);
		//report info
		Field numOfClassesField = new Field("numOfClasses",String.valueOf(v.getReport().getNumOfClasses()),Field.Store.YES,Field.Index.NO);
		doc.add(numOfClassesField);
		Field numOfPropertiesField = new Field("numOfProperties",String.valueOf(v.getReport().getNumOfProperties()),Field.Store.YES,Field.Index.NO);
		doc.add(numOfPropertiesField);
		Field extractorsField = new Field("extractors",v.getReport().getExtractors(),Field.Store.YES,Field.Index.NO);
		doc.add(extractorsField);
		Field importedDateField = new Field("importedDate",ParsingUtilities.dateToString(v.getReport().getImportedDate()),Field.Store.YES,Field.Index.NO);
		doc.add(importedDateField);
		return doc;
	}

	private Query prepareQuery(String s, String type, String projectId)
			throws ParseException, IOException {
		BooleanQuery q1 = new BooleanQuery();
		//q1.add(new TermQuery(new Term("projectId",GLOBAL_VOCABULARY_PLACE_HOLDER)), Occur.SHOULD);
		q1.add(new TermQuery(new Term("projectId",projectId)), Occur.MUST);
		
		BooleanQuery q2 = new BooleanQuery();
		q2.add(new TermQuery(new Term("type",type)), Occur.MUST);
		
		BooleanQuery q = new BooleanQuery();
		q.add(q1,Occur.MUST);
		q.add(q2,Occur.MUST);
		
		if (s != null && s.trim().length() > 0) {
			SimpleAnalyzer analyzer = new SimpleAnalyzer();
			if (s.indexOf(":") == -1) {
				//the query we need:
				// "projectId":projectId AND "type":type AND ("prefix":s* OR "localPart":s* OR "label":s* OR "description":s*)
				BooleanQuery q3 = new BooleanQuery();
				q3.add(new WildcardQuery(new Term("prefix",s + "*")), Occur.SHOULD);
				
				TokenStream stream = analyzer.tokenStream("localPart", new StringReader(s));
				// get the TermAttribute from the TokenStream
			    TermAttribute termAtt = stream.addAttribute(TermAttribute.class);

			    stream.reset();
				while(stream.incrementToken()){
					String tmp = termAtt.term() + "*";
					q3.add(new WildcardQuery(new Term("localPart", tmp)), Occur.SHOULD);
				}
				stream.close();
				stream.end();
				
				stream = analyzer.tokenStream("description", new StringReader(s));
				// get the TermAttribute from the TokenStream
			    termAtt = stream.addAttribute(TermAttribute.class);

			    stream.reset();
				while(stream.incrementToken()){
					String tmp = termAtt.term() + "*";
					q3.add(new WildcardQuery(new Term("description",tmp)), Occur.SHOULD);
				}
				stream.close();
				stream.end();
				
				stream = analyzer.tokenStream("label", new StringReader(s));
				// get the TermAttribute from the TokenStream
			    termAtt = stream.addAttribute(TermAttribute.class);

			    stream.reset();
				while(stream.incrementToken()){
					String tmp = termAtt.term() + "*";
					q3.add(new WildcardQuery(new Term("label",tmp)), Occur.SHOULD);
				}
				stream.close();
				stream.end();
				
				q.add(q3,Occur.MUST);
				return q;
			} else {
				//the query we need:
				// "projectId":projectId AND "type":type AND ("prefix":p1 AND "localPart":s*)
				String p1 = s.substring(0, s.indexOf(":"));
				String p2 = s.substring(s.indexOf(":") + 1);
				
				BooleanQuery q3 = new BooleanQuery();
				q3.add(new TermQuery(new Term("prefix",p1)), Occur.SHOULD);
				
				BooleanQuery q4 = new BooleanQuery();
				
				TokenStream stream = analyzer.tokenStream("localPart", new StringReader(p2));
				// get the TermAttribute from the TokenStream
			    TermAttribute termAtt = stream.addAttribute(TermAttribute.class);

			    stream.reset();
				if(!p2.isEmpty()){
					while(stream.incrementToken()){
						q4.add(new WildcardQuery(new Term("localPart",termAtt.term()+"*")), Occur.SHOULD);
					}
				}
				stream.close();
				stream.end();
				
				q.add(q3,Occur.MUST);
				if(!p2.isEmpty()){
					q.add(q4,Occur.MUST);
				}
				return q;
			}
		}else{
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
	
	private void update() throws CorruptIndexException, IOException{
		writer.commit();
		//TODO this shouldn't  be required but it is not working without it... check
		searcher.close();
		searcher = new IndexSearcher(_directory);
	}
	
	private boolean prefixExists(String name,String projectId) throws IOException{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term("type",VOCABULARY_TYPE)),Occur.MUST);
		query.add(new TermQuery(new Term("name",name)),Occur.MUST);
		query.add(new TermQuery(new Term("projectId",projectId)),Occur.MUST);
		TopDocs hits = searcher.search(query, getMaxDoc());
		return hits.totalHits!=0;
	}
	
	private void indexVocabulary(Vocabulary v,String projectId) throws CorruptIndexException, IOException{
		Document doc = buildVocabularyDoc(v, projectId);
		writer.addDocument(doc);
		this.update();
	}
	
	private void indexTerms(Vocabulary v,String projectId) throws CorruptIndexException, IOException{
		for (RDFSClass c : v.getClasses()) {
			indexRdfNode(c, CLASS_TYPE, projectId);
		}
		for (RDFSProperty p : v.getProperties()) {
			indexRdfNode(p, PROPERTY_TYPE, projectId);
		}

		this.update();
	}

	private Query prepareVocabulariesQuery(String projectId) throws IOException {
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term("type",VOCABULARY_TYPE)),Occur.MUST);
		query.add(new TermQuery(new Term("projectId",projectId)),Occur.MUST);
		return query;
	}

	private List<Vocabulary> prepareVocabulariess(TopDocs docs) throws CorruptIndexException, IOException {
		List<Vocabulary> vocabularies = new ArrayList<Vocabulary>();
		for (int i = 0; i < docs.totalHits; i++) {
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			String numOfClasses = doc.get("numOfClasses");
			String numOfProperties = doc.get("numOfProperties");
			Date importedDate = ParsingUtilities.stringToDate(doc.get("importedDate"));
			String uri = doc.get("uri");
			String name = doc.get("name");
			String extractors = doc.get("extractors");
			
			Vocabulary v = new Vocabulary(name, uri);
			VocabularyImportReport report = new VocabularyImportReport();
			report.setExtractors(extractors);
			report.setNumOfClasses(Integer.valueOf(numOfClasses));
			report.setNumOfProperties(Integer.valueOf(numOfProperties));
			report.setImportedDate(importedDate);
			v.setReport(report);
			
			vocabularies.add(v);		
		}
		
		return vocabularies;
	}
	
	private TopDocs getDocumentsOfProjectId(String projectId) throws IOException{
		//query for:
		// "projectId":projectId
		Query query = new TermQuery(new Term("projectId",projectId));
		return searcher.search(query, getMaxDoc());
	}
	
	private Map<String,String> getVocabulariesOfProjectId(String projectId) throws IOException{
		//query for:
		// "projectId":projectId AND "type":vocabulary
		BooleanQuery query = new BooleanQuery();
		TermQuery idQuery = new TermQuery(new Term("projectId",projectId));
		TermQuery typeQuery = new TermQuery(new Term("type",VOCABULARY_TYPE));
		
		query.add(idQuery,Occur.MUST);
		query.add(typeQuery,Occur.MUST);
		
		TopDocs docs = searcher.search(query, getMaxDoc());
		Map<String, String> vocabularies = new HashMap<String, String>();
		for (int i = 0; i < docs.totalHits; i++) {
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			String name = doc.get("name");
			String uri = doc.get("uri");
			
			vocabularies.put(name, uri);
		}
		
		return vocabularies;
	}
	
	
	private void addDocumentsToProject(TopDocs docs,String projectId) throws CorruptIndexException, IOException{
		for(int i=0;i<docs.totalHits;i++){
			Document doc = searcher.doc(docs.scoreDocs[i].doc);
			//TODO this needs to be changed into a more efficient impl
			Document newdoc = new Document();
			for(Fieldable f:doc.getFields()){
				newdoc.add(f);
			}
			newdoc.removeField("projectId");
			newdoc.add(new Field("projectId",projectId,Field.Store.YES,Field.Index.NOT_ANALYZED));
			writer.addDocument(newdoc);
		}
	}
	
	private int getMaxDoc() throws IOException{
		return searcher.maxDoc()>0?searcher.maxDoc():1000;
	}
	
	//added to enable testing. Do not use unless for testing
	VocabularySearcher() {
	}
}
