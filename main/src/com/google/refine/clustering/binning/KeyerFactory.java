/*******************************************************************************
 * Copyright (C) 2018, Antonin Delpeuch
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

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
        // Some keyers are disabled as they are super-seeded by others
        // See https://github.com/OpenRefine/OpenRefine/pull/1906

        put("fingerprint", new FingerprintKeyer());
        put("ngram-fingerprint", new NGramFingerprintKeyer());
        // put("metaphone", new MetaphoneKeyer());
        // put("double-metaphone", new DoubleMetaphoneKeyer());
        put("metaphone3", new Metaphone3Keyer());
        // put("soundex", new SoundexKeyer());
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
     * Set of available keyer, by names. The first keyer is considered the default one.
     */
    public static List<String> getKeyerNames() {
        return Collections.unmodifiableList(_keyerNames);
    }
}
