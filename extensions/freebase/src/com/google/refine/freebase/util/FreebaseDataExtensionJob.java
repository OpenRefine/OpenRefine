/*

Copyright 2010,2013 Google Inc. and contributors
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

package com.google.refine.freebase.util;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.FreebaseType;
import com.google.refine.model.ReconCandidate;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class FreebaseDataExtensionJob {
    static public class DataExtension {
        final public Object[][] data;
        
        public DataExtension(Object[][] data) {
            this.data = data;
        }
    }
    
    static public class ColumnInfo {
        final public List<String> names;
        final public List<String> path;
        final public FreebaseType expectedType;
        
        protected ColumnInfo(List<String> names, List<String> path, FreebaseType expectedType) {
            this.names = names;
            this.path = path;
            this.expectedType = expectedType;
        }
    }
    
    final public JSONObject         extension;
    final public int                columnCount;
    final public List<ColumnInfo>   columns = new ArrayList<ColumnInfo>();
    
    public FreebaseDataExtensionJob(JSONObject obj) throws JSONException {
        this.extension = obj;
        this.columnCount = (obj.has("properties") && !obj.isNull("properties")) ?
                countColumns(obj.getJSONArray("properties"), columns, new ArrayList<String>(), new ArrayList<String>()) : 0;
    }
    
    public Map<String, FreebaseDataExtensionJob.DataExtension> extend(
        Set<String> ids,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(ids, extension, writer);
        String query = writer.toString();
        
        String result = FreebaseUtils.mqlread(query);
        
        JSONObject o = ParsingUtilities.evaluateJsonStringToObject(result);
        Map<String, FreebaseDataExtensionJob.DataExtension> map = new HashMap<String, FreebaseDataExtensionJob.DataExtension>();
        if (o.has("result")) {
            JSONArray a = o.getJSONArray("result");
            int l = a.length();

            for (int i = 0; i < l; i++) {
                JSONObject o2 = a.getJSONObject(i);
                String id = o2.getString("id");
                FreebaseDataExtensionJob.DataExtension ext = collectResult(o2, reconCandidateMap);

                if (ext != null) {
                    map.put(id, ext);
                }
            }
        }

        return map;
    } 

    
    protected FreebaseDataExtensionJob.DataExtension collectResult(
        JSONObject obj,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws JSONException {
        List<Object[]> rows = new ArrayList<Object[]>();
        
        collectResult(rows, extension.getJSONArray("properties"), obj, 0, 0, reconCandidateMap);
        
        Object[][] data = new Object[rows.size()][columnCount];
        rows.toArray(data);
        
        return new DataExtension(data);
    }

    protected void storeKey(
            List<Object[]>  rows, 
            int row,
            int col,
            JSONObject key,
            Map<String, ReconCandidate> reconCandidateMap
        ) throws JSONException {
        String keyval = key.getString("value");
        while (row >= rows.size()) {
            rows.add(new Object[columnCount]);
        }
        rows.get(row)[col] = keyval;
    }
    
    protected void storeCell(
        List<Object[]>  rows, 
        int row,
        int col,
        Object value,
        Map<String, ReconCandidate> reconCandidateMap
    ) {
        while (row >= rows.size()) {
            rows.add(new Object[columnCount]);
        }
        rows.get(row)[col] = value;
    }
    
    protected void storeCell(
        List<Object[]>  rows, 
        int row,
        int col,
        JSONObject obj,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws JSONException {
        String id = obj.getString("id");
        ReconCandidate rc;
        if (reconCandidateMap.containsKey(id)) {
            rc = reconCandidateMap.get(id);
        } else {
            rc = new ReconCandidate(
                    obj.getString("id"),
                    obj.getString("name"),
                    JSONUtilities.getStringArray(obj, "type"),
                    100
                );
            
            reconCandidateMap.put(id, rc);
        }
        
        storeCell(rows, row, col, rc, reconCandidateMap);
    }
    
    protected int[] collectResult(
        List<Object[]>  rows, 
        JSONObject      extNode, 
        JSONObject      resultNode, 
        int             startRowIndex,
        int             startColumnIndex,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws JSONException {
        String propertyID = extNode.getString("id");
        String expectedTypeID = extNode.getJSONObject("expected").getString("id");
        
        JSONArray a = resultNode != null && resultNode.has(propertyID) && !resultNode.isNull(propertyID) ?
            resultNode.getJSONArray(propertyID) : null;

        if ("/type/key".equals(expectedTypeID)) {
            if (a != null) {
                int l = a.length();
                for (int r = 0; r < l; r++) {
                    Object o = a.isNull(r) ? null : a.get(r);
                    if (o instanceof JSONObject) {
                        storeKey(rows, startRowIndex++, startColumnIndex, (JSONObject) o, reconCandidateMap);
                    }
                }
            }
            
            // note that we still take up a column even if we don't have any data
            return new int[] { startRowIndex, startColumnIndex + 1 };
        } else if (expectedTypeID.startsWith("/type/")) {
            if (a != null) {
                int l = a.length();
                for (int r = 0; r < l; r++) {
                    Object o = a.isNull(r) ? null : a.get(r);
                    if (o instanceof Serializable) {
                        storeCell(rows, startRowIndex++, startColumnIndex, o, reconCandidateMap);
                    }
                }
            }
            
            // note that we still take up a column even if we don't have any data
            return new int[] { startRowIndex, startColumnIndex + 1 };
        } else {
            boolean hasSubProperties = (extNode.has("properties") && !extNode.isNull("properties")); 
            boolean isOwnColumn = !hasSubProperties || (extNode.has("included") && extNode.getBoolean("included"));
            
            if (a != null && a.length() > 0) {
                int maxColIndex = startColumnIndex;
                
                int l = a.length();
                for (int r = 0; r < l; r++) {
                    Object v = a.isNull(r) ? null : a.get(r);
                    JSONObject o = v != null && v instanceof JSONObject ? (JSONObject) v : null;
                    
                    int startColumnIndex2 = startColumnIndex;
                    int startRowIndex2 = startRowIndex;
                    
                    if (isOwnColumn) {
                        if (o != null) {
                            storeCell(rows, startRowIndex2++, startColumnIndex2++, o, reconCandidateMap);
                        } else {
                            storeCell(rows, startRowIndex2++, startColumnIndex2++, v, reconCandidateMap);
                        }
                    }
                    
                    if (hasSubProperties && o != null) {
                        int[] rowcol = collectResult(
                            rows,
                            extNode.getJSONArray("properties"),
                            o,
                            startRowIndex,
                            startColumnIndex2,
                            reconCandidateMap
                        );
                        
                        startRowIndex2 = rowcol[0];
                        startColumnIndex2 = rowcol[1];
                    }
                    
                    startRowIndex = startRowIndex2;
                    maxColIndex = Math.max(maxColIndex, startColumnIndex2);
                }
                
                return new int[] { startRowIndex, maxColIndex };
            } else {
                return new int[] {
                    startRowIndex,
                    startColumnIndex + countColumns(extNode, null, new ArrayList<String>(), new ArrayList<String>())
                };
            }
        }
    }
    
    protected int[] collectResult(
        List<Object[]>  rows, 
        JSONArray       subProperties, 
        JSONObject      resultNode, 
        int             startRowIndex,
        int             startColumnIndex,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws JSONException {
        int maxStartRowIndex = startRowIndex;
        
        int k = subProperties.length();
        for (int c = 0; c < k; c++) {
            int[] rowcol = collectResult(
                rows, 
                subProperties.getJSONObject(c),
                resultNode,
                startRowIndex,
                startColumnIndex,
                reconCandidateMap
            );
            
            maxStartRowIndex = Math.max(maxStartRowIndex, rowcol[0]);
            startColumnIndex = rowcol[1];
        }
        
        return new int[] { maxStartRowIndex, startColumnIndex };
    }


  
    static protected void formulateQuery(Set<String> ids, JSONObject node, Writer writer) throws JSONException {
        JSONWriter jsonWriter = new JSONWriter(writer);
        
            jsonWriter.array();
            jsonWriter.object();
            
                jsonWriter.key("id"); jsonWriter.value(null);
                jsonWriter.key("id|=");
                    jsonWriter.array();
                    for (String id : ids) {
                        if (id != null) {
                            jsonWriter.value(id);
                        }
                    }
                    jsonWriter.endArray();
                    
                formulateQueryNode(node.getJSONArray("properties"), jsonWriter);
            
            jsonWriter.endObject();
            jsonWriter.endArray();
    }
    
    static protected void formulateQueryNode(JSONObject node, JSONWriter writer) throws JSONException {
        String propertyID = node.getString("id");
        String expectedTypeID = node.getJSONObject("expected").getString("id");
        
        writer.key(propertyID);
        writer.array();
        {
            if (!expectedTypeID.startsWith("/type/")  // not literal
                    || "/type/key".equals(expectedTypeID)) {
                writer.object();
                writer.key("optional"); writer.value(true);
                
                boolean hasLimit = false;
                if (node.has("constraints") && !node.isNull("constraints")) {
                    JSONObject constraints = node.getJSONObject("constraints");
                    
                    String[] names = JSONObject.getNames(constraints);
                    for (String name : names) {
                        Object value = constraints.get(name);
                        if (name.equals("limit")) {
                            hasLimit = true;
                        }
                        
                        if (!name.contains(":") &&
                            !name.equals("limit") &&
                            !name.equals("optional") &&
                            !name.equals("count") &&
                            !name.equals("estimate-count") &&
                            !name.equals("sort") &&
                            !name.equals("return")) {
                            
                            if (name.startsWith("!")) {
                                name = "!c:" + name.substring(1);
                            } else {
                                name = "c:" + name;
                            }
                        }
                        if (name.equals("sort")) {
                            String sortKey = (String) value;
                            if (sortKey.startsWith("-")) {
                                sortKey = sortKey.substring(1);
                            }
                            writer.key(sortKey); writer.value(null);
                            writer.key(name); writer.value(value);
                        } else {
                            writer.key(name); writer.value(value);
                        }
                    }
                }
                if (!hasLimit) {
                    writer.key("limit"); writer.value(10);
                }
                
                {
                    boolean hasSubProperties = (node.has("properties") && !node.isNull("properties")); 
                        
                    if (!hasSubProperties || (node.has("included") && node.getBoolean("included"))) {
                        if ("/type/key".equals(expectedTypeID)) {
                            writer.key("value"); writer.value(null);
                        } else {
                            writer.key("name"); writer.value(null);
                            writer.key("id"); writer.value(null);
                        }
                        writer.key("type"); writer.array(); writer.endArray();
                    }
                    
                    if (hasSubProperties) {
                        formulateQueryNode(node.getJSONArray("properties"), writer);
                    }
                }
                writer.endObject();
            }
        }
        writer.endArray();
    }
    
    static protected void formulateQueryNode(JSONArray propertiesA, JSONWriter writer) throws JSONException {
        int l = propertiesA.length();
        
        for (int i = 0; i < l; i++) {
            formulateQueryNode(propertiesA.getJSONObject(i), writer);
        }
    }
    
    static protected int countColumns(JSONObject obj, List<ColumnInfo> columns, List<String> names, List<String> path) throws JSONException {
        String name = obj.getString("name");
        
        List<String> names2 = null;
        List<String> path2 = null;
        if (columns != null) {
            names2 = new ArrayList<String>(names);
            names2.add(name);
            
            path2 = new ArrayList<String>(path);
            path2.add(obj.getString("id"));
        }
        
        if (obj.has("properties") && !obj.isNull("properties")) {
            boolean included = (obj.has("included") && obj.getBoolean("included"));
            if (included && columns != null) {
                JSONObject expected = obj.getJSONObject("expected");
                
                columns.add(new ColumnInfo(names2, path2, 
                    new FreebaseType(expected.getString("id"), expected.getString("name"))));
            }
            
            return (included ? 1 : 0) + 
                countColumns(obj.getJSONArray("properties"), columns, names2, path2);
        } else {
            if (columns != null) {
                JSONObject expected = obj.getJSONObject("expected");
                
                columns.add(new ColumnInfo(names2, path2,
                    new FreebaseType(expected.getString("id"), expected.getString("name"))));
            }
            return 1;
        }
    }
    
    static protected int countColumns(JSONArray a, List<ColumnInfo> columns, List<String> names, List<String> path) throws JSONException {
        int c = 0;
        int l = a.length();
        for (int i = 0; i < l; i++) {
            c += countColumns(a.getJSONObject(i), columns, names, path);
        }
        return c;
    }
}