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

package org.openrefine.wikidata.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
//import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.wikidata.schema.WbLanguageConstant;
import org.openrefine.wikidata.schema.WbMonolingualExpr;
import org.openrefine.wikidata.schema.WbStringConstant;
import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.schema.entityvalues.ReconPropertyIdValue;

public class TestingData {

    public static final String inceptionWithNewQS = "Q1377\tP571\t+1919-00-00T00:00:00Z/9"
            + "\tS854\t\"http://www.ljubljana-slovenia.com/university-ljubljana\""
            + "\tS813\t+2018-02-28T00:00:00Z/11\n" + "Q865528\tP571\t+1965-00-00T00:00:00Z/9"
            + "\tS813\t+2018-02-28T00:00:00Z/11\n" + "CREATE\n" + "LAST\tP571\t+2016-00-00T00:00:00Z/9"
            + "\tS854\t\"http://new-uni.com/\"" + "\tS813\t+2018-02-28T00:00:00Z/11\n";

    public static ItemIdValue newIdA = makeNewItemIdValue(1234L, "new item A");
    public static ItemIdValue newIdB = makeNewItemIdValue(4567L, "new item B");
    public static ItemIdValue matchedId = makeMatchedItemIdValue("Q89", "eist");
    public static ItemIdValue existingId = Datamodel.makeWikidataItemIdValue("Q43");

    public static PropertyIdValue newPropertyIdA = makeNewPropertyIdValue(4321L, "new Property A");
    public static PropertyIdValue newPropertyIdB = makeNewPropertyIdValue(7654L, "new Property B");
    public static PropertyIdValue matchedPropertyID = makeMatchedPropertyIdValue("P38", "currency");
    public static PropertyIdValue existingPropertyId = Datamodel.makeWikidataPropertyIdValue("P43");

    protected static PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P38");

    public static class ReconStub extends Recon {

        private static final long serialVersionUID = -8141740928448038842L;

        public ReconStub(long judgmentHistoryEntry, String identifierSpace, String schemaSpace) {
            super(judgmentHistoryEntry, identifierSpace, schemaSpace);
        }
    }

    public static Recon makeNewItemRecon(long id) {
        // we keep the same judgment id because it is ignored
        Recon recon = new ReconStub(382398L, "http://www.wikidata.org/entity/", "http://www.wikidata.org/prop/direct/")
                .withId(id)
                .withJudgment(Judgment.New);
        return recon;
    }

    public static Recon makeMatchedRecon(String qid, String name, String[] types) {
        ReconCandidate candidate = new ReconCandidate(qid, name, types, 100.0);
        Recon recon = Recon.makeWikidataRecon(123456L)
                .withMatch(candidate)
                .withCandidates(Collections.singletonList(candidate))
                .withJudgment(Judgment.Matched);
        return recon;
    }

    public static Recon makeMatchedRecon(String qid, String name) {
        return makeMatchedRecon(qid, name, new String[0]);
    }

    public static Cell makeNewItemCell(long judgementId, String name) {
        return new Cell(name, makeNewItemRecon(judgementId));
    }

    public static Cell makeMatchedCell(String qid, String name) {
        return new Cell(name, makeMatchedRecon(qid, name));
    }

    public static ReconItemIdValue makeNewItemIdValue(long judgementId, String name) {
        return new ReconItemIdValue(makeNewItemRecon(judgementId), name);
    }

    public static ReconItemIdValue makeMatchedItemIdValue(String qid, String name) {
        return new ReconItemIdValue(makeMatchedRecon(qid, name), name);
    }

    public static ReconPropertyIdValue makeNewPropertyIdValue(long judgmentId, String name) {
        return new ReconPropertyIdValue(makeNewItemRecon(judgmentId), name);
    }

    public static ReconPropertyIdValue makeMatchedPropertyIdValue(String pid, String name) {
        return new ReconPropertyIdValue(makeMatchedRecon(pid, name), name);
    }

    public static WbMonolingualExpr getTestMonolingualExpr(String langCode, String langLabel, String text) {
        return new WbMonolingualExpr(new WbLanguageConstant(langCode, langLabel), new WbStringConstant(text));
    }

    /*
     * public static Statement generateStatement(EntityIdValue from, PropertyIdValue pid, EntityIdValue to) { Claim
     * claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList()); return
     * Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, ""); }
     * 
     * public static Statement generateStatement(EntityIdValue from, EntityIdValue to) { return generateStatement(from,
     * pid, to); }
     */
    public static Statement generateStatement(ItemIdValue from, PropertyIdValue pid, ItemIdValue to) {
        Claim claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList());
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    public static Statement generateStatement(ItemIdValue from, ItemIdValue to) {
        return generateStatement(from, pid, to);
    }

    public static Statement generateStatement(PropertyIdValue from, PropertyIdValue pid, PropertyIdValue to) {
        Claim claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList());
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    public static Statement generateStatement(PropertyIdValue from, PropertyIdValue to) {
        return generateStatement(from, pid, to);
    }

    public static String jsonFromFile(String filename)
            throws IOException {
        InputStream f = TestingData.class.getClassLoader().getResourceAsStream(filename);
        String decoded = IOUtils.toString(f);
        return decoded.trim();
    }

}
