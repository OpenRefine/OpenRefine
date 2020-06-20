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

import java.io.Serializable;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class MultiValuedCellsJoinTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "multivalued-cell-join", MultiValuedCellJoinOperation.class);
    }

    @Test
    public void serializeMultiValuedCellJoinOperation() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-join\","
                + "\"description\":\"Join multi-valued cells in column value column\","
                + "\"columnName\":\"value column\","
                + "\"keyColumnName\":\"key column\","
                + "\"separator\":\",\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellJoinOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    GridState initialState;

    @BeforeTest
    public void setUpGrid() {
        initialState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "b" },
                        { null, "c", "d" },
                        { "record2", "", "f" },
                        { null, "g", "" },
                        { null, null, null }
                });
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidColumn() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellJoinOperation("does_not_exist", "key", ",").createChange();
        SUT.apply(initialState);
    }

    @Test
    public void testJoin() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellJoinOperation("foo", "key", ",").createChange();
        GridState state = SUT.apply(initialState);

        GridState expected = createGrid(new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a,c", "b" },
                        { null, null, "d" },
                        { "record2", "g", "f" },
                        { null, null, null } // this line is a record on its own, so it is preserved
                });

        Assert.assertEquals(state.getColumnModel(), initialState.getColumnModel());
        List<IndexedRow> rows = state.collectRows();
        List<IndexedRow> expectedRows = expected.collectRows();
        Assert.assertEquals(rows, expectedRows);
    }

    @Test
    public void testCustomKey() throws DoesNotApplyException, ParsingException {
        Change SUT = new MultiValuedCellJoinOperation("bar", "foo", ",").createChange();
        GridState state = SUT.apply(initialState);

        GridState expected = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "b" },
                        { null, "c", "d,f" },
                        { "record2", "", null },
                        { null, "g", null },
                        { null, null, null }
                });

        Assert.assertEquals(state.getColumnModel(), initialState.getColumnModel());
        List<IndexedRow> rows = state.collectRows();
        List<IndexedRow> expectedRows = expected.collectRows();
        Assert.assertEquals(rows, expectedRows);
    }

}
