/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.util;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TestUtils {

    static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper = mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        // Reject duplicate fields because the previous implementation with org.json
        // does not accept them. It is also cleaner and more compact.
        mapper = mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper = mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    }

    /**
     * Create a temporary directory. NOTE: This is a quick and dirty implementation suitable for tests, not production
     * code.
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
     * Assert that two JSON strings are equal as JSON objects.
     *
     * @deprecated for 3.5 by Tom Morris Use the method with the same parameter order as the rest of the assert
     *             methods{@link #assertEqualsAsJson(String, String)}
     */
    public static void assertEqualAsJson(String expected, String actual) {
        assertEqualsAsJson(actual, expected);
    }

    /**
     * Assert that two JSON strings are equal as JSON objects.
     */
    public static void assertEqualsAsJson(String actual, String expected) {
        try {
            JsonNode jsonA = mapper.readValue(expected, JsonNode.class);
            JsonNode jsonB = mapper.readValue(actual, JsonNode.class);
            if (!jsonA.equals(jsonB)) {
                jsonDiff(expected, actual);
                fail("Objects above are not equal as JSON strings.");
            }
        } catch (Exception e) {
            fail("\"" + actual + "\" and \"" + expected + "\" are not equal as JSON strings.");
        }
    }

    public static boolean equalAsJson(String a, String b) {
        try {
            JsonNode jsonA = mapper.readValue(a, JsonNode.class);
            JsonNode jsonB = mapper.readValue(b, JsonNode.class);
            return (jsonA == null && jsonB == null) || jsonA.equals(jsonB);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks that a serializable object is serialized to the target JSON string.
     * 
     * @throws IOException
     */
    public static void isSerializedTo(Object o, String targetJson, boolean saveMode) {

        // also check Jackson serialization
        try {
            ObjectWriter writer = null;
            if (saveMode) {
                writer = ParsingUtilities.saveWriter;
            } else {
                writer = ParsingUtilities.defaultWriter;
            }
            String jacksonJson = writer.writeValueAsString(o);
            assertEqualsAsJson(jacksonJson, targetJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("jackson serialization failed");
        }
    }

    /**
     * Checks that a serializable object is serialized to the target JSON string.
     */
    public static void isSerializedTo(Object o, String targetJson) {
        isSerializedTo(o, targetJson, false);
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
                if (line.length() > maxLength) {
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
            while (lineA != null || lineB != null) {
                if (lineA == null) {
                    lineA = "";
                }
                if (lineB == null) {
                    lineB = "";
                }
                String paddedLineA = lineA + new String(new char[maxLength + 2 - lineA.length()]).replace("\0", " ");
                writer.write(paddedLineA);
                writer.write(lineB + "\n");
                lineA = readerA.readLine();
                lineB = readerB.readLine();
            }
            System.out.print(writer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
