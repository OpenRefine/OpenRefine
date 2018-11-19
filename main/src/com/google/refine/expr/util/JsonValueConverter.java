package com.google.refine.expr.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts the a JSON value
 * @author antonin
 *
 */
public class JsonValueConverter {

	public static Object convert(JsonNode value) {
		if (value == null) {
			return null;
		}
		if (value.isObject()) {
			return value;
		} else if (value.isArray()) {
    		return value;
    	} else if (value.isBigDecimal() || value.isDouble() || value.isFloat()) {
    		return value.asDouble();
    	} else if (value.isBigInteger()) {
    		return value.asLong();
    	} else if (value.isInt()) {
    		return value.asInt();
    	} else if (value.isBinary() || value.isTextual()) {
    		return value.asText();
    	} else if (value.isBoolean()) {
    		return value.asBoolean();
    	} else if (value.isNull()) {
    		return null;
    	} else {
    		return null;
    	}
	}

}
