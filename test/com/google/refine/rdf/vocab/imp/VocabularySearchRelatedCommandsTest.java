package com.google.refine.rdf.vocab.imp;

import static org.testng.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.commands.AddPrefixCommand;
import com.google.refine.rdf.commands.DeleteVocabularyCommand;
import com.google.refine.rdf.commands.RefreshVocabularyCommand;
import com.google.refine.rdf.commands.SuggestTermCommand;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.tests.ProjectFake;

import static org.testng.Assert.*;

/**
 * @author fadmaa
 * this is rather a functional test than unit test...
 */
public class VocabularySearchRelatedCommandsTest {

	private static final String TEMP_TEST_DIRECTORY = "tmp_VocabularySearchRelatedCommandsTest";
	private Map<String,String> predefinedVocabsNames = new HashMap<String, String>();
	
	List<Vocabulary> predefinedVocabs = new ArrayList<Vocabulary>();
	
	VocabularySearcher searcher;
	ApplicationContext ctxt;
	Project project1; RdfSchema schema1; 
	Project project2; RdfSchema schema2;
	
	@BeforeClass
	public void init() throws VocabularyIndexException, IOException, PrefixExistException{
		//gurad assert 
		assertFalse(new File(TEMP_TEST_DIRECTORY).exists());
		
		predefinedVocabsNames.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");//will be imported
		predefinedVocabsNames.put("dct","http://purl.org/dc/terms/");//will be imported
		predefinedVocabsNames.put("ns","http://doesnotexist.com/ns#"); //import will fail
		
		searcher = new VocabularySearcher(new File(TEMP_TEST_DIRECTORY));
		ctxt = new ApplicationContext();
		ctxt.setVocabularySearcher(searcher);
		ctxt.setPredefinedVocabularyManager(new PredefinedVocabularyManagerFake());
		
		//TODO we count on "import and index" to succeed...
		for(Entry<String, String> entry:this.predefinedVocabsNames.entrySet()){
			//the default is to add the vocabulary as a global one i.e. predefined vocabulary
			try {
				predefinedVocabs.add(searcher.importAndIndexVocabulary(entry.getKey(), entry.getValue()));
			} catch (VocabularyImportException e) {
				//ignore exception
			}
		}
		
		project1 = new ProjectFake(); schema1 = Util.getProjectSchema(ctxt, project1);
		project2 = new ProjectFake(); schema2 = Util.getProjectSchema(ctxt, project2);
	}
	
	@Test
	//verify suugestTermCommand works...
	public void suggestTermTest() throws ServletException, IOException, JSONException{
		SuggestTermCommandFake searchProject1 = new SuggestTermCommandFake(ctxt,project1);
		//search for rdf:ty ==> non empty
		assertSearchNotEmpty(searchProject1, "rdf:ty");
		//search for ns ==> one result
		assertSearchHitsSize(searchProject1, "ns",1);
		//search for ns:poo ==> one result
		assertSearchHitsSize(searchProject1, "ns:poo",1);
		//search for rdfs: ==> empty result
		assertSearchHitsSize(searchProject1, "rdfs:",0);
		//search for n: ==> empty result
		assertSearchHitsSize(searchProject1, "n:",0);
	}
	
	@Test
	//verify search works properly after adding and importing a new vocabulary and that other projects are not affected
	public void importVocabularyTest() throws VocabularyImportException, VocabularyIndexException, PrefixExistException, ServletException, IOException, JSONException{
		SuggestTermCommandFake searchProject1 = new SuggestTermCommandFake(ctxt,project1);
		SuggestTermCommandFake searchProject2 = new SuggestTermCommandFake(ctxt,project2);
		//Guard assertion
		assertSearchHitsSize(searchProject1,"void:",0);
		assertSearchHitsSize(searchProject2,"void:",0);
		
		importVocabulary(project1, "void", "http://rdfs.org/ns/void#");
		
		assertSearchNotEmpty(searchProject1,"void:");
		//project2 is intact
		assertSearchHitsSize(searchProject2,"void:",0);
	}
	
