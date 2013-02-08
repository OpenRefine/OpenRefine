package com.google.refine.tests.rdf.exporters;


import org.json.JSONObject;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RepositoryUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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


import static org.testng.Assert.*;

public class RdfExporterTest {

	Project project;
	Engine engine;
	RdfExporter exporter;
	Repository model;
	
	Repository expected;
	RdfSchema schema;
	
	@BeforeClass
	public void init()throws Exception{
		expected = buildExpectedModel();
		schema = getRdfSchema();
		
		ApplicationContext ctxt = new ApplicationContext();
		
		buildTheSampleProject();
		engine = new Engine(project);
		exporter = new RdfExporter(ctxt,RDFFormat.RDFXML);
		ControlFunctionRegistry.registerFunction("urlify", new Urlify());
		ExpressionUtils.registerBinder(new RdfBinder(ctxt));
	
		
		model = exporter.buildModel(project, engine,schema);
		
		assertEquals(project.rows.size(),2);
		assertEquals(project.columnModel.getColumnIndexByName("Job Title"),3);
	}

	@Test(groups={"rdf-schema-test"})
	public void testModel() throws RepositoryException{
		assertTrue(RepositoryUtil.equals(expected, model));
	}
	//helper methods
	
	void buildColumnModel()throws Exception{
		project.columnModel.addColumn(0, new Column(0,"Post unique reference"), true);
		project.columnModel.addColumn(1, new Column(1,"Name"), true);
		project.columnModel.addColumn(2, new Column(2,"Grade"), true);
		project.columnModel.addColumn(3, new Column(3,"Job Title"), true);
		project.columnModel.addColumn(4, new Column(4,"Job/Team Function"), true);
		project.columnModel.addColumn(5, new Column(5,"Parent Department"), true);
		project.columnModel.addColumn(6, new Column(6,"Organisation"), true);
		project.columnModel.addColumn(7, new Column(7,"Unit"), true);
		project.columnModel.addColumn(8, new Column(8,"Contact Phone"), true);
		project.columnModel.addColumn(9, new Column(9,"Contact e-mail"), true);
		project.columnModel.addColumn(10, new Column(10,"Reports To"), true);
		project.columnModel.addColumn(11, new Column(11,"Reports To (Name)"), true);
		project.columnModel.addColumn(12, new Column(12,"HRO-Grade"), true);
		project.columnModel.addColumn(13, new Column(13,"HRO-AssignmentStatus"), true);
		project.columnModel.addColumn(14, new Column(14, "removed"), true);
		project.columnModel.addColumn(15, new Column(15,"Post unique reference OK"), true);
		project.columnModel.addColumn(16, new Column(16,"Is Department"), true);
		project.columnModel.addColumn(17, new Column(17,"Grade OK"), true);
	}
	void buildTheSampleProject()throws Exception{
		project = new Project();
		buildColumnModel();
		Row row1 = new Row(17);
		row1.cells.add(new Cell("101198", null));
		row1.cells.add(new Cell("Paula Freedman", null));
		row1.cells.add(new Cell("G5", null));
		row1.cells.add(new Cell("Deputy Director - International Group", null));
		row1.cells.add(new Cell("tbc", null));
		row1.cells.add(new Cell("BIS", null));
		row1.cells.add(new Cell("BIS", null));
		row1.cells.add(new Cell("UK Trade and Investment", null));
		row1.cells.add(new Cell("020 7215 4320", null));
		row1.cells.add(new Cell("paula.freedman@ukti.gsi.gov.uk", null));
		row1.cells.add(new Cell("127460", null));
		row1.cells.add(new Cell("Susan Haird", null));
		row1.cells.add(new Cell("Senior Civil Service.Pay Band 1", null));
		row1.cells.add(new Cell("Active Assignment", null));
		row1.cells.add(new Cell(null,null));
		row1.cells.add(new Cell(true, null));
		row1.cells.add(new Cell(true, null));
		row1.cells.add(new Cell(false, null));
		
		project.rows.add(row1);
		
		/*
		 * 111912|	Bryan Welch|	G5|	Deputy Director - Legal Services Directorate B|	tbc|	BIS|	BIS|	Legal Services Group|	020 7215 3181|	bryan.welch@bis.gsi.gov.uk|	901575|	Stephen Braviner-Roman|	Senior Civil Service.Pay Band 1|	Active Assignment		TRUE	TRUE	FALSE
		 */
		Row row2 = new Row(17);
		row2.cells.add(new Cell("111912", null));
		row2.cells.add(new Cell("Bryan Welch", null));
		row2.cells.add(new Cell("G5", null));
		row2.cells.add(new Cell("Deputy Director - Legal Services Directorate B", null));
		row2.cells.add(new Cell("tbc", null));
		row2.cells.add(new Cell("BIS", null));
		row2.cells.add(new Cell("BIS", null));
		row2.cells.add(new Cell("Legal Services Group", null));
		row2.cells.add(new Cell("020 7215 3181", null));
		row2.cells.add(new Cell("bryan.welch@bis.gsi.gov.uk", null));
		row2.cells.add(new Cell("901575", null));
		row2.cells.add(new Cell("Stephen Braviner-Roman", null));
		row2.cells.add(new Cell("Senior Civil Service.Pay Band 1", null));
		row2.cells.add(new Cell("Active Assignment", null));
		row2.cells.add(new Cell(null,null));
		row2.cells.add(new Cell(true, null));
		row2.cells.add(new Cell(true, null));
		row2.cells.add(new Cell(false, null));
		
		project.rows.add(row2);
		
		project.update();
		//set model
		project.overlayModels.put("rdfSchema", schema );
	}
	
