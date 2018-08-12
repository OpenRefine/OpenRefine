package com.google.refine.tests.operations.recon;
import static org.mockito.Mockito.mock;

import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconMatchSpecificTopicOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ReconMatchSpecificTopicOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-match-specific-topic-to-cells", ReconMatchSpecificTopicOperation.class);
    }
    
    @Test
    public void serializeReconMatchSpecificTopicOperation() throws Exception {
        String json = "{\n" + 
                "    \"op\": \"core/recon-match-specific-topic-to-cells\",\n" + 
                "    \"description\": \"Match specific item Gangnam (Q489941) to cells in column researcher\",\n" + 
                "    \"engineConfig\": {\n" + 
                "      \"mode\": \"record-based\",\n" + 
                "      \"facets\": []\n" + 
                "    },\n" + 
                "    \"columnName\": \"researcher\",\n" + 
                "    \"match\": {\n" + 
                "      \"id\": \"Q489941\",\n" + 
                "      \"name\": \"Gangnam\",\n" + 
                "      \"types\": [\n" + 
                "        \"Q5\"\n" + 
                "      ]\n" + 
                "    },\n" + 
                "    \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
                "    \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\"\n" + 
                "  }";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ReconMatchSpecificTopicOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
