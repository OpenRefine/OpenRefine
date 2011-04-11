package com.google.refine.org.deri.reconcile.rdf.endpoints;

import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.LarqSparqlQueryFactory;
import com.google.refine.org.deri.reconcile.rdf.factories.SparqlQueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

public class QueryEndpointFactory {

	public QueryEndpoint getLarqQueryEndpoint(Model model){
		SparqlQueryFactory queryFactory = new LarqSparqlQueryFactory();
		QueryExecutor queryExecutor = new DumpQueryExecutor(model);
		return new QueryEndpointImpl(queryFactory, queryExecutor);
	}
}
