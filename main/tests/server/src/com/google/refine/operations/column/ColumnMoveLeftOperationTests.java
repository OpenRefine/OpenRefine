
package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnMoveLeftOperationTests extends RefineTest {

    protected Project project;

    @BeforeMethod
    public void setUpInitialState() {
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
    }

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-move-left", ColumnMoveLeftOperation.class);
    }

    @Test
    public void serializeColumnMoveLeftOperation() throws Exception {
        String json = "{\"op\":\"core/column-move-left\","
                + "\"description\":" + new TextNode(OperationDescription.column_move_left_brief("my column")).toString() + ","
                + "\"columnName\":\"my column\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnMoveLeftOperation.class), json);
    }

    @Test
    public void testValidate() {
        AbstractOperation missingColumnName = new ColumnMoveLeftOperation(null);
        assertThrows(IllegalArgumentException.class, () -> missingColumnName.validate());
    }

    @Test
    public void testMove() throws Exception {
        ColumnMoveLeftOperation operation = new ColumnMoveLeftOperation("bar");
        assertEquals(operation.getColumnDependencies().get(), Set.of("bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.empty());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "bar", "foo", "hello" },
                new Serializable[][] {
                        { "a", "v1", "d" },
                        { "a", "v3", "f" },
                        { "a", "", "g" },
                        { "b", "", "h" },
                        { "a", new EvalError("error"), "i" },
                        { "b", "v1", "j" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testSamePosition() throws Exception {
        AbstractOperation SUT = new ColumnMoveLeftOperation("foo");
        assertEquals(SUT.getColumnDependencies().get(), Set.of("foo"));
        assertEquals(SUT.getColumnsDiff().get(), ColumnsDiff.empty());

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testRename() {
        ColumnMoveLeftOperation SUT = new ColumnMoveLeftOperation("hello");

        ColumnMoveLeftOperation renamed = SUT.renameColumns(Map.of("hello", "world"));

        assertEquals(renamed._columnName, "world");
    }
}
