package org.openrefine.wikidata.testing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.testng.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonSerializationTest {
    private static ObjectMapper mapper = new ObjectMapper();
    
    public static void testSerialize(Object pojo, String expectedJson) {
        // Test that the pojo is correctly serialized
        try {
            JsonNode parsedExpectedJson = mapper.readValue(expectedJson, JsonNode.class);
            String actualJson = mapper.writeValueAsString(pojo);
            JsonNode parsedActualJson = mapper.readValue(actualJson, JsonNode.class);
            assertEquals(parsedExpectedJson, parsedActualJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assert.fail("Failed to serialize object");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Invalid test JSON provided");
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
}
