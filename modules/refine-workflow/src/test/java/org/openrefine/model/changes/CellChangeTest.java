
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import org.openrefine.RefineTest;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class CellChangeTest extends RefineTest {

    private Grid initialGrid;

    private String serializedChange = ""
            + "{\n" +
            "       \"newCellValue\" : \"changed\"," +
            "       \"columnName\": \"bar\"," +
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
        initialGrid = project.getCurrentGrid();
    }

    @Test
    public void testCellChange() throws DoesNotApplyException {
        Change change = new CellChange(0L, "foo", "changed");

        Change.ChangeResult changeResult = change.apply(initialGrid, mock(ChangeContext.class));
        Grid newGrid = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Assert.assertEquals(newGrid.getRow(0L),
                new Row(Arrays.asList(new Cell("changed", null), new Cell(1, null))));
        Assert.assertEquals(newGrid.getRow(1L),
                new Row(Arrays.asList(new Cell(3, null), new Cell(true, null))));
    }

    @Test
    public void testRoundTripSerialize() throws JsonParseException, JsonMappingException, IOException {
        Change change = ParsingUtilities.mapper.readValue(serializedChange, Change.class);
        TestUtils.isSerializedTo(change, serializedChange, ParsingUtilities.defaultWriter);
        Assert.assertTrue(change instanceof CellChange);
    }
}
