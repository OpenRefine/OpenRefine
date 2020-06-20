/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.operations.column;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.operations.Operation.NotImmediateOperationException;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.column.ColumnSplitOperation.ColumnSplitChange;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnSplitOperationTests extends RefineTest {

    protected GridState toSplit;
    protected GridState initialState;

    @BeforeTest
    public void createSplitProject() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        toSplit = createGrid(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "aBcDe", "d12t" },
                        { ",,d", "aFHiJ", "f34t" },
                        { ",,,,", "aT23L", "g18te" },
                        { 12, "b", "h" },
                        { new EvalError("error"), "a", "" },
                        { "12,true", "b", "g1" }
                });
        initialState = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });

    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "column-split", ColumnSplitOperation.class);
    }

    @Test
    public void serializeColumnSplitOperationBySeparator() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by separator\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"separator\",\n" +
                "    \"separator\": \"e\",\n" +
                "    \"regex\": false,\n" +
                "    \"maxColumns\": 0\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeColumnSplitOperationByLengths() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by field lengths\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"lengths\",\n" +
                "    \"fieldLengths\": [1,1]\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json, ParsingUtilities.defaultWriter);
    }

    @Test(expectedExceptions = Change.DoesNotApplyException.class)
    public void testDoesNotExist() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "does_not_exist", false, false, new int[] { 1, 2 }).createChange();
        SUT.apply(initialState);
    }

    @Test
    public void testSeparator() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "foo", false, false, ",", false, 0).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "foo 3", "foo 4", "foo 5", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "a");
        Assert.assertEquals(rows.get(0).getCellValue(2), "b");
        Assert.assertEquals(rows.get(0).getCellValue(3), "c");
        Assert.assertEquals(rows.get(0).getCellValue(4), null);
        Assert.assertEquals(rows.get(0).getCellValue(5), null);
        Assert.assertEquals(rows.get(0).getCellValue(6), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "d");
        Assert.assertEquals(rows.get(1).getCellValue(4), null);
        Assert.assertEquals(rows.get(1).getCellValue(5), null);
        Assert.assertEquals(rows.get(1).getCellValue(6), "aFHiJ");
        Assert.assertEquals(rows.get(3).getCellValue(0), 12);
        Assert.assertEquals(rows.get(3).getCellValue(1), null);
        Assert.assertEquals(rows.get(3).getCellValue(2), null);
        Assert.assertEquals(rows.get(3).getCellValue(3), null);
        Assert.assertEquals(rows.get(3).getCellValue(4), null);
        Assert.assertEquals(rows.get(3).getCellValue(5), null);
        Assert.assertEquals(rows.get(3).getCellValue(6), "b");
    }

    @Test
    public void testSeparatorMaxColumns() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "foo", false, false, ",", false, 2).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "a");
        Assert.assertEquals(rows.get(0).getCellValue(2), "b");
        Assert.assertEquals(rows.get(0).getCellValue(3), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "aFHiJ");
        Assert.assertEquals(rows.get(3).getCellValue(0), 12);
        Assert.assertEquals(rows.get(3).getCellValue(1), null);
        Assert.assertEquals(rows.get(3).getCellValue(2), null);
        Assert.assertEquals(rows.get(3).getCellValue(3), "b");
    }

    @Test
    public void testSeparatorDetectType() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "foo", true, false, ",", false, 2).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(5).getCellValue(0), "12,true");
        Assert.assertEquals(rows.get(5).getCellValue(1), 12L);
        Assert.assertEquals(rows.get(5).getCellValue(2), "true"); // we currently only parse numbers, curiously
        Assert.assertEquals(rows.get(5).getCellValue(3), "b");
    }

    @Test
    public void testSeparatorRemoveColumn() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "foo", true, true, ",", false, 2).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(5).getCellValue(0), 12L);
        Assert.assertEquals(rows.get(5).getCellValue(1), "true"); // we currently only parse numbers, curiously
        Assert.assertEquals(rows.get(5).getCellValue(2), "b");
    }

    @Test
    public void testRegex() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "bar", false, false, "[A-Z]", true, 0).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "bar", "bar 1", "bar 2", "bar 3", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "aBcDe");
        Assert.assertEquals(rows.get(0).getCellValue(2), "a");
        Assert.assertEquals(rows.get(0).getCellValue(3), "c");
        Assert.assertEquals(rows.get(0).getCellValue(4), "e");
        Assert.assertEquals(rows.get(0).getCellValue(5), "d12t");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "aFHiJ");
        Assert.assertEquals(rows.get(1).getCellValue(2), "a");
        Assert.assertEquals(rows.get(1).getCellValue(3), "");
        Assert.assertEquals(rows.get(1).getCellValue(4), "i");
        Assert.assertEquals(rows.get(1).getCellValue(5), "f34t");
    }

    @Test
    public void testLengths() throws DoesNotApplyException, NotImmediateOperationException {
        Change SUT = new ColumnSplitOperation(EngineConfig.ALL_ROWS, "hello", false, false, new int[] { 1, 2 }).createChange();
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "bar", "hello", "hello 1", "hello 2"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "aBcDe");
        Assert.assertEquals(rows.get(0).getCellValue(2), "d12t");
        Assert.assertEquals(rows.get(0).getCellValue(3), "d");
        Assert.assertEquals(rows.get(0).getCellValue(4), "12");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "aFHiJ");
        Assert.assertEquals(rows.get(1).getCellValue(2), "f34t");
        Assert.assertEquals(rows.get(1).getCellValue(3), "f");
        Assert.assertEquals(rows.get(1).getCellValue(4), "34");
    }

    @Test
    public void testRespectsFacets() throws DoesNotApplyException, NotImmediateOperationException {
        ColumnSplitChange SUT = (ColumnSplitChange) new ColumnSplitOperation(EngineConfig.ALL_ROWS, "foo", false, false, ",", false, 0)
                .createChange();

        Engine engine = mock(Engine.class);
        when(engine.getMode()).thenReturn(Engine.Mode.RowBased);
        when(engine.combinedRowFilters()).thenReturn(new OddRowFilter());
        when(engine.aggregateFilteredRows(any(), any())).thenReturn(3);
        ColumnSplitChange spied = spy(SUT);
        when(spied.getEngine(any())).thenReturn(engine);

        GridState result = spied.apply(toSplit);
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), null);
        Assert.assertEquals(rows.get(0).getCellValue(2), null);
        Assert.assertEquals(rows.get(0).getCellValue(3), null);
        Assert.assertEquals(rows.get(0).getCellValue(4), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "d");
    }

    protected static class OddRowFilter implements RowFilter {

        private static final long serialVersionUID = -4875472935974784439L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex % 2L == 1;
        }

    }
}
