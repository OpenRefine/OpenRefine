package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

public class WbLocationConstantTest extends WbExpressionTest<GlobeCoordinatesValue> {
    
    private GlobeCoordinatesValue loc = Datamodel.makeGlobeCoordinatesValue(1.2345, 6.7890,
            WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH);
    private GlobeCoordinatesValue locWithPrecision = Datamodel.makeGlobeCoordinatesValue(1.2345, 6.7890,
            0.1, GlobeCoordinatesValue.GLOBE_EARTH);
    private String input = "1.2345,6.7890";
    private String inputWithPrecision = "1.2345,6.7890,0.1";
    
    public void testParseValid() throws ParseException {
        assertEquals(loc, WbLocationConstant.parse(input));
        assertEquals(locWithPrecision, WbLocationConstant.parse(inputWithPrecision));
    }
    
    @Test(expectedExceptions=ParseException.class)
    public void testParseInvalid() throws ParseException {
        WbLocationConstant.parse("some bad value");
    }
    
    @Test
    public void testEvaluate() throws ParseException {
        evaluatesTo(loc, new WbLocationConstant(input));
    }
    
    @Test
    public void testEvaluateWithPrecision() throws ParseException {
        evaluatesTo(locWithPrecision, new WbLocationConstant(inputWithPrecision));
    }
    
    @Test(expectedExceptions=ParseException.class)
    public void constructInvalid() throws ParseException {
        new WbLocationConstant("some bad value");
    }
    
    @Test(expectedExceptions=ParseException.class)
    public void constructNotNumber() throws ParseException {
        new WbLocationConstant("lat,lng");
    }
    
    @Test(expectedExceptions=ParseException.class)
    public void constructtooManyParts() throws ParseException {
        new WbLocationConstant("0.1,2.3,4.5,6.7");
    }
    
    @Test
    public void testSerialization() throws ParseException {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, new WbLocationConstant(input),
                "{\"type\":\"wblocationconstant\",\"value\":\""+input+"\"}");
    }
}
