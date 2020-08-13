package org.openrefine.model.changes;

import java.io.IOException;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ColumnChangeByChangeDataTests {
    
    public static String changeJson = "{\n" + 
            "        \"type\": \"org.openrefine.model.changes.ColumnChangeByChangeData\"," +
            "        \"columnIndex\": 1," + 
            "        \"columnName\": \"foo\"," +
            "        \"changeDataId\": \"urls\",\n" + 
            "        \"dagSlice\": null\n" + 
            "      }";
    
    @Test
    public void testSerialize() throws JsonParseException, JsonMappingException, IOException {
        Change change = ParsingUtilities.mapper.readValue(changeJson, Change.class);
        TestUtils.equalAsJson(changeJson, ParsingUtilities.mapper.writeValueAsString(change));
    }
}
