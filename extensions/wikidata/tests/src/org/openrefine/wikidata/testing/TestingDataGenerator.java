package org.openrefine.wikidata.testing;

import java.util.Collections;

import org.openrefine.wikidata.schema.WbLanguageConstant;
import org.openrefine.wikidata.schema.WbMonolingualExpr;
import org.openrefine.wikidata.schema.WbStringConstant;
import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.schema.entityvalues.ReconPropertyIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

public class TestingDataGenerator {
    
    public static Recon makeNewItemRecon(long judgementId) {
        Recon recon = Recon.makeWikidataRecon(judgementId);
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
    
    public static ReconPropertyIdValue makeMatchedPropertyIdValue(String pid, String name) {
        return new ReconPropertyIdValue(makeMatchedRecon(pid, name), name);
    }
    
    public static WbMonolingualExpr getTestMonolingualExpr(String langCode, String langLabel, String text) {
        return new WbMonolingualExpr(new WbLanguageConstant(langCode, langLabel), new WbStringConstant(text));
    }
}
