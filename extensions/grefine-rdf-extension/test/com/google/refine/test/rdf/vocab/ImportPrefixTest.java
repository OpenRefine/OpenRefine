package com.google.refine.test.rdf.vocab;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.testng.annotations.Test;

import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyImporter;
import com.google.refine.rdf.vocab.imp.VocabularySearcher;

import static org.testng.Assert.*;

public class ImportPrefixTest {

	@Test
	public void testImportAndSeach()throws Exception{
		VocabularyImporter fakeImporter = new FakeImporter();
		VocabularySearcher searcher = new VocabularySearcher(new File("tmp"));
		searcher.importAndIndexVocabulary("foaf", "http://xmlns.com/foaf/0.1/", "http://xmlns.com/foaf/0.1/","1", fakeImporter);
		
		assertFalse(searcher.searchClasses("foaf:P", "1").isEmpty());
	}
	
}

class FakeImporter extends VocabularyImporter{

	@Override
	public void importVocabulary(String name, String uri, String fetchUrl,
			List<RDFSClass> classes, List<RDFSProperty> properties)	throws VocabularyImportException {
		try{
			InputStream in = getClass().getResourceAsStream("../../org/deri/reconcile/files/foaf.rdf");
			Repository repos = getRepository(in,RDFFormat.RDFXML);
			getTerms(repos, name, uri, classes, properties);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private Repository getRepository(InputStream in, RDFFormat format) throws Exception{
		Repository therepository = new SailRepository(new MemoryStore());
		therepository.initialize();
		RepositoryConnection con = therepository.getConnection();
		con.add(in, "", format);
		con.close();
		return therepository;
	}
	
}