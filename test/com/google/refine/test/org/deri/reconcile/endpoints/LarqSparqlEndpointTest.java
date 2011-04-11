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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationCandidate;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.SearchResultItem;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpoint;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpointImpl;
import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.LarqSparqlQueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author fadmaa
 * this class mainly assures that {org.deri.refine.reconcile.rdf.endpoints.QueryEndpointImpl QueryEndpointImpl} uses 
 * {org.deri.refine.reconcile.rdf.factories.LarqSparqlQueryFactory LarqSparqlQueryFactory} 
 * and {org.deri.refine.reconcile.rdf.executors.DumpQueryExecutor DumpQueryExecutor} correctly. i.e. queries are correct, executed and result is wrapped correctly.
 */
public class LarqSparqlEndpointTest {

	QueryExecutor executor;
	LarqSparqlQueryFactory factory;
	QueryEndpoint endpoint;
	
	//query
	int limit =3;
	String queryString = "godfather";
	String exactMatchQueryString = "Godfather";
	ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label",
																"http://www.w3.org/2004/02/skos/core#prefLabel");
	
	@BeforeClass
	public void init(){
		Model m = ModelFactory.createDefaultModel();
		InputStream in = this.getClass().getResourceAsStream("../files/films.ttl");
		m.read(in,null,"TTL");
		
		executor = new DumpQueryExecutor(m);
		factory = new LarqSparqlQueryFactory();
		endpoint = new QueryEndpointImpl(factory, executor);
	}
	
	@Test
	public void executeSimpleReconciliationQuery(){
		ReconciliationRequest request = new ReconciliationRequest(queryString, 6);
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 0.8);
		assertTrue(candidates.size()<=6);
		assertResultInOrder(null,candidates,"http://data.linkedmdb.org/resource/film/930","http://data.linkedmdb.org/resource/film/329",
			"http://data.linkedmdb.org/resource/film_series/261");
	}
	
	@Test
	public void noMoreThanOneMatch(){
		ReconciliationRequest request = new ReconciliationRequest("Anjali", limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		List<ReconciliationCandidate> candidates = endpoint.reconcileEntities(request, searchPropertyUris, 0.6);
		assertTrue(candidates.size()<=limit);
		assertResultMoreThanThresholdButNoMatch(candidates,0.6,"http://data.linkedmdb.org/resource/film/2410","http://data.linkedmdb.org/resource/film/test/2410");
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
	
	private void assertResultMoreThanThresholdButNoMatch(List<ReconciliationCandidate> candidates, double threshold, String... containedUri){
		Set<String> urisSet = new HashSet<String>(Arrays.asList(containedUri));
		for(ReconciliationCandidate candidate: candidates){
			String uri = candidate.getId();
			if(urisSet.contains(uri)){
				assertTrue(candidate.getScore()>threshold);
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
		LarqSparqlQueryFactory factory = new LarqSparqlQueryFactory();
		
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
		assertEquals(results.size(),2);
		SearchResultItem item;
		item = results.get(0);
		assertResultItem(item,"http://data.linkedmdb.org/resource/movie/film","film");
		item = results.get(1);
		assertResultItem(item,"http://data.linkedmdb.org/resource/movie/film_series","a film series");
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

	private void assertResultItem(SearchResultItem item, String expectedId, String expectedName){
		assertEquals(item.getId(), expectedId);
		assertEquals(item.getName(), expectedName);
	}
	
	/*
	 * sample instances
	 */
	@Test
	public void sampleInstancesTest(){
		List<SearchResultItem> items = endpoint.getSampleInstances("http://data.linkedmdb.org/resource/movie/fakeFilm", 
				ImmutableList.of("http://www.w3.org/2004/02/skos/core#prefLabel","http://www.w3.org/2000/01/rdf-schema#label"), limit);
		assertEquals(items.size(),1);
		assertEquals(items.get(0).getName(),"The Prefered Label");
	}
	
	/*
	 * search entites
	 */
	@Test
	public void searchEntitiesTest(){
		String prefix = "godf";
		List<SearchResultItem> items = endpoint.searchForEntities(prefix, searchPropertyUris, limit);
		assertEquals(items.size(), 3);
	}
}
