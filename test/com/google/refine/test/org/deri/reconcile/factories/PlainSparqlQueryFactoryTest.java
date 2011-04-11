package com.google.refine.test.org.deri.reconcile.factories;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.IdentifiedValueContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.PropertyContext;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequestContext.TextualValueContext;
import com.google.refine.org.deri.reconcile.rdf.factories.PlainSparqlQueryFactory;

public class PlainSparqlQueryFactoryTest {
	
	PlainSparqlQueryFactory factory = new PlainSparqlQueryFactory();
	int limit = 3;
	String query = "The Godfather";
	
	@Test
	public void oneLabelSimpleReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity ?label1 " +
			"WHERE" +
			"{" +
			"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + query + "','i'). " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void oneLabelOneTypeReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity ?label1 " +
			"WHERE" +
			"{" +
			"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + query + "','i'). " +
			"?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsWithTypesReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://purl.org/dc/terms/title");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film", "http://data.linkedmdb.org/resource/movie/film_series"});
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity ?label1 ?label2 " +
			"WHERE" +
			"{" +
			"{" +
			"OPTIONAL{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + query + "','i')" +
			"}" +
			"OPTIONAL{ ?entity <http://purl.org/dc/terms/title> ?label2. " +
			"FILTER regex(str(?label2),'" + query + "','i')" +
			"}" +
			"FILTER ( bound(?label1) || bound(?label2))" +
			"}" +
			"{" +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. } " +
			"UNION " +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film_series>. } " + 
			"}" +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsSimpleReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://purl.org/dc/terms/title");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		//this will assure that empty string for type is ignored
		request.setTypes(new String[] {""});
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity ?label1 ?label2 " +
			"WHERE" +
			"{" +
			"{" +
			"OPTIONAL{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + query + "','i')" +
			"}" +
			"OPTIONAL{ ?entity <http://purl.org/dc/terms/title> ?label2. " +
			"FILTER regex(str(?label2),'" + query + "','i')" +
			"}" +
			"FILTER ( bound(?label1) || bound(?label2))" +
			"}" +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void oneLabelOneTypeExactMatchReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity " +
			"WHERE" +
			"{" +
			"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (str(?label) = '" + query + "'). " +
			"?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsWithTypesExactMatchReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://purl.org/dc/terms/title");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film", "http://data.linkedmdb.org/resource/movie/film_series"});
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity " +
			"WHERE" +
			"{" +
			"{" +
			"{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. FILTER (str(?label1) = '" + query + "'). " +
			"}UNION " +
			"{ ?entity <http://purl.org/dc/terms/title> ?label2. FILTER (str(?label2) = '" + query + "'). " +
			"}" +
			"}" +
			"{" +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. } " +
			"UNION " +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film_series>. } " + 
			"}" +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsExactMatchReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://purl.org/dc/terms/title");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity " +
			"WHERE" +
			"{" +
			"{" +
			"{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. FILTER (str(?label1) = '" + query + "'). " +
			"}UNION " +
			"{ ?entity <http://purl.org/dc/terms/title> ?label2. FILTER (str(?label2) = '" + query + "'). " +
			"}" +
			"}" +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void multiLabelsWithTypeWithContextReconciliationTest(){
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film" , 
										"http://data.linkedmdb.org/resource/movie/film_series"});
		
		PropertyContext prop1 = new PropertyContext("http://example.org/ontology/worksFor", new IdentifiedValueContext("http://example.org/resource/DERI"));
		PropertyContext prop2 = new PropertyContext("http://xmlns.com/foaf/0.1/nick", new TextualValueContext("fadmaa"));
		request.setContext(new ReconciliationRequestContext(prop1, prop2));
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", 
																	"http://purl.org/dc/terms/title");
		String sparql = factory.getReconciliationSparqlQuery(request, searchPropertyUris);
		
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity ?label1 ?label2 " +
			"WHERE" +
			"{" +
			"{" +
			"OPTIONAL{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + query + "','i')" +
			"}" +
			"OPTIONAL{ ?entity <http://purl.org/dc/terms/title> ?label2. " +
			"FILTER regex(str(?label2),'" + query + "','i')" +
			"}" +
			"FILTER ( bound(?label1) || bound(?label2))" +
			"}" +
			"{" +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. } " +
			"UNION " +
			"{?entity rdf:type <http://data.linkedmdb.org/resource/movie/film_series>. } " + 
			"}" +
			"?entity <http://example.org/ontology/worksFor> <http://example.org/resource/DERI>. " +
			"?entity <http://xmlns.com/foaf/0.1/nick> 'fadmaa'. " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void oneLabelOneTypeWithContextExactMatchReconcileTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label");
		ReconciliationRequest request = new ReconciliationRequest(query, limit);
		request.setTypes(new String[] {"http://data.linkedmdb.org/resource/movie/film"});
		PropertyContext prop1 = new PropertyContext("http://example.org/ontology/worksFor", new IdentifiedValueContext("http://example.org/resource/DERI"));
		PropertyContext prop2 = new PropertyContext("http://xmlns.com/foaf/0.1/nick", new TextualValueContext("fadmaa"));
		request.setContext(new ReconciliationRequestContext(prop1, prop2));
		
		String sparql = factory.getExactMatchReconciliationSparqlQuery(request, searchPropertyUris);
		String expected = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
			"SELECT ?entity " +
			"WHERE" +
			"{" +
			"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (str(?label) = '" + query + "'). " +
			"?entity rdf:type <http://data.linkedmdb.org/resource/movie/film>. " +
			"?entity <http://example.org/ontology/worksFor> <http://example.org/resource/DERI>. " +
			"?entity <http://xmlns.com/foaf/0.1/nick> 'fadmaa'. " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
		
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
			"SELECT DISTINCT ?type ?label " +
			"WHERE{" +
			"[] a ?type. " +
			"?type ?p ?label. " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"FILTER regex(str(?label),'^" + prefix + "','i')" +
			"} LIMIT " + limit;
		
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
			"SELECT DISTINCT ?p ?label " +
		 	"WHERE{" +
		 	"[] a <http://xmlns.com/foaf/0.1/Person>;" +
		 	"?p ?v." +
		 	"?p ?label_prop ?label. " +
		 	"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		 	"FILTER regex(str(?label),'^" + prefix + "','i')" +
		 	"} LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}
	
	@Test
	public void suggestPropertyTest(){
		String prefix = "labe";
		String sparql = factory.getPropertySuggestSparqlQuery(prefix, limit);
		
		String expected =
			"SELECT DISTINCT ?p ?label " +
		 	"WHERE{" +
		 	"[] ?p ?v." +
		 	"?p ?label_prop ?label. " +
		 	"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		 	"FILTER regex(str(?label),'^" + prefix + "','i')" +
		 	"} LIMIT " + limit;
		
		assertEquals(sparql, expected);
	}
	
	/*
	 * SAMPLE INSTANCES
	 */
	@Test 
	public void sampleInstancesQueryTest(){
		String sparql = factory.getSampleInstancesSparqlQuery("http://data.linkedmdb.org/resource/movie/film", ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label"), 10);
		String expected = 
			"SELECT DISTINCT ?entity  ?label1 " +
			"WHERE{" +
			"?entity a <http://data.linkedmdb.org/resource/movie/film>. " +
			"OPTIONAL {?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1} " +
			"}LIMIT 10"
			;
		assertEquals(sparql, expected);
	}

	/*
	 * search entity
	 */
	@Test
	public void searchEntityQueryTest(){
		ImmutableList<String> searchPropertyUris = ImmutableList.of("http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2004/02/skos/core#prefLabel");
		String prefix = "fad";
		String sparql = factory.getEntitySearchSparqlQuery(prefix, searchPropertyUris, limit);
		String expected = 
			"SELECT ?entity ?label1 ?label2 " +
			"WHERE" +
			"{" +
			"OPTIONAL{ ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
			"FILTER regex(str(?label1),'" + prefix + "','i')" +
			"}" +
			"OPTIONAL{ ?entity <http://www.w3.org/2004/02/skos/core#prefLabel> ?label2. " +
			"FILTER regex(str(?label2),'" + prefix + "','i')" +
			"}" +
			"FILTER ( bound(?label1) || bound(?label2)). " +
			"FILTER isIRI(?entity). }LIMIT "  +limit;
			;
		assertEquals(sparql, expected);
	}
}
