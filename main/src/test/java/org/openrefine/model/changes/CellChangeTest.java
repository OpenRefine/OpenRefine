
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class CellChangeTest extends RefineTest {

    private GridState initialGrid;

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
        initialGrid = project.getCurrentGridState();
    }

    @Test
    public void testCellChange() throws DoesNotApplyException {
        Change change = new CellChange(0L, "foo", "changed");

        GridState newGrid = change.apply(initialGrid, mock(ChangeContext.class));

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
