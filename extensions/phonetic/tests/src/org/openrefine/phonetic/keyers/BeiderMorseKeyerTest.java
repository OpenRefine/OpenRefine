package org.openrefine.phonetic.keyers;

import com.google.refine.clustering.binning.Keyer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class BeiderMorseKeyerTest  {
    
    Keyer keyer = new BeiderMorseKeyer();
    
    @Test
    public void testKey() {
        assertTrue(keyer.key("Alphonse").contains("alponzi"));
    }
    
    @Test
    public void testAccents() {
        assertEquals(keyer.key("Éléonore"), "ilionor|ilionori");
    }

}
