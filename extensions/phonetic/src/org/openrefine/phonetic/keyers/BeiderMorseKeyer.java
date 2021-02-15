
package org.openrefine.phonetic.keyers;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.bm.BeiderMorseEncoder;

import org.openrefine.clustering.binning.Keyer;

public class BeiderMorseKeyer extends Keyer {

    protected BeiderMorseEncoder encoder = new BeiderMorseEncoder();

    @Override
    public String key(String string, Object... params) {
        try {
            /*
             * Beider Morse encoding can return multiple phonetic encodings, separated by |. Ideally the Keyer interface
             * should be changed to allow for multiple values to be returned (and the clustering code should be adapted
             * accordingly).
             * 
             * As a simple workaround we only return the first value. We could also return the entire list but it would
             * make matching harder.
             */
            return encoder.encode(string).split("\\|")[0];
        } catch (EncoderException e) {
            return string;
        }
    }

}
