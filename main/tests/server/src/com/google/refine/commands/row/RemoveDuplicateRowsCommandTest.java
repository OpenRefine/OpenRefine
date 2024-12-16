
package com.google.refine.commands.row;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowDuplicatesRemovalOperation;

public class RemoveDuplicateRowsCommandTest extends CommandTestBase {

    private Project project = null;
    protected RemoveDuplicateRowsCommand command;

    @BeforeMethod
    public void setUpCommand() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        command = new RemoveDuplicateRowsCommand();
    }

    @Test
    // If CSRF token is missing, respond with CSRF error
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testWithFacetRemoveDuplicateRows() throws Exception {
        initProject();
        List<String> duplicateRowCriteria = List.of(
                new String[] { "SITE_ID", "SITE_NUM", "LATITUDE",
                        "LONGITUDE", "ELEVATION", "UPDATE_DATE" });
        String _engineConfig = "{\"facets\":[{\"type\":\"list\",\"name\":\"SITE_ID\",\"columnName\":\"SITE_ID\",\"expression\":\"value\",\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"ABT147\",\"l\":\"ABT147\"}}],\"selectBlank\":false,\"selectError\":false,\"invert\":false}],\"mode\":\"row-based\"}";

        EngineConfig engineConfig = EngineConfig.reconstruct(_engineConfig);
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
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
        String _engineConfig = "{\"facets\":[],\"mode\":\"row-based\"}";

        EngineConfig engineConfig = EngineConfig.reconstruct(_engineConfig);
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
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
        String _engineConfig = "{\"facets\":[],\"mode\":\"row-based\"}";

        EngineConfig engineConfig = EngineConfig.reconstruct(_engineConfig);
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

    private void initProject() {
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
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
    }
}
