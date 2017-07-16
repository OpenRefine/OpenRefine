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

/**
 * 
 */
package com.google.refine.model.recon;

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

import com.google.refine.model.ReconType;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.expr.functions.ToDate;

public class ReconciledDataExtensionJob {
    static public class DataExtension {
        final public Object[][] data;
        
        public DataExtension(Object[][] data) {
            this.data = data;
        }
    }
    
    static public class ColumnInfo {
        final public String name;
        final public String id;
        final public ReconType expectedType;
        
        protected ColumnInfo(String name, String id, ReconType expectedType) {
            this.name = name;
            this.id = id;
            this.expectedType = expectedType;
        }
    }
    
    final public JSONObject         extension;
    final public String             endpoint;
    final public List<ColumnInfo>   columns = new ArrayList<ColumnInfo>();
    
    public ReconciledDataExtensionJob(JSONObject obj, String endpoint) throws JSONException {
        this.extension = obj;
        this.endpoint = endpoint;
    }
    
    public Map<String, ReconciledDataExtensionJob.DataExtension> extend(
        Set<String> ids,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(ids, extension, writer);

        String query = writer.toString();
        InputStream is = performQuery(this.endpoint, query);
        try {
            String s = ParsingUtilities.inputStreamToString(is);
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
          
            if(columns.size() == 0) {
                // Extract the column metadata
                gatherColumnInfo(o.getJSONArray("meta"), columns);    
            }
          
            Map<String, ReconciledDataExtensionJob.DataExtension> map = new HashMap<String, ReconciledDataExtensionJob.DataExtension>();
            if (o.has("rows")){
                JSONObject records = o.getJSONObject("rows");
                
                // for each identifier
                for (String id : ids) {
                    if (records.has(id)) {
                        JSONObject record = records.getJSONObject(id);
                        
                        ReconciledDataExtensionJob.DataExtension ext = collectResult(record, reconCandidateMap);
                        
                        if (ext != null) {
                            map.put(id, ext);
                        }
                    }
                }
            }
            
            return map;
        } finally {
            is.close();
        }
    }

    static protected InputStream performQuery(String endpoint, String query) throws IOException {
        URL url = new URL(endpoint);

        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);

        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
        try {
            String body = "extend=" + ParsingUtilities.encode(query);

            dos.writeBytes(body);
        } finally {
            dos.flush();
            dos.close();
        }

        connection.connect();

        return connection.getInputStream();
    }

    
    protected ReconciledDataExtensionJob.DataExtension collectResult(
        JSONObject record,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws JSONException {
        List<Object[]> rows = new ArrayList<Object[]>();

        // for each property
        int colindex = 0;
        for(ColumnInfo ci : columns) {
            String pid = ci.id;
            JSONArray values = record.getJSONArray(pid);        
            if (values == null) {
                continue;
            }

            // for each value
            for(int rowindex = 0; rowindex < values.length(); rowindex++) {
                JSONObject val = values.getJSONObject(rowindex);
                // store a reconciled value
                if (val.has("id")) {
                    storeCell(rows, rowindex, colindex, val, reconCandidateMap);
                } else if (val.has("str")) {
                // store a bare string
                    String str = val.getString("str");
                    storeCell(rows, rowindex, colindex, str); 
                } else if (val.has("float")) {
                    float v = Float.parseFloat(val.getString("float"));
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("int")) {
                    int v = Integer.parseInt(val.getString("int"));
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("date")) {
                    ToDate td = new ToDate();
                    String[] args = new String[1];
                    args[0] = val.getString("date");
                    Object v = td.call(null, args);
                    storeCell(rows, rowindex, colindex, v);
                } else if(val.has("bool")) {
                    boolean v = val.getString("bool") == "true";
                    storeCell(rows, rowindex, colindex, v);
                }
            }
            colindex++;
        }

       
        
        Object[][] data = new Object[rows.size()][columns.size()];
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
            rows.add(new Object[columns.size()]);
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
        
        storeCell(rows, row, col, rc);
    }

    
    static protected void formulateQuery(Set<String> ids, JSONObject node, Writer writer) throws JSONException {
        JSONWriter jsonWriter = new JSONWriter(writer);
        
        jsonWriter.object();

        jsonWriter.key("ids");
            jsonWriter.array();
            for (String id : ids) {
                if (id != null) {
                    jsonWriter.value(id);
                }
            }
            jsonWriter.endArray();

        jsonWriter.key("properties");
            jsonWriter.array();
            JSONArray properties = node.getJSONArray("properties");
            int l = properties.length();
        
            for (int i = 0; i < l; i++) {
                JSONObject property = properties.getJSONObject(i);
                jsonWriter.object();
                jsonWriter.key("id");
                jsonWriter.value(property.getString("id"));
                if (property.has("settings")) {
                    JSONObject settings = property.getJSONObject("settings");
                    jsonWriter.key("settings");
                    jsonWriter.value(settings);
                }
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
        jsonWriter.endObject();
    }
    
    static protected void gatherColumnInfo(JSONArray meta, List<ColumnInfo> columns) throws JSONException {
        for(int i = 0; i < meta.length(); i++) {
            JSONObject col = meta.getJSONObject(i);

            ReconType expectedType = null;
            if(col.has("type")) {
                JSONObject expectedObj = col.getJSONObject("type");
                expectedType = new ReconType(expectedObj.getString("id"), expectedObj.getString("name"));
            }
        
            columns.add(new ColumnInfo(
                col.getString("name"),
                col.getString("id"),
                expectedType));        
        }
   }
}
