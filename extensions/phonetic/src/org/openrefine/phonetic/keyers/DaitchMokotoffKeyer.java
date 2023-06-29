
package org.openrefine.phonetic.keyers;

import org.apache.commons.codec.language.DaitchMokotoffSoundex;

import org.openrefine.clustering.binning.Keyer;

public class DaitchMokotoffKeyer extends Keyer {

    protected DaitchMokotoffSoundex encoder = new DaitchMokotoffSoundex();

    @Override
    public String key(String string, Object... params) {
        return encoder.encode(string);
    }

}
