package com.google.refine.grel.controls;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;

public class ReduceTests extends GrelTestBase {

    // an example of evaluating grel code within a test
    private Object getResult(String expression) throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + expression);
        return eval.evaluate(bindings);
    }

    @Test
    public void testReduce() throws ParsingException {
        bindings = new Properties();
        bindings.put("v", "");
        Object result = getResult("reduce([1, 2, 3], v, acc, 0, v + acc)");
        Assert.assertEquals(result, 6L);
    }
}
