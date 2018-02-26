package org.openrefine.wikidata.testing;

import java.util.Collections;

import org.openrefine.wikidata.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

public class TestingDataGenerator {
    
    public static Recon makeNewItemRecon(long judgementId) {
        Recon recon = Recon.makeWikidataRecon(judgementId);
        recon.judgment = Recon.Judgment.New;
        return recon;
    }
    
    public static Recon makeMatchedRecon(String qid, String name) {
        Recon recon = Recon.makeWikidataRecon(123456L);
        recon.match = new ReconCandidate(qid, name, new String[0], 100.0);
        recon.candidates = Collections.singletonList(recon.match);
        recon.judgment = Recon.Judgment.Matched;
        return recon;
    }
    
    public static Cell makeNewItemCell(long judgementId, String name) {
        return new Cell(name, makeNewItemRecon(judgementId));
    }
    
    public static Cell makeMatchedCell(String qid, String name) {
        return new Cell(name, makeMatchedRecon(qid, name));
    }
    
    public static ReconEntityIdValue makeNewItemIdValue(long judgementId, String name) {
        return new ReconItemIdValue(makeNewItemRecon(judgementId), name);
    }
}
