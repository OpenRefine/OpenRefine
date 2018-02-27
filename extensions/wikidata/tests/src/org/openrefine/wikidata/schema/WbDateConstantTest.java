package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public class WbDateConstantTest extends WbExpressionTest<TimeValue> {
    
    private WbDateConstant year = new WbDateConstant("2018");
    private WbDateConstant month = new WbDateConstant("2018-02");
    private WbDateConstant day = new WbDateConstant("2018-02-27");
    private WbDateConstant whitespace = new WbDateConstant("   2018-02-27  ");
    private WbDateConstant hour = new WbDateConstant("2018-02-27T13");
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, year,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018\"}");
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, day,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018-02-27\"}");
    }
    
    @Test
    public void testEvaluate() {
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte)1, (byte)1, (byte)0, (byte)0,
                (byte)0, (byte)9, 0, 1, 0, TimeValue.CM_GREGORIAN_PRO), year);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte)2, (byte)1, (byte)0, (byte)0,
                (byte)0, (byte)10, 0, 1, 0, TimeValue.CM_GREGORIAN_PRO), month);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte)2, (byte)27, TimeValue.CM_GREGORIAN_PRO), day);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte)2, (byte)27, (byte)13, (byte)0,
                (byte)0, (byte)12, 0, 1, 0, TimeValue.CM_GREGORIAN_PRO), hour);
        
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte)2, (byte)27, TimeValue.CM_GREGORIAN_PRO), whitespace);
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testInvalid() {
        new WbDateConstant("invalid format");
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testPartlyValid() {  
        new WbDateConstant("2018-partly valid");
    }
}
