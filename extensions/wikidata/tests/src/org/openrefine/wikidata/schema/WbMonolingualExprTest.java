package org.openrefine.wikidata.schema;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class WbMonolingualExprTest extends WbExpressionTest<MonolingualTextValue> {
    
    private WbMonolingualExpr expr = new WbMonolingualExpr(
            new WbLanguageVariable("column A"),
            new WbStringVariable("column B"));
    
    @Test
    public void testEvaluateConstant() {
        evaluatesTo(Datamodel.makeMonolingualTextValue("hello", "en"),
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"),
                        new WbStringConstant("hello")));
    }
    
    @Test
    public void testEvaluateVariable() {
        setRow("en", "hello");
        evaluatesTo(Datamodel.makeMonolingualTextValue("hello", "en"), expr);
    }
    
    
    @Test
    public void testInvalidLanguageCode() {
        setRow("ueuue", "my label");
        isSkipped(expr);
    }
    
    @Test
    public void testEmptyLanguageCode() {
        setRow("", "my label");
        isSkipped(expr);
    }
    
    @Test
    public void testEmptyText() {
        setRow("en", "");
        isSkipped(expr);
    }
}
