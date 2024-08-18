
package com.google.refine.clustering.binning;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class BeiderMorseKeyerTest {

    Keyer keyer = new BeiderMorseKeyer();

    @Test
    public void testKey() {
        assertEquals(keyer.key("Alphonse"), "YlfYnzi");
    }

    @Test
    public void testAccents() {
        assertEquals(keyer.key("Éléonore"), "ilionor");
    }

    @Test
    public void testEmpty() {
        assertEquals(keyer.key(""), "");
    }

}
