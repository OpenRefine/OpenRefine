package com.google.refine.tests.operations.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Properties;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconUseValuesAsIdentifiersOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;


public class ReconUseValuesAsIdsOperationTests extends RefineTest {
    String json = "{"
            + "\"op\":\"core/recon-use-values-as-identifiers\","
            + "\"description\":\"Use values as reconciliation identifiers in column ids\","
            + "\"columnName\":\"ids\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"service\":\"http://localhost:8080/api\","
            + "\"identifierSpace\":\"http://test.org/entities\","
            + "\"schemaSpace\":\"http://test.org/schema\""
            + "}";
    
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-use-values-as-identifiers", ReconUseValuesAsIdentifiersOperation.class);
    }
    
    @Test
    public void serializeReconUseValuesAsIdentifiersOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconUseValuesAsIdentifiersOperation.class), json);
    }
    
    @Test
    public void testUseValuesAsIds() throws Exception {
        Project project = createCSVProject("ids,v\n"
                + "Q343,hello\n"
                + ",world\n"
                + "Q31,test");
        ReconUseValuesAsIdentifiersOperation op = ParsingUtilities.mapper.readValue(json, ReconUseValuesAsIdentifiersOperation.class);
        op.createProcess(project, new Properties()).performImmediate();
        
        assertEquals("Q343", project.rows.get(0).cells.get(0).recon.match.id);
        assertEquals("http://test.org/entities", project.rows.get(0).cells.get(0).recon.identifierSpace);
        assertNull(project.rows.get(1).cells.get(0));
        assertEquals("Q31", project.rows.get(2).cells.get(0).recon.match.id);
        assertEquals(2, project.columnModel.columns.get(0).getReconStats().matchedTopics);
        assertEquals("http://test.org/schema", ((StandardReconConfig)project.columnModel.columns.get(0).getReconConfig()).schemaSpace);
    }
}
