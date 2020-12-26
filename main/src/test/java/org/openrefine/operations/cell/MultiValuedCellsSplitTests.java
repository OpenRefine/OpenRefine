/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class MultiValuedCellsSplitTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation("core", "multivalued-cell-split", MultiValuedCellSplitOperation.class);
    }

    @Test
    public void serializeMultiValuedCellSplitOperationWithSeparator() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-split\","
                + "\"description\":\"Split multi-valued cells in column Value\","
                + "\"columnName\":\"Value\","
                + "\"keyColumnName\":\"Key\","
                + "\"mode\":\"separator\","
                + "\"separator\":\":\","
                + "\"regex\":false}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellSplitOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeMultiValuedCellSplitOperationWithLengths() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-split\","
                + "\"description\":\"Split multi-valued cells in column Value\","
                + "\"columnName\":\"Value\","
                + "\"keyColumnName\":\"Key\","
                + "\"mode\":\"lengths\","
                + "\"fieldLengths\":[1,1]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellSplitOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    GridState initialState;
    GridState smallGrid;

    @BeforeTest
    public void setUpGrid() {
        initialState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a||b", "c" },
                        { null, "c|d", "e" },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h|i", "" },
                        { null, null, "j" },
                        { null, null, null }
                });

        smallGrid = createGrid(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen" } });
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidColumn() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellSplitOperation("does_not_exist", "key", ",", false).createChange();
        SUT.apply(initialState, mock(ChangeContext.class));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidKeyColumn() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellSplitOperation("foo", "does_not_exist", ",", false).createChange();
        SUT.apply(initialState, mock(ChangeContext.class));
    }

    @Test
    public void testSplit() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellSplitOperation("foo", "key", "|", false).createChange();
        GridState applied = SUT.apply(initialState, mock(ChangeContext.class));

        GridState expectedState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "c" },
                        { null, "", null },
                        { null, "b", null },
                        { null, "c", "e" },
                        { null, "d", null },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h", "" },
                        { null, "i", "j" },
                        { null, null, null }
                });

        Assert.assertEquals(applied.getColumnModel(), initialState.getColumnModel());
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(rows, expectedState.collectRows());
    }

    @Test
    public void testSplitRespectsKeyColumn() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellSplitOperation("foo", "bar", "|", false).createChange();
        GridState applied = SUT.apply(initialState, mock(ChangeContext.class));

        GridState expectedState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "c" },
                        { null, "", null },
                        { null, "b", null },
                        { null, "c", "e" },
                        { null, "d", null },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h", "" },
                        { null, "i", null },
                        { null, null, "j" },
                        { null, null, null }
                });

        Assert.assertEquals(applied.getColumnModel(), initialState.getColumnModel());
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(rows, expectedState.collectRows());
    }

    /**
     * Test to demonstrate the intended behaviour of the function, for issue #1268
     * https://github.com/OpenRefine/OpenRefine/issues/1268
     */

    @Test
    public void testSplitMultiValuedCellsTextSeparator() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                ":",
                false).createChange();
        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<IndexedRow> rows = applied.collectRows();

        Assert.assertEquals(rows.get(0).getRow().getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "one");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsRegExSeparator() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "\\W",
                true).createChange();
        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(1), "one");
        Assert.assertEquals(rows.get(1).getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getCellValue(1), "two");
        Assert.assertEquals(rows.get(2).getCellValue(0), null);
        Assert.assertEquals(rows.get(2).getCellValue(1), "three");
        Assert.assertEquals(rows.get(3).getCellValue(0), null);
        Assert.assertEquals(rows.get(3).getCellValue(1), "four");
    }

    @Test
    public void testSplitMultiValuedCellsLengths() throws Exception {
        int[] lengths = { 4, 4, 6, 4 };

        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                lengths).createChange();

        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(1), "one:");
        Assert.assertEquals(rows.get(1).getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getCellValue(1), "two;");
        Assert.assertEquals(rows.get(2).getCellValue(0), null);
        Assert.assertEquals(rows.get(2).getCellValue(1), "three ");
        Assert.assertEquals(rows.get(3).getCellValue(0), null);
        Assert.assertEquals(rows.get(3).getCellValue(1), "four");
    }

    @Test
    public void testSplitMultiValuedCellsTextCase() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Lower}|[\\p{Lower}][\\s])(?=\\p{Upper})",
                true).createChange();

        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        int keyCol = 0;
        int valueCol = 1;

        Assert.assertEquals(rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(valueCol), "one:two;three four;five");
        Assert.assertEquals(rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(1).getCellValue(valueCol), "Six ");
        Assert.assertEquals(rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(2).getCellValue(valueCol), "Sevè");
        Assert.assertEquals(rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(3).getCellValue(valueCol), "Ëight;niné91011twelve thirteen 14Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextCaseReverse() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Upper}|[\\p{Upper}][\\s])(?=\\p{Lower})",
                true).createChange();

        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        int keyCol = 0;
        int valueCol = 1;

        Assert.assertEquals(rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(valueCol), "one:two;three four;fiveS");
        Assert.assertEquals(rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(1).getCellValue(valueCol), "ix S");
        Assert.assertEquals(rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(2).getCellValue(valueCol), "evèË");
        Assert.assertEquals(rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(3).getCellValue(valueCol), "ight;niné91011twelve thirteen 14À");
        Assert.assertEquals(rows.get(4).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(4).getCellValue(valueCol), "ifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextNumber() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Digit}|[\\p{Digit}][\\s])(?=\\p{L})",
                true).createChange();

        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        int keyCol = 0;
        int valueCol = 1;

        Assert.assertEquals(rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(valueCol), "one:two;three four;fiveSix SevèËight;niné91011");
        Assert.assertEquals(rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(1).getCellValue(valueCol), "twelve thirteen 14");
        Assert.assertEquals(rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(2).getCellValue(valueCol), "Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextNumberReverse() throws Exception {
        Change change = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{L}|[\\p{L}][\\s])(?=\\p{Digit})",
                true).createChange();

        GridState applied = change.apply(smallGrid, mock(ChangeContext.class));

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());

        int keyCol = 0;
        int valueCol = 1;

        Assert.assertEquals(rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(valueCol), "one:two;three four;fiveSix SevèËight;niné");
        Assert.assertEquals(rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(1).getCellValue(valueCol), "91011twelve thirteen ");
        Assert.assertEquals(rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(rows.get(2).getCellValue(valueCol), "14Àifteen");
    }

}
