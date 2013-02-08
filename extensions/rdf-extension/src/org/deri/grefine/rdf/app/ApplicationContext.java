package org.deri.grefine.rdf.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.deri.grefine.rdf.vocab.IPredefinedVocabularyManager;
import org.deri.grefine.rdf.vocab.IVocabularySearcher;
import org.deri.grefine.rdf.vocab.PrefixManager;
import org.deri.grefine.rdf.vocab.imp.PredefinedVocabularyManager;
import org.deri.grefine.rdf.vocab.imp.VocabularySearcher;
import org.json.JSONException;


public class ApplicationContext {

	private File workingDir;
	private IPredefinedVocabularyManager predefinedVocabularyManager;
	private IVocabularySearcher vocabularySearcher;
	private PrefixManager prefixManager;
	
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
		InputStream in = this.getClass().getResourceAsStream("/files/prefixes");
		this.prefixManager = new PrefixManager(in);
	}

	public void setPredefinedVocabularyManager(
			IPredefinedVocabularyManager predefinedVocabularyManager) {
		this.predefinedVocabularyManager = predefinedVocabularyManager;
	}

	public void setVocabularySearcher(IVocabularySearcher vocabularySearcher) {
		this.vocabularySearcher = vocabularySearcher;
	}
	
	public PrefixManager getPrefixManager() {
		return prefixManager;
	}
	
}
