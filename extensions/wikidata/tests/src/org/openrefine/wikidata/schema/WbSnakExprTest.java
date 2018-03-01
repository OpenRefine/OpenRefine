package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WbSnakExprTest extends WbExpressionTest<Snak> {
    private PropertyIdValue propStringId = Datamodel.makeWikidataPropertyIdValue("P89");
    private WbPropConstant propStringExpr = new WbPropConstant("P89", "prop label", "string");
    private WbSnakExpr expr = new WbSnakExpr(propStringExpr, new WbStringVariable("column A"));
    
    public String jsonRepresentation = "{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P89\","
            +"\"label\":\"prop label\",\"datatype\":\"string\"},\"value\":"
            +"{\"type\":\"wbstringvariable\",\"columnName\":\"column A\"}}";
    
    @Test
    public void testEvaluate() {
        setRow("cinema");
        evaluatesTo(Datamodel.makeValueSnak(propStringId, Datamodel.makeStringValue("cinema")), expr);
    }
    
    @Test
    public void testSerialize() throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbSnakExpr.class, expr, jsonRepresentation);
    }
   
    // TODO check that the datatype of the property matches that of the datavalue (important when we introduce property variables)
}
