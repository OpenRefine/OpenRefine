package org.deri.grefine.reconcile.rdf.endpoints;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.PreviewResourceCannedQuery;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.ResultSet;

public class QueryEndpointImpl implements QueryEndpoint{
	final static Logger logger = LoggerFactory.getLogger("QueryEndpointImpl");
	protected SparqlQueryFactory queryFactory;
	protected QueryExecutor queryExecutor;
	
	public QueryEndpointImpl(SparqlQueryFactory queryFactory, QueryExecutor queryExecutor) {
		this.queryFactory = queryFactory;
		this.queryExecutor = queryExecutor;
	}

	@Override
	public List<ReconciliationCandidate> reconcileEntities(ReconciliationRequest request, ImmutableList<String> searchPropertyUris, double matchThreshold) {
		long start = System.currentTimeMillis();
		String sparql = this.queryFactory.getReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);

		List<ReconciliationCandidate> candidates = this.queryFactory.wrapReconciliationResultset(resultSet, request.getQueryString(), searchPropertyUris, request.getLimit(), matchThreshold);
		//if type is not specified, populate types
		if(request.getTypes().length==0 && candidates.size()>0){
			List<String> entities = new ArrayList<String>();
			for(ReconciliationCandidate candidate:candidates){
				entities.add(candidate.getId());
			}
			String typeSparql = this.queryFactory.getTypesOfEntitiesQuery(ImmutableList.copyOf(entities));
			ResultSet typeResultSet = this.queryExecutor.sparql(typeSparql);
			Multimap<String, String> typesMap = this.queryFactory.wrapTypesOfEntities(typeResultSet);
			for(ReconciliationCandidate candidate:candidates){
				candidate.setTypes(typesMap.get(candidate.getId()).toArray(new String[]{}));
			}
		}
		long time = System.currentTimeMillis() - start;
		logger.debug("reconciling " + request.getQueryString() + " took " + time + " milliseconds");
		return candidates;
	}

	
	@Override
	public void save(String serviceId, FileOutputStream out) throws IOException{
		queryExecutor.save(serviceId, out);
	}

	@Override
	public void write(JSONWriter writer)throws JSONException {
		writer.object();
		writer.key("type"); writer.value(getType());
		writer.key("queryFactory"); this.queryFactory.write(writer);
		writer.key("queryExecutor"); this.queryExecutor.write(writer);
		writer.endObject();
	}
	
	@Override
	public ImmutableList<SearchResultItem> suggestType(String prefix, int limit) {
		String sparql = this.queryFactory.getTypeSuggestSparqlQuery(prefix, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return queryFactory.wrapTypeSuggestResultSet(resultSet, prefix, limit);
	}

	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, String typeUri, int limit) {
		String sparql = this.queryFactory.getPropertySuggestSparqlQuery(prefix, typeUri, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return queryFactory.wrapPropertySuggestResultSet(resultSet, prefix, limit);
	}

	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, int limit) {
		String sparql = this.queryFactory.getPropertySuggestSparqlQuery(prefix, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return queryFactory.wrapPropertySuggestResultSet(resultSet, prefix, limit);
	}

	
	@Override
	public ImmutableList<SearchResultItem> getSampleInstances(String typeUri, ImmutableList<String> searchPropertyUris, int limit) {
		String sparql = this.queryFactory.getSampleInstancesSparqlQuery(typeUri, searchPropertyUris, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return this.queryFactory.wrapSampleInstancesResultSet(resultSet, typeUri, searchPropertyUris, limit);
	}
	
	@Override
	public ImmutableList<String[]> getSampleValuesOfProperty(String propertyUri, int limit) {
		String sparql = this.queryFactory.getSampleValuesOfPropertySparqlQuery(propertyUri, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return this.queryFactory.wrapSampleValuesOfPropertyResultSet(resultSet, propertyUri, limit);
	}

	@Override
	public Multimap<String, String> getResourcePropertiesMap(String resourceUri, int limit) {
		String sparql = this.queryFactory.getResourcePropertiesMapSparqlQuery(resourceUri, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return this.queryFactory.wrapResourcePropertiesMapResultSet(resultSet, resourceUri, limit);
	}

	
	@Override
	public Multimap<String, String> getResourcePropertiesMap(PreviewResourceCannedQuery cannedQuery, String resourceUri) {
		String sparql = this.queryFactory.getResourcePropertiesMapSparqlQuery(cannedQuery,resourceUri);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return this.queryFactory.wrapResourcePropertiesMapResultSet(cannedQuery, resultSet);
	}

	@Override
	public ImmutableList<SearchResultItem> searchForEntities(String prefix, ImmutableList<String> searchPropertyUris, int limit) {
		String sparql = this.queryFactory.getEntitySearchSparqlQuery(prefix, searchPropertyUris, limit);
		ResultSet resultSet = this.queryExecutor.sparql(sparql);
		return this.queryFactory.wrapEntitySearchResultSet(resultSet, limit);
	}
	
	@Override
	public void initialize(FileInputStream in){
		queryExecutor.initialize(in);
	}
	//String key to identify the type of the Query Endpoint
	protected String getType(){
		return "default";
	}
}
