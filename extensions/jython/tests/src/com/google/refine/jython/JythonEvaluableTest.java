
package com.google.refine.jython;

import static org.testng.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

import org.python.core.PyObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.CellTuple;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * @author Maxim Galushka
 */
public class JythonEvaluableTest {

    private Properties createBindings() {
        Project project = new Project();
        Row row = new Row(2);
        row.setCell(0, new Cell("one", null));
        row.setCell(0, new Cell("1", null));
        Properties bindings = new Properties();
        bindings.put("columnName", "number");
        bindings.put("true", "true");
        bindings.put("false", "false");
        bindings.put("rowIndex", "0");
        bindings.put("value", 1);
        bindings.put("project", project);
        bindings.put("call", "number");
        bindings.put("PI", "3.141592654");
        bindings.put("cells", new CellTuple(project, row));
        return bindings;
    }

    // Reproduces the situation when result is a PyObject
    // Version with a test case which only calls the existing evaluate method
    @Test
    public void unwrapPyObjectTest() {
        Properties props = createBindings();
        String funcExpression = "class Foo(object):\n" +
                "    bar = 1\n" +
                "\n" +
                "return Foo()";
        JythonEvaluable eval1 = new JythonEvaluable(funcExpression);
        PyObject po = (PyObject) eval1.evaluate(props);
        Assert.assertEquals(po.__getattr__("bar").toString(), "1");
    }

    @Test
    public void testJythonConcurrent() {
        Properties props = createBindings();

        Evaluable eval1 = new JythonEvaluable("a = value\nreturn a * 2");
        Long value1 = (Long) eval1.evaluate(props);

        // create some unrelated evaluable
        new JythonEvaluable("a = value\nreturn a * 10");

        // repeat same previous test
        Long value2 = (Long) eval1.evaluate(props);
        Assert.assertEquals(value1, value2);
    }

    @Test
    public void testJythonDate() {
        Properties bindings = createBindings();
        OffsetDateTime expectedDate = OffsetDateTime.of(2024, 10, 2, 12, 30, 30, 0, ZoneOffset.UTC);

        // Test passing a date out from Python (wow, Python 2.7 timezone handling is primitive!)
        Evaluable evaluable = new JythonEvaluable("from datetime import datetime,timedelta,tzinfo\n" +
                "\n" +
                "ZERO = timedelta(0)\n" +
                "\n" +
                "class UTC(tzinfo):\n" +
                "    \"\"\"UTC\"\"\"\n" +
                "\n" +
                "    def utcoffset(self, dt):\n" +
                "        return ZERO\n" +
                "\n" +
                "    def tzname(self, dt):\n" +
                "        return \"UTC\"\n" +
                "\n" +
                "    def dst(self, dt):\n" +
                "        return ZERO\n" +
                "\n" +
                "utc = UTC()" +
                "\n" +
                "return datetime(2024, 10, 2, 12, 30, 30, tzinfo=utc)");
        Object result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, expectedDate);

        // Test passing a date in
        bindings.put("value", expectedDate);
        evaluable = new JythonEvaluable("import datetime\n" +
                "return isinstance(value,datetime.datetime)");
        result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, 1L); // Python's version of true

        evaluable = new JythonEvaluable("import datetime\n" +
                "return str(value)");
        result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, "2024-10-02 12:30:30");
    }

    @Test
    public void testJythonDataTypes() {
        Properties bindings = new Properties(); // tests are self-contained, so no bindings needed

        // Test sequences
        Evaluable evaluable = new JythonEvaluable("return (1,2)");
        Object result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, new Long[] { 1L, 2L });

        // Test floats
        evaluable = new JythonEvaluable("return 1.2");
        result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, 1.2);

        // Test integers
        evaluable = new JythonEvaluable("return 1");
        result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, 1L);

        // Test strings
        evaluable = new JythonEvaluable("return 'foo'");
        result = evaluable.evaluate(bindings);
        Assert.assertEquals(result, "foo");

    }

    @Test
    public void testGetters() {
        Evaluable evaluable = new JythonEvaluable("return (1,2)");

        assertEquals(evaluable.getSource(), "return (1,2)");
        assertEquals(evaluable.getLanguagePrefix(), "jython");
    }

    @Test(expectedExceptions = ParsingException.class)
    public void testParseError() throws ParsingException {
        JythonEvaluable.createParser().parse("foo(", "jython");
    }
}
