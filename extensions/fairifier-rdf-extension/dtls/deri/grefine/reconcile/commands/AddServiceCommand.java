package org.deri.grefine.reconcile.commands;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


import org.deri.grefine.reconcile.GRefineServiceManager;
import org.deri.grefine.reconcile.model.ReconciliationService;
import org.deri.grefine.reconcile.rdf.RdfReconciliationService;
import org.deri.grefine.reconcile.rdf.endpoints.PlainSparqlQueryEndpoint;
import org.deri.grefine.reconcile.rdf.endpoints.QueryEndpoint;
import org.deri.grefine.reconcile.rdf.endpoints.QueryEndpointImpl;
import org.deri.grefine.reconcile.rdf.executors.DumpQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.RemoteQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.VirtuosoRemoteQueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.BigOwlImSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.JenaTextSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.PlainSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.VirtuosoSparqlQueryFactory;
import org.json.JSONException;

public class AddServiceCommand extends AbstractAddServiceCommand{

	final static double DEFAULT_MATCH_THRESHOLD = 0.9;
	
	@Override
	protected ReconciliationService getReconciliationService(HttpServletRequest request) throws JSONException, IOException {
		String name = request.getParameter("name");
		String id = getIdForString(name);
		String url = request.getParameter("url");
		
		//make sure that id is unique
		if(GRefineServiceManager.singleton.hasService(id)){
			//id already exist
			throw new RuntimeException("A service with name '" + id + "' already exist!");
		}
		String labelProps = request.getParameter("properties");
		ImmutableList<String> propUris = asImmutableList(labelProps);
		//validate
		if(name.isEmpty() || url.isEmpty() || propUris.size()==0){
			throw new RuntimeException("name, endpoint URL and at least one label property ar needed");
		}
		String datasource = request.getParameter("datasource");
		ReconciliationService service;
		if(datasource.equals("sparql")){
			service = getSparqlService(name,id,url,propUris,request);
			GRefineServiceManager.singleton.addService(service);
		}else{
			String format = request.getParameter("file_format");
			if(format.equals("autodetect")){
				format = null;
			}
			service = getRdfService(name,id,url,format,propUris,request);
			GRefineServiceManager.singleton.addAndSaveService(service);
		}
		
		return service;
	}
	
	private ReconciliationService getRdfService(String name, String id, String url,String format, ImmutableList<String> propUris, HttpServletRequest request) {
		Model model = ModelFactory.createDefaultModel();
		if(format==null){
			model.read(url);
		}else{
			model.read(url,format);
		}
		
		QueryExecutor queryExecutor;
		if(propUris.size()==1){
			queryExecutor = new DumpQueryExecutor(model, propUris.get(0));
		}else{
			queryExecutor = new DumpQueryExecutor(model);
		}
		SparqlQueryFactory queryFactory = new JenaTextSparqlQueryFactory();
		QueryEndpoint queryEndpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
		return new RdfReconciliationService(id, name, queryEndpoint, DEFAULT_MATCH_THRESHOLD);
	}

	private ReconciliationService getSparqlService(String name, String id,String url, ImmutableList<String> propUris, HttpServletRequest request) {
		String type = request.getParameter("type");
		String graph = request.getParameter("graph");

		graph = graph==null || graph.trim().isEmpty()?null:graph;
		QueryEndpoint queryEndpoint;
		if(type.equals("jena-text")){
			SparqlQueryFactory queryFactory = new JenaTextSparqlQueryFactory();
			QueryExecutor queryExecutor = new RemoteQueryExecutor(url, graph);
			queryEndpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
		}else if(type.equals("virtuoso")){
			SparqlQueryFactory queryFactory = new VirtuosoSparqlQueryFactory();
			QueryExecutor queryExecutor = new VirtuosoRemoteQueryExecutor(url, graph);;
			queryEndpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
		}else if(type.equals("bigowlim")){
			SparqlQueryFactory queryFactory = new BigOwlImSparqlQueryFactory();
			QueryExecutor queryExecutor = new RemoteQueryExecutor(url, graph);
			queryEndpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
		}else{
			//plain
			PlainSparqlQueryFactory queryFactory = new PlainSparqlQueryFactory();
			QueryExecutor queryExecutor = new RemoteQueryExecutor(url, graph);
			queryEndpoint = new PlainSparqlQueryEndpoint(queryFactory, queryExecutor);
		}
		ReconciliationService service = new RdfReconciliationService(id, name, propUris, queryEndpoint, DEFAULT_MATCH_THRESHOLD);
		return service;
	}
}
