/**
 * 
 */
package com.metaweb.gridworks.util;

import java.io.DataOutputStream; 
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.model.ReconCandidate;

public class FreebaseDataExtensionJob {
    static public class DataExtension {
        final public Object[][] data;
        
        protected DataExtension(Object[][] data) {
            this.data = data;
        }
    }
    
    final public JSONObject     extension;
    final public int            columnCount;
    final public List<String>   columnNames = new ArrayList<String>();
    
    public FreebaseDataExtensionJob(JSONObject obj) throws JSONException {
        this.extension = obj;
        this.columnCount = (obj.has("properties") && !obj.isNull("properties")) ?
                countColumns(obj.getJSONArray("properties"), columnNames) : 0;
    }
    
    public Map<String, FreebaseDataExtensionJob.DataExtension> extend(Set<String> guids) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(guids, extension, writer);
        
        InputStream is = doMqlRead(writer.toString());
        try {
            String s = ParsingUtilities.inputStreamToString(is);
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
            
            Map<String, FreebaseDataExtensionJob.DataExtension> map = new HashMap<String, FreebaseDataExtensionJob.DataExtension>();
            JSONArray a = o.getJSONArray("result");
            int l = a.length();
            
            for (int i = 0; i < l; i++) {
                JSONObject o2 = a.getJSONObject(i);
                String guid = o2.getString("guid");
                FreebaseDataExtensionJob.DataExtension ext = collectResult(o2);
                
                if (ext != null) {
                    map.put(guid, ext);
                }
            }
            
            return map;
        } finally {
            is.close();
        }
    }
    
    protected FreebaseDataExtensionJob.DataExtension collectResult(JSONObject obj) throws JSONException {
        List<Object[]> rows = new ArrayList<Object[]>();
        
        collectResult(rows, extension.getJSONArray("properties"), obj, 0, 0);
        
        Object[][] data = new Object[rows.size()][columnCount];
        rows.toArray(data);
        
        return new DataExtension(data);
    }
    
    protected void storeCell(
        List<Object[]>  rows, 
        int row,
        int col,
        Object value
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
        JSONObject obj
    ) throws JSONException {
        storeCell(rows, row, col,
            new ReconCandidate(
                obj.getString("id"),
                obj.getString("guid"),
                obj.getString("name"),
                JSONUtilities.getStringArray(obj, "type"),
                100
            )
        );
    }
    
    protected int[] collectResult(
        List<Object[]>  rows, 
        JSONObject      extNode, 
        JSONObject      resultNode, 
        int             startRowIndex,
        int             startColumnIndex
    ) throws JSONException {
        String propertyID = extNode.getString("id");
        String expectedTypeID = extNode.getString("expected");
        
        JSONArray a = resultNode != null && resultNode.has(propertyID) && !resultNode.isNull(propertyID) ?
            resultNode.getJSONArray(propertyID) : null;
            
        if (expectedTypeID.startsWith("/type/")) {
            if (a != null) {
                int l = a.length();
                for (int r = 0; r < l; r++) {
                    Object o = a.isNull(r) ? null : a.get(r);
                    if (o instanceof Serializable) {
                        storeCell(rows, startRowIndex++, startColumnIndex, o);
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
                    JSONObject o = a.isNull(r) ? null : a.getJSONObject(r);
                    
                    int startColumnIndex2 = startColumnIndex;
                    int startRowIndex2 = startRowIndex;
                    
                    if (isOwnColumn) {
                        storeCell(rows, startRowIndex2++, startColumnIndex2++, o);
                    }
                    
                    if (hasSubProperties) {
                        int[] rowcol = collectResult(
                            rows,
                            extNode.getJSONArray("properties"),
                            o,
                            startRowIndex,
                            startColumnIndex2
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
                    startColumnIndex + countColumns(extNode, null)
                };
            }
        }
    }
    
    protected int[] collectResult(
        List<Object[]>  rows, 
        JSONArray       subProperties, 
        JSONObject      resultNode, 
        int             startRowIndex,
        int             startColumnIndex
    ) throws JSONException {
        int maxStartRowIndex = startRowIndex;
        
        int k = subProperties.length();
        for (int c = 0; c < k; c++) {
            int[] rowcol = collectResult(
                rows, 
                subProperties.getJSONObject(c),
                resultNode,
                startRowIndex,
                startColumnIndex
            );
            
            maxStartRowIndex = Math.max(maxStartRowIndex, rowcol[0]);
            startColumnIndex = rowcol[1];
        }
        
        return new int[] { maxStartRowIndex, startColumnIndex };
    }


    static protected InputStream doMqlRead(String query) throws IOException {
        URL url = new URL("http://api.freebase.com/api/service/mqlread");

        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        
        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
        try {
            String body = "extended=1&query=" + ParsingUtilities.encode(query);
            
            dos.writeBytes(body);
        } finally {
            dos.flush();
            dos.close();
        }
        
        connection.connect();
        
        return connection.getInputStream();
    }
    
    static protected void formulateQuery(Set<String> guids, JSONObject node, Writer writer) throws JSONException {
        JSONWriter jsonWriter = new JSONWriter(writer);
        
        jsonWriter.object();
        jsonWriter.key("query");
            jsonWriter.array();
            jsonWriter.object();
            
                jsonWriter.key("guid"); jsonWriter.value(null);
                jsonWriter.key("guid|=");
                    jsonWriter.array();
                    for (String guid : guids) {
                        jsonWriter.value(guid);
                    }
                    jsonWriter.endArray();
                    
                formulateQueryNode(node.getJSONArray("properties"), jsonWriter);
            
            jsonWriter.endObject();
            jsonWriter.endArray();
        jsonWriter.endObject();
    }
    
    static protected void formulateQueryNode(JSONObject node, JSONWriter writer) throws JSONException {
        String propertyID = node.getString("id");
        String expectedTypeID = node.getString("expected");
        
        writer.key(propertyID);
        writer.array();
        {
            if (!expectedTypeID.startsWith("/type/")) { // not literal
                writer.object();
                writer.key("limit"); writer.value(10);
                writer.key("optional"); writer.value(true);
                {
                    boolean hasSubProperties = (node.has("properties") && !node.isNull("properties")); 
                        
                    if (!hasSubProperties || (node.has("included") && node.getBoolean("included"))) {
                        writer.key("name"); writer.value(null);
                        writer.key("id"); writer.value(null);
                        writer.key("guid"); writer.value(null);
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
    
    static protected int countColumns(JSONObject obj, List<String> columnNames) throws JSONException {
        if (obj.has("properties") && !obj.isNull("properties")) {
            boolean included = (obj.has("included") && obj.getBoolean("included"));
            if (included && columnNames != null) {
                columnNames.add(obj.getString("name"));
            }
            return (included ? 1 : 0) + countColumns(obj.getJSONArray("properties"), columnNames);
        } else {
            if (columnNames != null) {
                columnNames.add(obj.getString("name"));
            }
            return 1;
        }
    }
    
    static protected int countColumns(JSONArray a, List<String> columnNames) throws JSONException {
        int c = 0;
        int l = a.length();
        for (int i = 0; i < l; i++) {
            c += countColumns(a.getJSONObject(i), columnNames);
        }
        return c;
    }
}