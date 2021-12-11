
package org.openrefine.expr.functions.xml;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.grel.FunctionTestBase;

public class WholeTextTests extends FunctionTestBase {

    @Test
    public void testWholeText() {
        Assert.assertTrue(invoke("wholeText") instanceof EvalError);
        Assert.assertTrue(invoke("wholeText", "test") instanceof EvalError);

        EvalError evalError = (EvalError) invoke("wholeText", "test");
        Assert.assertEquals(evalError.toString(),
                "wholeText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function");
    }
}
