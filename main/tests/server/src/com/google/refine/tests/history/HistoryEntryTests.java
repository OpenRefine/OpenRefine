package com.google.refine.tests.history;

import static org.mockito.Mockito.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class HistoryEntryTests extends RefineTest {
    
    Project project;
    
    @BeforeTest
    public void register() {
        OperationRegistry.registerOperation(getCoreModule(), "column-addition", ColumnAdditionOperation.class);
    }
    
    @BeforeMethod
    public void setUp() {
        project = mock(Project.class);
    }
    
    @Test
    public void serializeHistoryEntry() throws Exception {
        String json = "{\"id\":1533651837506,"
                + "\"description\":\"Discard recon judgment for single cell on row 76, column organization_name, containing \\\"Catholic University Leuven\\\"\","
                + "\"time\":\"2018-08-07T14:18:29Z\"}";
        TestUtils.isSerializedTo(HistoryEntry.load(project, json), json);
    }
    
    @Test
    public void serializeHistoryEntryWithOperation() throws Exception {
        String json = "{"
                + "\"id\":1533633623158,"
                + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "\"time\":\"2018-08-07T09:06:37Z\","
                + "\"operation\":{\"op\":\"core/column-addition\","
                + "   \"description\":\"Create column uri at index 2 based on column country using expression grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "   \"newColumnName\":\"uri\","
                + "   \"columnInsertIndex\":2,"
                + "   \"baseColumnName\":\"country\","
                + "   \"expression\":\"grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "   \"onError\":\"set-to-blank\"}"
                + "}";
        String jsonSimple = "{"
                + "\"id\":1533633623158,"
                + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "\"time\":\"2018-08-07T09:06:37Z\"}";
        
        HistoryEntry historyEntry = HistoryEntry.load(project, json);
        TestUtils.isSerializedTo(historyEntry, jsonSimple, false);
        TestUtils.isSerializedTo(historyEntry, json, true);
    }
}
