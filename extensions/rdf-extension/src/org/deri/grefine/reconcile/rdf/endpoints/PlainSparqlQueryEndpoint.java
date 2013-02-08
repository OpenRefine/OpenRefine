package org.deri.grefine.reconcile.rdf.endpoints;

import java.util.ArrayList;
import java.util.List;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.PlainSparqlQueryFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.ResultSet;

public class PlainSparqlQueryEndpoint extends QueryEndpointImpl {

	private PlainSparqlQueryFactory plainQueryFactory;
	public PlainSparqlQueryEndpoint(PlainSparqlQueryFactory queryFactory, QueryExecutor queryExecutor) {
		super(queryFactory, queryExecutor);
		plainQueryFactory = queryFactory;
		
	}

	/* (non-Javadoc)
	 * @see org.deri.grefine.reconcile.rdf.endpoints.QueryEndpointImpl#reconcileEntities(com.google.refine.com.google.refine.org.deri.reconcile.model.ReconciliationRequest, com.google.common.collect.ImmutableList)
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
			return candidates;
		}
		return super.reconcileEntities(request, searchPropertyUris, matchThreshold);
	}

	@Override
	protected String getType() {
		return "plain";
	}

	
}
