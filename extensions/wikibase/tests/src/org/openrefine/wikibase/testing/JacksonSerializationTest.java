/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.testing;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class JacksonSerializationTest {

    private static ObjectMapper mapper = ParsingUtilities.mapper;

    public static void testSerialize(Object pojo, String expectedJson) {
        // Test that the pojo is correctly serialized
        try {

            String actualJson = ParsingUtilities.defaultWriter.writeValueAsString(pojo);
            TestUtils.assertEqualAsJson(expectedJson, actualJson);
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
}
