package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;


public class WbLocationVariableTest extends WbVariableTest<GlobeCoordinatesValue> {

    @Override
    public WbVariableExpr<GlobeCoordinatesValue> initVariableExpr() {
        return new WbLocationVariable();
    }

    @Test
    public void testWithSlash() {
        evaluatesTo(Datamodel.makeGlobeCoordinatesValue(1.234, 5.678,
                WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH), "1.234/5.678");
    }
    
    @Test
    public void testWithComma() {
        evaluatesTo(Datamodel.makeGlobeCoordinatesValue(1.234, 5.678,
                WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH), "1.234,5.678");
    }
    
    @Test
    public void testWhitespace() {
        evaluatesTo(Datamodel.makeGlobeCoordinatesValue(1.234, 5.678,
                WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH), "  1.234, 5.678");
    }
    
    @Test
    public void testOnlyOneValue() {
        isSkipped("1.2348");
    }
    
    @Test
    public void testEmpty() {
        isSkipped("");
    }
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wblocationvariable\",\"columnName\":\"column A\"}");
    }
}
