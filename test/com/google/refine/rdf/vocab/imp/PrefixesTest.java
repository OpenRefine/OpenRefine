package com.google.refine.rdf.vocab.imp;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryParser.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.commands.AddPrefixCommand;
import com.google.refine.rdf.commands.DeleteVocabularyCommand;
import com.google.refine.rdf.commands.RdfCommand;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.rdf.vocab.imp.PredefinedVocabularyManager;
import com.google.refine.tests.ProjectFake;

import static org.testng.Assert.*;

public class PrefixesTest {

	private Project project1; private RdfSchema schema1;
	private Project project2; private RdfSchema schema2;
	
	ApplicationContext ctxt;
	
	private Map<String, String> predefinedPrefixesMap = new HashMap<String, String>();
	
	@BeforeClass
	public void init(){
		ctxt = new ApplicationContext();
		ctxt.setPredefinedVocabularyManager(new PredefinedVocabularyManagerFake());
		ctxt.setVocabularySearcher(new VocabularySearcherStub());
		
		predefinedPrefixesMap.put("ns1", "http://ns1.com/ns#");
		predefinedPrefixesMap.put("void","http://rdfs.org/ns/void#");
		
		project1 = new ProjectFake();
		project2 = new ProjectFake();
		
	}
	
	@Test
	//verify that any newly created project will have the default prefixes added to it
	public void defaultPrefixes() throws VocabularyIndexException, IOException{
		schema1 = Util.getProjectSchema(ctxt, project1);
		schema2 = Util.getProjectSchema(ctxt, project2);

		assertEquals(schema1.getPrefixesMap().entrySet(), predefinedPrefixesMap.entrySet());
		assertEquals(schema2.getPrefixesMap().entrySet(), predefinedPrefixesMap.entrySet());
	}
	
	@Test(dependsOnMethods={"defaultPrefixes"})
	//verify that adding a new prefix to project1 does not affect project2
	public void prefixManipulationOnlyAffectsProject() throws ServletException, IOException{
		String name = "foaf";
		String uri = "http://xmlns.com/foaf/0.1/";
		
		RdfCommand addPrefixCommand = new AddPrefixCommandFake(ctxt);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addParameter("name",name);
		request.addParameter("uri",uri);
		//any long suffices. it is not used. see the RdfCommandFake we use
		request.addParameter("projectId","1");
		addPrefixCommand.doGet(request, response);
		
		//schema2 prefixes intact
		assertEquals(schema2.getPrefixesMap().entrySet(), predefinedPrefixesMap.entrySet());
		//foaf is added to schema1 prefixes
		assertEquals(schema1.getPrefixesMap().get(name), uri);
		
		//delete prefix from project 2
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		
		String vocabToDeleteName = "void";
		request.addParameter("name", vocabToDeleteName);
		request.addParameter("project", "2");
		
		DeleteVocabularyCommandFake cmd = new DeleteVocabularyCommandFake(ctxt);
		cmd.doPost(request, response);
		//schema1 prefixes intact
		assertTrue(schema1.getPrefixesMap().containsKey(vocabToDeleteName));
		//void is deleted from schema2 prefixes
		assertFalse(schema2.getPrefixesMap().containsKey(vocabToDeleteName));
		
	}
	
	@Test
	//verify that prefixes are saved/loaded properly
	public void saveLoadPrefixes() throws Exception{
		
		//copy prefixes
		Map<String, String> schema1PrefixesBefore = copyMap(schema1.getPrefixesMap());
		Map<String, String> schema2PrefixesBefore = copyMap(schema2.getPrefixesMap());
		
		String s1 = writeAsJsonString(schema1);
		String s2 = writeAsJsonString(schema2);
		
		JSONObject o1 = new JSONObject(s1);
		JSONObject o2 = new JSONObject(s2);
		RdfSchema schema1AfterLoad = RdfSchema.load(project1, o1);
		RdfSchema schema2AfterLoad = RdfSchema.load(project2, o2);
		
		assertEquals(schema1AfterLoad.getPrefixesMap().entrySet(), schema1PrefixesBefore.entrySet());
		assertEquals(schema2AfterLoad.getPrefixesMap().entrySet(), schema2PrefixesBefore.entrySet());
	}
	
	private Map<String, String> copyMap(Map<String, String> original){
		Map<String, String> copy = new HashMap<String, String>();
		for(Entry<String, String> entry : original.entrySet()){
			copy.put(entry.getKey(), entry.getValue());
		}
		return copy;
	}
	
	private String writeAsJsonString(RdfSchema schema) throws JSONException{
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);
		Properties options = new Properties();

		schema.write(writer, options);
		
		sw.flush();
		
		return sw.toString();
	}
	
	private class PredefinedVocabularyManagerFake extends PredefinedVocabularyManager{

		@Override
		public Map<String, String> getPredefinedPrefixesMap() {
			return predefinedPrefixesMap;
		}
	}
	
	private class VocabularySearcherStub extends VocabularySearcher{

		@Override
		public void addPredefinedVocabulariesToProject(long projectId)
				throws VocabularyIndexException, IOException {
			//DO Nothing
		}

		@Override
		public Vocabulary importAndIndexVocabulary(String name, String uri,
				String projectId) throws VocabularyImportException,
				VocabularyIndexException, PrefixExistException {
			
			//DO Nothing
			return null;
		}

		@Override
		public Vocabulary importAndIndexVocabulary(String name, String uri)
				throws VocabularyImportException, VocabularyIndexException,
				PrefixExistException {
			//DO Nothing
			return null;
		}

		@Override
		public void deleteVocabulary(String name, String projectId)
				throws ParseException, IOException {
			//DO Nothing
		}
		
		
	}
	
	private class AddPrefixCommandFake extends AddPrefixCommand{

		public AddPrefixCommandFake(ApplicationContext ctxt) {
			super(ctxt);
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project1;
		}
	}
	
	private class DeleteVocabularyCommandFake extends DeleteVocabularyCommand{
		
		public DeleteVocabularyCommandFake(ApplicationContext ctxt) {
			super(ctxt);
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project2;
		}
	}

}