package com.google.refine.tests.rdf.exporters;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RepositoryUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.browsing.Engine;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.importers.TsvCsvImporter;
import com.google.refine.model.Project;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.exporters.RdfExporter;
import com.google.refine.rdf.expr.RdfBinder;
import com.google.refine.rdf.expr.functions.strings.Urlify;
import com.google.refine.util.ParsingUtilities;

public class RdfExporterPaymentDataTest {

	private Project project ;
	private Repository model;
	private Repository expected;
	RdfExporter exporter;
	Engine engine;
	
	private String json = "{\"baseUri\":\"http://www.rbwm.gov.uk/public/finance_supplier_payments_2010_q2#\",\"rootNodes\":[{\"nodeType\":\"resource\",\"value\":\"http://www.rbwm.gov.uk/public/finance_supplier_payments_2010_q2\",\"rdfTypes\":[],\"links\":[{\"uri\":\"http://purl.org/linked-data/cube#slice\",\"curie\":\"http://purl.org/linked-data/cube#slice\",\"target\":{\"nodeType\":\"cell-as-resource\",\"expression\":\"'http://www.rbwm.gov.uk/id/transaction/' + value\",\"columnName\":\"TransNo\",\"isRowNumberCell\":false,\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/payment#Payment\",\"curie\":\"http://reference.data.gov.uk/def/payment#Payment\"},{\"uri\":\"http://purl.org/linked-data/cube#Slice\",\"curie\":\"http://purl.org/linked-data/cube#Slice\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/payment#payer\",\"curie\":\"http://reference.data.gov.uk/def/payment#payer\",\"target\":{\"nodeType\":\"resource\",\"value\":\"http://statistics.data.gov.uk/id/local-authority/00ME\",\"rdfTypes\":[],\"links\":[]}},{\"uri\":\"http://reference.data.gov.uk/def/payment#payee\",\"curie\":\"http://reference.data.gov.uk/def/payment#payee\",\"target\":{\"nodeType\":\"cell-as-resource\",\"expression\":\"'http://www.rbwm.gov.uk/id/supplier/' +value.urlify()\",\"columnName\":\"Supplier Name\",\"isRowNumberCell\":false,\"rdfTypes\":[],\"links\":[]}},{\"uri\":\"http://reference.data.gov.uk/def/payment#transactionReference\",\"curie\":\"http://reference.data.gov.uk/def/payment#transactionReference\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"columnName\":\"TransNo\",\"isRowNumberCell\":false}},{\"uri\":\"http://www.w3.org/2000/01/rdf-schema#label\",\"curie\":\"rdfs:label\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"lang\":\"en\",\"columnName\":\"TransNo\",\"isRowNumberCell\":false}},{\"uri\":\"http://purl.org/linked-data/cube#sliceStructure\",\"curie\":\"http://purl.org/linked-data/cube#sliceStructure\",\"target\":{\"nodeType\":\"resource\",\"value\":\"http://reference.data.gov.uk/def/payment#payment-slice\",\"rdfTypes\":[],\"links\":[]}},{\"uri\":\"http://reference.data.gov.uk/def/payment#expenditureLine\",\"curie\":\"http://reference.data.gov.uk/def/payment#expenditureLine\",\"target\":{\"nodeType\":\"cell-as-resource\",\"expression\":\"row.index.urlify()\",\"isRowNumberCell\":true,\"rdfTypes\":[{\"uri\":\"http://reference.data.gov.uk/def/payment#ExpenditureLine\",\"curie\":\"http://reference.data.gov.uk/def/payment#ExpenditureLine\"}],\"links\":[{\"uri\":\"http://reference.data.gov.uk/def/payment#amountExcludingVAT\",\"curie\":\"http://reference.data.gov.uk/def/payment#amountExcludingVAT\",\"target\":{\"nodeType\":\"cell-as-literal\",\"expression\":\"value\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#double\",\"columnName\":\"Amount excl vat\",\"isRowNumberCell\":false}},{\"uri\":\"http://reference.data.gov.uk/def/payment#expenditureCode\",\"curie\":\"http://reference.data.gov.uk/def/payment#expenditureCode\",\"target\":{\"nodeType\":\"cell-as-resource\",\"expression\":\"'http://www.rbwm.gov.uk/def/cost-centre/' + value\",\"columnName\":\"Cost Centre\",\"isRowNumberCell\":false,\"rdfTypes\":[],\"links\":[]}},{\"uri\":\"http://purl.org/linked-data/cube#dataset\",\"curie\":\"http://purl.org/linked-data/cube#dataset\",\"target\":{\"nodeType\":\"resource\",\"value\":\"http://www.rbwm.gov.uk/public/finance_supplier_payments_2010_q2\",\"rdfTypes\":[],\"links\":[]}}]}}]}}]}]}";
	
	@BeforeClass
	public void init()throws Exception{
		InputStream in = this.getClass().getResourceAsStream("/rdfschema-payment.csv");
		ApplicationContext ctxt = new ApplicationContext();
		
		ControlFunctionRegistry.registerFunction("urlify", new Urlify());
		ExpressionUtils.registerBinder(new RdfBinder(ctxt));
		
		project = new Project();
		ProjectMetadata meta = new ProjectMetadata();
		
		Properties options = new Properties();
//		options.put("ignore", "1");
		new TsvCsvImporter().read(in, project,meta, options);
		project.update();
		//Guard assertion
		assertEquals(project.rows.size(),2);
		
		//preparing the RDF schema skeleton
		RdfSchema schema = RdfSchema.reconstruct(ParsingUtilities.evaluateJsonStringToObject(json));
		project.overlayModels.put("rdfSchema", schema);
		
		//building the expected model
		expected = new SailRepository(new MemoryStore());
		expected.initialize();
		RepositoryConnection con = expected.getConnection();
		con.add(this.getClass().getResourceAsStream("/rdfschema-payment.rdf"),"",RDFFormat.RDFXML);
		
		//export the project
		engine = new Engine(project);
		exporter = new RdfExporter(ctxt,RDFFormat.RDFXML);
		model = exporter.buildModel(project, engine, schema);
	}
	
	@Test(groups={"rdf-schema-test"})
	public void testModel() throws RepositoryException{
		assertTrue(RepositoryUtil.equals(expected, model));
	}
}
