
package org.openrefine.jython;

import java.util.Arrays;
import java.util.Properties;

import org.python.core.PyObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.CellTuple;
import org.openrefine.expr.Evaluable;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;

/**
 * @author Maxim Galushka
 */
public class JythonEvaluableTest {

    // Reproduces the situation when result is a PyObject
    // Version with a test case which only calls the existing evaluate method
    @Test
    public void unwrapPyObjectTest() {
        Properties props = new Properties();
        ColumnModel project = new ColumnModel(Arrays.asList(new ColumnMetadata("foo")));

        Row row = new Row(Arrays.asList(new Cell("one", null), new Cell("1", null)));

        props.put("columnName", "number");
        props.put("true", "true");
        props.put("false", "false");
        props.put("rowIndex", "0");
        props.put("value", 1);
        props.put("project", project);
        props.put("call", "number");
        props.put("PI", "3.141592654");
        props.put("cells", new CellTuple(project, row));
        String funcExpression = "class Foo(object):\n" +
                "    bar = 1\n" +
                "\n" +
                "return Foo()";
        JythonEvaluable eval1 = new JythonEvaluable(funcExpression, "jython");
        PyObject po = (PyObject) eval1.evaluate(props);
        Assert.assertEquals(po.__getattr__("bar").toString(), "1");
    }

    @Test
    public void testJythonConcurrent() {
        Properties props = new Properties();

        props.put("columnName", "number");
        props.put("true", "true");
        props.put("false", "false");
        props.put("rowIndex", "0");
        props.put("value", 1);
        props.put("call", "number");
        props.put("PI", "3.141592654");

        Evaluable eval1 = new JythonEvaluable("a = value\nreturn a * 2", "jython");
        Long value1 = (Long) eval1.evaluate(props);

        // create some unrelated evaluable
        new JythonEvaluable("a = value\nreturn a * 10", "jython");

        // repeat same previous test
        Long value2 = (Long) eval1.evaluate(props);
        Assert.assertEquals(value1, value2);
    }
}
