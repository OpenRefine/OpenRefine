
package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class CellEditOperationTests extends RefineTest {

    Grid initialGrid;
    private String serializedOperation = ""
            + "{\n" +
            "       \"newCellValue\" : \"changed\"," +
            "       \"columnName\": \"bar\"," +
            "       \"description\" : \"Edit single cell on row 15, column bar\"," +
            "       \"rowId\" : 14,\n" +
            "       \"op\" : \"core/cell-edit\"\n" +
            "     }";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "cell-edit", CellEditOperation.class);
    }

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
    public void testCellChange() throws DoesNotApplyException, ParsingException {
        Operation operation = new CellEditOperation(0L, "foo", "changed");

        Change.ChangeResult changeResult = operation.apply(initialGrid, mock(ChangeContext.class));
        Grid newGrid = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Assert.assertEquals(newGrid.getRow(0L),
                new Row(Arrays.asList(new Cell("changed", null), new Cell(1, null))));
        Assert.assertEquals(newGrid.getRow(1L),
                new Row(Arrays.asList(new Cell(3, null), new Cell(true, null))));
    }

    @Test
    public void testRoundTripSerialize() throws JsonParseException, JsonMappingException, IOException {
        Operation operation = ParsingUtilities.mapper.readValue(serializedOperation, Operation.class);
        TestUtils.isSerializedTo(operation, serializedOperation, ParsingUtilities.defaultWriter);
        Assert.assertTrue(operation instanceof CellEditOperation);
    }
}
