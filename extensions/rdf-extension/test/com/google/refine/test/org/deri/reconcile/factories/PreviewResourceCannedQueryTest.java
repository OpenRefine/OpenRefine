package com.google.refine.test.org.deri.reconcile.factories;

import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.Multimap;
import com.google.refine.org.deri.reconcile.rdf.factories.PreviewResourceCannedQuery;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class PreviewResourceCannedQueryTest {

	@Test
	public void testSparqlQuery()throws Exception{
		String uri = "http://example.org/resource/1";
		PreviewResourceCannedQuery cannedQuery = new PreviewResourceCannedQuery(this.getClass().getResourceAsStream("files/preview_properties.properties"));
		String sparql = cannedQuery.getPreviewQueryForResource(uri);
		
		String expected = 
			"SELECT ?en_label0 ?label0 ?en_label1 ?label1 ?en_label2 ?label2 ?en_desc0 ?desc0 ?en_desc1 ?desc1 ?en_desc2 ?desc2 ?img0 " +
			"WHERE{ " +
			"OPTIONAL { <http://example.org/resource/1> <http://www.w3.org/2004/02/skos/core#prefLabel> ?en_label0 FILTER langMatches(lang(?en_label0),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://www.w3.org/2004/02/skos/core#prefLabel> ?label0 FILTER langMatches(lang(?label0),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://xmlns.com/foaf/0.1/givenName> ?en_label1 FILTER langMatches(lang(?en_label1),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://xmlns.com/foaf/0.1/givenName> ?label1 FILTER langMatches(lang(?label1),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://xmlns.com/foaf/0.1/familyName> ?en_label2 FILTER langMatches(lang(?en_label2),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://xmlns.com/foaf/0.1/familyName> ?label2 FILTER langMatches(lang(?label2),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://www.w3.org/2004/02/skos/core#definition> ?en_desc0 FILTER langMatches(lang(?en_desc0),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://www.w3.org/2004/02/skos/core#definition> ?desc0 FILTER langMatches(lang(?desc0),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://purl.org/dc/elements/1.1/description> ?en_desc1 FILTER langMatches(lang(?en_desc1),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://purl.org/dc/elements/1.1/description> ?desc1 FILTER langMatches(lang(?desc1),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://purl.org/dc/terms/description> ?en_desc2 FILTER langMatches(lang(?en_desc2),'en') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://purl.org/dc/terms/description> ?desc2 FILTER langMatches(lang(?desc2),'') } " +
			"OPTIONAL { <http://example.org/resource/1> <http://xmlns.com/foaf/0.1/img> ?img0} " +
			"}LIMIT 1"
			;
		assertEquals(sparql, expected);
	}
	
	@Test
	public void testWrapping()throws Exception{
		Model model = ModelFactory.createDefaultModel();
		InputStream in = this.getClass().getResourceAsStream("files/sample.rdf");
		model.read(in,null);
		String uri = "http://www.deri.ie/about/team/member/renaud_delbru#me";
		PreviewResourceCannedQuery cannedQuery = new PreviewResourceCannedQuery(this.getClass().getResourceAsStream("files/preview_properties.properties"));
		String sparql = cannedQuery.getPreviewQueryForResource(uri);

		Query query = QueryFactory.create(sparql, Syntax.syntaxSPARQL_11);
		QueryExecution qExec = QueryExecutionFactory.create(query, model);
		
		ResultSet res = qExec.execSelect();
		Multimap<String, String> map = cannedQuery.wrapResourcePropertiesMapResultSet(res);
		assertEquals(map.size(),2);
		assertEquals(map.get("labels"), Collections.singletonList("EN-Renaud"));
	}
}
