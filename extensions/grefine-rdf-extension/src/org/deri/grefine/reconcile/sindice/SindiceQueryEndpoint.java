package org.deri.grefine.reconcile.sindice;

import java.util.ArrayList;
import java.util.List;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.rdf.executors.DumpQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class SindiceQueryEndpoint {

	private SparqlQueryFactory queryFactory;
	
	public SindiceQueryEndpoint(SparqlQueryFactory factory){
		this.queryFactory = factory;
	}
	
	public boolean hasResult(Model model, String queryString, ImmutableList<String> searchPropertyUris, int limit) {
		//reconcile then get types for each element in the reconciliation result assure that order is preserved
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		QueryExecutor queryExecutor = new DumpQueryExecutor(model);
		List<ReconciliationCandidate> candidates = reconcileEntities(queryExecutor, request, searchPropertyUris, 2.0);
		return ! candidates.isEmpty();
	}
	
	public LinkedHashMultimap<String, String> guessType(Model model, String queryString, ImmutableList<String> searchPropertyUris, int limit) {
		//reconcile then get types for each element in the reconciliation result assure that order is preserved
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		QueryExecutor queryExecutor = new DumpQueryExecutor(model);
		List<ReconciliationCandidate> candidates = reconcileEntities(queryExecutor, request, searchPropertyUris, 2.0);
		List<String> entities = new ArrayList<String>();
		for(ReconciliationCandidate candidate:candidates){
			entities.add(candidate.getId());
		}
		if(entities.isEmpty()){
			//return empty result
			return LinkedHashMultimap.create();
		}
		String sparql = this.queryFactory.getTypesOfEntitiesQuery(ImmutableList.copyOf(entities));
		ResultSet resultSet = queryExecutor.sparql(sparql);
		Multimap<String, String> typesMap = this.queryFactory.wrapTypesOfEntities(resultSet);
		//order
		LinkedHashMultimap<String, String> result = LinkedHashMultimap.create();
		for(String uri:entities){
			result.putAll(uri, typesMap.get(uri));
		}
		return result;
	}
	
	private List<ReconciliationCandidate> reconcileEntities(QueryExecutor queryExecutor, ReconciliationRequest request, ImmutableList<String> searchPropertyUris, double matchThreshold) {
		String sparql = this.queryFactory.getReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultSet = queryExecutor.sparql(sparql);
		List<ReconciliationCandidate> candidates = this.queryFactory.wrapReconciliationResultset(resultSet, request.getQueryString(), searchPropertyUris, request.getLimit(), matchThreshold);
		return candidates;
	}

}
