package com.metaweb.gridworks.clustering.binning;

import org.apache.commons.codec.language.Metaphone;

public class MetaphoneKeyer extends Keyer {

    private Metaphone _metaphone = new Metaphone();
    
    public String key(String s, Object... o) {
        return _metaphone.metaphone(s);
    }

}
