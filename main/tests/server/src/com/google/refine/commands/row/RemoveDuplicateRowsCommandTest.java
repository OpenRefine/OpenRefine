
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
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowDuplicatesRemovalOperation;

public class RemoveDuplicateRowsCommandTest extends CommandTestBase {

    private Project project = null;
    private final List<String> duplicateRowCriteria = List.of(
            new String[] { "SITE_ID", "SITE_NUM", "SITE_NAME", "ACTIVE", "INACTIVE", "AGENCY", "STATE", "COUNTY", "TIME_ZONE", "LATITUDE",
                    "LONGITUDE", "ELEVATION", "MAPID", "LAND_USE", "TERRAIN", "JSONDATA", "NADP_ID", "NADP_DISTANCE", "UPDATE_DATE" });
    private final String _engineConfig = "{\n" +
            "    \"facets\": [\n" +
            "        {\n" +
            "            \"type\": \"list\",\n" +
            "            \"name\": \"SITE_ID\",\n" +
            "            \"columnName\": \"SITE_ID\",\n" +
            "            \"expression\": \"value\",\n" +
            "            \"omitBlank\": false,\n" +
            "            \"omitError\": false,\n" +
            "            \"selection\": [],\n" +
            "            \"selectBlank\": false,\n" +
            "            \"selectError\": false,\n" +
            "            \"invert\": false\n" +
            "        }\n" +
            "    ],\n" +
            "        \"mode\": \"row-based\"\n" +
            "}";

    protected RemoveDuplicateRowsCommand command;

