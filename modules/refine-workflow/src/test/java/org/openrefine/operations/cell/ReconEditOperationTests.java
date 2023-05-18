
package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconEditOperationTests extends RefineTest {

    Grid initialGrid;
    private String serializedOperation = ""
            + "{\n" +
            "       \"columnName\": \"bar\"," +
            "       \"rowId\" : 14,\n" +
            "       \"judgment\" : \"matched\",\n" +
            "       \"identifierSpace\" : \"http://foo.com/space\",\n" +
            "       \"schemaSpace\" : \"http://foo.com/schema\",\n" +
            "       \"match\" : {\"id\":\"Q123\", \"name\":\"Entity\", \"score\": 29.2, \"types\": []},\n" +
            "       \"cellValue\" : \"some value to reconcile\",\n" +
            "       \"description\" : \"Match Entity (Q123) to single cell on row 15, column bar, containing \\\"some value to reconcile\\\"\","
            +
            "       \"op\" : \"core/recon-edit\"\n" +
            "     }";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon-edit", ReconEditOperation.class);
    }

    @BeforeTest
    public void setUpGrid() {
        Project project = createProject("test project",
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("c", "d", Judgment.Matched)) },
                        { 3, new Cell("p", testRecon("c", "d", Judgment.None)) }
                });
        initialGrid = project.getCurrentGrid();
    }

    @Test
    public void testMatch() throws DoesNotApplyException, ParsingException {
        Operation operation = new ReconEditOperation(1L, "bar", Judgment.Matched, null, null, new ReconCandidate("u", "v", null, 48), "p");
        Assert.assertEquals(operation.getDescription(), "Match v (u) to single cell on row 2, column bar, containing \"p\"");
        Change change = operation.createChange();

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(6789L);
        Change.ChangeResult changeResult = change.apply(initialGrid, context);
        Grid newGrid = changeResult.getGrid();

        Recon expectedRecon = testRecon("c", "d", Judgment.None)
                .withJudgment(Judgment.Matched)
                .withMatch(new ReconCandidate("u", "v", null, 48))
                .withJudgmentAction("single")
                .withJudgmentHistoryEntry(6789L);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Assert.assertEquals(newGrid.getRow(0L), initialGrid.getRow(0L));
        Assert.assertEquals(newGrid.getRow(1L),
                new Row(Arrays.asList(new Cell(3, null), new Cell("p", expectedRecon))));
    }

    @Test
    public void testNew() throws DoesNotApplyException, ParsingException {
        Operation operation = new ReconEditOperation(1L, "bar", Judgment.New, null, null, null, "p");
        Assert.assertEquals(operation.getDescription(), "Mark to create new item for single cell on row 2, column bar, containing \"p\"");
        Change change = operation
                .createChange();

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(6789L);
        Change.ChangeResult changeResult = change.apply(initialGrid, context);
        Grid newGrid = changeResult.getGrid();

        Recon expectedRecon = testRecon("c", "d", Judgment.New)
                .withJudgmentAction("single")
                .withJudgmentHistoryEntry(6789L);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Assert.assertEquals(newGrid.getRow(0L), initialGrid.getRow(0L));
        Assert.assertEquals(newGrid.getRow(1L),
                new Row(Arrays.asList(new Cell(3, null), new Cell("p", expectedRecon))));
    }

    @Test
    public void testUnmatch() throws DoesNotApplyException, ParsingException {
        Operation operation = new ReconEditOperation(0L, "bar", Judgment.None, null, null, null, "b");
        Assert.assertEquals(operation.getDescription(), "Discard recon judgment for single cell on row 1, column bar, containing \"b\"");
        Change change = operation.createChange();

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(6789L);
        Change.ChangeResult changeResult = change.apply(initialGrid, context);
        Grid newGrid = changeResult.getGrid();

        Recon expectedRecon = testRecon("c", "d", Judgment.None)
                .withJudgmentAction("single")
                .withJudgmentHistoryEntry(6789L);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Assert.assertEquals(newGrid.getRow(0L),
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", expectedRecon))));
        Assert.assertEquals(newGrid.getRow(1L), initialGrid.getRow(1L));
    }

    @Test
    public void testClear() throws DoesNotApplyException, ParsingException {
        Operation operation = new ReconEditOperation(0L, "bar", null, null, null, null, "b");
        Assert.assertEquals(operation.getDescription(), "Clear recon data for single cell on row 1, column bar, containing \"b\"");
        Change change = operation.createChange();

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(6789L);
        Change.ChangeResult changeResult = change.apply(initialGrid, context);
        Grid newGrid = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Assert.assertEquals(newGrid.getRow(0L),
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", null))));
        Assert.assertEquals(newGrid.getRow(1L), initialGrid.getRow(1L));
    }

    @Test
    public void testRoundTripSerialize() throws JsonParseException, JsonMappingException, IOException {
        Operation operation = ParsingUtilities.mapper.readValue(serializedOperation, Operation.class);
        TestUtils.isSerializedTo(operation, serializedOperation, ParsingUtilities.defaultWriter);
        Assert.assertTrue(operation instanceof ReconEditOperation);
    }
}
