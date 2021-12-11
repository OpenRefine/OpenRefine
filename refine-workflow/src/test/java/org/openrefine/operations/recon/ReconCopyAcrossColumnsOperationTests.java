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

package org.openrefine.operations.recon;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconCopyAcrossColumnsOperationTests extends RefineTest {

    GridState initialState;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon-copy-across-columns", ReconCopyAcrossColumnsOperation.class);
    }

    @Test
    public void serializeReconCopyAcrossColumnsOperation() throws Exception {
        String json = "{\"op\":\"core/recon-copy-across-columns\","
                + "\"description\":\"Copy recon judgments from column source column to [first, second]\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"fromColumnName\":\"source column\","
                + "\"toColumnNames\":[\"first\",\"second\"],"
                + "\"judgments\":[\"matched\",\"new\"],"
                + "\"applyToJudgedCells\":true}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconCopyAcrossColumnsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @BeforeTest
    public void setupInitialState() {
        initialState = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "d", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "b", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });
    }

    @Test
    public void testReconCopyAcrossColumns() throws DoesNotApplyException {
        Change change = new ReconCopyAcrossColumnsOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                Collections.singletonList("foo"),
                Arrays.asList(Recon.Judgment.Matched, Recon.Judgment.None),
                true).createChange();

        GridState applied = change.apply(initialState, mock(ChangeContext.class));

        GridState expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { new Cell("d", testRecon("b", "j", Recon.Judgment.None)),
                                new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)),
                                new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });

        assertGridEquals(applied, expected);
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidSourceColumn() throws DoesNotApplyException {
        Change change = new ReconCopyAcrossColumnsOperation(
                EngineConfig.ALL_ROWS,
                "does_not_exist",
                Collections.singletonList("foo"),
                Arrays.asList(Recon.Judgment.Matched, Recon.Judgment.None),
                true).createChange();

        change.apply(initialState, mock(ChangeContext.class));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidTargetColumn() throws DoesNotApplyException {
        Change change = new ReconCopyAcrossColumnsOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                Collections.singletonList("does_not_exist"),
                Arrays.asList(Recon.Judgment.Matched, Recon.Judgment.None),
                true).createChange();

        change.apply(initialState, mock(ChangeContext.class));
    }
}
