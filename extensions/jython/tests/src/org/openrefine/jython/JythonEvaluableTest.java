
package org.openrefine.jython;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.CellTuple;
import org.openrefine.expr.Evaluable;
import org.openrefine.jython.JythonEvaluable;
import org.openrefine.model.Cell;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

/**
 * @author Maxim Galushka
 */
public class JythonEvaluableTest {

    @Test
    public void testJythonConcurrent() {
        Properties props = new Properties();
        Project project = new Project();

        Row row = new Row(2);
        row.setCell(0, new Cell("one", null));
        row.setCell(0, new Cell("1", null));

        props.put("columnName", "number");
        props.put("true", "true");
        props.put("false", "false");
        props.put("rowIndex", "0");
        props.put("value", 1);
        props.put("project", project);
        props.put("call", "number");
        props.put("PI", "3.141592654");
        props.put("cells", new CellTuple(project, row));

        Evaluable eval1 = new JythonEvaluable("a = value\nreturn a * 2");
        Long value1 = (Long) eval1.evaluate(props);

        // create some unrelated evaluable
        new JythonEvaluable("a = value\nreturn a * 10");

        // repeat same previous test
        Long value2 = (Long) eval1.evaluate(props);
        Assert.assertEquals(value1, value2);
    }
}
