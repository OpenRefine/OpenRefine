package org.openrefine.model.changes;

import java.io.IOException;

import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

public class IndexedDataTests {
    
    @Test
    public void testDeserialize() throws JsonParseException, JsonMappingException, IOException {
        String json = "{\"i\":12,\"d\":\"foo\"}";
        IndexedData<String> deserialized = ParsingUtilities.mapper.readValue(json, new TypeReference<IndexedData<String>>() {});
        Assert.assertEquals(deserialized.getData(), "foo");
        Assert.assertEquals(deserialized.getId(), 12L);
    }
    
    @Test
    public void testEquals() {
        Assert.assertNotEquals(new IndexedData<String>(1L, "foo"), 3);
        Assert.assertEquals(new IndexedData<String>(3L, "bar"), new IndexedData<String>(3L, "bar"));
        Assert.assertEquals(new IndexedData<String>(3L, "bar").hashCode(), new IndexedData<String>(3L, "bar").hashCode());
    }
    
    @Test
    public void testToString() {
        Assert.assertEquals(new IndexedData<String>(1L, "foo").toString(), "[IndexedData 1 foo]");
    }
}
