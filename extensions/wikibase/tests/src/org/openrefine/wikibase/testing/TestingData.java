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

package org.openrefine.wikibase.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.openrefine.wikibase.schema.WbLanguageConstant;
import org.openrefine.wikibase.schema.WbMonolingualExpr;
import org.openrefine.wikibase.schema.WbStringConstant;
import org.openrefine.wikibase.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikibase.schema.entityvalues.ReconMediaInfoIdValue;
import org.openrefine.wikibase.schema.entityvalues.ReconPropertyIdValue;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.updates.StatementEdit;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
//import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

public class TestingData {

    public static final String inceptionCsv = "subject,inception,reference\n"
            + "Q1377,1919,http://www.ljubljana-slovenia.com/university-ljubljana\n" + "Q865528,1965,";
    public static final String inceptionWithNewCsv = "subject,inception,reference\n"
            + "Q1377,1919,http://www.ljubljana-slovenia.com/university-ljubljana\n" + "Q865528,1965,\n"
            + "new uni,2016,http://new-uni.com/";
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

    public static MediaInfoIdValue newMidA = makeNewMediaInfoIdValue(3412L, "new MediaInfo A");
    public static MediaInfoIdValue newMidB = makeNewMediaInfoIdValue(6745L, "new MediaInfo B");
    public static MediaInfoIdValue matchedMid = makeMatchedMediaInfoIdValue("M89", "eist");
    public static MediaInfoIdValue existingMid = Datamodel.makeWikimediaCommonsMediaInfoIdValue("M43");

    protected static PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P38");

    public static class ReconStub extends Recon {

        public ReconStub(long id, long judgmentHistoryEntry) {
            super(id, judgmentHistoryEntry);
        }
    }

    public static Recon makeNewItemRecon(long id) {
        Recon recon = new ReconStub(id, 382398L); // we keep the same judgment id because it is ignored
        recon.identifierSpace = "http://www.wikidata.org/entity/";
        recon.schemaSpace = "http://www.wikidata.org/prop/direct/";
        recon.judgment = Recon.Judgment.New;
        return recon;
    }

    public static Recon makeMatchedRecon(String qid, String name, String[] types) {
        Recon recon = Recon.makeWikidataRecon(123456L);
        recon.match = new ReconCandidate(qid, name, types, 100.0);
        recon.candidates = Collections.singletonList(recon.match);
        recon.judgment = Recon.Judgment.Matched;
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

    public static MediaInfoIdValue makeNewMediaInfoIdValue(long judgmentId, String name) {
        return new ReconMediaInfoIdValue(makeNewItemRecon(judgmentId), name);
    }

    public static ReconPropertyIdValue makeMatchedPropertyIdValue(String pid, String name) {
        return new ReconPropertyIdValue(makeMatchedRecon(pid, name), name);
    }

    public static MediaInfoIdValue makeMatchedMediaInfoIdValue(String mid, String name) {
        return new ReconMediaInfoIdValue(makeMatchedRecon(mid, name), name);
    }

    public static WbMonolingualExpr getTestMonolingualExpr(String langCode, String langLabel, String text) {
        return new WbMonolingualExpr(new WbLanguageConstant(langCode, langLabel), new WbStringConstant(text));
    }

    public static Statement generateStatement(EntityIdValue from, PropertyIdValue pid, EntityIdValue to) {
        Claim claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList());
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    public static Statement generateStatement(EntityIdValue from, EntityIdValue to) {
        return generateStatement(from, pid, to);
    }

    public static StatementEdit generateStatementAddition(EntityIdValue from, EntityIdValue to) {
        return new StatementEdit(generateStatement(from, to), StatementMerger.FORMER_DEFAULT_STRATEGY, StatementEditingMode.ADD_OR_MERGE);
    }

    public static StatementEdit generateStatementDeletion(EntityIdValue from, EntityIdValue to) {
        return new StatementEdit(generateStatement(from, to), StatementMerger.FORMER_DEFAULT_STRATEGY, StatementEditingMode.DELETE);
    }

    public static Statement generateStatement(EntityIdValue from, PropertyIdValue pid, Value to) {
        Claim claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList());
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    public static String jsonFromFile(String filename)
            throws IOException {
        InputStream f = TestingData.class.getClassLoader().getResourceAsStream(filename);
        String decoded = IOUtils.toString(f);
        return decoded.trim();
    }

    public static void reconcileInceptionCells(Project project) {
        project.rows.get(0).cells.set(0, TestingData.makeMatchedCell("Q1377", "University of Ljubljana"));
        project.rows.get(1).cells.set(0, TestingData.makeMatchedCell("Q865528", "University of Warwick"));
        project.rows.get(2).cells.set(0, TestingData.makeNewItemCell(1234L, "new uni"));
    }

}
