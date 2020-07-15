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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.RefineServlet;
import com.google.refine.expr.functions.ToDate;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconType;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;

public class ReconciledDataExtensionJob {

    
    static public class DataExtensionProperty  {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        @JsonView(JsonViews.NonSaveMode.class)
        public final String name;
        @JsonProperty("settings")
        @JsonInclude(Include.NON_NULL)
        public final Map<String, Object> settings;
        
        @JsonCreator
        public DataExtensionProperty(
                @JsonProperty("id")
                String id,
                @JsonProperty("name")
                String name,
                @JsonProperty("settings")
                Map<String, Object> settings) {
            this.id = id;
            this.name = name;
            this.settings = settings;
        }    
    }
    
    static public class DataExtensionConfig  {
        
        @JsonProperty("properties")
        public final List<DataExtensionProperty> properties;
        
        @JsonCreator
        public DataExtensionConfig(
                @JsonProperty("properties")
                List<DataExtensionProperty> properties) {
            this.properties = properties;
        }
        
        public static DataExtensionConfig reconstruct(String json) throws IOException {
            return ParsingUtilities.mapper.readValue(json, DataExtensionConfig.class);
        }     
    }
    
    static public class DataExtensionQuery extends DataExtensionConfig {
        
        @JsonProperty("ids")
        public final List<String> ids;

        @JsonCreator
        public DataExtensionQuery(
                @JsonProperty("ids")
                List<String> ids,
                @JsonProperty("properties")
                List<DataExtensionProperty> properties) {
            super(properties);
            this.ids = ids;
        }   
    }
    
    static public class DataExtension {
        final public Object[][] data;
        
        public DataExtension(Object[][] data) {
            this.data = data;
        }
    }
    
    // Json serialization is used in PreviewExtendDataCommand
    static public class ColumnInfo {
        @JsonProperty("name")
        final public String name;
        @JsonProperty("id")
        final public String id;
        final public ReconType expectedType;
        
        @JsonCreator
        protected ColumnInfo(
        		@JsonProperty("name")
        		String name,
        		@JsonProperty("id")
        		String id,
        		@JsonProperty("type")
        		ReconType expectedType) {
            this.name = name;
            this.id = id;
            this.expectedType = expectedType;
        }
    }
    
    final public DataExtensionConfig extension;
    final public String              endpoint;
    final public List<ColumnInfo>    columns = new ArrayList<ColumnInfo>();
    
    // not final: initialized lazily
    private static CloseableHttpClient httpClient = null;
    
    public ReconciledDataExtensionJob(DataExtensionConfig obj, String endpoint) {
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
        String response = performQuery(this.endpoint, query);

        ObjectNode o = ParsingUtilities.mapper.readValue(response, ObjectNode.class);

        if(columns.size() == 0) {
            // Extract the column metadata
            List<ColumnInfo> newColumns = ParsingUtilities.mapper.convertValue(o.get("meta"), new TypeReference<List<ColumnInfo>>() {});  
            columns.addAll(newColumns);
        }

        Map<String, ReconciledDataExtensionJob.DataExtension> map = new HashMap<String, ReconciledDataExtensionJob.DataExtension>();
        if (o.has("rows") && o.get("rows") instanceof ObjectNode){
            ObjectNode records = (ObjectNode) o.get("rows");

            // for each identifier
            for (String id : ids) {
                if (records.has(id) && records.get(id) instanceof ObjectNode) {
                    ObjectNode record = (ObjectNode) records.get(id);

                    ReconciledDataExtensionJob.DataExtension ext = collectResult(record, reconCandidateMap);

                    if (ext != null) {
                        map.put(id, ext);
                    }
                }
            }
        }
        
        return map;
    }

    /**
     * @todo this should be refactored to be unified with the HTTP querying code
     * from StandardReconConfig. We should ideally extract a library to query
     * reconciliation services and expose it as such for others to reuse.
     */
    
    static protected String performQuery(String endpoint, String query) throws IOException {
        HttpPost request = new HttpPost(endpoint);
        List<NameValuePair> body = Collections.singletonList(
                new BasicNameValuePair("extend", query));
        request.setEntity(new UrlEncodedFormEntity(body, Consts.UTF_8));
        
        try (CloseableHttpResponse response = getHttpClient().execute(request)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 400) {
                throw new IOException("Data extension query failed - code: "
                        + Integer.toString(statusLine.getStatusCode())
                        + " message: " + statusLine.getReasonPhrase());
            } else {
                return ParsingUtilities.inputStreamToString(response.getEntity().getContent());
            }
        }
    }

    private static CloseableHttpClient getHttpClient() {
        if (httpClient != null) {
            return httpClient;
        }
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(30 * 1000)
                .build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setUserAgent(RefineServlet.getUserAgent())
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultRequestConfig(defaultRequestConfig);
        httpClient = httpClientBuilder.build();
        return httpClient;
    }
    
    protected ReconciledDataExtensionJob.DataExtension collectResult(
        ObjectNode record,
        Map<String, ReconCandidate> reconCandidateMap
    ) {
        List<Object[]> rows = new ArrayList<Object[]>();

        // for each property
        int colindex = 0;
        for(ColumnInfo ci : columns) {
            String pid = ci.id;
            ArrayNode values = JSONUtilities.getArray(record, pid);     
            if (values == null) {
                continue;
            }

            // for each value
            for(int rowindex = 0; rowindex < values.size(); rowindex++) {
            	if (!(values.get(rowindex) instanceof ObjectNode)) {
            		continue;
            	}
                ObjectNode val = (ObjectNode) values.get(rowindex);
                // store a reconciled value
                if (val.has("id")) {
                    storeCell(rows, rowindex, colindex, val, reconCandidateMap);
                } else if (val.has("str")) {
                // store a bare string
                    String str = val.get("str").asText();
                    storeCell(rows, rowindex, colindex, str); 
                } else if (val.has("float")) {
                    double v = val.get("float").asDouble();
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("int")) {
                    int v = val.get("int").asInt();
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("date")) {
                    ToDate td = new ToDate();
                    String[] args = new String[1];
                    args[0] = val.get("date").asText();
                    Object v = td.call(null, args);
                    storeCell(rows, rowindex, colindex, v);
                } else if(val.has("bool")) {
                    boolean v = val.get("bool").asBoolean();
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
        ObjectNode obj,
        Map<String, ReconCandidate> reconCandidateMap
    ) {
        String id = obj.get("id").asText();
        ReconCandidate rc;
        if (reconCandidateMap.containsKey(id)) {
            rc = reconCandidateMap.get(id);
        } else {
            rc = new ReconCandidate(
                    obj.get("id").asText(),
                    obj.get("name").asText(),
                    JSONUtilities.getStringArray(obj, "type"),
                    100
                );
            
            reconCandidateMap.put(id, rc);
        }
        
        storeCell(rows, row, col, rc);
    }

    
    static protected void formulateQuery(Set<String> ids, DataExtensionConfig node, Writer writer) throws IOException {
        DataExtensionQuery query = new DataExtensionQuery(ids.stream().filter(e -> e != null).collect(Collectors.toList()), node.properties);
        ParsingUtilities.saveWriter.writeValue(writer, query);
    }
}
