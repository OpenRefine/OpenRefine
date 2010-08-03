package com.google.gridworks.rdf.vocab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import com.google.gridworks.GridworksServlet;

/**
 * @author fadmaa
 *
 */
public class VocabularyManager {

    private static final String CLASS_TYPE = "class";
    private static final String PROPERTY_TYPE = "property";
    private static final String VOCABULARY_TYPE = "vocabulary";
    
    private IndexWriter writer;
    private IndexSearcher searcher;
    
    private Directory _directory;
    
    private static VocabularyManager singleton;
    
    private List<Vocabulary> vocabularies = new ArrayList<Vocabulary>();
    
    static public VocabularyManager getSingleton(GridworksServlet servlet) {
        return singleton != null ? singleton : (singleton = new VocabularyManager(servlet));
    }
    
    private VocabularyManager(GridworksServlet servlet) {
        try{
            synchronized (this) {
                File dir = servlet.getCacheDir("rdfImporter");
                _directory = new SimpleFSDirectory(new File(dir, "luceneIndex"));
                writer = new IndexWriter(_directory, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.LIMITED);
                searcher = new IndexSearcher(_directory);
                updateVocabulariesList();                
            }
        } catch (CorruptIndexException e) {
            throw new RuntimeException("Failed initialize vocabulary search",e);
        } catch (IOException e) {
            throw new RuntimeException("Failed initialize vocabulary search",e);
        }
    }
    
    
    /**
     * @param url where to get the vocabulary description from
     * @param prefix preferred prefix for vocabulary e.g. foaf, dc, skos, dcat
     * @param namespace the base URI of the vocabulary. usually but not alway the same as url
     * @param format the format of the RDF description of the vocabulary at the end of url (default to RDF/XML)
     * @throws IOException 
     * @throws CorruptIndexException 
     * @throws VocabularyExistException 
     */
    public void addVocabulary(String url, String prefix, String namespace, String format) throws CorruptIndexException, IOException, VocabularyExistException{
        if (defined(namespace)){
            throw new VocabularyExistException(namespace + " already exists!");
        }
        VocabularyImporter importer = new VocabularyImporter();
        Vocabulary vocabulary = importer.getVocabulary(url,prefix,namespace,format);
        indexVocabulary(vocabulary);
        updateSearcher();    
//        updateVocabulariesList();
        this.vocabularies.add(vocabulary);
    }
    
    public void addVocabulary(String url, String prefix, String namespace) throws CorruptIndexException, IOException, VocabularyExistException{
        addVocabulary(url, prefix, namespace,"RDF/XML");
    }
    private void indexVocabulary(Vocabulary v) throws CorruptIndexException, IOException{
        Document doc = new Document();
        doc.add(new Field("type",VOCABULARY_TYPE,Field.Store.YES,Field.Index.NOT_ANALYZED));
        doc.add(new Field("name",v.getName(),Field.Store.YES,Field.Index.NO));
        doc.add(new Field("uri",v.getUri(),Field.Store.YES,Field.Index.NOT_ANALYZED));
        writer.addDocument(doc);
        
        for(RDFSClass c:v.getClasses()){
            indexRdfNode(c, CLASS_TYPE);
        }
        for(RDFSProperty p:v.getProperties()){
            indexRdfNode(p, PROPERTY_TYPE);
        }
        
        writer.commit();
    }
    
    public List<RDFNode> searchClasses(String str)throws ParseException, IOException{
        List<RDFNode> res = new ArrayList<RDFNode>();
        org.apache.lucene.search.Query query = prepareQuery(str, "class");
        TopDocs docs = searcher.search(query, 1000);
        for(int i=0;i<docs.totalHits;i++){
            Document doc= searcher.doc(docs.scoreDocs[i].doc);
            String uri = doc.get("uri");
            String label = doc.get("label");
            String description = doc.get("description");
            String namespace = doc.get("namespace"); 
            String prefix = doc.get("prefix");
            RDFSClass node = new RDFSClass(uri,label,description,prefix,namespace);
            res.add(node);
        }
        
        return res;
    }
    
