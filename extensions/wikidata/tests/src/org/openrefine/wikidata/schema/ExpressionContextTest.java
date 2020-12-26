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

package org.openrefine.wikidata.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;

public class ExpressionContextTest extends RefineTest {

    GridState grid = null;
    List<Row> rows = null;

    @BeforeMethod
    public void setUp() {
        grid = createGrid(new String[] { "a", "b" },
                new Serializable[][] { { "c", null }, { "d", "e" } });
        rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
    }

    @Test
    public void testGetCellByColumnName() {
        ExpressionContext ctxt = new ExpressionContext("foo:", "https://www.wikidata.org/w/api.php", 1, rows.get(1), grid.getColumnModel(),
                null);
        assertEquals("e", ctxt.getCellByName("b").value);
    }

    @Test
    public void testNonExistentColumn() {
        ExpressionContext ctxt = new ExpressionContext("foo:", "https://www.wikidata.org/w/api.php", 1, rows.get(1), grid.getColumnModel(),
                null);
        assertNull(ctxt.getCellByName("auie"));
    }

    @Test
    public void testGetRowId() {
        ExpressionContext ctxt = new ExpressionContext("foo:", "https://www.wikidata.org/w/api.php", 1, rows.get(1), grid.getColumnModel(),
                null);
        assertEquals(1, ctxt.getRowId());
    }
}
