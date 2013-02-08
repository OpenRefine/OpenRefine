package com.google.refine.test.org.deri.reconcile;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.json.JSONException;
import org.json.JSONWriter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.org.deri.reconcile.GRefineServiceManager;
import com.google.refine.org.deri.reconcile.ServiceRegistry;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationService;
import com.google.refine.org.deri.reconcile.rdf.RdfReconciliationService;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpointImpl;
import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.RemoteQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.LarqSparqlQueryFactory;
import com.google.refine.org.deri.reconcile.util.GRefineJsonUtilitiesImpl;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GRefineServiceManagerTest {

	String url = "http://example.org/endpoint";
	File dir = new File("tmp");
	
	@BeforeClass
	public void setUp() throws IOException{
		//empty dir if it exists
		if(dir.exists()){
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
                boolean success = new File(dir, children[i]).delete();
                if (!success) {
                    throw new IOException("unable to delete " + children[i]);
                }
            }
			boolean success = dir.delete();
			if(!success){
				throw new IOException("unable to delete " + dir);
			}
		}
		
		
		boolean res = dir.mkdir();
		if(!res){
			throw new IOException("unable to create " + dir);
		}
	
		File file = new File(dir,"services");
		file.createNewFile();
		
	}
	
	@Test
	public void saveServiceTest() throws JSONException, IOException{
		String id = "sparql-test";
		ServiceRegistry registry = new ServiceRegistry(new GRefineJsonUtilitiesImpl(),null);
		GRefineServiceManager manager = new GRefineServiceManager(registry, dir);
		
		ReconciliationService service = new RdfReconciliationService(id, id, new QueryEndpointImpl(new LarqSparqlQueryFactory(), new RemoteQueryExecutor(url, null)), 0);
		manager.addService(service);
		
		assertTrue(registry.hasService(id));
		//verify service is saved
		
		registry = new ServiceRegistry(new GRefineJsonUtilitiesImpl(),null);
		//verify no service
		assertFalse(registry.hasService(id));
		
		File file = new File(dir,"services");
		//verify saved
		assertTrue(file.exists());
		
		FileInputStream in = new FileInputStream(file);

		registry.loadFromFile(in);
		//verify service is loaded
		verifyCorrectService(registry.getService(id, null), service);
	}
	
	@Test
	public void saveRdfServiceTest() throws JSONException, IOException{
		String id = "rdf-test";
		ServiceRegistry registry = new ServiceRegistry(new GRefineJsonUtilitiesImpl(),null);
		GRefineServiceManager manager = new GRefineServiceManager(registry, dir);
		
		Model m = ModelFactory.createDefaultModel();
		ReconciliationService service = new RdfReconciliationService(id,id, new QueryEndpointImpl(new LarqSparqlQueryFactory(), 
				new DumpQueryExecutor(m)), 0);
		manager.addAndSaveService(service);
		
		assertTrue(registry.hasService(id));
		//verify service is saved
		
		registry = new ServiceRegistry(new GRefineJsonUtilitiesImpl(),null);
		//verify no service
		assertFalse(registry.hasService(id));
		
		File file = new File(dir,"services");
		//verify saved
		assertTrue(file.exists());
		
		FileInputStream in = new FileInputStream(file);

		registry.loadFromFile(in);
		//verify service is loaded
		ReconciliationService service2 = registry.getService(id, null);
		verifyCorrectService(service2, service);
		//verify service is not initialized
		ReconciliationRequest request = new ReconciliationRequest("query", 10);
		String msg = "";
		try{
			service2.reconcile(request);
		}catch(RuntimeException e){
			msg = e.getMessage();
		}
		assertTrue(msg.equals("Model is not loaded"));
		
		FileInputStream modelIn = new FileInputStream(new File(dir,id + ".ttl"));
		ReconciliationService service3 = registry.getService(id, modelIn);
		assertTrue(service3.reconcile(request).getResults().isEmpty());
	}

	private void verifyCorrectService(ReconciliationService service,ReconciliationService expected) throws JSONException {
		StringWriter w1 = new StringWriter();
		JSONWriter j1 = new JSONWriter(w1);
		StringWriter w2 = new StringWriter();
		JSONWriter j2 = new JSONWriter(w2);
		service.writeAsJson(j1);
		expected.writeAsJson(j2);
		w1.flush(); w2.flush();
		assertEquals(w1.toString(), w2.toString());
	}
}
