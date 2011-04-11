package com.google.refine.org.deri.reconcile.rdf.endpoints;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationCandidate;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.PlainSparqlQueryFactory;
import com.hp.hpl.jena.query.ResultSet;

public class PlainSparqlQueryEndpoint extends QueryEndpointImpl {

	private PlainSparqlQueryFactory plainQueryFactory;
	public PlainSparqlQueryEndpoint(PlainSparqlQueryFactory queryFactory, QueryExecutor queryExecutor) {
		super(queryFactory, queryExecutor);
		plainQueryFactory = queryFactory;
		
	}

	/* (non-Javadoc)
	 * @see com.google.refine.com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpointImpl#reconcileEntities(com.google.refine.com.google.refine.org.deri.reconcile.model.ReconciliationRequest, com.google.common.collect.ImmutableList)
	 * 
	 * this method will try first to search for exact match...if it succeeds the return results. if not, it resorts to the 
	 * more costly method define in its superclass 
	 */
	@Override
	public List<ReconciliationCandidate> reconcileEntities(ReconciliationRequest request, ImmutableList<String> searchPropertyUris, double matchThreshold) {
		String sparql = plainQueryFactory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultSet = queryExecutor.sparql(sparql);
		List<ReconciliationCandidate> candidates = plainQueryFactory.wrapResultset(resultSet, request.getQueryString(),matchThreshold);
		if(candidates.size()>0){
			return candidates;
		}
		return super.reconcileEntities(request, searchPropertyUris, matchThreshold);
	}

	@Override
	protected String getType() {
		return "plain";
	}

	
}
