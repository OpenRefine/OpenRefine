
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Arrays;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;

public class BlankDownChangeTests extends RefineTest {

    GridState toBlankDown;
    ListFacetConfig facet;

    @BeforeTest
    public void createSplitProject() {
        toBlankDown = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        facet = new ListFacetConfig();
        facet.columnName = "hello";
        facet.setExpression("grel:value");
    }

    @Test
    public void testBlankDownRowsNoFacets() throws DoesNotApplyException {
        Change change = new BlankDownChange(EngineConfig.ALL_ROWS, "bar");
        GridState applied = change.apply(toBlankDown);

        GridState expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testBlankDownRecordsNoFacets() throws DoesNotApplyException {
        Change change = new BlankDownChange(EngineConfig.ALL_RECORDS, "bar");
        GridState applied = change.apply(toBlankDown);

        GridState expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testBlankDownRowsFacets() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"),
                new DecoratedValue("f", "f"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        Change change = new BlankDownChange(engineConfig, "bar");
        GridState applied = change.apply(toBlankDown);

        GridState expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testBlankDownRecordsFacets() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Change change = new BlankDownChange(engineConfig, "bar");
        GridState applied = change.apply(toBlankDown);

        GridState expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }
}
