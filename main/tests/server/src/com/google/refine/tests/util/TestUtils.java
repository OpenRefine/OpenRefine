package com.google.refine.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.Jsonizable;
import com.google.refine.util.JSONUtilities;


public class TestUtils {
    
    static ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a temporary directory. NOTE: This is a quick and dirty
     * implementation suitable for tests, not production code.
     * 
     * @param name
     * @return
     * @throws IOException
     */
    public static File createTempDirectory(String name)
            throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }
    
    /**
     * Compare two JSON strings for equality.
     */
    public static void equalAsJson(String expected, String actual) {
        try {
            JsonNode jsonA = mapper.readValue(expected, JsonNode.class);
            JsonNode jsonB = mapper.readValue(actual, JsonNode.class);
            assertEquals(jsonA, jsonB);
        } catch(Exception e) {
            fail("\""+expected+"\" and \""+actual+"\" are not equal as JSON strings.");
        }
    }
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     */
    public static void isSerializedTo(Jsonizable o, String targetJson, Properties options) {
        equalAsJson(targetJson, JSONUtilities.serialize(o, options));
        
        // also check Jackson serialization
        try {
			equalAsJson(targetJson, mapper.writeValueAsString(o));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			fail("jackson serialization failed");
		}
    }
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     */
    public static void isSerializedTo(Jsonizable o, String targetJson) {
        isSerializedTo(o, targetJson, new Properties());
    }
}
