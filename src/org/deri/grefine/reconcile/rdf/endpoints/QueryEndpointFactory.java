package org.deri.grefine.reconcile.rdf.endpoints;

import org.deri.grefine.reconcile.rdf.executors.DumpQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.JenaTextSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class QueryEndpointFactory {

	public QueryEndpoint getLarqQueryEndpoint(Model model){
		SparqlQueryFactory queryFactory = new JenaTextSparqlQueryFactory();
		QueryExecutor queryExecutor = new DumpQueryExecutor(model);
		return new QueryEndpointImpl(queryFactory, queryExecutor);
	}
}
