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

package com.google.refine.expr.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts the a JSON value
 * 
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

    @SuppressWarnings("rawtypes")
    public static Comparable convertComparable(JsonNode value) {
        if (value == null) {
            return null;
        }
        if (value.isContainerNode()) {
            // TODO: return null instead (like fallthrough case)
            throw new IllegalArgumentException("Arrays and objects aren't comparable");
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
