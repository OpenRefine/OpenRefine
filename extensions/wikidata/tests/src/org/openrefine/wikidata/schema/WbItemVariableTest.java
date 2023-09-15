/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikidata.schema;

import java.util.Collections;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.testing.JacksonSerializationTest;

public class WbItemVariableTest extends WbVariableTest<ItemIdValue> {

    @Override
    public WbVariableExpr<ItemIdValue> initVariableExpr() {
        return new WbItemVariable();
    }

    @Test
    public void testReconciledCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L)
                .withJudgment(Judgment.Matched)
                .withMatch(new ReconCandidate("Q123", "some item", null, 100.0));
        Cell cell = new Cell("some value", recon);
        evaluatesTo(new ReconItemIdValue(recon, "some value"), cell);
    }

    @Test
    public void testNewItemCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L)
                .withJudgment(Judgment.New)
                .withCandidates(Collections.singletonList(new ReconCandidate("Q123", "some item", null, 100.0)));
        Cell cell = new Cell("some value", recon);
        evaluatesTo(new ReconItemIdValue(recon, "some value"), cell);
    }

    @Test
    public void testUnmatchedCell() {
        Recon recon = Recon.makeWikidataRecon(3782378L)
                .withJudgment(Judgment.None)
                .withCandidates(Collections.singletonList(new ReconCandidate("Q123", "some item", null, 100.0)));
        Cell cell = new Cell("some value", recon);
        isSkipped(cell);
    }

    @Test
    public void testInvalidSpace() {
        ReconCandidate reconCandidate = new ReconCandidate("Q123", "some item", null, 100.0);
        Recon recon = Recon.makeWikidataRecon(34989L)
                .withIdentifierSpace("http://my.own.wikiba.se/")
                .withCandidates(Collections.singletonList(reconCandidate))
                .withMatch(reconCandidate)
                .withJudgment(Judgment.Matched);
        Cell cell = new Cell("some value", recon);
        isSkipped(cell);
    }

    @Test
    public void testUnreconciledCell() {
        isSkipped("some value");
    }

    @Test
    public void testNullCell() {
        isSkipped((Cell) null);
    }

    @Test
    public void testNullStringValue() {
        isSkipped((String) null);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wbitemvariable\",\"columnName\":\"column A\"}");
    }

    // TODO: test with column reconciled against different identifier space
}