	@Test
	//verify search works properly after refreshing an existing vocabulary and that other projects are not affected
	public void refreshVocabularyTest() throws VocabularyImportException, VocabularyIndexException, PrefixExistException, ServletException, IOException, JSONException{
		SuggestTermCommandFake searchProject1 = new SuggestTermCommandFake(ctxt,project1);
		SuggestTermCommandFake searchProject2 = new SuggestTermCommandFake(ctxt,project2);
		//Guard assertion
		assertSearchNotEmpty(searchProject1,"rdf:");
		assertSearchNotEmpty(searchProject2,"rdf:");
		assertSearchHitsSize(searchProject1, "ns",1);
		assertSearchHitsSize(searchProject2, "ns",1);

		refreshVocabulary(project1, "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		refreshVocabulary(project2, "ns", "http://doesnotexist.com/ns#");
		
		assertSearchNotEmpty(searchProject1,"rdf:");
		assertSearchNotEmpty(searchProject2,"rdf:");
		assertSearchHitsSize(searchProject1, "ns",1);
		assertSearchHitsSize(searchProject2, "ns",1);
	}
	
	@Test
	//verify search works properly after deleting an existing vocabulary and that other projects are not affected
	public void deleteVocabularyTest() throws VocabularyImportException, VocabularyIndexException, PrefixExistException, ServletException, IOException, JSONException{
		SuggestTermCommandFake searchProject2 = new SuggestTermCommandFake(ctxt,project2);
		SuggestTermCommandFake searchProject1 = new SuggestTermCommandFake(ctxt,project1);
		//Guard assertion
		assertSearchNotEmpty(searchProject2,"dct:");
		assertSearchNotEmpty(searchProject1,"dct:");

		deleteVocabulary(project2, "dct");
		
		assertSearchHitsSize(searchProject2, "dct:",0);
		assertSearchNotEmpty(searchProject1,"dct:");
	}
	
	@AfterClass
	public void tearDown() throws Exception{
		this.searcher.dispose();
		FileUtils.deleteDirectory(new File(TEMP_TEST_DIRECTORY));
	}
	
	private void assertSearchNotEmpty(SuggestTermCommandFake cmd,String query) throws ServletException, IOException, JSONException{
		JSONObject o = getSearchResult(cmd,query);
		assertTrue(o.getJSONArray("result").length()>0);
	}
	
	private void assertSearchHitsSize(SuggestTermCommandFake cmd,String query, int expected) throws ServletException, IOException, JSONException{
		JSONObject o = getSearchResult(cmd,query);
		assertEquals(o.getJSONArray("result").length(),expected);
	}
	
	private void importVocabulary(Project project, String name, String uri) throws ServletException, IOException{
		AddPrefixCommand cmd  = new AddPrefixCommandFake(ctxt, project);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		request.addParameter("name", name);
		request.addParameter("uri", uri);
		request.addParameter("project", String.valueOf(project.id));
		
		cmd.doGet(request, response);
	}
	
	private void refreshVocabulary(Project project, String name, String uri) throws ServletException, IOException{
		RefreshVocabularyCommand cmd  = new RefreshVocabularyCommandFake(ctxt, project);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		request.addParameter("name", name);
		request.addParameter("uri", uri);
		request.addParameter("project", String.valueOf(project.id));
		
		cmd.doPost(request, response);
	}
	
	private void deleteVocabulary(Project project, String name) throws ServletException, IOException{
		DeleteVocabularyCommand cmd  = new DeleteVocabularyCommandFake(ctxt, project);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		request.addParameter("name", name);
		request.addParameter("project", String.valueOf(project.id));
		
		cmd.doPost(request, response);
	}
	
	private JSONObject getSearchResult(SuggestTermCommandFake cmd, String query) throws ServletException, IOException, JSONException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		//	we are obliged to use parameters available from the autocomplete library... that's why name are not intuitive 
		request.addParameter("type_strict", "property");
		request.addParameter("type", String.valueOf(cmd.project.id));
		request.addParameter("prefix", query);
	
		cmd.doGet(request, response);
	
		JSONObject o = new JSONObject(response.getContentAsString());
		return o;
	}
	
	private final class PredefinedVocabularyManagerFake extends PredefinedVocabularyManager{
		@Override
		public Map<String, String> getPredefinedPrefixesMap() {
			return predefinedVocabsNames;
		}
	}
	
	private final class DeleteVocabularyCommandFake extends DeleteVocabularyCommand{
		private Project project;
		public DeleteVocabularyCommandFake(ApplicationContext ctxt,Project project) {
			super(ctxt);
			this.project = project;
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project;
		}
	}
	
	private static class SuggestTermCommandFake extends SuggestTermCommand{

		private Project project;
		public SuggestTermCommandFake(ApplicationContext ctxt,Project project) {
			super(ctxt);
			this.project = project;
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project;
		}
	}
	
	private static class AddPrefixCommandFake extends AddPrefixCommand{

		private Project project;
		public AddPrefixCommandFake(ApplicationContext ctxt,Project project) {
			super(ctxt);
			this.project = project;
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project;
		}
	}
	
	private static class RefreshVocabularyCommandFake extends RefreshVocabularyCommand{

		private Project project;
		public RefreshVocabularyCommandFake(ApplicationContext ctxt,Project project) {
			super(ctxt);
			this.project = project;
		}

		@Override
		protected Project getProject(HttpServletRequest request)
				throws ServletException {
			return project;
		}
	}
}
