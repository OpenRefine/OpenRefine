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
import com.metaweb.gridworks.protograph.FreebaseType;

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
    
    final public JSONObject     	extension;
    final public int            	columnCount;
    final public List<ColumnInfo>   columns = new ArrayList<ColumnInfo>();
    
    public FreebaseDataExtensionJob(JSONObject obj) throws JSONException {
        this.extension = obj;
        this.columnCount = (obj.has("properties") && !obj.isNull("properties")) ?
                countColumns(obj.getJSONArray("properties"), columns, new ArrayList<String>(), new ArrayList<String>()) : 0;
    }
    
    public Map<String, FreebaseDataExtensionJob.DataExtension> extend(
		Set<String> guids,
        Map<String, ReconCandidate> reconCandidateMap
	) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(guids, extension, writer);
        
        String query = writer.toString();
        InputStream is = doMqlRead(query);
        try {
            String s = ParsingUtilities.inputStreamToString(is);
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
            
            Map<String, FreebaseDataExtensionJob.DataExtension> map = new HashMap<String, FreebaseDataExtensionJob.DataExtension>();
            if (o.has("result")) {
                JSONArray a = o.getJSONArray("result");
                int l = a.length();
                
                for (int i = 0; i < l; i++) {
                    JSONObject o2 = a.getJSONObject(i);
                    String guid = o2.getString("guid");
                    FreebaseDataExtensionJob.DataExtension ext = collectResult(o2, reconCandidateMap);
                    
                    if (ext != null) {
                        map.put(guid, ext);
                    }
                }
            }
            
            return map;
        } finally {
            is.close();
        }
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
    	String guid = obj.getString("guid");
    	ReconCandidate rc;
    	if (reconCandidateMap.containsKey(guid)) {
    		rc = reconCandidateMap.get(guid);
    	} else {
    		rc = new ReconCandidate(
                    obj.getString("id"),
                    obj.getString("guid"),
                    obj.getString("name"),
                    JSONUtilities.getStringArray(obj, "type"),
                    100
                );
    		
    		reconCandidateMap.put(guid, rc);
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
            
        if (expectedTypeID.startsWith("/type/")) {
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
                        if (guid != null) {
                            jsonWriter.value(guid);
                        }
                    }
                    jsonWriter.endArray();
                    
                formulateQueryNode(node.getJSONArray("properties"), jsonWriter);
            
            jsonWriter.endObject();
            jsonWriter.endArray();
        jsonWriter.endObject();
    }
    
    static protected void formulateQueryNode(JSONObject node, JSONWriter writer) throws JSONException {
        String propertyID = node.getString("id");
        String expectedTypeID = node.getJSONObject("expected").getString("id");
        
        writer.key(propertyID);
        writer.array();
        {
            if (!expectedTypeID.startsWith("/type/")) { // not literal
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
                        writer.key(name);
                        writer.value(value);
                    }
                }
                if (!hasLimit) {
                    writer.key("limit"); writer.value(10);
                }
                
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