	RdfSchema getRdfSchema()throws Exception{
		String json = "{\"baseUri\":\"http://data.bis.gov.uk/data/organogram/2010-08-26/\",\"rootNodes\":[{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/CivilServicePost\",\"curie\":\"gov:CivilServicePost\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"curie\":\"http://reference.data.gov.uk/def/central-government/heldBy\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":true,\"expression\":\"('#person' + row.index).urlify()\",\"rdfTypes\":[{\"uri\":\"http://xmlns.com/foaf/0.1/Person\",\"curie\":\"foaf:Person\"}],\"links\":[{\"uri\":\"http://xmlns.com/foaf/0.1/name\",\"curie\":\"foaf:name\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"columnName\":\"Name\"}},{\"uri\":\"http://xmlns.com/foaf/0.1/mbox\",\"curie\":\"foaf:mbox\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'mailto:' + value\",\"columnName\":\"Contact e-mail\",\"rdfTypes\":[],\"links\":[]}}]}},{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"isRowNumberCell\":false,\"lang\":\"en\",\"columnName\":\"Job Title\"}},{\"uri\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"curie\":\"http://reference.data.gov.uk/def/central-government/postIn\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"value.urlify()\",\"columnName\":\"Unit\",\"rdfTypes\":[],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"curie\":\"http://reference.data.gov.uk/def/central-government/hasPost\",\"target\":{\"nodeType\":\"cell-as-resource\",\"isRowNumberCell\":false,\"expression\":\"'http://reference.data.gov.uk/id/department/bis/post/' + value\",\"columnName\":\"Post unique reference\",\"rdfTypes\":[],\"links\":[]}}]}}]}]}";
		JSONObject o = ParsingUtilities.evaluateJsonStringToObject(json);
		return RdfSchema.reconstruct(o);
	}
	
	Repository buildExpectedModel()throws RepositoryException{
		//see test1.xlsx and rdfschema1.png
		
		RepositoryConnection con = null;
		try{
			Repository repo = new SailRepository(new MemoryStore());
			repo.initialize();
			con = repo.getConnection();
			ValueFactory vf = con.getValueFactory();
		
			URI post = vf.createURI("http://reference.data.gov.uk/id/department/bis/post/101198");
			con.add(vf.createStatement(post, RDF.TYPE, vf.createURI("http://reference.data.gov.uk/def/central-government/CivilServicePost")));
		
			con.add(vf.createStatement(post, RDFS.LABEL, (Value)vf.createLiteral("Deputy Director - International Group","en")));
		
			URI person = vf.createURI("http://data.bis.gov.uk/data/organogram/2010-08-26/person0");
			con.add(vf.createStatement(person, RDF.TYPE, vf.createURI("http://xmlns.com/foaf/0.1/Person")));
			con.add(vf.createStatement(person, 
								vf.createURI("http://xmlns.com/foaf/0.1/name"), vf.createLiteral( "Paula Freedman")));
			con.add(vf.createStatement(person,
					vf.createURI("http://xmlns.com/foaf/0.1/mbox"), vf.createURI("mailto:paula.freedman@ukti.gsi.gov.uk")));
			
			Resource unit = vf.createURI("http://data.bis.gov.uk/data/organogram/2010-08-26/uk-trade-and-investment");
			con.add(vf.createStatement(unit, vf.createURI("http://reference.data.gov.uk/def/central-government/hasPost"), post));
			con.add(vf.createStatement(post, vf.createURI("http://reference.data.gov.uk/def/central-government/postIn"), unit));
			con.add(vf.createStatement(post, vf.createURI("http://reference.data.gov.uk/def/central-government/heldBy"), person));
			
			/*
			 * 111912|	Bryan Welch|	G5|	Deputy Director - Legal Services Directorate B|	tbc|	BIS|	BIS|	Legal Services Group|	020 7215 3181|	bryan.welch@bis.gsi.gov.uk|	901575|	Stephen Braviner-Roman|	Senior Civil Service.Pay Band 1|	Active Assignment		TRUE	TRUE	FALSE
			 */
			URI post2 = vf.createURI("http://reference.data.gov.uk/id/department/bis/post/111912");
			con.add(vf.createStatement(post2,RDF.TYPE,vf.createURI("http://reference.data.gov.uk/def/central-government/CivilServicePost")));
			con.add(vf.createStatement(post2,RDFS.LABEL,vf.createLiteral("Deputy Director - Legal Services Directorate B","en")));
			
			URI person2 = vf.createURI("http://data.bis.gov.uk/data/organogram/2010-08-26/person1");
			con.add(vf.createStatement(person2,RDF.TYPE,vf.createURI("http://xmlns.com/foaf/0.1/Person")));
			con.add(vf.createStatement(person2,vf.createURI("http://xmlns.com/foaf/0.1/name"),vf.createLiteral("Bryan Welch")));
			con.add(vf.createStatement(person2,vf.createURI("http://xmlns.com/foaf/0.1/mbox"),vf.createURI("mailto:bryan.welch@bis.gsi.gov.uk")));
			
			URI unit2 = vf.createURI("http://data.bis.gov.uk/data/organogram/2010-08-26/legal-services-group");
			
			con.add(vf.createStatement(unit2, vf.createURI("http://reference.data.gov.uk/def/central-government/hasPost"), post2));
			con.add(vf.createStatement(post2, vf.createURI("http://reference.data.gov.uk/def/central-government/postIn"), unit2));
			con.add(vf.createStatement(post2, vf.createURI("http://reference.data.gov.uk/def/central-government/heldBy"), person2));
			
			return repo;
		}finally{
			con.close();
		}
	}
}
