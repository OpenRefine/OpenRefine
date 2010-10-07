package com.google.refine.rdf.vocab.imp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryParser.ParseException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.SearchResultItem;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.tests.ProjectFake;

import static org.testng.Assert.*;

public class VocabularySearcherTest {

	private static final String TEMP_TEST_DIRECTORY = "temp_rdf_searcher_test";
	private Map<String,String> predefinedVocabsNames = new HashMap<String, String>();
	
	List<Vocabulary> predefinedVocabs = new ArrayList<Vocabulary>();
	
	VocabularySearcher searcher;
	ApplicationContext ctxt;
	Project project1; RdfSchema schema1; 
	Project project2; RdfSchema schema2; 
	
	@BeforeClass
	public void init() throws IOException,  VocabularyIndexException, PrefixExistException{
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
	//verify that a newly created project will get the predefined vocabularies
	public void newProjectGetsPredefinedVocabs() throws ParseException, IOException{
		List<Vocabulary> project1Vocabularies = searcher.getProjectVocabularies(String.valueOf(project1.id));
		
		assertSameAsPredefindeVocabs(project1Vocabularies);
	}
	
	@Test
	//verify that search works well for a newly created project (searching the predefined vocabularies)
	public void searchTest() throws ParseException, IOException{
		List<SearchResultItem> results = searcher.searchClasses("dct:agent", (String.valueOf(project1.id)));
		assertFalse(results.isEmpty());
		results = searcher.searchProperties("dct:ti", (String.valueOf(project1.id)));
		assertFalse(results.isEmpty());
		results = searcher.searchProperties("rdfs:", (String.valueOf(project1.id)));
		assertTrue(results.isEmpty());
	}
	
	@Test
	//verify that deleting a vocabulary remove it from the index and that other projects are not affected (existing and newly created)
	public void deleteTest() throws ParseException, IOException, VocabularyIndexException{
		//Guard assertion
		assertFalse(searcher.searchProperties("rdf:ty", String.valueOf(project2.id)).isEmpty());
		assertFalse(searcher.searchProperties("rdf:ty", String.valueOf(project1.id)).isEmpty());
		
		searcher.deleteVocabulary("rdf", String.valueOf(project2.id));
		
		//test a newly created project
		Project tmpProject = new ProjectFake();
		Util.getProjectSchema(ctxt, tmpProject);
		
		assertTrue(searcher.searchProperties("rdf:ty", String.valueOf(project2.id)).isEmpty());
		//other projects are not affected
		assertFalse(searcher.searchProperties("rdf:ty", String.valueOf(project1.id)).isEmpty());
		assertFalse(searcher.searchProperties("rdf:ty", String.valueOf(tmpProject.id)).isEmpty());
	}
	
	@AfterClass
	public void tearDown() throws Exception{
		this.searcher.dispose();
		FileUtils.deleteDirectory(new File(TEMP_TEST_DIRECTORY));
	}
	
	private void assertSameAsPredefindeVocabs(List<Vocabulary> actuals){
		Map<String, String> actualVocabsMap = new HashMap<String, String>();
		for(Vocabulary v:actuals){
			actualVocabsMap.put(v.getName(), v.getUri());
		}
		assertEquals(actualVocabsMap.entrySet(), predefinedVocabsNames.entrySet());
	}
	
	private final class PredefinedVocabularyManagerFake extends PredefinedVocabularyManager{

		@Override
		public Map<String, String> getPredefinedPrefixesMap() {
			return predefinedVocabsNames;
		}
		
	}
}
