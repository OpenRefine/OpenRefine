/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.util;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.refine.expr.util.JsonValueConverter;

public class JSONUtilities {

    static public ObjectNode getObject(ObjectNode obj, String key) {
        JsonNode node = obj.get(key);
        if (node != null && node instanceof ObjectNode) {
            return (ObjectNode) node;
        }
        return null;
    }

    static public String getString(JsonNode obj, String key, String def) {
        if (obj.has(key)) {
            return obj.get(key).textValue();
        } else {
            return def;
        }
    }

    static public int getInt(JsonNode obj, String key, int def) {
        if (obj.has(key)) {
            return obj.get(key).asInt(def);
        } else {
            return def;
        }
    }

    static public boolean getBoolean(JsonNode obj, String key, boolean def) {
        if (obj.has(key)) {
            return obj.get(key).asBoolean(def);
        } else {
            return def;
        }
    }

    static public ArrayNode getArray(ObjectNode obj, String key) {
        if (obj.has(key) && obj.get(key) instanceof ArrayNode) {
            return (ArrayNode) obj.get(key);
        }
        return null;
    }

    static public List<JsonNode> getArray(JsonNode obj, String key) {
        if (obj.has(key) && obj.get(key).getNodeType().equals(JsonNodeType.ARRAY)) {
            return Lists.newArrayList(obj.get(key).elements());
        } else {
            return null;
        }
    }

    static public int[] getIntArray(ObjectNode obj, String key) {
        ArrayNode a = getArray(obj, key);
        if (a == null) {
            return new int[0];
        }
        int[] r = new int[a.size()];
        int i = 0;
        for (JsonNode n : a) {
            r[i] = n.asInt();
            i++;
        }
        return r;
    }

    static public String[] getStringArray(ObjectNode obj, String key) {
        ArrayNode a = getArray(obj, key);
        if (a == null) {
            return new String[0];
        }
        String[] r = new String[a.size()];

        for (int i = 0; i < r.length; i++) {
            r[i] = a.get(i).asText();
        }

        return r;
    }

    static public ObjectNode getObjectElement(ArrayNode a, int i) {
        JsonNode n = a.get(i);
        if (n != null && n instanceof ObjectNode) {
            return (ObjectNode) n;
        }
        return null;
    }

    static public int getIntElement(ArrayNode a, int i, int def) {
        if (a.get(i) != null) {
            return a.get(i).asInt(def);
        }
        return def;
    }

    static public void append(ArrayNode sheetRecords, ObjectNode sheetRecord) {
        sheetRecords.add(sheetRecord);
    }

    static public void append(ArrayNode array, int v) {
        array.add(v);
    }

    static public void append(ArrayNode a, String element) {
        a.add(element);
    }

    static public void safePut(ObjectNode options, String key, JsonNode rootElement) {
        options.set(key, rootElement);
    }

    static public void safeInc(ObjectNode obj, String key) {
        int currentValue = getInt(obj, key, 0);
        safePut(obj, key, currentValue + 1);
    }

    static public void safePut(ObjectNode obj, String key, long value) {
        obj.put(key, value);
    }

    static public void safePut(ObjectNode obj, String key, double value) {
        obj.put(key, value);
    }

    static public void safePut(ObjectNode obj, String key, boolean value) {
        obj.put(key, value);
    }

    static public void safePut(ObjectNode obj, String key, String value) {
        obj.put(key, value);
    }

    public static Object[] toArray(ArrayNode v) {
        if (v == null) {
            return null;
        }
        Object[] result = new Object[v.size()];
        for (int i = 0; i != v.size(); i++) {
            result[i] = JsonValueConverter.convert(v.get(i));
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public static Comparable[] toSortableArray(ArrayNode v) {
        if (v == null) {
            return null;
        }
        Comparable[] result = new Comparable[v.size()];
        for (int i = 0; i != v.size(); i++) {
            result[i] = JsonValueConverter.convertComparable(v.get(i));
        }
        return result;
    }

}
