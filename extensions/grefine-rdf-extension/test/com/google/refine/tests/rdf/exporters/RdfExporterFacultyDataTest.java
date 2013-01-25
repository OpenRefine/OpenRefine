package com.google.refine.tests.rdf.exporters;

import static org.testng.Assert.assertTrue;

import org.json.JSONObject;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RepositoryUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import com.google.refine.browsing.Engine;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.exporters.RdfExporter;
import com.google.refine.rdf.expr.RdfBinder;
import com.google.refine.rdf.expr.functions.strings.Urlify;

public class RdfExporterFacultyDataTest {

	Project project;
	Engine engine;
	RdfExporter exporter;
	Repository model;
	
	Repository expected;
	RdfSchema schema;
	@BeforeClass
	public void init()throws Exception{
		expected = buildExpectedModel();
		ApplicationContext ctxt = new ApplicationContext();
		schema = getRdfSchema();
		project = buildTheSampleProject(schema);
		engine = new Engine(project);
		exporter = new RdfExporter(ctxt,RDFFormat.RDFXML);
		ControlFunctionRegistry.registerFunction("urlify", new Urlify());
		ExpressionUtils.registerBinder(new RdfBinder(ctxt));
			    
		model = exporter.buildModel(project, engine, schema);
		
		assertEquals(project.rows.size(),3);
		assertEquals(project.columnModel.getColumnIndexByName("Advisor"),5);
		
		Row row = project.rows.get(0);
		assertEquals(row.cells.get(5).value,"");
		row = project.rows.get(1);
		assertEquals(row.cells.get(5).value,"Tim Finin");
		row = project.rows.get(2);
		assertEquals(row.cells.get(5).value,"Anupam Joshi");
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testModel()throws Exception{
		assertTrue(RepositoryUtil.equals(expected, model));
	}
	
	Repository buildExpectedModel() throws Exception{
		//see test2.xlsx and rdfschema2.png

		Repository model = new SailRepository(new MemoryStore());
		model.initialize();
		
		RepositoryConnection con = null;
		try{
			con = model.getConnection();
			ValueFactory vf = con.getValueFactory();
			
			//create persons 
			URI person1 = addFoafPerson(vf, con, "http://lab.linkeddata.deri.ie/test#tim-finin", "Tim Finin", "finin@umbc.edu");
			URI person2 = addFoafPerson(vf, con, "http://lab.linkeddata.deri.ie/test#lushan-han", "Lushan Han", "lushan@umbc.edu");
			URI person3 = addFoafPerson(vf, con, "http://lab.linkeddata.deri.ie/test#wenjia-li", "Wenjia Li", "wenjia@umbc.edu");
		
			//office numbers
			URI officeNumberProp = vf.createURI("http://lab.linkeddata.deri.ie/test#officeNumber");
			con.add(vf.createStatement(person1, officeNumberProp, vf.createLiteral(329)));
			con.add(vf.createStatement(person2, officeNumberProp, vf.createLiteral(377)));
			con.add(vf.createStatement(person3, officeNumberProp, vf.createLiteral(377)));
		
			//advisor
			URI advisor = vf.createURI("http://lab.linkeddata.deri.ie/test#anupam-joshi");
			con.add(vf.createStatement(advisor, vf.createURI("http://xmlns.com/foaf/0.1/name"), vf.createLiteral("Anupam Joshi")));
			URI advisorType = vf.createURI("http://lab.linkeddata.deri.ie/test#Advisor");
			con.add(vf.createStatement(advisor, RDF.TYPE, advisorType));
			con.add(vf.createStatement(person1, RDF.TYPE, advisorType));
		
			//university
			URI university = vf.createURI("http://example.org/UMBC");
			con.add(vf.createStatement(university,RDF.TYPE, vf.createURI("http://xmlns.com/foaf/0.1/Organization")));
			con.add(vf.createStatement(university,RDFS.LABEL,vf.createLiteral("University of Maryland Baltimore County","en")));
		
			URI memberProp = vf.createURI("http://xmlns.com/foaf/0.1/member");
			con.add(vf.createStatement(person1,memberProp,university));
			con.add(vf.createStatement(person2,memberProp,university));
			con.add(vf.createStatement(person3,memberProp,university));
		
			//add advisor
			URI advisorProp = vf.createURI("http://lab.linkeddata.deri.ie/test#advisor");
			con.add(vf.createStatement(person2,advisorProp,person1));
			con.add(vf.createStatement(person3,advisorProp,advisor));
		
			return model;
		}finally{
			con.close();
		}
	}

	static void buildColumnModel(Project project)throws Exception{
		project.columnModel.addColumn(0, new Column(0,"Name"), true);
		project.columnModel.addColumn(1, new Column(1,"Email"), true);
		project.columnModel.addColumn(2, new Column(2,"Office"), true);
		project.columnModel.addColumn(3, new Column(3,"Faculty"), true);
		project.columnModel.addColumn(4, new Column(4,"Coffee Due"), true);
		project.columnModel.addColumn(5, new Column(5,"Advisor"), true);
	}
	
	static Project buildTheSampleProject(RdfSchema schema)throws Exception{
		Project project = new Project();
		buildColumnModel(project);
		Row row1= new Row(6);
		row1.cells.add(new Cell("Tim Finin",null));
		row1.cells.add(new Cell("finin@umbc.edu",null));
		row1.cells.add(new Cell("329",null));
		row1.cells.add(new Cell("Yes",null));
		row1.cells.add(new Cell("$10",null));
		row1.cells.add(new Cell("",null));
		project.rows.add(row1);
		
		Row row2= new Row(6);
		row2.cells.add(new Cell("Lushan Han",null));
		row2.cells.add(new Cell("lushan@umbc.edu",null));
		row2.cells.add(new Cell("377",null));
		row2.cells.add(new Cell("No",null));
		row2.cells.add(new Cell("",null));
		row2.cells.add(new Cell("Tim Finin",null));
		project.rows.add(row2);
		
		Row row3= new Row(6);
		row3.cells.add(new Cell("Wenjia Li",null));
		row3.cells.add(new Cell("wenjia@umbc.edu",null));
		row3.cells.add(new Cell("377",null));
		row3.cells.add(new Cell("No",null));
		row3.cells.add(new Cell("",null));
		row3.cells.add(new Cell("Anupam Joshi",null));
		project.rows.add(row3);
		
		project.update();
		//set model
		project.overlayModels.put("rdfSchema", schema );
		
		return project;
	}
	
	RdfSchema getRdfSchema()throws Exception{
		String json = "{\"baseUri\":\"http://lab.linkeddata.deri.ie/test#\",\"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"value.urlify()\",\"columnName\":\"Name\",\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Person\",\"curie\":\"foaf:Person\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"columnName\":\"Name\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/mbox\",\"curie\":\"foaf:mbox\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'mailto:' + value\",\"rdfTypes\":[],\"columnName\":\"Email\",\"links\":[]}},{\"uri\":\"officeNumber\",\"curie\":\":officeNumber\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"valueType\":\"http://www.w3.org/2001/XMLSchema#int\",\"columnName\":\"Office\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/member\",\"curie\":\"foaf:member\",\"target\":{\"nodeType\":\"resource\",\"value\":\"http://example.org/UMBC\",\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Organization\",\"curie\":\"foaf:Organization\"}],\"links\":[{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"literal\",\"value\":\"University of Maryland Baltimore County\",\"lang\":\"en\"}}]}},{\"uri\":\"advisor\",\"curie\":\":advisor\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"value.urlify()\",\"columnName\":\"Advisor\",\"rdfTypes\":[{\"uri\":\"Advisor\",\"curie\":\":Advisor\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"columnName\":\"Advisor\"}}]}}]}]}";
		JSONObject o = ParsingUtilities.evaluateJsonStringToObject(json);
		return RdfSchema.reconstruct(o);
	}
	
	private URI addFoafPerson(ValueFactory vf,RepositoryConnection con,String uri,String name,String email)throws Exception{
		URI person = vf.createURI(uri);
		con.add(vf.createStatement(person, RDF.TYPE, vf.createURI("http://xmlns.com/foaf/0.1/Person")));
		con.add(vf.createStatement(person,vf.createURI("http://xmlns.com/foaf/0.1/name"),vf.createLiteral(name)));
		con.add(vf.createStatement(person,vf.createURI("http://xmlns.com/foaf/0.1/mbox"),vf.createURI("mailto:" + email)));
		
		return person;
	}
}
