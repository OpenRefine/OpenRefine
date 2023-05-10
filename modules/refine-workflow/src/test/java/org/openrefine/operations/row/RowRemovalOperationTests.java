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

package org.openrefine.operations.row;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.functions.FacetCount;
import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.Function;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RowRemovalOperationTests extends RefineTest {

    // Equivalent to duplicate facet on Column A with true selected
    static final String ENGINE_JSON_DUPLICATES = "{\"facets\":"
            + "[{\"type\":\"list\","
            + "\"name\":\"facet A\","
            + "\"columnName\":\"foo\","
            + "\"expression\":\"facetCount(value, 'value', 'foo') > 1\","
            + "\"omitBlank\":false,"
            + "\"omitError\":false,"
            + "\"selection\":[{\"v\":{\"v\":true,\"l\":\"true\"}}],"
            + "\"selectBlank\":false,"
            + "\"selectError\":false,"
            + "\"invert\":false}],"
            + "\"mode\":\"row-based\"}}";
    EngineConfig engineConfig;

    @BeforeTest
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "row-removal", RowRemovalOperation.class);
        FacetConfigResolver.registerFacetConfig("core", "list", ListFacetConfig.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        ControlFunctionRegistry.registerFunction("facetCount", new FacetCount());
        engineConfig = EngineConfig.reconstruct(ENGINE_JSON_DUPLICATES);
    }

    @Test
    public void serializeRowRemovalOperation() throws IOException {
        String json = "{"
                + "\"op\":\"core/row-removal\","
                + "\"description\":\"Remove rows\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowRemovalOperation.class), json, ParsingUtilities.defaultWriter);
    }

    Grid initial;
    ListFacetConfig facet;

    @BeforeTest
    public void createProject() {
        initial = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        facet = new ListFacetConfig("hello", "grel:value", "hello");
    }

    @Test
    public void testRemoveRows() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        Change change = new RowRemovalOperation(engineConfig).createChange();
        Change.ChangeResult changeResult = change.apply(initial, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testRemoveRecords() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Change change = new RowRemovalOperation(engineConfig).createChange();
        Change.ChangeResult changeResult = change.apply(initial, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" }
                });

        assertGridEquals(applied, expected);
    }

    private void checkRowCounts(Project project, long all, long filtered) {
        Engine engine = new Engine(project.getCurrentGrid(), engineConfig, project.getId());

        assertEquals(engine.getTotalCount(), all);
        assertEquals(engine.getFilteredCount(), filtered);
    }

    /**
     * Test for issue 567. Problem doesn't seem to occur when testing interactively, but this demonstrates that the
     * facet count cache can get stale after row removal operations
     */
    @Test
    public void testIssue567() throws Exception {
        Project project = createProject(new String[] { "foo" },
                new Serializable[][] {
                        { "a" },
                        { "a" },
                        { "a" },
                        { "a" },
                        { "b" }
                });

        checkRowCounts(project, 5, 4);

        EngineDependentOperation op = new RowRemovalOperation(engineConfig);
        project.getHistory().addEntry(op);
        checkRowCounts(project, 1, 0);

        project.getHistory().undoRedo(0L);
        checkRowCounts(project, 5, 4);
    }

}
