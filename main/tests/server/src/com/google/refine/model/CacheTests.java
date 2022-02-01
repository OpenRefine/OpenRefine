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

package com.google.refine.model;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.functions.FacetCount;
import com.google.refine.grel.Function;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.row.RowRemovalOperation;

public class CacheTests extends RefineTest {

    // Equivalent to duplicate facet on Column A with true selected
    static final String ENGINE_JSON_DUPLICATES = "{\"facets\":[{\"type\":\"list\",\"name\":\"facet A\",\"columnName\":\"Column A\",\"expression\":\"facetCount(value, 'value', 'Column A') > 1\",\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":true,\"l\":\"true\"}}],\"selectBlank\":false,\"selectError\":false,\"invert\":false}],\"mode\":\"row-based\"}}";

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

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        project = createProjectWithColumns("CacheTests", "Column A");

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
        engine.getAllRows().accept(project, new CountingRowVisitor(5));
        engine.getAllFilteredRows().accept(project, new CountingRowVisitor(4));
        Function fc = new FacetCount();
        Integer count = (Integer) fc.call(bindings, new Object[] { "a", "value", "Column A" });
        Assert.assertEquals(count.intValue(), 4);
        EngineDependentOperation op = new RowRemovalOperation(engine_config);
        op.createProcess(project, options).performImmediate();
        engine.getAllRows().accept(project, new CountingRowVisitor(1));
        engine.getAllFilteredRows().accept(project, new CountingRowVisitor(0));
        count = (Integer) fc.call(bindings, new Object[] { "a", "value", "Column A" });
        Assert.assertEquals(count.intValue(), 0);
    }

    class CountingRowVisitor implements RowVisitor {

        private int count = 0;
        private int target;

        private CountingRowVisitor(int targetCount) {
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
