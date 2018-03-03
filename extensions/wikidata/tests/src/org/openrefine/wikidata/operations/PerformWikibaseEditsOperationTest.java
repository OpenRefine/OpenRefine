package org.openrefine.wikidata.operations;

import static org.junit.Assert.assertEquals;

import java.io.LineNumberReader;

import org.json.JSONObject;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Recon;

public class PerformWikibaseEditsOperationTest extends OperationTest {
    
    @BeforeMethod
    public void registerOperation() {
        registerOperation("perform-wikibase-edits", PerformWikibaseEditsOperation.class);
    }
    
    @Override
    public AbstractOperation reconstruct() throws Exception {
        JSONObject json = getJson();
        return PerformWikibaseEditsOperation.reconstruct(project, json);
    }

    @Override
    public JSONObject getJson() throws Exception {
        return TestingData.jsonFromFile("data/operations/perform-edits.json");
    }
    
    @Test
    public void testLoadChange() throws Exception {
        String changeString = "newItems={\"qidMap\":{\"1234\":\"Q789\"}}\n" + 
                "/ec/\n";
        LineNumberReader reader = makeReader(changeString);
        Change change = PerformWikibaseEditsOperation.PerformWikibaseEditsChange.load(reader, pool);
        
        project.rows.get(0).cells.set(0, TestingData.makeNewItemCell(1234L, "my new item"));
        
        change.apply(project);
        
        assertEquals(Recon.Judgment.Matched, project.rows.get(0).cells.get(0).recon.judgment);
        assertEquals("Q789", project.rows.get(0).cells.get(0).recon.match.id);
        
        change.revert(project);
        
        assertEquals(Recon.Judgment.New, project.rows.get(0).cells.get(0).recon.judgment);
        
        assertEquals(changeString, saveChange(change));
    }

}
