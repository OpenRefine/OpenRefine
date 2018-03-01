package org.openrefine.wikidata.schema;

import java.util.Collections;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class WbItemDocumentExprTest extends WbExpressionTest<ItemUpdate> {
    
    public WbItemDocumentExpr expr;
    ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    MonolingualTextValue alias = Datamodel.makeMonolingualTextValue("my alias", "en");
    Statement fullStatement;
    
    public String jsonRepresentation;
    
    public WbItemDocumentExprTest() {
        WbStatementGroupExprTest sgt = new WbStatementGroupExprTest();
        WbNameDescExpr nde = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"),
                        new WbStringVariable("column D")));
        WbItemVariable subjectExpr = new WbItemVariable("column E");
        expr = new WbItemDocumentExpr(subjectExpr,
                Collections.singletonList(nde),
                Collections.singletonList(sgt.expr));
        fullStatement = sgt.statementGroup.getStatements().get(0);
        
        jsonRepresentation = "{\"subject\":{\"type\":\"wbitemvariable\",\"columnName\":\"column E\"},"+
                "\"nameDescs\":[{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"+
                "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},"+
                "\"value\":{\"type\":\"wbstringvariable\",\"columnName\":\"column D\"}}}"+
                "],\"statementGroups\":["+sgt.jsonRepresentation+"]}";
    }
    
    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", recon("Q23"));
        ItemUpdate result = new ItemUpdate(subject);
        result.addAlias(alias);
        result.addStatement(fullStatement);
        evaluatesTo(result, expr);
    }
    
    @Test
    public void testSubjectSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", "not reconciled");
        isSkipped(expr);
    }
    
    @Test
    public void testStatementSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid4.389", "my alias", recon("Q23"));
        ItemUpdate result = new ItemUpdate(subject);
        result.addAlias(alias);
        evaluatesTo(result, expr);
    }
    
    @Test
    public void testAliasSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "", recon("Q23"));
        ItemUpdate result = new ItemUpdate(subject);
        result.addStatement(fullStatement);
        evaluatesTo(result, expr);
    }
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbItemDocumentExpr.class, expr, jsonRepresentation);
    }
}
