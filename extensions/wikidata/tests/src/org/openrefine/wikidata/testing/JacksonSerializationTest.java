package org.openrefine.wikidata.testing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.testng.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonSerializationTest {
    private static ObjectMapper mapper = new ObjectMapper();
    
    public static void testSerialize(Object pojo, String expectedJson) {
        // Test that the pojo is correctly serialized
        try {
            
            String actualJson = mapper.writeValueAsString(pojo);   
            assertJsonEquals(expectedJson, actualJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assert.fail("Failed to serialize object");
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void testDeserialize(Class targetClass, Object pojo, String inputJson) {
        try {
            Object deserialized = mapper.readValue(inputJson, targetClass);
            assertEquals(pojo, deserialized);
            assertEquals(pojo.hashCode(), deserialized.hashCode());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Failed to deserialize object");
        }
    }
    
    @SuppressWarnings("rawtypes")
    public static void canonicalSerialization(Class targetClass, Object pojo, String json) {
        testSerialize(pojo, json);
        testDeserialize(targetClass, pojo, json);
    }
    
    public static void assertJsonEquals(String expectedJson, String actualJson) {
        JsonNode parsedExpectedJson;
        try {
            parsedExpectedJson = mapper.readValue(expectedJson, JsonNode.class);
            JsonNode parsedActualJson = mapper.readValue(actualJson, JsonNode.class);
            assertEquals(parsedExpectedJson, parsedActualJson);
        } catch (IOException e) {
            Assert.fail("Invalid JSON");
        }
    }
}
