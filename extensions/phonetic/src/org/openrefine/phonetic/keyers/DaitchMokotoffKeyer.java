
package org.openrefine.phonetic.keyers;

import com.google.refine.clustering.binning.Keyer;
import org.apache.commons.codec.language.DaitchMokotoffSoundex;

public class DaitchMokotoffKeyer extends Keyer {

    protected DaitchMokotoffSoundex encoder = new DaitchMokotoffSoundex();

    @Override
    public String key(String string, Object... params) {
        return encoder.encode(string);
    }

}
