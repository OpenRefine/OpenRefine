
package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnMultiRemovalOperationTests extends RefineTest {

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
        OperationRegistry.registerOperation(getCoreModule(), "column-multi-removal", ColumnMultiRemovalOperation.class);
    }

    @Test
    public void serializeColumnRemovalOperation() throws Exception {
        String json = "{\"op\":\"core/column-multi-removal\","
                + "\"description\":" + new TextNode(OperationDescription.column_multi_removal_brief(2)).toString() + ","
                + "\"columnNames\":[\"my column\", \"some other column\"]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnMultiRemovalOperation.class), json);
    }

    @Test
    public void testValidate() {
        ColumnMultiRemovalOperation SUT = new ColumnMultiRemovalOperation(null);
        assertThrows(IllegalArgumentException.class, () -> SUT.validate());
    }

    @Test
    public void testRemoval() throws Exception {
        ColumnMultiRemovalOperation SUT = new ColumnMultiRemovalOperation(List.of("foo", "bar"));
        assertEquals(SUT.getColumnDependencies().get(), Set.of("foo", "bar"));
        assertEquals(SUT.getColumnsDiff().get(), ColumnsDiff.builder().deleteColumn("foo").deleteColumn("bar").build());

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "hello" },
                new Serializable[][] {
                        { "d" },
                        { "f" },
                        { "g" },
                        { "h" },
                        { "i" },
                        { "j" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testRename() {
        ColumnMultiRemovalOperation SUT = new ColumnMultiRemovalOperation(List.of("foo", "bar"));

        ColumnMultiRemovalOperation renamed = SUT.renameColumns(Map.of("foo", "foo2"));

        assertEquals(renamed._columnNames, List.of("foo2", "bar"));
    }
}
