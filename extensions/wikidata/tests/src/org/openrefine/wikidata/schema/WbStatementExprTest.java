package org.openrefine.wikidata.schema;

import java.util.Collections;

import org.testng.annotations.Test;

public class WbStatementExprTest {
    @Test
    public void testCreation() {
        WbItemConstant q5 = new WbItemConstant("Q5", "human");
        new WbStatementExpr(q5, Collections.emptyList(), Collections.emptyList());
    }
}
