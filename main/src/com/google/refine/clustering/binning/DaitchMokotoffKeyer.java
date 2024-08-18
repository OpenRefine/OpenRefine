
package com.google.refine.clustering.binning;

import org.apache.commons.codec.language.DaitchMokotoffSoundex;

public class DaitchMokotoffKeyer extends Keyer {

    protected DaitchMokotoffSoundex encoder = new DaitchMokotoffSoundex();

    @Override
    public String key(String string, Object... params) {
        return encoder.encode(string);
    }

}
