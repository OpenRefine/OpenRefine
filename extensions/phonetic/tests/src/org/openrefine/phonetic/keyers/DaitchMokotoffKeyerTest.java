
package org.openrefine.phonetic.keyers;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.refine.clustering.binning.Keyer;

public class DaitchMokotoffKeyerTest {

    protected Keyer keyer = new DaitchMokotoffKeyer();

    @Test
    public void testDaitchMokotoff() {
        assertEquals(keyer.key("Alphonse"), "087640");
    }

    @Test
    public void testAccents() {
        assertEquals(keyer.key("Éléonore"), "086900");
    }

    @Test
    public void testEmpty() {
        assertEquals(keyer.key(""), "000000");
    }
}
