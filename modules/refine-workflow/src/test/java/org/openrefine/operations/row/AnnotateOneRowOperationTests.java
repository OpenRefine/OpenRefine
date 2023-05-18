
package org.openrefine.operations.row;

import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class AnnotateOneRowOperationTests extends RefineTest {

    Grid initialGrid;

    @BeforeTest
    public void setUpInitialGrid() {
        initialGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" }
                });
    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "annotate-one-row", RowFlagOperation.class);
    }

    @Test
    public void testFlag() throws DoesNotApplyException, ParsingException {
        Operation operation = new AnnotateOneRowOperation(0L, false, true);
        Assert.assertEquals(operation.getDescription(), "Flag row 1");

        Grid applied = operation.apply(initialGrid, mock(ChangeContext.class)).getGrid();

        Assert.assertEquals(applied.getColumnModel(), initialGrid.getColumnModel());
        Assert.assertEquals(applied.getRow(0L), initialGrid.getRow(0L).withFlagged(true));
        Assert.assertEquals(applied.getRow(1L), initialGrid.getRow(1L));

        TestUtils.isSerializedTo(operation, "{\n"
                + "  \"description\" : \"Flag row 1\",\n"
                + "  \"op\" : \"null\",\n"
                + "  \"rowId\" : 0,\n"
                + "  \"star\" : false,\n"
                + "  \"value\" : true\n"
                + "}", ParsingUtilities.defaultWriter);
    }

    @Test
    public void testStar() throws DoesNotApplyException, ParsingException {
        Operation operation = new AnnotateOneRowOperation(1L, true, true);
        Assert.assertEquals(operation.getDescription(), "Star row 2");

        Grid applied = operation.apply(initialGrid, mock(ChangeContext.class)).getGrid();

        Assert.assertEquals(applied.getColumnModel(), initialGrid.getColumnModel());
        Assert.assertEquals(applied.getRow(0L), initialGrid.getRow(0L));
        Assert.assertEquals(applied.getRow(1L), initialGrid.getRow(1L).withStarred(true));

        TestUtils.isSerializedTo(operation, "{\n"
                + "  \"description\" : \"Star row 2\",\n"
                + "  \"op\" : \"null\",\n"
                + "  \"rowId\" : 1,\n"
                + "  \"star\" : true,\n"
                + "  \"value\" : true\n"
                + "} ", ParsingUtilities.defaultWriter);
    }

}
