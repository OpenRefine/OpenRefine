
package com.google.refine.util;

import static org.testng.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.testng.annotations.Test;

public class JSONUtilitiesTest {

    @Test
    public void testGetIntElement_NullArray() {
        int result = JSONUtilities.getIntElement(null, 0, 99);
        assertEquals(result, 99);
    }

    @Test
    public void testGetIntElement_NegativeIndex() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        array.add(10);
        array.add(20);
        int result = JSONUtilities.getIntElement(array, -1, 99);
        assertEquals(result, 99);
    }

    @Test
    public void testGetIntElement_IndexOutOfBounds() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        array.add(10);
        int result = JSONUtilities.getIntElement(array, 5, 99);
        assertEquals(result, 99);
    }

    @Test
    public void testGetIntElement_ValidIndex() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        array.add(42);
        int result = JSONUtilities.getIntElement(array, 0, 99);
        assertEquals(result, 42);
    }
}
