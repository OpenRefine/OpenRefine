package org.deri.grefine.reconcile.rdf.executors;

import java.util.Collections;

import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class VirtuosoRemoteQueryExecutor extends RemoteQueryExecutor{

	public VirtuosoRemoteQueryExecutor(String sparqlEndpointUrl,String defaultGraphUri) {
		super(sparqlEndpointUrl,defaultGraphUri);
	}

	@Override
	public ResultSet sparql(String sparql) {
		//we use QueryEngineHTTP to skip query validation as Virtuoso needs non-standardised extensions and will not pass ARQ validation
		QueryEngineHTTP qExec = new QueryEngineHTTP(sparqlEndpointUrl, sparql);
		if(defaultGraphUri!=null){
			qExec.setDefaultGraphURIs(Collections.singletonList(defaultGraphUri));
		}
		ResultSet res = qExec.execSelect();
		return res;
	}
	
	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type");	writer.value("remote-virtuoso");
		writer.key("sparql-url"); writer.value(sparqlEndpointUrl);
		if(defaultGraphUri!=null){
			writer.key("default-graph-uri"); writer.value(defaultGraphUri);
		}
		writer.endObject();
	}
}
