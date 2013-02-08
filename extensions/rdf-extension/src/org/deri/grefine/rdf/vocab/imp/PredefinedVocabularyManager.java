package org.deri.grefine.rdf.vocab.imp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.IPredefinedVocabularyManager;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyImporter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PredefinedVocabularyManager implements IPredefinedVocabularyManager{
	final static Logger logger = LoggerFactory.getLogger("predefined_vocabulary_manager");
	private static final String PREDEFINED_VOCABS_FILE_NAME = "predefined_vocabs.tsv";
	private static final String SAVED_VOCABULARIES_FILE_NAME = "vocabularies_meta.json";
	
	private final File workingDir;
	private ApplicationContext applicationContext;
	private Map<String, Vocabulary> predefinedVocabulariesMap = new HashMap<String,Vocabulary>();
	
	public PredefinedVocabularyManager(ApplicationContext ctxt, File workingDir) throws IOException, JSONException{
		this.workingDir = workingDir;
		this.applicationContext = ctxt;
		try{
			reconstructVocabulariesFromFile();
		}catch(FileNotFoundException ex){
			addPredefinedVocabularies();
			save();
		}
	}
	
	public Map<String,Vocabulary> getPredefinedVocabulariesMap(){
		return predefinedVocabulariesMap;
	}

	/*
	 * Private methods
	 */
	private void addPredefinedVocabularies() throws IOException {
		InputStream in = getPredefinedVocabularyFile();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringTokenizer tokenizer;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			tokenizer = new StringTokenizer(strLine, "\t");
			try {
				String name = tokenizer.nextToken();
				String uri = tokenizer.nextToken();
				String url = tokenizer.nextToken();
				//import and index
				this.applicationContext.getVocabularySearcher().importAndIndexVocabulary(name, uri,url, new VocabularyImporter());
				this.predefinedVocabulariesMap.put(name,new Vocabulary(name, uri));
			} catch (Exception e) {
				// predefined vocabularies are not defined properly
				// ignore the exception, just log it
				logger.warn("unable to add predefined vocabularies", e);
			}

		}
		br.close();
		applicationContext.getVocabularySearcher().update();
	}
	
	protected InputStream getPredefinedVocabularyFile(){
		return this.getClass().getResourceAsStream(PREDEFINED_VOCABS_FILE_NAME);
	}
	
	private void reconstructVocabulariesFromFile() throws IOException, JSONException{
		File vocabulariesFile =  new File(workingDir, SAVED_VOCABULARIES_FILE_NAME);
		if(vocabulariesFile.exists()){
			load();
		}else{
			throw new FileNotFoundException();
		}
	}
	
	private void save()	throws IOException {
        File tempFile = new File(workingDir, "vocabs.temp.json");
        try {
            saveToFile(tempFile);
        } catch (Exception e) {
        	e.printStackTrace();
            logger.error("Failed to save project metadata",e);
            return;
        }

        File file = new File(workingDir, SAVED_VOCABULARIES_FILE_NAME);
        File oldFile = new File(workingDir, "vocabs.old.json");

        if (file.exists()) {
            file.renameTo(oldFile);
        }
        tempFile.renameTo(file);
        if (oldFile.exists()) {
            oldFile.delete();
        }
	}
	
	private void saveToFile(File metadataFile) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            write(jsonWriter,new Properties());
        } finally {
            writer.close();
        }
    }

    protected void load() throws IOException, JSONException{
    	File vocabsFile = new File(workingDir,SAVED_VOCABULARIES_FILE_NAME);
    	FileReader reader = new FileReader(vocabsFile);
        try {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();
            JSONArray prefixes = obj.getJSONArray("prefixes");
            for(int i=0;i<prefixes.length();i++){
            	JSONObject p = prefixes.getJSONObject(i);
            	String name = p.getString("name");
            	String uri = p.getString("uri");
            	this.predefinedVocabulariesMap.put(name,new Vocabulary(name,uri));
            }
        } finally {
            reader.close();
        }

    }
    
    private void write(JSONWriter writer,Properties options) throws JSONException{
    	writer.object();
    	writer.key("prefixes");
    	writer.array();
    	for(Vocabulary v:this.predefinedVocabulariesMap.values()){
    		v.write(writer);
    	}
    	writer.endArray();
		writer.endObject();
	}
    
    //this is added just to enable testing
    PredefinedVocabularyManager(){
    	this.workingDir = null;
    }
}
