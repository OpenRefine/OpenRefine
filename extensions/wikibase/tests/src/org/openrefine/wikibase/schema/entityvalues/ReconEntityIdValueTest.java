/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema.entityvalues;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

import com.google.refine.model.Recon;

public class ReconEntityIdValueTest {

    private ReconEntityIdValue newItem = TestingData.makeNewItemIdValue(1234L, "new item");
    private ReconEntityIdValue sameNewItem = TestingData.makeNewItemIdValue(1234L, "different text");
    private ReconEntityIdValue differentNewItem = TestingData.makeNewItemIdValue(7890L, "new item");
    private ReconEntityIdValue newProp = TestingData.makeNewPropertyIdValue(1234L, "new prop");
    private ReconEntityIdValue existingProp = TestingData.makeMatchedPropertyIdValue("P53", "new prop");
    private ReconEntityIdValue existingItem = TestingData.makeMatchedItemIdValue("Q42", "existing item");

    @Test
    public void testIsNew() {
        assertTrue(newItem.isNew());
        assertFalse(existingItem.isNew());
    }

    @Test
    public void testGetLabel() {
        assertEquals("new item", newItem.getLabel());
        assertEquals("existing item", existingItem.getLabel());
    }

    @Test
    public void testGetTypes() {
        String[] types = { "Q5" };
        Recon matchedRecon = TestingData.makeMatchedRecon("Q453", "other item", types);
        ReconEntityIdValue existingIdWithTypes = new ReconItemIdValue(matchedRecon, "cell content");
        assertEquals(Collections.singletonList("Q5"), existingIdWithTypes.getTypes());
        assertEquals(Collections.emptyList(), existingItem.getTypes());
        assertEquals(Collections.emptyList(), newItem.getTypes());
    }

    @Test
    public void testGetId() {
        assertEquals("Q42", existingItem.getId());
        assertEquals("Q1234", newItem.getId());
        assertEquals("P53", existingProp.getId());
        assertEquals("P1234", newProp.getId());
    }

    @Test
    public void testGetIri() {
        assertEquals("http://www.wikidata.org/entity/Q42", existingItem.getIri());
        assertEquals(EntityIdValue.SITE_LOCAL + "Q1234", newItem.getIri());
    }

    @Test
    public void testGetSiteIri() {
        assertEquals("http://www.wikidata.org/entity/", existingItem.getSiteIri());
        assertEquals(EntityIdValue.SITE_LOCAL, newItem.getSiteIri());
    }

    @Test
    public void testEquality() {
        // simple cases
        assertEquals(newItem, newItem);
        assertEquals(existingItem, existingItem);
        assertNotEquals(newItem, existingItem);
        assertNotEquals(existingItem, newItem);

        // a matched cell is equal to the canonical entity id of its item
        assertEquals(Datamodel.makeWikidataItemIdValue("Q42"), existingItem);
        // just checking this is symmetrical
        assertEquals(existingItem, Datamodel.makeWikidataItemIdValue("Q42"));

        // new item equality relies on the cell's recon id
        assertEquals(newItem, sameNewItem);
        assertNotEquals(newItem, differentNewItem);
        // and on datatype
        assertNotEquals(newProp, newItem);
    }

    @Test
    public void testHashCode() {
        assertEquals(newItem.hashCode(), sameNewItem.hashCode());
        assertEquals(existingItem.hashCode(), Datamodel.makeWikidataItemIdValue("Q42").hashCode());
    }

    @Test
    public void testGetRecon() {
        assertEquals(newItem.getReconInternalId(), newItem.getRecon().id);
    }

    @Test
    public void testToString() {
        assertTrue(existingItem.toString().contains("Q42"));
        assertTrue(newItem.toString().contains("new"));
    }
}
