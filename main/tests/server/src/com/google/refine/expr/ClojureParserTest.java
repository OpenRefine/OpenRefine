
package com.google.refine.expr;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class ClojureParserTest {

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

    @Test
    public void testBasicClojureExpression() throws ParsingException {
        Properties bindings = createBindings();
        // just make sure that value is a number
        bindings.put("value", 1);

        Evaluable evaluable = (new ClojureParser()).parse("(+ 0 value)", "clojure");
        Object result = evaluable.evaluate(bindings);
        assertEquals(new BigDecimal((long) result), new BigDecimal((int) bindings.get("value")),
                "Adding 0 to a number should keep the number.");
    }

    @Test(expectedExceptions = ParsingException.class)
    public void testEvalErrorOnSomeBindings() throws ParsingException {
        String testKey = "columnName";
        Properties bindings = createBindings();
        assertTrue(bindings.containsKey(testKey), "Bindings should contain '" + testKey + "'.");

        Evaluable evaluable = (new ClojureParser()).parse("(count " + testKey + ")", "clojure");
        evaluable.evaluate(bindings);
    }

    @Test
    public void testSpecialValuesForUserDefinedDistance() throws ParsingException {
        Properties bindings = createBindings();
        String a = "aaaa";
        String b = "bbb";
        // special values used in custom clustering via UserDefinedDistance
        bindings.put("value1", a);
        bindings.put("value2", b);

        long expectedLength = a.length() - b.length();

        Evaluable evaluable = (new ClojureParser()).parse("(- (count value1) (count value2))", "clojure");
        Object result = evaluable.evaluate(bindings);
        assertEquals(result, expectedLength, "Length difference should be the same.");
    }
}
