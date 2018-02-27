package org.openrefine.wikidata.schema;

import java.util.Collections;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

public class WbItemVariableTest extends WbVariableTest<ItemIdValue> {

    @Override
    public WbVariableExpr<ItemIdValue> initVariableExpr() {
        return new WbItemVariable();
    }
    
    @Test
    public void testReconciledCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L);
        recon.judgment = Recon.Judgment.Matched;
        recon.match = new ReconCandidate("Q123", "some item", null, 100.0);
        Cell cell = new Cell("some value", recon);
        evaluatesTo(new ReconItemIdValue(recon, "some value"), cell);
    }
    
    @Test
    public void testNewItemCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L);
        recon.judgment = Recon.Judgment.New;
        recon.candidates = Collections.singletonList(new ReconCandidate("Q123", "some item", null, 100.0));
        Cell cell = new Cell("some value", recon);
        evaluatesTo(new ReconItemIdValue(recon, "some value"), cell);
    }
    
    @Test
    public void testUnmatchedCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L);
        recon.judgment = Recon.Judgment.None;
        recon.candidates = Collections.singletonList(new ReconCandidate("Q123", "some item", null, 100.0));
        Cell cell = new Cell("some value", recon);
        isSkipped(cell);
    }
    
    @Test
    public void testUnreconciledCell() {
        isSkipped("some value");
    }
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wbitemvariable\",\"columnName\":\"column A\"}");
    }
}
