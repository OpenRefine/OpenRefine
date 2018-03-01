package org.openrefine.wikidata.schema;

import java.util.Arrays;
import java.util.Collections;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WbReferenceExprTest extends WbExpressionTest<Reference> {
    
    private WbReferenceExpr expr = new WbReferenceExpr(
            Arrays.asList(
                    new WbSnakExpr(new WbPropConstant("P87","retrieved","time"),
                                   new WbDateVariable("column A")),
                    new WbSnakExpr(new WbPropConstant("P347","reference URL","url"),
                                   new WbStringVariable("column B")))
            );
    
    private Snak snak1 = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P87"),
            Datamodel.makeTimeValue(2018, (byte)3, (byte)28, TimeValue.CM_GREGORIAN_PRO));
    private Snak snak2 = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P347"),
            Datamodel.makeStringValue("http://gnu.org/"));
    
    private String jsonRepresentation = "{\"snaks\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P87\","
            +"\"label\":\"retrieved\",\"datatype\":\"time\"},\"value\":{\"type\":\"wbdatevariable\","
            +"\"columnName\":\"column A\"}},{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P347\","
            +"\"label\":\"reference URL\",\"datatype\":\"url\"},\"value\":{\"type\":\"wbstringvariable\","
            +"\"columnName\":\"column B\"}}]}";
    
    @Test
    public void testEvaluate() {
        setRow("2018-03-28", "http://gnu.org/");   
        evaluatesTo(Datamodel.makeReference(Arrays.asList(
                Datamodel.makeSnakGroup(Collections.singletonList(snak1)),
                Datamodel.makeSnakGroup(Collections.singletonList(snak2)))), expr);
    }
    
    @Test
    public void testEvaluateWithOneSkip() {
        setRow("invalid date", "http://gnu.org/");   
        evaluatesTo(Datamodel.makeReference(Arrays.asList(
                Datamodel.makeSnakGroup(Collections.singletonList(snak2)))), expr);
    }
    
    @Test
    public void testNoValidSnak() {
        setRow("invalid date", "");   
        isSkipped(expr);
    }
    
    @Test
    public void testSerialize() throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbReferenceExpr.class, expr, jsonRepresentation);
    }
}
