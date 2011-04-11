package com.google.refine.test.rdf.vocab;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.index.CorruptIndexException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.commands.AddPrefixCommand;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyImporter;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.rdf.vocab.imp.VocabularySearcher;

import static org.testng.Assert.*;

public class AddPrefixCommandTest{
	private static final String TEMP_TEST_DIRECTORY = "tmp_VocabularySearchRelatedCommandsTest";
	ApplicationContext ctxt;
	VocabularySearcher searcher;
	VocabularyImporter importer;
	
	String name= "foaf";
	String uri = "http://xmlns.com/foaf/0.1/";
	String projectId = "1";
	
	@BeforeClass
	public void init() throws VocabularyIndexException, IOException, PrefixExistException{
		//gurad assert 
		assertFalse(new File(TEMP_TEST_DIRECTORY).exists());
		importer = new FakeImporter();
		searcher = new FakeVocabularySearcher(new File(TEMP_TEST_DIRECTORY),this.importer);
		ctxt = new ApplicationContext();
		ctxt.setVocabularySearcher(searcher);
	}
	
	@Test
	public void testAddPrefixCommand() throws Exception{
		RdfSchema schema = new RdfSchema();
		AddPrefixCommand command = new FakeAddPrefixCommand(ctxt,schema);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		request.addParameter("name", name);
		request.addParameter("uri", uri);
		request.addParameter("fetch", "web");
		request.addParameter("project", projectId);
		request.addParameter("fetch-url", uri);
		
		assertFalse(schema.getPrefixesMap().containsKey("foaf"));
		assertTrue(searcher.searchClasses("foaf:P", projectId).isEmpty());
		command.doPost(request, response);
		//verification
		
		//prefix is added to the project
		assertTrue(schema.getPrefixesMap().containsKey("foaf"));
		//search
		assertFalse(searcher.searchClasses("foaf:P", projectId).isEmpty());
	}
}

class FakeVocabularySearcher extends VocabularySearcher{

	VocabularyImporter importer;
	
	public FakeVocabularySearcher(File dir,VocabularyImporter importer) throws IOException {
		super(dir);
		this.importer = importer;
	}

	@Override
	public void importAndIndexVocabulary(String name, String uri,
			String fetchUrl, String projectId, VocabularyImporter im)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException, CorruptIndexException, IOException {
		super.importAndIndexVocabulary(name, uri, fetchUrl, projectId, this.importer);
	}
}

class FakeAddPrefixCommand extends AddPrefixCommand{

	RdfSchema schema;
	public FakeAddPrefixCommand(ApplicationContext ctxt,RdfSchema schema) {
		super(ctxt);
		this.schema = schema;
	}

	@Override
	public RdfSchema getRdfSchema(HttpServletRequest request)
			throws ServletException {
		return schema;
	}
	
	
}
