package org.deri.grefine.reconcile.rdf.executors;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.LARQ;
import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * @author fadmaa
 * execute SPARQL queries agains Dump RDF and supports LARQ for
 * full text searc
 * as index in built with the model this calss can be costly to build
 * consider sharing instances of this class It is thread-safe 
 */
public class DumpQueryExecutor implements QueryExecutor {

	private Model model;
	private IndexLARQ index;
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
	
	/*public DumpQueryExecutor(Model m, boolean ngramIndex,int minGram, int maxGram){
		loaded = true;
		LARQ.setMinGram(minGram);LARQ.setMaxGram(maxGram);
		this.model = m;
		IndexBuilderString larqBuilder = new IndexBuilderString(ngramIndex?LARQ.NGRAM_INDEX:LARQ.STANDARD_INDEX) ;
		larqBuilder.indexStatements(model.listStatements()) ;
		larqBuilder.closeWriter() ;
		this.index = larqBuilder.getIndex() ;
	}*/
	
	public DumpQueryExecutor(Model m, String propertyUri, boolean ngramIndex,int minGram, int maxGram){
		loaded = true;
		this.model = m;
		this.propertyUri = propertyUri;
		IndexBuilderString larqBuilder;
		if(propertyUri != null){
			Property p = model.getProperty(propertyUri);
			larqBuilder= new IndexBuilderString(p) ;
		}else{
			larqBuilder= new IndexBuilderString() ;
		}
		larqBuilder.indexStatements(model.listStatements()) ;
		larqBuilder.closeWriter() ;
		this.index = larqBuilder.getIndex() ;
	}
	
	@Override
	public ResultSet sparql(String sparql) {
		if(!loaded){
			throw new RuntimeException("Model is not loaded");
		}
		Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
		QueryExecution qExec = QueryExecutionFactory.create(query, model);
		LARQ.setDefaultIndex(qExec.getContext(), index);
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
		model = null; //free the memory used for the model
	}
	
	public synchronized void initialize(FileInputStream in) {
		if(loaded){
			return;
		}
		loaded = true;
		// -- Read and index all literal strings.
		IndexBuilderString larqBuilder;
		model = ModelFactory.createDefaultModel();
		model.read(in, null,"TTL");
		if(propertyUri==null){
			larqBuilder = new IndexBuilderString() ;
		}else{
			Property p = model.getProperty(propertyUri);
			larqBuilder = new IndexBuilderString(p);
		}

		larqBuilder.indexStatements(model.listStatements()) ;
		// -- Finish indexing
		larqBuilder.closeWriter() ;

		// -- Create the access index  
		index = larqBuilder.getIndex() ;
	}
	
	private static final int DEFAULT_MIN_NGRAM = 3;
	private static final int DEFAULT_MAX_NGRAM = 3;

	@Override
	public void save(String serviceId, FileOutputStream out) throws IOException {
		model.write(out, "TTL");
		out.close();
	}
}