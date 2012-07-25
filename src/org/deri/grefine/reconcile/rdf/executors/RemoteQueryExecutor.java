package org.deri.grefine.reconcile.rdf.executors;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author fadmaa
 * query a remote SPARQL endpoint
 */
public class RemoteQueryExecutor implements QueryExecutor{
	protected String sparqlEndpointUrl;
	protected String defaultGraphUri;
	
	public RemoteQueryExecutor(String sparqlEndpointUrl,String defaultGraphUri) {
		this.sparqlEndpointUrl = sparqlEndpointUrl;
		this.defaultGraphUri = defaultGraphUri;
	}

	@Override
	public ResultSet sparql(String sparql) {
		QueryExecution qExec;
		if(defaultGraphUri==null){
			qExec = QueryExecutionFactory.sparqlService(sparqlEndpointUrl, sparql);
		}else{
			qExec = QueryExecutionFactory.sparqlService(sparqlEndpointUrl, sparql,defaultGraphUri);
		}
		ResultSet res = qExec.execSelect();
		return res;
	}

	@Override
	public void save(String serviceId, FileOutputStream baseDir) throws IOException{
		//nothing to save... all data is external
	}
	
	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type"); writer.value("remote");
		writer.key("sparql-url"); writer.value(sparqlEndpointUrl);
		if(defaultGraphUri!=null){
			writer.key("default-graph-uri"); writer.value(defaultGraphUri);
		}
		writer.endObject();
	}

	@Override
	public void initialize(FileInputStream in) {
		//nothing to initialize
		
	}
}
