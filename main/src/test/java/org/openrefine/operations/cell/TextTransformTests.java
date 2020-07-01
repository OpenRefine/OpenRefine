
package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OnError;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class TextTransformTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "text-transform", TextTransformOperation.class);
    }

    protected GridState initialState;

    @BeforeMethod
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        initialState = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
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
    public void testTransformColumn() throws DoesNotApplyException {
        Change change = new TextTransformOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                false, 0).createChange();

        GridState applied = change.apply(initialState, mock(ChangeContext.class));

        GridState expected = createGrid(
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
}
