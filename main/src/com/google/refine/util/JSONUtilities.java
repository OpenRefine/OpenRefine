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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.refine.expr.util.JsonValueConverter;


public class JSONUtilities {
	
    static public JSONObject getObject(JSONObject obj, String key) {
        try {
            return obj.getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }
    
    static public ObjectNode getObject(ObjectNode obj, String key) {
    	JsonNode node = obj.get(key);
    	if(node != null && node instanceof ObjectNode) {
    		return (ObjectNode)node;
    	}
    	return null;
    }
    
    static public String getString(JSONObject obj, String key, String def) {
        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public String getString(JsonNode obj, String key, String def) {
    	if (obj.has(key)) {
    		return obj.get(key).textValue();
    	} else {
    		return def;
    	}
    }
    
    static public int getInt(JSONObject obj, String key, int def) {
        try {
            return obj.getInt(key);
        } catch (JSONException e) {
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
    
    static public boolean getBoolean(JSONObject obj, String key, boolean def) {
        try {
            return obj.getBoolean(key);
        } catch (JSONException e) {
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
    
    static public double getDouble(JSONObject obj, String key, double def) {
        try {
            return obj.getDouble(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public long getLong(JSONObject obj, String key, long def) {
        try {
            return obj.getLong(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public OffsetDateTime getDate(JSONObject obj, String key, OffsetDateTime def) {
        try {
            OffsetDateTime d = ParsingUtilities.stringToDate(obj.getString(key));
            
            return d != null ? d : def;
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public LocalDateTime getLocalDate(JSONObject obj, String key, LocalDateTime def) {
        try {
            LocalDateTime d = ParsingUtilities.stringToLocalDate(obj.getString(key));
            
            return d != null ? d : def;
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public JSONArray getArray(JSONObject obj, String key) {
        try {
            return obj.getJSONArray(key);
        } catch (JSONException e) {
            return null;
        }
    }
    
    static public ArrayNode getArray(ObjectNode obj, String key) {
    	JsonNode v = obj.get(key);
        if( obj.has(key) && obj.get(key) instanceof ArrayNode) {
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
    
    static public JSONArray arrayToJSONArray(String[] array) {
        return new JSONArray(Arrays.asList(array));
    }
    
    static public int[] getIntArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int[] r = new int[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getInt(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new int[0];
        }
    }
    
    static public int[] getIntArray(ObjectNode obj, String key) {
    	ArrayNode a = getArray(obj, key);
    	if (a == null) {
    		return new int[0];
    	}
    	int[] r = new int[a.size()];
    	int i = 0;
    	for(JsonNode n : a) {
    		r[i] = n.asInt();
    		i++;
    	}
        return r;
    }
    
    static public String[] getStringArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            String[] r = new String[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getString(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new String[0];
        }
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
    
    static public void getStringList(JSONObject obj, String key, List<String> list) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int count = a.length();
            
            for (int i = 0; i < count; i++) {
                list.add(a.getString(i));
            }
        } catch (JSONException e) {
        }
    }
    
    static public void putField(JSONObject obj, String key, Object value) throws JSONException {
        if (value instanceof Integer) {
            obj.put(key, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            obj.put(key, ((Long) value).intValue());
        } else if (value instanceof Number) {
            obj.put(key, ((Double) value).doubleValue());
        } else if (value instanceof Boolean) {
            obj.put(key, value);
        } else if (value instanceof Calendar) {
            obj.put(key, ParsingUtilities.dateToString(OffsetDateTime.ofInstant(((Calendar)value).toInstant(), ZoneId.of("Z"))));
        } else if (value instanceof String) {
            obj.put(key, value);
        } else {
            obj.put(key, value.toString());
        }
    }
    
    static public JSONObject getObjectElement(JSONArray a, int i) {
        try {
            return a.getJSONObject(i);
        } catch (JSONException e) {
            return null;
        }
    }
    
    static public ObjectNode getObjectElement(ArrayNode a, int i) {
    	JsonNode n = a.get(i);
    	if (n != null && n instanceof ObjectNode) {
    		return (ObjectNode) n;
    	}
    	return null;
    }
    
    static public int getIntElement(JSONArray a, int i, int def) {
        try {
            return a.getInt(i);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public int getIntElement(ArrayNode a, int i, int def) {
        if (a.get(i) != null) {
            return a.get(i).asInt(def);
        }
        return def;
    }
    
    static public void append(JSONArray sheetRecords, JSONObject sheetRecord) {
        try {
            sheetRecords.put(sheetRecords.length(), sheetRecord);
        } catch (JSONException e) {
        }
    }
    
    static public void append(ArrayNode sheetRecords, ObjectNode sheetRecord) {
        sheetRecords.add(sheetRecord);
    }
    
    static public void append(ArrayNode array, int v) {
        array.add(v);
    }
    
    static public void append(JSONArray a, Object element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(JSONArray a, int element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(JSONArray a, long element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(JSONArray a, double element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(JSONArray a, boolean element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(JSONArray a, String element) {
        try {
            a.put(a.length(), element);
        } catch (JSONException e) {
        }
    }
    
    static public void append(ArrayNode a, String element) {
        a.add(element);
    }
    
    static public void safePut(ObjectNode options, String key, JsonNode rootElement) {
        try {
            options.put(key, rootElement);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public void safeInc(JSONObject obj, String key) {
        try {
            int currentValue = obj.getInt(key);
            safePut(obj, key, currentValue + 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public void safeInc(ObjectNode obj, String key) {
        int currentValue = getInt(obj, key, 0);
        safePut(obj, key, currentValue + 1);
    }
    
    static public void safePut(ObjectNode obj, String key, long value) {
    	obj.put(key, value);
    }
    
    static public void safePut(JSONObject obj, String key, long value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public void safePut(ObjectNode obj, String key, double value) {
    	obj.put(key, value);
    }
    
    static public void safePut(JSONObject obj, String key, double value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public void safePut(ObjectNode obj, String key, boolean value) {
    	obj.put(key, value);
    }
    
    static public void safePut(JSONObject obj, String key, boolean value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public void safePut(ObjectNode obj, String key, String value) {
    	obj.put(key, value);
    }
    
    static public void safePut(JSONObject obj, String key, String value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public void safePut(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            // Ignore: the JSONObject is just too happy about throwing exceptions.
        }
    }
    
    static public Object[] toArray(JSONArray a) throws JSONException {
        int l = a.length();
        
        Object[] a2 = new Object[l];
        for (int i = 0; i < l; i++) {
            a2[i] = a.get(i);
        }
        
        return a2;
    }
    
    static public List<String> toStringList(JSONArray a) throws JSONException {
        int l = a.length();
        
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < l; i++) {
            list.add(a.getString(i));
        }
        
        return list;
    }
    
    static public void concatArray(JSONArray destArray, JSONArray srcArray)
            throws JSONException {
        for (int i = 0; i < srcArray.length(); i++) {
            destArray.put(srcArray.get(i));
        }
    }
    
    // temporary method used during migratino
    static public ObjectNode jsonObjectToObjectNode(JSONObject obj) {
    	return ParsingUtilities.evaluateJsonStringToObjectNode(obj.toString());
    }

	public static JSONObject objectNodeToJsonNode(ObjectNode fieldJsonObj) {
		return new JSONObject(fieldJsonObj.toString());
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
}
