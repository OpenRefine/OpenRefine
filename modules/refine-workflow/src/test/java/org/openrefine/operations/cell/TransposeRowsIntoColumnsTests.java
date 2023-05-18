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

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class TransposeRowsIntoColumnsTests extends RefineTest {

    Grid initial;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation("core", "transpose-rows-into-columns", TransposeRowsIntoColumnsOperation.class);
    }

    @Test
    public void testTransposeRowsIntoColumnsOperation() throws Exception {
        String json = "{\"op\":\"core/transpose-rows-into-columns\","
                + "\"description\":\"Transpose every 3 cells in column start column into separate columns\","
                + "\"columnName\":\"start column\","
                + "\"rowCount\":3}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, TransposeRowsIntoColumnsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @BeforeTest
    public void setUpGrid() {
        initial = createGrid(
                new String[] { "a", "b", "c" },
                new Serializable[][] {
                        { "1", "2", "3" },
                        { "4", "5", "6" },
                        { "7", "8", "9" },
                        { "10", "11", "12" }
                });
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testDoesNotApply() throws DoesNotApplyException, ParsingException {
        Operation operation = new TransposeRowsIntoColumnsOperation("d", 2);
        operation.apply(initial, mock(ChangeContext.class));
    }

    @Test
    public void testTransposeRowsIntoColumns() throws DoesNotApplyException, ParsingException {
        Operation operation = new TransposeRowsIntoColumnsOperation("b", 2);

        Grid expected = createGrid(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "4", null, null, "6" },
                        { "7", "8", "11", "9" },
                        { "10", null, null, "12" }
                });

        assertGridEquals(operation.apply(initial, mock(ChangeContext.class)).getGrid(), expected);
    }

    @Test
    public void testTransposeRecordsIntoRows() throws DoesNotApplyException, ParsingException {
        Grid initialRecords = createGrid(
                new String[] { "a", "b", "c" },
                new Serializable[][] {
                        { "1", "2", "3" },
                        { null, "5", null },
                        { "7", "8", "9" },
                        { null, "11", null }
                });

        Operation operation = new TransposeRowsIntoColumnsOperation("b", 2);

        Grid expected = createGrid(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        assertGridEquals(operation.apply(initialRecords, mock(ChangeContext.class)).getGrid(), expected);
    }

}
