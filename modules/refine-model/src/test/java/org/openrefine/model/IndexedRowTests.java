
package org.openrefine.model;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class IndexedRowTests {

    @Test
    public void saveIndexedRow() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        IndexedRow tuple = new IndexedRow(1234L, row);
        TestUtils.isSerializedTo(
                tuple,
                "{\"i\":1234,\"r\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void readIndexedRow() throws JsonParseException, JsonMappingException, IOException {
        IndexedRow tuple = ParsingUtilities.mapper.readValue(
                "{\"i\":1234,\"r\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                IndexedRow.class);
        Assert.assertEquals(tuple.getIndex(), 1234L);
    }

    @Test
    public void saveIndexedRowWithOriginalId() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        IndexedRow tuple = new IndexedRow(1234L, 5678L, row);
        TestUtils.isSerializedTo(
                tuple,
                "{\"i\":1234,\"o\":5678,\"r\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void readIndexedRowWithOriginalId() throws JsonParseException, JsonMappingException, IOException {
        IndexedRow tuple = ParsingUtilities.mapper.readValue(
                "{\"i\":1234,\"o\":5678,\"r\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                IndexedRow.class);
        Assert.assertEquals(tuple.getIndex(), 1234L);
        Assert.assertEquals(tuple.getOriginalIndex(), 5678L);
    }
}
