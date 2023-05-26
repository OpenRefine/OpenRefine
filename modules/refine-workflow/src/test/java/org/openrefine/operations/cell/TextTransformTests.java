
package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OnError;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class TextTransformTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "text-transform", TextTransformOperation.class);
    }

    protected Grid initialState;
    protected Project project;

    @BeforeMethod
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
        initialState = project.getCurrentGrid();
    }

    @Test
    public void serializeColumnAdditionOperation() throws Exception {
        String json = "{"
                + "   \"op\":\"core/text-transform\","
                + "   \"description\":\"Text transform on cells in column organization_json using expression grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "   \"columnName\":\"organization_json\","
                + "   \"expression\":\"grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"onError\":\"set-to-blank\","
                + "   \"repeat\": false,"
                + "   \"repeatCount\": 0"
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, TextTransformOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testTransformColumnInRowsMode() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new TextTransformOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                false, 0);

        Operation.ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "v1_a", "d" },
                        { "v3", "v3_a", "f" },
                        { "", "_a", "g" },
                        { "", "_b", "h" },
                        { new EvalError("error"), null, "i" },
                        { "v1", "v1_b", "j" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testTransformColumnInRowsModeWithPendingCells() throws Operation.DoesNotApplyException, ParsingException {
        Grid pendingGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", Cell.PENDING_NULL },
                        { "v3", Cell.PENDING_NULL, "f" },
                        { Cell.PENDING_NULL, "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
        Operation operation = new TextTransformOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                false, 0);

        Operation.ChangeResult changeResult = operation.apply(pendingGrid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "v1_a", Cell.PENDING_NULL },
                        { "v3", Cell.PENDING_NULL, "f" },
                        { Cell.PENDING_NULL, new Cell("a", null, true), "g" },
                        { "", "_b", "h" },
                        { new EvalError("error"), null, "i" },
                        { "v1", "v1_b", "j" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testTransformIdentity() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new TextTransformOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:value",
                OnError.SetToBlank,
                false, 0);

        Operation.ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        assertGridEquals(applied, initialState);
    }

    @Test
    public void testTransformColumnInRecordsMode() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new TextTransformOperation(
                EngineConfig.ALL_RECORDS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+row.record.rowCount",
                OnError.SetToBlank,
                false, 0);

        Operation.ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "v1_1", "d" },
                        { "v3", "v3_4", "f" },
                        { "", "_4", "g" },
                        { "", "_4", "h" },
                        { new EvalError("error"), null, "i" },
                        { "v1", "v1_1", "j" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testTransformColumnNonLocalOperationInRowsMode() throws Exception {
        Operation operation = new TextTransformOperation(
                EngineConfig.ALL_RECORDS,
                "bar",
                "grel:value + '_' + facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                false, 0);

        project.getHistory().addEntry(operation);
        Grid applied = project.getCurrentGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a_4", "d" },
                        { "v3", "a_4", "f" },
                        { "", "a_4", "g" },
                        { "", "b_2", "h" },
                        { new EvalError("error"), "a_4", "i" },
                        { "v1", "b_2", "j" }
                });
        assertGridEquals(applied, expected);
    }
}
