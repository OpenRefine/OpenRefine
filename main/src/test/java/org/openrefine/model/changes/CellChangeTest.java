
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class CellChangeTest extends RefineTest {

    private GridState initialGrid;

    private String serializedChange = ""
            + "{\n" +
            "       \"cellIndex\" : 23,\n" +
            "       \"newCell\" : {\n" +
            "         \"v\" : \"changed\"\n" +
            "       },\n" +
            "       \"columnName\": \"bar\"," +
            "       \"dagSlice\" : {\n" +
            "         \"column\": \"bar\",\n" +
            "         \"inputs\": [ ],\n" +
            "         \"type\": \"transformation\"\n" +
            "      },\n" +
            "       \"rowId\" : 14,\n" +
            "       \"type\" : \"org.openrefine.model.changes.CellChange\"\n" +
            "     }";

    @BeforeTest
    public void setUpGrid() {
        Project project = createProject("test project",
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", 1 },
                        { 3, true }
                });
        initialGrid = project.getCurrentGridState();
    }

    @Test
    public void testCellChange() {
        Change change = new CellChange(0L, 0, "foo", new Cell("changed", null));

        GridState newGrid = change.apply(initialGrid);

        Assert.assertEquals(newGrid.getRow(0L),
                new Row(Arrays.asList(new Cell("changed", null), new Cell(1, null))));
        Assert.assertEquals(newGrid.getRow(1L),
                new Row(Arrays.asList(new Cell(3, null), new Cell(true, null))));
    }

    @Test
    public void testSerialize() {
        Change change = new CellChange(14L, 23, "bar", new Cell("changed", null));
        TestUtils.isSerializedTo(change, serializedChange, ParsingUtilities.defaultWriter);
    }
}
