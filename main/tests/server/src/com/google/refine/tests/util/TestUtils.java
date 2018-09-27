package com.google.refine.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.google.refine.Jsonizable;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;


public class TestUtils {
    
    static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper = mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

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
    public static void assertEqualAsJson(String expected, String actual) {
        try {
            JsonNode jsonA = mapper.readValue(expected, JsonNode.class);
            JsonNode jsonB = mapper.readValue(actual, JsonNode.class);
            assertEquals(jsonA, jsonB);
        } catch(Exception e) {
            fail("\""+expected+"\" and \""+actual+"\" are not equal as JSON strings.");
        }
    }
    
    public static boolean equalAsJson(String expected, String actual)  {
        try {
            JsonNode jsonA = mapper.readValue(expected, JsonNode.class);
            JsonNode jsonB = mapper.readValue(actual, JsonNode.class);
            return (jsonA == null && jsonB == null) || jsonA.equals(jsonB);
        } catch(Exception e) {
            return false;
        }
    }
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     * @throws IOException 
     */
    public static void isSerializedTo(Jsonizable o, String targetJson, Properties options) {
        String orgJson = JSONUtilities.serialize(o, options);
        if(!equalAsJson(targetJson, orgJson)) {
            System.out.println("org.json, "+o.getClass().getName());
            try {
                jsonDiff(targetJson, orgJson);
            } catch (JsonParseException | JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        assertEqualAsJson(targetJson, orgJson);
        
        // also check Jackson serialization
        try {
            String saveMode = options.getProperty("mode");
            ObjectWriter writer = null;
            if("save".equals(saveMode)) {
                writer = ParsingUtilities.saveWriter;
            } else {
                writer = ParsingUtilities.defaultWriter;
            }
            String jacksonJson = writer.writeValueAsString(o);
            if(!equalAsJson(targetJson, jacksonJson)) {
                System.out.println("jackson, "+o.getClass().getName());
                jsonDiff(targetJson, jacksonJson);
            }
    	    assertEqualAsJson(targetJson, jacksonJson);
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
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     * This specifies the "save mode" for objects that are stored differently depending on
     * whether they are written to disk or sent over the network.
     */
    public static void isSerializedTo(Jsonizable o, String targetJson, boolean saveMode) {
        Properties options = new Properties();
        if(saveMode) {
            options.setProperty("mode", "save");
            options.put("mode", "save");
        }
        isSerializedTo(o, targetJson, options);
    }
    
    public static void jsonDiff(String a, String b) throws JsonParseException, JsonMappingException {
        ObjectMapper myMapper = mapper.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            JsonNode nodeA = myMapper.readValue(a, JsonNode.class);
            JsonNode nodeB = myMapper.readValue(b, JsonNode.class);
            String prettyA = myMapper.writeValueAsString(myMapper.treeToValue(nodeA, Object.class));
            String prettyB = myMapper.writeValueAsString(myMapper.treeToValue(nodeB, Object.class));
            
            // Compute the max line length of A
            LineNumberReader readerA = new LineNumberReader(new StringReader(prettyA));
            int maxLength = 0;
            String line = readerA.readLine();
            while (line != null) {
                if(line.length() > maxLength) {
                    maxLength = line.length();
                }
                line = readerA.readLine();
            }
            
            // Pad all lines
            readerA = new LineNumberReader(new StringReader(prettyA));
            LineNumberReader readerB = new LineNumberReader(new StringReader(prettyB));
            StringWriter writer = new StringWriter();
            String lineA = readerA.readLine();
            String lineB = readerB.readLine();
            while(lineA != null || lineB != null) {
                if (lineA == null) {
                    lineA = "";
                }
                if (lineB == null) {
                    lineB = "";
                }
                String paddedLineA = lineA +  new String(new char[maxLength + 2 - lineA.length()]).replace("\0", " ");
                writer.write(paddedLineA);
                writer.write(lineB + "\n");
                lineA = readerA.readLine();
                lineB = readerB.readLine();
            }
            System.out.print(writer.toString());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
