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
import com.google.refine.org.deri.reconcile.rdf.factories.BigOwlImSparqlQueryFactory;
import com.google.refine.org.deri.reconcile.rdf.factories.SparqlQueryFactory;

public class BigOwlImSparqlQueryFactoryTest {

	int limit = 10;
	String query = "Fadi Maali";
	SparqlQueryFactory factory ;
	
	@BeforeMethod
	public void setUp(){
		factory = new BigOwlImSparqlQueryFactory();
	}
	
	@Test
	public void oneLabelTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		//this will assure that empty string for type is ignored
		request.setTypes(new String[] {""});
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
			"SELECT ?entity ?label " +
			"WHERE" +
			"{" +
			    "?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label. " +
			    "?entity <http://www.ontotext.com/luceneQuery> \"" + query + "\". " +
			    "FILTER (isIRI(?entity))" + 
			"}LIMIT "  + String.valueOf(limit * searchPropertyUris.size());
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void oneLabelWithTypeTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		//this will assure that empty string for type is ignored
		request.setTypes(new String[] {"http://xmlns.com/foaf/0.1/Person" , "http://example.org/ontology/Person"});
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
			"SELECT ?entity ?label " +
			"WHERE" +
			"{" +
			    "?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label. " +
			    "?entity <http://www.ontotext.com/luceneQuery> \"" + query + "\". " +
			    "{" +
		            "{?entity rdf:type <http://xmlns.com/foaf/0.1/Person>. } " +
		            "UNION " +
		            "{?entity rdf:type <http://example.org/ontology/Person>. }" + 
		        "} " + 
			    "FILTER (isIRI(?entity))" + 
			"}LIMIT "  + String.valueOf(limit * searchPropertyUris.size());
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void oneLabelWithTypeWithContextTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		//this will assure that empty string for type is ignored
		request.setTypes(new String[] {"http://xmlns.com/foaf/0.1/Person" , "http://example.org/ontology/Person"});
		PropertyContext prop1 = new PropertyContext("http://example.org/ontology/worksFor", new IdentifiedValueContext("http://example.org/resource/DERI"));
		PropertyContext prop2 = new PropertyContext("http://xmlns.com/foaf/0.1/nick", new TextualValueContext("fadmaa"));
		request.setContext(new ReconciliationRequestContext(prop1, prop2));
		
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
			"SELECT ?entity ?label " +
			"WHERE" +
			"{" +
			    "?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label. " +
			    "?entity <http://www.ontotext.com/luceneQuery> \"" + query + "\". " +
			    "{" +
		            "{?entity rdf:type <http://xmlns.com/foaf/0.1/Person>. } " +
		            "UNION " +
		            "{?entity rdf:type <http://example.org/ontology/Person>. }" + 
		        "}" +
		        "?entity <http://example.org/ontology/worksFor> <http://example.org/resource/DERI>. " +
		        "?entity <http://xmlns.com/foaf/0.1/nick> 'fadmaa'. " + 
			    " FILTER (isIRI(?entity))" + 
			"}LIMIT "  + String.valueOf(limit * searchPropertyUris.size());
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label" ,
				"http://www.w3.org/2004/02/skos/core#prefLabel");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
			"SELECT ?entity ?label " +
			"WHERE" +
			"{" +
			    "?entity ?p ?label. " +
			    "?entity <http://www.ontotext.com/luceneQuery> \"" + query + "\". " +
			    "FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>) " +
			    "FILTER (isIRI(?entity))" + 
			"}LIMIT "  + String.valueOf(limit * searchPropertyUris.size());
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsWithTypeTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://xmlns.com/foaf/0.1/Person" });
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label" ,
				"http://www.w3.org/2004/02/skos/core#prefLabel");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
			"SELECT ?entity ?label " +
			"WHERE" +
			"{" +
			    "?entity ?p ?label. " +
			    "?entity <http://www.ontotext.com/luceneQuery> \"" + query + "\". " +
			    "FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>) " +
			    "{" +
		            "{?entity rdf:type <http://xmlns.com/foaf/0.1/Person>. }" +
		        "} " + 
			    "FILTER (isIRI(?entity))" + 
			"}LIMIT "  + String.valueOf(limit * searchPropertyUris.size());
		
		assertEquals(sparql, expected);
	}
	
	
	/*
	 * SUGGEST TYPE TESTS
	 */

	@Test
	public void suggestTypeTest(){
		String prefix = "Person";
		String sparql = factory.getTypeSuggestSparqlQuery(prefix, limit);
		
		String expected =
			"SELECT DISTINCT ?type ?label1 " +
			"WHERE{" +
			"[] a ?type. " +
			"?type <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"?type <http://www.ontotext.com/luceneQuery> \"" + prefix + "*\". " +  
			"} LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}
	
	/*
	 * SUGGEST PROPERTY TESTS
	 */

	@Test
	public void suggestPropertyTest(){
		String prefix = "firstn";
		String typeUri = "http://xmlns.com/foaf/0.1/Person"; 
		String sparql = factory.getPropertySuggestSparqlQuery(prefix, typeUri, limit);
		
		String expected =
			"SELECT DISTINCT ?p ?label1 " +
			"WHERE{" +
			"[] a <" + typeUri + ">;" +
			"?p ?o. " +
			"?p <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"?p <http://www.ontotext.com/luceneQuery> \"" + prefix + "*\". " +  
			"} LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}

}
