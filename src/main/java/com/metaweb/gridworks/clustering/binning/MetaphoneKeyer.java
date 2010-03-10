package com.metaweb.gridworks.clustering.binning;

import org.apache.commons.codec.language.Metaphone;

public class MetaphoneKeyer extends Keyer {

    private Metaphone _metaphone;

    public MetaphoneKeyer() {
        _metaphone = new Metaphone();
        _metaphone.setMaxCodeLen(2000);
    }
    
    public String key(String s, Object... o) {
        return _metaphone.metaphone(s);
    }

}
