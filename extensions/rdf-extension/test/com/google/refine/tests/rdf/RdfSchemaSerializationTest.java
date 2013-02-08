package com.google.refine.tests.rdf;

import java.io.StringWriter;
import java.util.Properties;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;
import com.google.refine.rdf.RdfSchema;

import static org.testng.Assert.*;

public class RdfSchemaSerializationTest {

	private RdfSchema schema;
	private String json;
	
	@BeforeClass
	public void init() throws Exception{
		json = "{\"baseUri\":\"http://data.bis.gov.uk/data/organogram/2010-08-26/\",\"prefixes\":[],\"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/CivilServicePost\",\"curie\":\"gov:CivilServicePost\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"curie\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":true,\"expression\":\"('#person' + value).urlify()\",\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Person\",\"curie\":\"foaf:Person\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"columnName\":\"Name\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/mbox\",\"curie\":\"foaf:mbox\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'mailto:' + value\",\"columnName\":\"Contact e-mail\",\"rdfTypes\":[],\"links\":[]}}]}},{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"lang\":\"en\",\"columnName\":\"Job Title\"}},{\"uri\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"curie\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"value.urlify()\",\"columnName\":\"Unit\",\"rdfTypes\":[],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"curie\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[],\"links\":[]}}]}}]}]}";
//		json = "{\"baseUri\":\"http://data.bis.gov.uk/data/organogram/2010-08-26/\", \"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"expression\":\"'http'\",\"links\":[],\"rdfTypes\":[]}]}";
		JSONObject o = ParsingUtilities.evaluateJsonStringToObject(json);
		schema = RdfSchema.reconstruct(o);
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testSerialization() throws Exception{
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);
		schema.write(writer, new Properties());
		
		sw.flush();
		testJsonEquivalence(sw.toString(), json);
	}
	
	void testJsonEquivalence(String actual, String expected)throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = mapper.getJsonFactory();
		JsonParser jp = factory.createJsonParser(actual);
		JsonNode actualObj = mapper.readTree(jp);
		
		jp = factory.createJsonParser(expected);
		JsonNode  expectedObj = mapper.readTree(jp);
		assertEquals(actualObj, expectedObj);
	}
}