    @BeforeMethod
    public void setUpCommand() {
        command = new RemoveDuplicateRowsCommand();
        project = createProject(
                new String[] { "SITE_ID",
                        "SITE_NUM",
                        "SITE_NAME",
                        "ACTIVE",
                        "INACTIVE",
                        "AGENCY",
                        "STATE",
                        "COUNTY",
                        "TIME_ZONE",
                        "COUNTY",
                        "LATITUDE",
                        "LONGITUDE",
                        "ELEVATION",
                        "MAPID",
                        "LAND_USE",
                        "TERRAIN",
                        "JSONDATA",
                        "NADP_ID",
                        "NADP_DISTANCE",
                        "UPDATE_DATE" },
                new Serializable[][] {
                        { "ABT147",
                                147,
                                "Abington",
                                "domp",
                                "w",
                                "EPA",
                                "CT",
                                "Windham",
                                "EA",
                                41.8402,
                                -72.01,
                                209,
                                "Hampton",
                                "CT",
                                "Urban/Agric",
                                "Rolling",
                                "{\"\"SITE_ID\"\": \"\"ABT147\"\",\n" +
                                        "    \"\"SITE_NUM\"\": 147,\n" +
                                        "    \"\"SITE_NAME\"\": \"\"Abington\"\",\n" +
                                        "    \"\"ACTIVE\"\": \"\"domp\"\",\n" +
                                        "    \"\"INACTIVE\"\": \"\"w\"\",\n" +
                                        "    \"\"AGENCY\"\": \"\"EPA\"\",\n" +
                                        "    \"\"STATE\"\": \"\"CT\"\",\n" +
                                        "    \"\"COUNTY\"\": \"\"Windham\"\",\n" +
                                        "    \"\"TIME_ZONE\"\": \"\"EA\"\",\n" +
                                        "    \"\"LATITUDE\"\": 41.8402,\n" +
                                        "    \"\"LONGITUDE\"\": -72.01,\n" +
                                        "    \"\"ELEVATION\"\": 209,\n" +
                                        "    \"\"MAPID\"\": \"\"Hampton, CT\"\",\n" +
                                        "    \"\"LAND_USE\"\": \"\"Urban/Agric\"\",\n" +
                                        "    \"\"TERRAIN\"\": \"\"Rolling\"\",\n" +
                                        "    \"\"MLM\"\": \"\"Mixed\"\",\n" +
                                        "    \"\"NADP_ID\"\": \"\"CT15\"\",\n" +
                                        "    \"\"NADP_DISTANCE\"\": \"\".0236\"\",\n" +
                                        "    \"\"UPDATE_DATE\"\": \"\"2004-11-03 08:41:33\"\"}",
                                "CT15",
                                .0236,
                                "2004-11-03 08:41:33" },
                        { "ABT147",
                                147,
                                "Abington",
                                "domp",
                                "w",
                                "EPA",
                                "CT",
                                "Windham",
                                "EA",
                                41.8402,
                                -72.01, 209,
                                "Hampton",
                                "CT",
                                "Urban/Agric",
                                "Rolling",
                                "{\"\"SITE_ID\"\": \"\"ABT147\"\",\n" +
                                        "    \"\"SITE_NUM\"\": 147,\n" +
                                        "    \"\"SITE_NAME\"\": \"\"Abington\"\",\n" +
                                        "    \"\"ACTIVE\"\": \"\"domp\"\",\n" +
                                        "    \"\"INACTIVE\"\": \"\"w\"\",\n" +
                                        "    \"\"AGENCY\"\": \"\"EPA\"\",\n" +
                                        "    \"\"STATE\"\": \"\"CT\"\",\n" +
                                        "    \"\"COUNTY\"\": \"\"Windham\"\",\n" +
                                        "    \"\"TIME_ZONE\"\": \"\"EA\"\",\n" +
                                        "    \"\"LATITUDE\"\": 41.8402,\n" +
                                        "    \"\"LONGITUDE\"\": -72.01,\n" +
                                        "    \"\"ELEVATION\"\": 209,\n" +
                                        "    \"\"MAPID\"\": \"\"Hampton, CT\"\",\n" +
                                        "    \"\"LAND_USE\"\": \"\"Urban/Agric\"\",\n" +
                                        "    \"\"TERRAIN\"\": \"\"Rolling\"\",\n" +
                                        "    \"\"MLM\"\": \"\"Mixed\"\",\n" +
                                        "    \"\"NADP_ID\"\": \"\"CT15\"\",\n" +
                                        "    \"\"NADP_DISTANCE\"\": \"\".0236\"\",\n" +
                                        "    \"\"UPDATE_DATE\"\": \"\"2004-11-03 08:41:33\"\"}",
                                "CT15", .0236, "2004-11-03 08:41:33" },
                        { "ABT147",
                                147,
                                "Abington",
                                "domp",
                                "w",
                                "EPA",
                                "CT",
                                "Windham",
                                "EA",
                                41.8402,
                                -72.01, 209,
                                "Hampton",
                                "CT",
                                "Urban/Agric",
                                "Rolling",
                                "{\"\"SITE_ID\"\": \"\"ABT147\"\",\n" +
                                        "    \"\"SITE_NUM\"\": 147,\n" +
                                        "    \"\"SITE_NAME\"\": \"\"Abington\"\",\n" +
                                        "    \"\"ACTIVE\"\": \"\"domp\"\",\n" +
                                        "    \"\"INACTIVE\"\": \"\"w\"\",\n" +
                                        "    \"\"AGENCY\"\": \"\"EPA\"\",\n" +
                                        "    \"\"STATE\"\": \"\"CT\"\",\n" +
                                        "    \"\"COUNTY\"\": \"\"Windham\"\",\n" +
                                        "    \"\"TIME_ZONE\"\": \"\"EA\"\",\n" +
                                        "    \"\"LATITUDE\"\": 41.8402,\n" +
                                        "    \"\"LONGITUDE\"\": -72.01,\n" +
                                        "    \"\"ELEVATION\"\": 209,\n" +
                                        "    \"\"MAPID\"\": \"\"Hampton, CT\"\",\n" +
                                        "    \"\"LAND_USE\"\": \"\"Urban/Agric\"\",\n" +
                                        "    \"\"TERRAIN\"\": \"\"Rolling\"\",\n" +
                                        "    \"\"MLM\"\": \"\"Mixed\"\",\n" +
                                        "    \"\"NADP_ID\"\": \"\"CT15\"\",\n" +
                                        "    \"\"NADP_DISTANCE\"\": \"\".0236\"\",\n" +
                                        "    \"\"UPDATE_DATE\"\": \"\"2004-11-03 08:41:33\"\"}",
                                "CT15", .0236, "2004-11-03 08:41:34" },
                        { "ABT147",
                                147,
                                "Abington",
                                "domp",
                                "w",
                                "EPA",
                                "CT",
                                "Windham",
                                "EA",
                                41.8402,
                                -72.01, 209,
                                "Hampton",
                                "CT",
                                "Urban/Agric",
                                "Rolling",
                                "{\"\"SITE_ID\"\": \"\"ABT147\"\",\n" +
                                        "    \"\"SITE_NUM\"\": 147,\n" +
                                        "    \"\"SITE_NAME\"\": \"\"Abington\"\",\n" +
                                        "    \"\"ACTIVE\"\": \"\"domp\"\",\n" +
                                        "    \"\"INACTIVE\"\": \"\"w\"\",\n" +
                                        "    \"\"AGENCY\"\": \"\"EPA\"\",\n" +
                                        "    \"\"STATE\"\": \"\"CT\"\",\n" +
                                        "    \"\"COUNTY\"\": \"\"Windham\"\",\n" +
                                        "    \"\"TIME_ZONE\"\": \"\"EA\"\",\n" +
                                        "    \"\"LATITUDE\"\": 41.8402,\n" +
                                        "    \"\"LONGITUDE\"\": -72.01,\n" +
                                        "    \"\"ELEVATION\"\": 209,\n" +
                                        "    \"\"MAPID\"\": \"\"Hampton, CT\"\",\n" +
                                        "    \"\"LAND_USE\"\": \"\"Urban/Agric\"\",\n" +
                                        "    \"\"TERRAIN\"\": \"\"Rolling\"\",\n" +
                                        "    \"\"MLM\"\": \"\"Mixed\"\",\n" +
                                        "    \"\"NADP_ID\"\": \"\"CT15\"\",\n" +
                                        "    \"\"NADP_DISTANCE\"\": \"\".0236\"\",\n" +
                                        "    \"\"UPDATE_DATE\"\": \"\"2004-11-03 08:41:34\"\"}",
                                "CT15", .0236, "2004-11-03 08:41:33" }

                });
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
    }

    @Test
    // If CSRF token is missing, respond with CSRF error
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testRemoveDuplicateRows() throws Exception {
        EngineConfig engineConfig = EngineConfig.reconstruct(_engineConfig);
        RowDuplicatesRemovalOperation operation = new RowDuplicatesRemovalOperation(engineConfig, duplicateRowCriteria);
        runOperation(operation, project);
        assertEquals(project.rows.size(), 3);
        assertEquals(project.history.getLastPastEntries(1).get(0).description, "Remove 1 rows");
    }
}
