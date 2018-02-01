package org.openrefine.wikidata.schema;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public class WbStringVariableTest extends WbVariableTest<StringValue> {

    @Override
    public WbVariableExpr<StringValue> initVariableExpr() {
        return new WbStringVariable();
    }
    
    @Test
    public void testEmpty() {
        isSkipped("");
    }
    
    @Test
    public void testSimpleString() {
        evaluatesTo(Datamodel.makeStringValue("apfelstrudel"), "apfelstrudel");
    }

    /**
     * It is not up to the evaluator to clean up the strings it gets.
     * This is flagged later on by scrutinizers.
     */
    @Test
    public void testTrailingWhitespace() {
        evaluatesTo(Datamodel.makeStringValue("dirty \t"), "dirty \t");
    }
    
    @Test
    public void testLeadingWhitespace() {
        evaluatesTo(Datamodel.makeStringValue(" dirty"), " dirty");
    }
    
    @Test
    public void testDoubleWhitespace() {
        evaluatesTo(Datamodel.makeStringValue("very  dirty"), "very  dirty");
    }
}
