package com.google.refine.expr.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;

public class ToNumberJUnitTests {

    private static final double EPSILON = 1e-6;

    private final Function f = new ToNumber();
    private final Properties bindings = new Properties();

    @Test
    public void testIntegerStringParsesToLong() {
        assertEquals(Long.valueOf(12345), f.call(bindings, new Object[] { "12345" }));
        assertEquals(Long.valueOf(0), f.call(bindings, new Object[] { "0" }));
        assertEquals(Long.valueOf(123), f.call(bindings, new Object[] { "00123" }));
        assertEquals(Long.valueOf(-42), f.call(bindings, new Object[] { "-42" }));
        assertEquals(Long.valueOf(77), f.call(bindings, new Object[] { "+77" }));
    }

    @Test
    public void testFloatingStringParsesToDouble() {
        Object r = f.call(bindings, new Object[] { "123.456" });
        assertInstanceOf(Double.class, r);
        assertTrue(Math.abs((Double) r - 123.456) < EPSILON);

        Object r2 = f.call(bindings, new Object[] { "001.234" });
        assertInstanceOf(Double.class, r2);
        assertTrue(Math.abs((Double) r2 - 1.234) < EPSILON);
    }

    @Test
    public void testScientificNotationParsesToDouble() {
        Object r = f.call(bindings, new Object[] { "1e3" });
        assertInstanceOf(Double.class, r);
        assertTrue(Math.abs((Double) r - 1000.0) < EPSILON);

        Object r2 = f.call(bindings, new Object[] { "2E2" });
        assertInstanceOf(Double.class, r2);
        assertTrue(Math.abs((Double) r2 - 200.0) < EPSILON);
    }

    @Test
    public void testNonNumericInputsProduceEvalError() {
        assertInstanceOf(EvalError.class, f.call(bindings, new Object[] { "abc" }));
        assertInstanceOf(EvalError.class, f.call(bindings, new Object[] { "12a" }));
        assertInstanceOf(EvalError.class, f.call(bindings, new Object[] { new Object() }));
    }

    @Test
    public void testEmptyAndNullInputs() {
        assertInstanceOf(EvalError.class, f.call(bindings, new Object[] { "" }));
        assertInstanceOf(EvalError.class, f.call(bindings, new Object[] { (Object) null }));
    }

    @Test
    public void testNumberInputsReturnedUnchanged() {
        assertEquals(Long.valueOf(11), f.call(bindings, new Object[] { Long.valueOf(11) }));
        assertEquals(Double.valueOf(3.14), f.call(bindings, new Object[] { Double.valueOf(3.14) }));

        Integer in = Integer.valueOf(5);
        Object out = f.call(bindings, new Object[] { in });
        assertInstanceOf(Number.class, out);
        assertEquals(5, ((Number) out).intValue());
    }

    @Test
    public void testNonStringObjectWithNumericToStringParses() {
        Object r = f.call(bindings, new Object[] { new StringBuilder("88") });
        assertEquals(Long.valueOf(88), r);
    }

    @Test
public void testWhitespaceNumericString() {
    Object r = f.call(bindings, new Object[] { "  12  " });
    assertInstanceOf(Number.class, r);
    assertEquals(12.0, ((Number) r).doubleValue(), EPSILON);
}
}