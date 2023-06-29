/*******************************************************************************
 * Copyright (C) 2012,2023, OpenRefine contributors
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

package org.openrefine.operations.row;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.expr.functions.FacetCount;
import org.openrefine.grel.Function;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class RowRemovalOperationTests extends RefineTest {

    // Equivalent to duplicate facet on Column A with true selected
    static final String ENGINE_JSON_DUPLICATES = "{\"facets\":[{\"type\":\"list\",\"name\":\"facet A\",\"columnName\":\"Column A\",\"expression\":\"facetCount(value, 'value', 'Column A') > 1\",\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":true,\"l\":\"true\"}}],\"selectBlank\":false,\"selectError\":false,\"invert\":false}],\"mode\":\"row-based\"}}";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-removal", RowRemovalOperation.class);
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project project;
    Properties options;
    EngineConfig engine_config;
    Engine engine;
    Properties bindings;

    @Test
    public void serializeRowRemovalOperation() throws IOException {
        String json = "{"
                + "\"op\":\"core/row-removal\","
                + "\"description\":\"Remove rows\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowRemovalOperation.class), json);
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        project = createProjectWithColumns("RowRemovalOperationTests", "Column A");

        engine = new Engine(project);
        engine_config = EngineConfig.reconstruct(ENGINE_JSON_DUPLICATES);
        engine.initializeFromConfig(engine_config);
        engine.setMode(Engine.Mode.RowBased);

        bindings = new Properties();
        bindings.put("project", project);

    }

    @AfterMethod
    public void TearDown() {
        project = null;
        engine = null;
        bindings = null;
    }

    private void checkRowCounts(int all, int filtered) {
        engine.getAllRows().accept(project, new CountVerificationRowVisitor(all));
        engine.getAllFilteredRows().accept(project, new CountVerificationRowVisitor(filtered));

        Function fc = new FacetCount();
        Integer count = (Integer) fc.call(bindings, new Object[] { "a", "value", "Column A" });
        Assert.assertEquals(count.intValue(), filtered);
    }

    /**
     * Test for issue 567. Problem doesn't seem to occur when testing interactively, but this demonstrates that the
     * facet count cache can get stale after row removal operations
     *
     * @throws Exception
     */
    @Test
    public void testIssue567() throws Exception {
        for (int i = 0; i < 5; i++) {
            Row row = new Row(5);
            row.setCell(0, new Cell(i < 4 ? "a" : "b", null));
            project.rows.add(row);
        }
        checkRowCounts(5, 4);

        EngineDependentOperation op = new RowRemovalOperation(engine_config);
        HistoryEntry historyEntry = op.createProcess(project, options).performImmediate();
        checkRowCounts(1, 0);

        historyEntry.revert(project);
        checkRowCounts(5, 4);
    }

    class CountVerificationRowVisitor implements RowVisitor {

        private int count = 0;
        private int target;

        private CountVerificationRowVisitor(int targetCount) {
            target = targetCount;
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            count++;
            return false;
        }

        @Override
        public void start(Project project) {
            count = 0;
        }

        @Override
        public void end(Project project) {
            Assert.assertEquals(count, target);
        }
    }

}
