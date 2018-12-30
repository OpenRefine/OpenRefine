package org.openrefine.phonetic.keyers;

import com.google.refine.clustering.binning.Keyer;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.bm.BeiderMorseEncoder;

public class BeiderMorseKeyer extends Keyer {
    
    protected BeiderMorseEncoder encoder = new BeiderMorseEncoder();

    @Override
    public String key(String string, Object... params) {
        try {
            return encoder.encode(string);
        } catch (EncoderException e) {
            return string;
        }
    }

}
