package org.openrefine.wikidata.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.LineNumberReader;

import org.json.JSONObject;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;


public class SaveWikibaseSchemaOperationTest extends OperationTest {
    
    @BeforeMethod
    public void registerOperation() {
        registerOperation("save-wikibase-schema", SaveWikibaseSchemaOperation.class);
    }

    @Override
    public AbstractOperation reconstruct() throws Exception {
        return SaveWikibaseSchemaOperation.reconstruct(project, getJson());
    }

    @Override
    public JSONObject getJson() throws Exception {
        return TestingData.jsonFromFile("data/operations/save-schema.json");
    }
    
    @Test
    public void testLoadChange() throws Exception {
        JSONObject schemaJson = TestingData.jsonFromFile("data/schema/inception.json");
        String changeString =
                "newSchema="+schemaJson.toString()+"\n" + 
                "oldSchema=\n" + 
                "/ec/";
        WikibaseSchema schema = WikibaseSchema.reconstruct(schemaJson);

        LineNumberReader reader = makeReader(changeString);
        Change change = SaveWikibaseSchemaOperation.WikibaseSchemaChange.load(reader, pool);

        change.apply(project);
        
        assertEquals(schema, project.overlayModels.get(SaveWikibaseSchemaOperation.WikibaseSchemaChange.overlayModelKey));
        
        change.revert(project);
        
        assertNull(project.overlayModels.get(SaveWikibaseSchemaOperation.WikibaseSchemaChange.overlayModelKey));
        
        saveChange(change); // not checking for equality because JSON serialization varies
    }
}
