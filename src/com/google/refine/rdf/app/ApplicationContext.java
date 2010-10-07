package com.google.refine.rdf.app;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;

import com.google.refine.rdf.vocab.IPredefinedVocabularyManager;
import com.google.refine.rdf.vocab.IVocabularySearcher;
import com.google.refine.rdf.vocab.imp.PredefinedVocabularyManager;
import com.google.refine.rdf.vocab.imp.VocabularySearcher;

public class ApplicationContext {

	private File workingDir;
	private IPredefinedVocabularyManager predefinedVocabularyManager;
	private IVocabularySearcher vocabularySearcher;
	private static ApplicationContext instance;
	
	/**
	 * @return
	 *  This is not singleton pattern but I will create only one instance... and add it in the {@link #init(File)} method 
	 */
	public static ApplicationContext instance(){
		return instance;
	}
	
	public IPredefinedVocabularyManager getPredefinedVocabularyManager() {
		return predefinedVocabularyManager;
	}
	
	public IVocabularySearcher getVocabularySearcher() {
		return vocabularySearcher;
	}

	protected void init(File workingDir) throws IOException, JSONException{
		this.workingDir = workingDir;
		this.vocabularySearcher = new VocabularySearcher(this.workingDir);
		this.predefinedVocabularyManager = new PredefinedVocabularyManager(this,this.workingDir);
		instance = this;
	}

	public void setPredefinedVocabularyManager(
			IPredefinedVocabularyManager predefinedVocabularyManager) {
		this.predefinedVocabularyManager = predefinedVocabularyManager;
	}

	public void setVocabularySearcher(IVocabularySearcher vocabularySearcher) {
		this.vocabularySearcher = vocabularySearcher;
	}
}
