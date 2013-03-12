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

/**
 * 
 */
package com.google.refine.freebase.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.freebase.Freebase;
import com.google.api.services.freebase.FreebaseRequestInitializer;

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
        JSONObject o = doMqlRead(query);
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


    /**
     * This RPC call works for the Reconcile API, but MQLread is not supported by JSONRPC
     */
    static private JSONObject rpcCall(String query) throws JSONException, UnsupportedEncodingException, IOException {
        URL url = new URL("https://www.googleapis.com/rpc");

        JSONObject params = new JSONObject();
        params.put("query",query);
        params.put("key", FreebaseUtils.API_KEY);
       
        JSONObject req1 = new JSONObject();
        req1.put("jsonrpc","2.0");    
        req1.put("id","q0");
        req1.put("method","freebase.mqlread");
        req1.put("apiVersion", "v1");
        req1.put("params",params);

        JSONArray body = new JSONArray();
        body.put(req1);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json"); //
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(),"utf-8");
        try {
            writer.write(body.toString());
        } finally {
            writer.flush();
            writer.close();
        }
        
        connection.connect();
        JSONArray result = null;
        if (connection.getResponseCode() >= 400) {
           String responseMessage = connection.getResponseMessage();
           String errorStream = ParsingUtilities.inputStreamToString(connection.getErrorStream());
        } else {
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                result = ParsingUtilities.evaluateJsonStringToArray(s);
            } finally {
                is.close();
            }
        }
        return result.getJSONObject(0);
    }

    private static MimeBodyPart queryToMimeBodyPart(String query_name, 
            String query, String service_url, String api_key) 
                    throws MessagingException, IOException {
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setHeader("Content-Transfer-Encoding", "binary");
        mbp.setHeader("Content-ID", "<" + query_name + ">");
        mbp.setHeader("Content-type", "application/http"); 
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("query",query));
        params.add(new BasicNameValuePair("key", api_key));
        UrlEncodedFormEntity param_string = new UrlEncodedFormEntity(params, "UTF-8");
        
        String body = "GET " + service_url + "?" + ParsingUtilities.inputStreamToString(param_string.getContent()) + "\n";
        mbp.setText(body, "UTF-8");
        mbp.getLineCount();
        mbp.getAllHeaderLines();
        return mbp;
    }

    
    /**
     * The beginnings of a homebrew attempt to generate multi-part MIME messages
     * so that we can parse response bodies ourselves and avoid the limitations
     * of the Google Client library.
     */
    static private JSONObject batchCallOld(String query) throws JSONException, MessagingException, IOException {
        URL url = new URL("https://www.googleapis.com/batch");
        String service_url = "https://www.googleapis.com/freebase/v1/mqlread";
        MimeMultipart mp = new MimeMultipart("mixed");

        MimeBodyPart mbp = queryToMimeBodyPart("q0", query, service_url, FreebaseUtils.API_KEY);
        mp.addBodyPart(mbp);
        mp.getPreamble();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", mp.getContentType()); 
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mp.writeTo(baos);
        String foo = baos.toString("UTF-8");
        
        mp.writeTo(connection.getOutputStream());
        
        connection.connect();
        JSONArray result = null;
        if (connection.getResponseCode() >= 400) {
           String responseMessage = connection.getResponseMessage();
           String errorStream = ParsingUtilities.inputStreamToString(connection.getErrorStream());
        } else {
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                result = ParsingUtilities.evaluateJsonStringToArray(s);
            } finally {
                is.close();
            }
        }
        return result.getJSONObject(0);        
    }

    
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    

    private static final FreebaseRequestInitializer REQUEST_INITIALIZER = 
            new FreebaseRequestInitializer(FreebaseUtils.API_KEY);
  
    static private JSONObject batchCall1(String query) throws IOException, JSONException {

        JsonBatchCallback<Freebase> callback = new JsonBatchCallback<Freebase>() {
            public void onSuccess(Freebase res, HttpHeaders responseHeaders) {
                System.out.println(res);
            }
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
              System.out.println("Error Message: " + e.getMessage());
            }
          };

          Freebase client = new Freebase.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
            .setApplicationName("OpenRefine")
            .setFreebaseRequestInitializer(REQUEST_INITIALIZER)
            .build();
          
          // TODO: Batch doesn't work with MqlRead since it extends FreebaseRequest<Void>
//          BatchRequest batch = client.batch();
//          client.mqlread(query).queue(batch, callback); 
//          batch.execute();

          String s = ParsingUtilities.inputStreamToString(client.mqlread(query).executeAsInputStream());
          JSONObject response = ParsingUtilities.evaluateJsonStringToObject(s);

           return response;
    }
    
    static protected JSONObject doMqlRead(String query) 
            throws IOException, JSONException, MessagingException {
//        JSONObject result = rpcCall(query);
//        JSONObject resutl =  batchCallOld(query);
        JSONObject result = batchCall1(query);

        return result;
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