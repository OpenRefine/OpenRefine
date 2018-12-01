package com.google.refine.tests.expr.util;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.util.ParsingUtilities;

public class JsonValueConverterTests {
	
	private void fieldEquals(String json, Object expectedValue) {
		try {
			ObjectNode n = (ObjectNode) ParsingUtilities.mapper.readTree(json);
			assertEquals(expectedValue, JsonValueConverter.convert(n.get("foo")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testConvertJsonObject() throws IOException {
		fieldEquals("{\"foo\":{\"ob\":\"ject\"}}", ParsingUtilities.mapper.readTree("{\"ob\":\"ject\"}"));
	}
	
	@Test
	public void testConvertJsonArray() throws IOException {
		fieldEquals("{\"foo\":[1,2]}", ParsingUtilities.mapper.readTree("[1,2]"));
	}
	
	@Test
	public void testConvertInt() {
		fieldEquals("{\"foo\":3}", 3);
	}
	
	@Test
	public void testConvertFloat() {
		fieldEquals("{\"foo\":3.14}", 3.14);
	}
	
	@Test
	public void testConvertBool() {
		fieldEquals("{\"foo\":true}", true);
	}

	@Test
	public void testConvertNull() {
		fieldEquals("{\"foo\":null}", null);
	}
	
	@Test
	public void testConvertString() {
		fieldEquals("{\"foo\":\"bar\"}", "bar");
	}
	
	@Test
	public void testConvertNoField() {
		fieldEquals("{}", null);
	}
}
