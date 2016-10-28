package com.google.refine.test.org.deri.reconcile.executors;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.IdentifiedValueContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.PropertyContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.TextualValueContext;
import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.PlainSparqlQueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class PlainSparqlQueryExecutorTest {

	QueryExecutor executor;
	PlainSparqlQueryFactory factory;
	
	//query
	int limit =8;
	String queryString = "godfather";
	String exactMatchQueryString = "Godfather";
	ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label",
														"http://www.w3.org/2004/02/skos/core#prefLabel");
	
	@BeforeClass
	public void setUp(){
		Model m = ModelFactory.createDefaultModel();
		InputStream in = this.getClass().getResourceAsStream("../files/films.ttl");
		m.read(in,null,"TTL");
		
		executor = new DumpQueryExecutor(m);
		factory = new PlainSparqlQueryFactory();
	}
	
	@Test
	public void executeSimpleReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultset = executor.sparql(sparql);
		assertResult(null,resultset,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329",
		"http://data.linkedmdb.org/resource/film_series/261");
	}
	
	@Test
	public void executeOneLabelOneTypeReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getReconciliationSparqlQuery(request, ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label"));
		ResultSet resultset = executor.sparql(sparql);
		assertResult(null,resultset,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329");
	}
	
	@Test
	public void executeReconciliationQueryWithType(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultset = executor.sparql(sparql);
		assertResult("http://data.linkedmdb.org/resource/film_series/261", resultset,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329");
	}
	
	@Test
	public void executeSimpleExactMatchReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultset = executor.sparql(sparql);
		assertResult("http://data.linkedmdb.org/resource/film/329",resultset,"http://data.linkedmdb.org/resource/film/930");
	}
	
	@Test
	public void executeOneLabelOneTypeExactMatchReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label"));
		ResultSet resultset = executor.sparql(sparql);
		assertResult("http://data.linkedmdb.org/resource/film/329",resultset,"http://data.linkedmdb.org/resource/film/930");
	}
	
	@Test
	public void executeExactMatchReconciliationQueryWithType(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultset = executor.sparql(sparql);
		assertResult("http://data.linkedmdb.org/resource/film_series/261", resultset,"http://data.linkedmdb.org/resource/film/930");
	}

	@Test
	public void executeExactMatchWithContextReconciliationQueryWithType(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		PropertyContext prop1 = new PropertyContext("http://example.org/ontology/worksFor", new IdentifiedValueContext("http://example.org/resource/DERI"));
		PropertyContext prop2 = new PropertyContext("http://xmlns.com/foaf/0.1/nick", new TextualValueContext("fadmaa"));
		request.setContext(new ReconciliationRequestContext(prop1, prop2));
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		ResultSet resultset = executor.sparql(sparql);
		assertTrue(!resultset.hasNext());//empty result
	}
	
	private void assertResult(String notContainedUri, ResultSet resultset, String... containedUris){
		Set<String> urisSet = new HashSet<String>(Arrays.asList(containedUris));
		while(resultset.hasNext()){
			QuerySolution solution = resultset.nextSolution();
			Resource r = solution.getResource("entity");
			String uri = r.getURI();
			urisSet.remove(uri);
			if(uri.equals(notContainedUri)){
				fail(notContainedUri + " was found in the result") ;
			}
		}
		
		assertTrue(urisSet.isEmpty(), urisSet + " were not found in the result");
	}
	
	
	/*
	 * Suggest type 
	 */
	@Test
	public void sugestTypeTest(){
		String prefix = "fil";
		String sparql = factory.getTypeSuggestSparqlQuery(prefix, limit);
		ResultSet resultset = executor.sparql(sparql);
		assertResultsetExact("type",resultset,"http://data.linkedmdb.org/resource/movie/film");
	}
	
	private void assertResultsetExact(String propertyName, ResultSet resultset, String... containedUris){
		Set<String> urisSet = new HashSet<String>(Arrays.asList(containedUris));
		while(resultset.hasNext()){
			QuerySolution solution = resultset.nextSolution();
			Resource r = solution.getResource(propertyName);
			String uri = r.getURI();
			assertTrue(urisSet.remove(uri));
		}
		
		assertTrue(urisSet.isEmpty(), urisSet + " were not found in the result");
	}
	
	/*
	 * Suggest property 
	 */
	@Test
	public void sugestPropertyTest(){
		String prefix = "init";
		String sparql = factory.getPropertySuggestSparqlQuery(prefix, "http://data.linkedmdb.org/resource/movie/film", limit);
		ResultSet resultset = executor.sparql(sparql);
		assertResultsetExact("p", resultset,"http://data.linkedmdb.org/resource/movie/initial_release_date");
	}
	
	/*
	 * search entity
	 */
	@Test
	public void searchEntityTest(){
		String prefix = "godf";
		String sparql = factory.getEntitySearchSparqlQuery(prefix, searchPropertyUris, limit);
		ResultSet resultset = executor.sparql(sparql);
		assertResult(null,resultset,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329",
		"http://data.linkedmdb.org/resource/film_series/261");
	}
}
