package com.google.refine.jython;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.HasFields;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.CellTuple;
import com.google.refine.expr.Evaluable;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * @author Maxim Galushka
 */
public class JythonEvaluableTest {

  @Test
  public void pyObjectTest(){
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

    JythonEvaluable eval1 = new JythonEvaluable("a = value\nreturn a * 2");
    //Long value1 = (Long) eval1.evaluateTest(props);
    double value1 = (double) eval1.evaluatePyobjectTest(props);

    // create some unrelated evaluable
    new JythonEvaluable("a = value\nreturn a * 10");

    // repeat same previous test
    double value2 = (double) eval1.evaluatePyobjectTest(props);
    Assert.assertEquals(value1, value2);
  }

  @Test
  public void testJythonConcurrent(){
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
