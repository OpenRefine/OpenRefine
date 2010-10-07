package com.google.refine.rdf.vocab.imp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.util.ParsingUtilities;

public class Vocabulary {
	private String name;
	private String uri;
	private VocabularyImportReport report = new VocabularyImportReport();
	private List<RDFSClass> classes = new ArrayList<RDFSClass>();
    private List<RDFSProperty> properties = new ArrayList<RDFSProperty>();

    public Vocabulary(String name, String uri){
    	this.name = name;
    	this.uri = uri;
    	this.report = new VocabularyImportReport();
    }
    public void addClass(RDFSClass clazz){
        this.classes.add(clazz);
    }
    
    public void addProperty(RDFSProperty prop){
        this.properties.add(prop);
    }

	public VocabularyImportReport getReport() {
		return report;
	}

	public List<RDFSClass> getClasses() {
		return classes;
	}

	public List<RDFSProperty> getProperties() {
		return properties;
	}
	public String getName() {
		return name;
	}
	public String getUri() {
		return uri;
	}
	
	public void setReport(VocabularyImportReport report){
		this.report = report;
	}
	
    public void write(JSONWriter writer, boolean imported)
            throws JSONException {
        writer.object();
        
        writer.key("prefix"); writer.value(name);
        writer.key("uri"); writer.value(uri);
        writer.key("imported"); writer.value(imported);
        writer.key("numOfClasses"); writer.value(report.numOfClasses);
        writer.key("numOfProperties"); writer.value(report.numOfProperties);
        writer.key("url"); writer.value(report.url);
        writer.key("importDate"); writer.value(ParsingUtilities.dateToString(report.importedDate));
        writer.key("extractors"); writer.value(report.extractors);
        
        writer.endObject();
    }

}


class VocabularyImportReport {
	public int numOfClasses;
	public int numOfProperties;
	public String extractors;
	public Date importedDate;
	public String url;
	
	
	public VocabularyImportReport(){
		this.importedDate = new Date();
		this.extractors = "[]";
	}
	
	public int getNumOfClasses() {
		return numOfClasses;
	}

	public int getNumOfProperties() {
		return numOfProperties;
	}

	public void setNumOfClasses(int numOfClasses) {
		this.numOfClasses = numOfClasses;
	}

	public void setNumOfProperties(int numOfProperties) {
		this.numOfProperties = numOfProperties;
	}

	public void setExtractors(String extractors) {
		this.extractors = extractors;
	}

	public void setImportedDate(Date importedDate) {
		this.importedDate = importedDate;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getExtractors() {
		return extractors;
	}

	public Date getImportedDate() {
		return importedDate;
	}

	public String getUrl() {
		return url;
	}
	
}
