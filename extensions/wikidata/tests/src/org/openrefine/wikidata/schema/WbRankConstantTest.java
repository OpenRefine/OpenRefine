package org.openrefine.wikidata.schema;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.openrefine.wikidata.testing.JacksonSerializationTest;

public class WbRankConstantTest extends WbExpressionTest<StatementRank> {

    private WbRankConstant rankConstant = new WbRankConstant("normal");

    @Test
    public void testEmptyCreation() {
        WbRankConstant withNull = new WbRankConstant(null);
        assertEquals(rankConstant, withNull);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, rankConstant,
            "{\"type\":\"wbrankconstant\",\"rank\":\"normal\"}");
    }

    @Test
    public void testEvaluate() {
        evaluatesTo(StatementRank.NORMAL, rankConstant);
    }


}