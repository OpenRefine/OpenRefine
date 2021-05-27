package com.google.refine.jython;

import java.io.File;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.HasFields;
import org.python.core.*;
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

  // Reproduces the situation when result is a PyObject
  public Object evaluatePyobjectTest(Properties bindings,JythonEvaluable eval1, String funcExpression) {
    // Declear the functionName and engine
    String s_functionName;
    PythonInterpreter _engine;
    File libPath = new File("webapp/WEB-INF/lib/jython");
    if (!libPath.exists() && !libPath.canRead()) {
      libPath = new File("main/webapp/WEB-INF/lib/jython");
      if (!libPath.exists() && !libPath.canRead()) {
        libPath = null;
      }
    }
    if (libPath != null) {
      Properties props = new Properties();
      props.setProperty("python.path", libPath.getAbsolutePath());
      PythonInterpreter.initialize(System.getProperties(), props, new String[] { "" });
    }
    _engine = new PythonInterpreter();

    //get function and engine ready for __call__
    String s = funcExpression;
    s_functionName = String.format("__temp_%d__", Math.abs(s.hashCode()));

    // indent and create a function out of the code
    String[] lines = s.split("\r\n|\r|\n");
    StringBuffer sb = new StringBuffer(1024);
    sb.append("def ");
    sb.append(s_functionName);
    sb.append("(value, cell, cells, row, rowIndex):");
    for (String line : lines) {
      sb.append("\n  ");
      sb.append(line);
    }
    _engine.exec(sb.toString());

    try {
      // call the temporary PyFunction directly
      PyObject result = (_engine.get(s_functionName).__call__(
              new PyObject[] {
                      Py.java2py( bindings.get("value") ),
                      new JythonHasFieldsWrapper((HasFields) bindings.get("cell"), bindings),
                      new JythonHasFieldsWrapper((HasFields) bindings.get("cells"), bindings),
                      new JythonHasFieldsWrapper((HasFields) bindings.get("row"), bindings),
                      Py.java2py( bindings.get("rowIndex") )
              }
      ));
      return eval1.unwrap(result);
    } catch (PyException e) {
      return new EvalError(e.toString());
    }
  }

  @Test
  public void unwrapPyObjectTest(){
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

    String funcExpression = "a = value\nreturn a * 2";
    JythonEvaluable eval1 = new JythonEvaluable("a = value\nreturn a * 2");
    //Long value1 = (Long) eval1.evaluateTest(props);
    double value1 = (double) evaluatePyobjectTest(props,eval1,funcExpression);

    // create some unrelated evaluable
    new JythonEvaluable("a = value\nreturn a * 10");

    // repeat same previous test
    double value2 = (double) evaluatePyobjectTest(props,eval1,funcExpression);;
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
