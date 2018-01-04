package org.openrefine.wikidata.schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;

public class SchemaSerializationTest {
    
    static JSONObject jsonFromFile(String filename) throws IOException, JSONException {
        byte[] contents = Files.readAllBytes(Paths.get(filename));
        String decoded = new String(contents, "utf-8");
        return ParsingUtilities.evaluateJsonStringToObject(decoded);
    }
    
    @Test
    public void testDeserializeHistoryOfMedicine() throws JSONException, IOException {
        JSONObject serialized = jsonFromFile("data/schema/history_of_medicine.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
    }
    
    @Test
    public void testDeserializeROARMAP() throws JSONException, IOException {
        JSONObject serialized = jsonFromFile("data/schema/roarmap.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
    }
}
