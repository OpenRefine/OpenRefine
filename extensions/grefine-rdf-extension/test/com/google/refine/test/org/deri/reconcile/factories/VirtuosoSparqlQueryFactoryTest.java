package com.google.refine.test.org.deri.reconcile.factories;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.IdentifiedValueContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.PropertyContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.TextualValueContext;
import com.google.refine.org.deri.reconcile.rdf.factories.SparqlQueryFactory;
import com.google.refine.org.deri.reconcile.rdf.factories.VirtuosoSparqlQueryFactory;

public class VirtuosoSparqlQueryFactoryTest {

	int limit = 10;
	String query = "United  States";
	String formattedQuery = "+United +States";
	ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
	SparqlQueryFactory factory ;
	
	@BeforeMethod
	public void setUp(){
		factory = new VirtuosoSparqlQueryFactory();
	}
	
	@Test
	public void queryForReconcileTest(){
		int computedLimit = limit * 2;
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label","http://www.w3.org/2004/02/skos/core#prefLabel");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"SELECT DISTINCT ?entity ?label ?score1 " +
			"WHERE" +
			"{" +
			"?entity ?p ?label. " +
			"?label <bif:contains> \"'" + formattedQuery + "'\" OPTION(score ?score1). " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"FILTER isIRI(?entity). } ORDER BY desc(?score1) LIMIT " + computedLimit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void queryWithTypeForReconcileTest(){
		int computedLimit = limit * 2;
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://xmlns.com/foaf/0.1/Agent","http://xmlns.com/foaf/0.1/Person"});
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"SELECT DISTINCT ?entity ?label ?score1 " +
			"WHERE" +
			"{" +
			"?entity ?p ?label. " +
			"?label <bif:contains> \"'" + formattedQuery + "'\" OPTION(score ?score1). " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label>). " +
			"?entity a ?type. " +
			"FILTER (?type IN (<http://xmlns.com/foaf/0.1/Agent>,<http://xmlns.com/foaf/0.1/Person>)). " + 
			"FILTER isIRI(?entity). } ORDER BY desc(?score1) LIMIT " + computedLimit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsWithTypeWithContextReconciliationTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://xmlns.com/foaf/0.1/Person" , "http://example.org/ontology/Person"});
		
		PropertyContext prop1 = new PropertyContext("http://example.org/ontology/worksFor", new IdentifiedValueContext("http://example.org/resource/DERI"));
		PropertyContext prop2 = new PropertyContext("http://xmlns.com/foaf/0.1/nick", new TextualValueContext("fadmaa"));
		request.setContext(new ReconciliationRequestContext(prop1, prop2));
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2004/02/skos/core#prefLabel");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"SELECT DISTINCT ?entity ?label ?score1 " +
			"WHERE{" +
			"?entity ?p ?label. " +
			"?label <bif:contains> \"'+United +States'\" OPTION(score ?score1). " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"?entity a ?type. " +
			"FILTER (?type IN (<http://xmlns.com/foaf/0.1/Person>,<http://example.org/ontology/Person>)). " +
			"?entity <http://example.org/ontology/worksFor> <http://example.org/resource/DERI>. " +
			"?entity <http://xmlns.com/foaf/0.1/nick> 'fadmaa'. " +
			"FILTER isIRI(?entity). " +
			"} ORDER BY desc(?score1) LIMIT 20";
		
		assertEquals(sparql, expected);
	}
	
	
	/*
	 * SUGGEST TYPE TESTS
	 */

	@Test
	public void suggestTypeTest(){
		String prefix = "Pers";
		String sparql = factory.getTypeSuggestSparqlQuery(prefix, limit);
		
		String expected =
			"SELECT DISTINCT ?type ?label ?score1 " +
			"WHERE " +
			"{" +
			"[] a ?type. " +
			"?type ?label_prop ?label. " +
			"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"?label <bif:contains> \"'+" + prefix + "*'\" OPTION(score ?score1). " +
			"} ORDER BY desc(?score1) LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}

	/*
	 * SUGGEST PROPERTY TESTS
	 */

	@Test
	public void suggestPropertyWithSpecificSiubjectsTypeTest(){
		String prefix = "labe";
		String typeUri = "http://xmlns.com/foaf/0.1/Person";
		String sparql = factory.getPropertySuggestSparqlQuery(prefix, typeUri, limit);
		
		String expected =
			"SELECT DISTINCT ?p ?label ?score1 " +
			"WHERE " +
			"{" +
			"[] a <http://xmlns.com/foaf/0.1/Person>; " +
			"?p ?v. " +
			"?p ?label_prop ?label. " +
			"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"?label <bif:contains> \"'+" +prefix + "*'\" OPTION(score ?score1). " +
			"} ORDER BY desc(?score1) LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void suggestPropertyTest(){
		String prefix = "labe";
		String sparql = factory.getPropertySuggestSparqlQuery(prefix, limit);
		
		String expected =
			"SELECT DISTINCT ?p ?label ?score1 " +
			"WHERE " +
			"{" +
			"[] ?p ?v. " +
			"?p ?label_prop ?label. " +
			"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"?label <bif:contains> \"'+" +prefix + "*'\" OPTION(score ?score1). " +
			"} ORDER BY desc(?score1) LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}
	
	/*
	 * ENTITY SEARCH
	 */

	@Test 
	public void entitySearchTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2004/02/skos/core#prefLabel");
		String prefix = "fad";
		String sparql = factory.getEntitySearchSparqlQuery(prefix,searchPropertyUris, 10);
		String expected = 
			"SELECT DISTINCT ?entity ?label " +
			"WHERE" +
			"{" +
			"?entity ?p ?label. " +
			"?label <bif:contains> \"'+" + prefix + "*'\" OPTION(score ?score1). " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"FILTER isIRI(?entity). } ORDER BY desc(?score1) LIMIT " + limit
			;
		assertEquals(sparql, expected);
	}

}
