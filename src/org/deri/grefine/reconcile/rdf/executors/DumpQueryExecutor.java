package org.deri.grefine.reconcile.rdf.executors;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author fadmaa
 * execute SPARQL queries agains Dump RDF and supports LARQ for
 * full text searc
 * as index in built with the model this calss can be costly to build
 * consider sharing instances of this class It is thread-safe 
 */
public class DumpQueryExecutor implements QueryExecutor {

	private Dataset index;
	private boolean loaded = false;
	//property used for index/search (only if one property is used)
	private String propertyUri;
	
	public DumpQueryExecutor(){
		
	}
	
	public DumpQueryExecutor(String propertyUri){
		this.propertyUri = propertyUri;
	}
	
	public DumpQueryExecutor(Model m, String propUri){
		this(m,propUri,false,DEFAULT_MIN_NGRAM, DEFAULT_MAX_NGRAM);
	}
	
	public DumpQueryExecutor(Model m){
		this(m,null,false,DEFAULT_MIN_NGRAM, DEFAULT_MAX_NGRAM);
	}
	
	public DumpQueryExecutor(Model m, String propertyUri, boolean ngramIndex,int minGram, int maxGram){
		loaded = true;
		this.propertyUri = propertyUri;
		
		Dataset ds1 = DatasetFactory.createMem();
		EntityDefinition entDef = new EntityDefinition("uri", "text",m.getResource(propertyUri)) ;

        // Lucene, in memory.
        Directory dir =  new RAMDirectory();
        
        // Join together into a dataset
        this.index = TextDatasetFactory.createLucene(ds1, dir, entDef) ;
        this.index.getDefaultModel().add(m);
        //this.index.commit();
	}
	
	@Override
	public ResultSet sparql(String sparql) {
		if(!loaded){
			throw new RuntimeException("Model is not loaded");
		}
		//this.index.begin(ReadWrite.READ) ;
		Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
		QueryExecution qExec = QueryExecutionFactory.create(query, this.index);
		ResultSet result =  qExec.execSelect();
		return result;
	}
	
	
	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type");	writer.value("dump");
		if(propertyUri!=null){
			writer.key("propertyUri");	writer.value(propertyUri);
		}
		writer.endObject();
	}
	
	public void dispose(){
		this.index.close();
		this.index = null; //free the memory used for the model
	}
	
	public synchronized void initialize(FileInputStream in) {
		if(loaded){
			return;
		}
		loaded = true;
		// -- Read and index all literal strings.
		Model model = ModelFactory.createDefaultModel();
		model.read(in, null,"TTL");
		
		Dataset ds1 = DatasetFactory.createMem();
		EntityDefinition entDef = new EntityDefinition("uri", "text",model.getResource(propertyUri)) ;

        // Lucene, in memory.
        Directory dir =  new RAMDirectory();
        
        // Join together into a dataset
        this.index = TextDatasetFactory.createLucene(ds1, dir, entDef) ;
        this.index.getDefaultModel().add(model);
        this.index.commit();
	}
	
	private static final int DEFAULT_MIN_NGRAM = 3;
	private static final int DEFAULT_MAX_NGRAM = 3;

	@Override
	public void save(String serviceId, FileOutputStream out) throws IOException {
		this.index.getDefaultModel().write(out, "TTL");
		out.close();
	}
}