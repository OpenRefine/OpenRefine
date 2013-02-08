package com.google.refine.test.org.deri.reconcile.endpoints;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationCandidate;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.SearchResultItem;
import com.google.refine.org.deri.reconcile.rdf.endpoints.PlainSparqlQueryEndpoint;
import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.PlainSparqlQueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class PlainSparqlEndpointTest {

	QueryExecutor executor;
	PlainSparqlQueryFactory factory;
	PlainSparqlQueryEndpoint endpoint;
	
	//query
	int limit =8;
	String queryString = "godfather";
	String exactMatchQueryString = "Godfather";
	ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label",
																"http://www.w3.org/2004/02/skos/core#prefLabel");
	
	@BeforeMethod
	public void setUp(){
		Model m = ModelFactory.createDefaultModel();
		InputStream in = this.getClass().getResourceAsStream("../files/films.ttl");
		m.read(in,null,"TTL");
		
		executor = new DumpQueryExecutor(m);
		factory = new PlainSparqlQueryFactory();
		endpoint = new PlainSparqlQueryEndpoint(factory, executor);
	}
	
	@Test
	public void executeSimpleReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 1.0);
		assertResultInOrder(null,candidates,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329",
			"http://data.linkedmdb.org/resource/film_series/261");
	}
	
	@Test
	public void executeSimpleReconciliationQueryWithType(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 1.0);
		assertResultInOrder("http://data.linkedmdb.org/resource/film_series/261",candidates,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329");
	}
	
	@Test
	public void executeExactMatchReconciliationQueryWithType(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 1.0);
		assertTrue(candidates.size()>0);
		asserrtResultContainsWithScoreOne(candidates,"http://data.linkedmdb.org/resource/film/930");
	}
	
	@Test
	public void executeExactMatchReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(exactMatchQueryString, limit);
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 1.0);
		asserrtResultContainsWithScoreOne(candidates,"http://data.linkedmdb.org/resource/film/930");
	}
	
	@Test
	public void noMoreThanOneMatch(){
		ReconciliationRequest request = new ReconciliationRequest("Anjali", limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 1.0);
		assertResultContainsWithScoreOneButNoMatch(candidates,"http://data.linkedmdb.org/resource/film/2410","http://data.linkedmdb.org/resource/film/test/2410");
	}
	
	private void assertResultInOrder(String notContainedUri, List<ReconciliationCandidate> candidates, String... containedUris){
		List<String> urisList = new ArrayList<String>(Arrays.asList(containedUris));
		List<String> actuals = new ArrayList<String>();
		for(ReconciliationCandidate candidate: candidates){
			String uri = candidate.getId();
			actuals.add(uri);
			if(urisList.get(0).equals(uri)){
				urisList.remove(0);
				if(urisList.isEmpty()){
					//all are removed ==> success
					break;
				}
			}
			if(uri.equals(notContainedUri)){
				fail(notContainedUri + " was found in the result") ;
			}
		}
		
		assertTrue(urisList.isEmpty(), urisList + " were not found in the result or were not in the correct order we got this list instead " + actuals);
	}
	
	private void asserrtResultContainsWithScoreOne(List<ReconciliationCandidate> candidates, String containedUri){
		
		for(ReconciliationCandidate candidate: candidates){
			String uri = candidate.getId();
			if(uri.equals(containedUri)){
				if(candidate.equals(uri)){
					//make sure the score is one
					assertEquals(candidate.getScore(), 1.0d);
				}
				return;
			}
		}
		fail(containedUri + " was not found!");
	}
	
	private void assertResultContainsWithScoreOneButNoMatch(List<ReconciliationCandidate> candidates, String... containedUri){
		Set<String> urisSet = new HashSet<String>(Arrays.asList(containedUri));
		for(ReconciliationCandidate candidate: candidates){
			String uri = candidate.getId();
			if(urisSet.contains(uri)){
				//assure score = 1 and match = false
				assertEquals(candidate.getScore(), 1.0d);
				assertFalse(candidate.isMatch());
				
				urisSet.remove(uri);
			}
		}
		assertTrue(urisSet.isEmpty());
	}
	
	/*
	 * Guess type
	 */
	/*@Test
	public void guessTypeTest(){
		Model m = ModelFactory.createDefaultModel();
		InputStream in = this.getClass().getResourceAsStream("../files/sample.ttl");
		m.read(in,null,"TTL");
		
		DumpQueryExecutor executor = new DumpQueryExecutor(m);
		PlainSparqlQueryFactory factory = new PlainSparqlQueryFactory();
		
		QueryEndpoint endpoint = new QueryEndpointImpl(factory, executor);
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		
		LinkedHashMultimap<String, String> typesMap = endpoint.guessType("sample", searchPropertyUris, 3);
		//TODO testing.. the dump way
		Set<String> keys = typesMap.keySet();
		//order is significant, thus toArray
		assertEquals(keys.toArray(new String[keys.size()]), new String[] {"http://example.org/resource/film/137", 
																"http://example.org/resource/film/200",
																"http://example.org/resource/film/110"});
		
		//for types, order is insignificant
		Set<String> expectedTypes = new HashSet<String>();
		expectedTypes.add("http://example.org/sample/Movie");expectedTypes.add("http://data.linkedmdb.org/resource/movie/film");
		assertEquals(typesMap.get("http://example.org/resource/film/137"),expectedTypes);
		
		expectedTypes = new HashSet<String>();
		expectedTypes.add("http://data.linkedmdb.org/resource/movie/film_series");
		assertEquals(typesMap.get("http://example.org/resource/film/200"),expectedTypes);
		
		expectedTypes = new HashSet<String>();
		expectedTypes.add("http://example.org/sample/Show");expectedTypes.add("http://data.linkedmdb.org/resource/movie/film");
		assertEquals(typesMap.get("http://example.org/resource/film/110"),expectedTypes);
	}*/
	
	/*
	 * suggest type
	 */
	@Test
	public void sugestTypeTest(){
		String prefix = "fil";
		List<SearchResultItem> results = endpoint.suggestType(prefix, limit);
		assertEquals(results.size(),1);
		SearchResultItem item;
		item = results.get(0);
		assertResultItem(item,"http://data.linkedmdb.org/resource/movie/film","film");
	}
	
	private void assertResultItem(SearchResultItem item, String expectedId, String expectedName){
		assertEquals(item.getId(), expectedId);
		assertEquals(item.getName(), expectedName);
	}
	
	/*
	 * suggest property
	 */
	@Test
	public void suggestPropertyTest(){
		String prefix = "initi";
		List<SearchResultItem> results = endpoint.suggestProperty(prefix, limit);
		assertEquals(results.size(),1);
		SearchResultItem item;
		item = results.get(0);
		assertResultItem(item,"http://data.linkedmdb.org/resource/movie/initial_release_date","initial release date");
	}
	
	@Test
	public void suggestPropertyEmptyResultTest(){
		String prefix = "initi";
		String typeUri = "http://data.linkedmdb.org/resource/movie/film_series";
		List<SearchResultItem> results = endpoint.suggestProperty(prefix, typeUri, limit);
		assertEquals(results.size(),0);
	}
}
