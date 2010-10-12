package com.google.refine.tests.rdf;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.google.refine.util.ParsingUtilities;
import com.google.refine.rdf.CellLiteralNode;
import com.google.refine.rdf.CellResourceNode;
import com.google.refine.rdf.Link;
import com.google.refine.rdf.Node;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.ResourceNode;
import com.google.refine.rdf.ResourceNode.RdfType;

public class RdfSchemaTest {

	private RdfSchema schema;
	private CellResourceNode root;
	
	@BeforeClass
	public void init()throws Exception{
		//see image in docs for the schema
		//String json = "{\"baseUri\":\"http://data.bis.gov.uk/data/organogram/2010-08-26/\",\"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/CivilServicePost\",\"curie\":\"gov:CivilServicePost\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"curie\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"target\":{\"nodeType\":\"cell-as-resource\",\"columnIndex\":-1,\"expression\":\"('#person' + value).urlify(baseURI)\",\"isRowNumberCell\":true,\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Person\",\"curie\":\"foaf:Person\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"columnIndex\":2,\"valueType\":\"untyped\",\"columnName\":\"Name\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/mbox\",\"curie\":\"foaf:mbox\",\"target\":{\"nodeType\":\"cell-as-resource\",\"columnIndex\":12,\"expression\":\"'mailto:' + value\",\"isRowNumberCell\":false,\"columnName\":\"Contact e-mail\",\"rdfTypes\":[],\"links\":[]}}]}},{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"cell-as-literal\",\"columnIndex\":5,\"valueType\":\"untyped\",\"lang\":\"en\",\"columnName\":\"Job Title\"}},{\"uri\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"curie\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"target\":{\"nodeType\":\"cell-as-resource\",\"columnIndex\":10,\"expression\":\"value.urlify(baseURI)\",\"isRowNumberCell\":false,\"columnName\":\"Unit\",\"rdfTypes\":[],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"curie\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"target\":{\"nodeType\":\"cell-as-resource\",\"columnIndex\":0,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"isRowNumberCell\":false,\"columnName\":\"Post unique reference\",\"rdfTypes\":[],\"links\":[]}}]}}]}]}";
		String json = "{\"baseUri\":\"http://data.bis.gov.uk/data/organogram/2010-08-26/\",\"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/CivilServicePost\",\"curie\":\"gov:CivilServicePost\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"curie\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":true,\"expression\":\"('#person' + value).urlify()\",\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Person\",\"curie\":\"foaf:Person\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"isRowNumberCell\":false,\"columnName\":\"Name\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/mbox\",\"curie\":\"foaf:mbox\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'mailto:' + value\",\"columnName\":\"Contact e-mail\",\"rdfTypes\":[],\"links\":[]}}]}},{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"cell-as-literal\",\"isRowNumberCell\":false,\"lang\":\"en\",\"columnName\":\"Job Title\"}},{\"uri\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"curie\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"value.urlify()\",\"columnName\":\"Unit\",\"rdfTypes\":[],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"curie\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[],\"links\":[]}}]}}]}]}";
		JSONObject o = ParsingUtilities.evaluateJsonStringToObject(json);
		schema = RdfSchema.reconstruct(o);
		root = (CellResourceNode)schema.getRoots().get(0);
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testRootNodes(){
		assertNotNull(root);
		assertEquals(root.getLinkCount(),3);
		assertEquals(root.getTypes().size(), 1);
		assertEquals(root.getUriExpression(),"'http://reference.data.gov.uk/id/department/bis/post/' + value");
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testJobTitleLiteral(){
		Node jobTitleNode = getLink(root, "http://www.w3.org/2000/01/rdf-schema#label");
		assertNotNull(jobTitleNode);
		assertTrue(jobTitleNode instanceof CellLiteralNode);
		CellLiteralNode jobTitle = (CellLiteralNode)jobTitleNode;
		//test language
		assertEquals(jobTitle.getLang(),"en");
		//test type: null stands for "untyped"
		assertNull(jobTitle.getValueType());
		//test column name
		assertEquals(jobTitle.getColumnName(), "Job Title");
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testRowNumberCell(){
		Node rowNumberNode = getLink(root,"http://reference.data.gov.uk/def/central-government/heldBy");
		assertNotNull(rowNumberNode);
		assertTrue(rowNumberNode instanceof CellResourceNode );
		CellResourceNode rowNumber = (CellResourceNode) rowNumberNode;
		//test URI expression
		assertEquals(rowNumber.getUriExpression(),"('#person' + value).urlify()");
		//test column name
		assertNull(rowNumber.getColumnName());
		testLinks(rowNumber, "http://xmlns.com/foaf/0.1/name","http://xmlns.com/foaf/0.1/mbox");
		testRdfTypes(rowNumber, "http://xmlns.com/foaf/0.1/Person");
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testNameCell(){
		CellResourceNode rowNumber = (CellResourceNode)getLink(root,"http://reference.data.gov.uk/def/central-government/heldBy");
		Node nameNode = getLink(rowNumber, "http://xmlns.com/foaf/0.1/name");
		assertTrue(nameNode instanceof CellLiteralNode );
		CellLiteralNode name = (CellLiteralNode) nameNode;
		assertNull(name.getLang());
		assertEquals(name.getColumnName(), "Name");
		assertNull(name.getValueType());		
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testEmailCell(){
		CellResourceNode rowNumber = (CellResourceNode)getLink(root,"http://reference.data.gov.uk/def/central-government/heldBy");
		Node emailNode = getLink(rowNumber, "http://xmlns.com/foaf/0.1/mbox");
		assertTrue(emailNode instanceof CellResourceNode);
		CellResourceNode email = (CellResourceNode) emailNode;
		
		assertEquals(email.getUriExpression(), "'mailto:' + value");
		testLinks(email);
		testRdfTypes(email);
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testInverseProperty(){
		CellResourceNode n1 = (CellResourceNode)getLink(root,"http://reference.data.gov.uk/def/central-government/postIn");
		CellResourceNode n2 = (CellResourceNode)getLink(n1,"http://reference.data.gov.uk/def/central-government/hasPost");
		assertEquals(n2.getUriExpression(),"'http://reference.data.gov.uk/id/department/bis/post/' + value");
		assertEquals(n2.getColumnName(), "Post unique reference");
		testLinks(n2);
		testRdfTypes(n2);
	}
	
	protected Node getLink(ResourceNode resource, String propertyUri){
		for(Link link:resource.getLinks()){
			if(link.propertyUri.equals(propertyUri)){
				return link.target;
			}
		}
		return null;
	}
	
	void testLinks(ResourceNode node, String... expectedPropertiesUris){
		assertEquals(node.getLinkCount(), expectedPropertiesUris.length);
		List<String> actuals = new ArrayList<String>();
		for(Link link:node.getLinks()){
			actuals.add(link.propertyUri);
		}
		for(int i=0;i<expectedPropertiesUris.length;i++){
			String prop = expectedPropertiesUris[i];
			assertTrue(actuals.remove(prop));
		}
		assertTrue(actuals.isEmpty());
	}
	
	void testRdfTypes(ResourceNode node, String... expectedTypesUris){
		assertEquals(node.getTypes().size(), expectedTypesUris.length);
		List<String> actuals = new ArrayList<String>();
		for(RdfType type:node.getTypes()){
			actuals.add(type.getUri());
		}
		for(int i=0;i<expectedTypesUris.length;i++){
			assertTrue(actuals.remove(expectedTypesUris[i]));
		}
		assertTrue(actuals.isEmpty());
	}
}
