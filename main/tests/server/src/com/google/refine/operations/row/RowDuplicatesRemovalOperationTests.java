
package com.google.refine.operations.row;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;

public class RowDuplicatesRemovalOperationTests extends RefineTest {

    private Project project = null;

    @BeforeMethod
    private void initProject() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        project = createProject(
                new String[] { "SITE_ID", "SITE_NUM", "SITE_NAME",
                        "LATITUDE", "LONGITUDE", "ELEVATION", "UPDATE_DATE" },
                new Serializable[][] {
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:34" },
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT148", 148, "Blighton",
                                41.8402, -72.01, 209, "2004-11-23 08:41:33" },
                        { "ABT149", 149, "Coppertown",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT150", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT151", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT152", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" }

                });
    }

    @Test
    public void testWithFacetRemoveDuplicateRows() throws Exception {
        initProject();
        List<String> duplicateRowCriteria = List.of(
                new String[] { "SITE_ID", "SITE_NUM", "LATITUDE",
                        "LONGITUDE", "ELEVATION", "UPDATE_DATE" });
        String _engineConfig = "{\"facets\":[{\"type\":\"list\",\"name\":\"SITE_ID\",\"columnName\":\"SITE_ID\",\"expression\":\"value\",\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"ABT147\",\"l\":\"ABT147\"}}],\"selectBlank\":false,\"selectError\":false,\"invert\":false}],\"mode\":\"row-based\"}";

        EngineConfig engineConfig = EngineConfig.deserialize(_engineConfig);
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
        assertEquals(operation.getColumnDependencies(),
                Optional.of(Set.of("SITE_ID", "SITE_NUM", "LATITUDE", "LONGITUDE", "ELEVATION", "UPDATE_DATE")));
        assertEquals(operation.getColumnsDiff(), Optional.of(ColumnsDiff.empty()));

        runOperation(operation, project);
        assertEquals(project.rows.size(), 7);
        assertEquals(project.history.getLastPastEntries(1).get(0).description, "Remove 2 rows");

        Project expected = createProject(
                new String[] { "SITE_ID", "SITE_NUM", "SITE_NAME",
                        "LATITUDE", "LONGITUDE", "ELEVATION", "UPDATE_DATE" },
                new Serializable[][] {
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:34" },
                        { "ABT148", 148, "Blighton",
                                41.8402, -72.01, 209, "2004-11-23 08:41:33" },
                        { "ABT149", 149, "Coppertown",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT150", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT151", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT152", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" }

                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testWithSingleCriteriaRemoveDuplicateRows() throws Exception {
        initProject();
        List<String> duplicateRowCriteria = List.of(
                new String[] { "SITE_ID" });

        EngineConfig engineConfig = EngineConfig.defaultRowBased();
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
        assertEquals(operation.getColumnDependencies(), Optional.of(Set.of("SITE_ID")));
        assertEquals(operation.getColumnsDiff(), Optional.of(ColumnsDiff.empty()));

        runOperation(operation, project);
        assertEquals(project.rows.size(), 6);
        assertEquals(project.history.getLastPastEntries(1).get(0).description, "Remove 3 rows");

        Project expected = createProject(
                new String[] { "SITE_ID", "SITE_NUM", "SITE_NAME",
                        "LATITUDE", "LONGITUDE", "ELEVATION", "UPDATE_DATE" },
                new Serializable[][] {
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT148", 148, "Blighton",
                                41.8402, -72.01, 209, "2004-11-23 08:41:33" },
                        { "ABT149", 149, "Coppertown",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT150", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT151", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT152", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" }

                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testWithSingleNumberCriteriaRemoveDuplicateRows() throws Exception {
        initProject();
        List<String> duplicateRowCriteria = List.of(
                new String[] { "SITE_NUM" });

        EngineConfig engineConfig = EngineConfig.defaultRowBased();
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
        runOperation(operation, project);
        assertEquals(project.rows.size(), 4);
        assertEquals(project.history.getLastPastEntries(1).get(0).description, "Remove 5 rows");

        Project expected = createProject(
                new String[] { "SITE_ID", "SITE_NUM", "SITE_NAME",
                        "LATITUDE", "LONGITUDE", "ELEVATION", "UPDATE_DATE" },
                new Serializable[][] {
                        { "ABT147", 147, "Abington",
                                41.8402, -72.01, 209, "2004-11-03 08:41:33" },
                        { "ABT148", 148, "Blighton",
                                41.8402, -72.01, 209, "2004-11-23 08:41:33" },
                        { "ABT149", 149, "Coppertown",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" },
                        { "ABT150", 150, "Duluth",
                                41.8402, -72.01, 209, "2004-11-13 08:41:33" }
                });

        assertProjectEquals(project, expected);
    }

    @Test
    public void testRenameColumns() {
        RowDuplicatesRemovalOperation SUT = new RowDuplicatesRemovalOperation(EngineConfig.defaultRowBased(), List.of("foo", "bar"));

        RowDuplicatesRemovalOperation renamed = SUT.renameColumns(Map.of("foo", "foo2"));

        assertEquals(renamed._criteria, List.of("foo2", "bar"));
    }

}
