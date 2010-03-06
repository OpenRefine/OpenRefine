package com.metaweb.gridworks.clustering.binning;

import org.apache.commons.codec.language.Soundex;

public class SoundexKeyer extends Keyer {

    private Soundex _soundex = new Soundex();
    
    public String key(String s, Object... o) {
        return _soundex.soundex(s);
    }

}
