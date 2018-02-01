package org.openrefine.wikidata.schema;

import org.testng.annotations.Test;

public class WbLanguageVariableTest extends WbVariableTest<String> {

    @Override
    public WbVariableExpr<String> initVariableExpr() {
        return new WbLanguageVariable();
    }
    
    @Test
    public void testValidLanguageCode() {
        // TODO
    }

}