    public List<RDFNode> searchProperties(String str)throws ParseException, IOException{
        List<RDFNode> res = new ArrayList<RDFNode>();
        org.apache.lucene.search.Query query = prepareQuery(str, "property");
        TopDocs docs = searcher.search(query, 1000);
        for(int i=0;i<docs.totalHits;i++){
            Document doc= searcher.doc(docs.scoreDocs[i].doc);
            String uri = doc.get("uri");
            String label = doc.get("label");
            String description = doc.get("description");
            String namespace = doc.get("namespace"); 
            String prefix = doc.get("prefix");
            RDFNode node = new RDFSProperty(uri,label,description,prefix,namespace);
            res.add(node);
        }
        
        return res;
    }
    
    public List<Vocabulary> getVocabularies(){
        return vocabularies;
    }
    
    public void deleteVocabulary(String uri) throws IOException{
        Vocabulary vocab = null;
        for(Vocabulary v:vocabularies){
            if(v.getUri().equals(uri)){
                vocab = v;
                break;
            }
        }
        if(vocab==null){
            throw new RuntimeException("Vocabulary " + uri + " not found");
        }
        vocabularies.remove(vocab);
        Term t = new Term("uri",uri);
        writer.deleteDocuments(t);
        t = new Term("namespace",uri);
        writer.deleteDocuments(t);
        
        writer.commit();
        updateSearcher();
    }
    
    private org.apache.lucene.search.Query prepareQuery(String s,String type)throws ParseException{
        QueryParser parser = new QueryParser(Version.LUCENE_30,"description",new StandardAnalyzer(Version.LUCENE_30));
        String queryStr = "type:" + type ;
        if(s!=null && s.trim().length()>0){
            s =s.trim();
            if(s.indexOf("*")==-1){
                s += "*";
            }
            if(s.indexOf(":")==-1){
                queryStr += " AND (curie:" + s ;
                queryStr += " OR description:" + s ;
                queryStr += " OR label:" + s + ")";
            }else{
                String p1 = s.substring(0,s.indexOf(":"));
                String p2 = s.substring(s.lastIndexOf(":")+1);
                queryStr += " AND prefix:" + p1;
                if(p2.length()>1){
                    //queryStr += " AND (description:" + p2;
                    queryStr += " AND label:" + p2;
                }
            }
            
        }
        return parser.parse(queryStr);
    }
    
    private void indexRdfNode(RDFNode node, String type) throws CorruptIndexException, IOException{
        //TODO weight fields... setBoost
        Document doc = new Document();
        doc.add(new Field("type",type,Field.Store.YES,Field.Index.NOT_ANALYZED));
        doc.add(new Field("prefix",node.getVocabularyPrefix(),Field.Store.YES,Field.Index.NOT_ANALYZED));
        String l = node.getLabel()==null?"":node.getLabel();
        Field labelField = new Field("label",l,Field.Store.NO,Field.Index.ANALYZED);
        doc.add(labelField);
        String d = node.getDescription()==null?"":node.getDescription();
        Field descriptionField = new Field("description",d,Field.Store.YES,Field.Index.ANALYZED);
        doc.add(descriptionField);
        doc.add(new Field("uri", node.getURI(),Field.Store.YES,Field.Index.NO));
        Field curieField = new Field("curie", node.getPreferredCURIE(),Field.Store.YES,Field.Index.ANALYZED);
        doc.add(curieField);
        Field namespaceField = new Field("namespace", node.getVocabularyUri(),Field.Store.YES,Field.Index.NOT_ANALYZED);
        doc.add(namespaceField);
        writer.addDocument(doc);
    }
    
    private void updateVocabulariesList() throws IOException{
        Term typeTerm = new Term("type", VOCABULARY_TYPE);
        Query query = new TermQuery(typeTerm);
        //TODO 1000 :O
        TopDocs vocabDocs = searcher.search(query, 1000);
        for(int i=0;i<vocabDocs.totalHits;i++){
            Document doc = searcher.doc(vocabDocs.scoreDocs[i].doc);
            String name = doc.get("name");
            String uri = doc.get("uri");
            Vocabulary vocab = new Vocabulary(name, uri);
            this.vocabularies.add(vocab);
        }
        
    }
    
    private void updateSearcher() throws IOException{
        this.searcher = new IndexSearcher(_directory);
    }
    
    private boolean defined(String namespace){
        if(namespace==null){
            return false;
        }
        namespace = namespace.trim();
        for(Vocabulary v:vocabularies){
            if(v.getUri().equals(namespace)){
                return true;
            }
        }
        return false;
    }
}
