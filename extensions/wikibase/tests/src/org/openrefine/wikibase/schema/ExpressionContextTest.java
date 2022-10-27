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

package org.openrefine.wikibase.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.openrefine.wikibase.testing.WikidataRefineTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.model.Project;

public class ExpressionContextTest extends WikidataRefineTest {

    Project project = null;

    @BeforeMethod
    public void setUp() {
        project = createCSVProject("a,b\nc\nd,e");
    }

    @Test
    public void testGetCellByColumnName() {
        ExpressionContext ctxt = new ExpressionContext("foo:", null, "https://www.wikidata.org/w/api.php", 1, project.rows.get(1),
                project.columnModel, null);
        assertEquals("e", ctxt.getCellByName("b").value);
    }

    @Test
    public void testNonExistentColumn() {
        ExpressionContext ctxt = new ExpressionContext("foo:", null, "https://www.wikidata.org/w/api.php", 1, project.rows.get(1),
                project.columnModel, null);
        assertNull(ctxt.getCellByName("auie"));
    }

    @Test
    public void testGetRowId() {
        ExpressionContext ctxt = new ExpressionContext("foo:", null, "https://www.wikidata.org/w/api.php", 1, project.rows.get(1),
                project.columnModel, null);
        assertEquals(1, ctxt.getRowId());
    }
}
