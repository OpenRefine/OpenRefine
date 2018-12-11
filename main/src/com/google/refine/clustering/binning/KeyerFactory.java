package com.google.refine.clustering.binning;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Registry of keyers for clustering.
 * 
 * @author Antonin Delpeuch
 *
 */
public class KeyerFactory {

	static final private Map<String, Keyer> _keyers = new HashMap<String, Keyer>();
	// We cannot derive this from the hashmap as the order matters
	static final private List<String> _keyerNames = new LinkedList<>();

    static {
        put("fingerprint", new FingerprintKeyer());
        put("ngram-fingerprint", new NGramFingerprintKeyer());
        put("metaphone", new MetaphoneKeyer());
        put("double-metaphone", new DoubleMetaphoneKeyer());
        put("metaphone3", new Metaphone3Keyer());
        put("soundex", new SoundexKeyer());
        put("cologne-phonetic", new ColognePhoneticKeyer());
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
    	_keyerNames.add(name);
    }
    
    /**
     * Set of available keyer, by names.
     * The first keyer is considered the default one.
     */
    public static List<String> getKeyerNames() {
    	return Collections.unmodifiableList(_keyerNames);
    }
}
