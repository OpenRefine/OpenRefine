package com.google.refine.test.rdf.vocab;

import org.testng.annotations.Test;

import com.google.refine.rdf.RdfSchema;

import static org.testng.Assert.*;

public class AddPrefixTest {

	@Test
	public void testAddNewPrefix() throws Exception{
		RdfSchema schema = new RdfSchema();
		assertFalse(schema.getPrefixesMap().containsKey("foaf"));
		schema.addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		
		assertTrue(schema.getPrefixesMap().containsKey("foaf"));
	}
	
}
