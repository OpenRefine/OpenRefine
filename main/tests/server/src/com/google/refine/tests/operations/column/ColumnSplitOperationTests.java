package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnSplitOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ColumnSplitOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "column-split", ColumnSplitOperation.class);
    }
    
    @Test
    public void serializeColumnSplitOperationBySeparator() throws Exception {
        String json = "{\n" + 
                "    \"op\": \"core/column-split\",\n" + 
                "    \"description\": \"Split column ea by separator\",\n" + 
                "    \"engineConfig\": {\n" + 
                "      \"mode\": \"row-based\",\n" + 
                "      \"facets\": []\n" + 
                "    },\n" + 
                "    \"columnName\": \"ea\",\n" + 
                "    \"guessCellType\": true,\n" + 
                "    \"removeOriginalColumn\": true,\n" + 
                "    \"mode\": \"separator\",\n" + 
                "    \"separator\": \"e\",\n" + 
                "    \"regex\": false,\n" + 
                "    \"maxColumns\": 0\n" + 
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }
    
    @Test
    public void serializeColumnSplitOperationByLengths() throws Exception {
        String json = "{\n" + 
                "    \"op\": \"core/column-split\",\n" + 
                "    \"description\": \"Split column ea by field lengths\",\n" + 
                "    \"engineConfig\": {\n" + 
                "      \"mode\": \"row-based\",\n" + 
                "      \"facets\": []\n" + 
                "    },\n" + 
                "    \"columnName\": \"ea\",\n" + 
                "    \"guessCellType\": true,\n" + 
                "    \"removeOriginalColumn\": true,\n" + 
                "    \"mode\": \"lengths\",\n" + 
                "    \"fieldLengths\": [1,1]\n" + 
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }
}
