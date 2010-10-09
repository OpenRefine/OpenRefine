package com.google.refine.rdf.vocab;

import java.util.Map;

public interface IPredefinedVocabularyManager {
	public Map<String, String> getPredefinedPrefixesMap();
	public void setPredefinedPrefixesMap(Map<String,String> prefixes);
}
