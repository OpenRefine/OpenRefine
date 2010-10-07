package com.google.refine;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.rdf.vocab.imp.Vocabulary;
import com.google.refine.rdf.vocab.imp.VocabularyImporter;

import static org.testng.Assert.*;

public class VocabularyImporterTest {

	VocabularyImporter importer ;
	Vocabulary v;
	
	@BeforeMethod
	public void init(){
		importer = new VocabularyImporter();
	}
	@Test(groups={"rdf-schema-test"})
	public void importRdfsTest() throws Exception{
		String name = "rdfs";
		String uri ="http://www.w3.org/2000/01/rdf-schema#";
		v = importer.importVocabulary(name, uri);
		validateVocabularyClasses( new String[]{
				"http://www.w3.org/2000/01/rdf-schema#Class",
				"http://www.w3.org/2000/01/rdf-schema#Datatype"
		});
		
		validateVocabularyProperties( new String[]{
				"http://www.w3.org/2000/01/rdf-schema#subClassOf",
				"http://www.w3.org/2000/01/rdf-schema#label"
		});
	}
	
	@Test(groups={"rdf-schema-test"})
	public void importqbTest()throws Exception{
		String uri = "http://purl.org/linked-data/cube#";
		String name = "qb";
		v = importer.importVocabulary(name,uri);
		validateVocabularyClasses(new String[]{
				uri + "Slice",	
				uri + "Observation",
				uri + "MeasureProperty",
		});
		validateVocabularyProperties(new String[]{
				uri + "dataSet",
				uri + "observation",
		});
		
		validateNoClasses(new String[]{
				"http://purl.org/NET/scovo#Dataset"		
		});
		
		validateNoProperties(new String[]{
				"http://www.w3.org/2000/01/rdf-schema#subClassOf",
				"http://www.w3.org/2000/01/rdf-schema#label"
		});
	}
	
	private void validateVocabularyClasses(String... classes){
		for(int i=0;i<classes.length;i++){
			assertTrue(v.getClasses().contains(new RDFSClass(classes[i])),"Testing " + classes[i]);
		}
	}

	private void validateNoClasses(String... classes){
		for(int i=0;i<classes.length;i++){
			assertFalse(v.getClasses().contains(new RDFSClass(classes[i])));
		}
	}
	
	
	private void validateVocabularyProperties(String... properties){
		for(int i=0;i<properties.length;i++){
			assertTrue(v.getProperties().contains(new RDFSProperty(properties[i])),"Testing existence of " + properties[i]);
		}
	}
	
	private void validateNoProperties(String... properties){
		for(int i=0;i<properties.length;i++){
			assertFalse(v.getProperties().contains(new RDFSProperty(properties[i])),"Testing non-existence of " + properties[i]);
		}
	}
}
