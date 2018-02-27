package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public class WbDateVariableTest extends WbVariableTest<TimeValue> {
    
    private TimeValue year = Datamodel.makeTimeValue(2018, (byte)1, (byte)1, (byte)0, (byte)0,
            (byte)0, (byte)9, 0, 1, 0, TimeValue.CM_GREGORIAN_PRO);
    private TimeValue day = Datamodel.makeTimeValue(2018, (byte)2, (byte)27, TimeValue.CM_GREGORIAN_PRO);
    
    @Override
    public WbVariableExpr<TimeValue> initVariableExpr() {
        return new WbDateVariable();
    }
    
    @Test
    public void testValidFormat() {
        evaluatesTo(year, "2018");
        evaluatesTo(day, "2018-02-27");
    }

    @Test
    public void testWhitespace() {
        evaluatesTo(year, "  2018");
        evaluatesTo(day, "2018-02-27  ");
    }
    
    @Test
    public void testSkipped() {
        isSkipped("  2018-XX");
        isSkipped("invalid format");
    }
    
    // TODO accept parsed dates with default precision
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wbdatevariable\",\"columnName\":\"column A\"}");
    }

}
