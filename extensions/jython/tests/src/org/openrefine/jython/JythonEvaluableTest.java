
package org.openrefine.jython;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.Evaluable;
import org.openrefine.jython.JythonEvaluable;

/**
 * @author Maxim Galushka
 */
public class JythonEvaluableTest {

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
