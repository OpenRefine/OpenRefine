package com.google.refine.clustering.binning;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of keyers for clustering.
 * 
 * @author Antonin Delpeuch
 *
 */
public class KeyerFactory {

	static final protected Map<String, Keyer> _keyers = new HashMap<String, Keyer>();

    static {
        _keyers.put("fingerprint", new FingerprintKeyer());
        _keyers.put("ngram-fingerprint", new NGramFingerprintKeyer());
        _keyers.put("metaphone", new MetaphoneKeyer());
        _keyers.put("double-metaphone", new DoubleMetaphoneKeyer());
        _keyers.put("metaphone3", new Metaphone3Keyer());
        _keyers.put("soundex", new SoundexKeyer());
        _keyers.put("cologne-phonetic", new ColognePhoneticKeyer());
    }
    
    /**
     * Returns the keyer registered under a given name, or null if it does not exist.
     */
    public static Keyer get(String name) {
    	return _keyers.get(name);
    }
    
    /**
     * Registers a keyer under a code name.
     */
    public static void put(String name, Keyer keyer) {
    	_keyers.put(name, keyer);
    }
    
    /**
     * Set of available keyer, by names.
     */
    public static Set<String> getKeyerNames() {
    	return _keyers.keySet();
    }
}
