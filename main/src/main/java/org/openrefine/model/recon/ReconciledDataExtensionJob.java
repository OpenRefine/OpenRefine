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
package org.openrefine.model.recon;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.expr.functions.ToDate;
import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.JsonViews;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReconciledDataExtensionJob implements Serializable {

	private static final long serialVersionUID = -8114269895487484756L;

	static public class DataExtensionProperty implements Serializable {

		private static final long serialVersionUID = -6309521399471519189L;
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
    
    static public class DataExtensionConfig implements Serializable {

		private static final long serialVersionUID = -8000193899836494952L;
		private final List<DataExtensionProperty> properties;
        private final int batchSize;
        
        @JsonCreator
        public DataExtensionConfig(
                @JsonProperty("properties")
                List<DataExtensionProperty> properties,
                @JsonProperty("batchSize")
                int batchSize) {
            this.properties = properties;
            this.batchSize = batchSize <= 0 ? 10 : batchSize;
        }
        
        @JsonProperty("batchSize")
        public int getBatchSize() {
        	return batchSize;
        }
        
        @JsonProperty("properties")
		public List<DataExtensionProperty> getProperties() {
			return properties;
		}
        
        public static DataExtensionConfig reconstruct(String json) throws IOException {
            return ParsingUtilities.mapper.readValue(json, DataExtensionConfig.class);
        } 
    }
    
    static public class DataExtensionQuery {
        
    	private final List<DataExtensionProperty> properties;
        private final List<String> ids;

        @JsonCreator
        public DataExtensionQuery(
                @JsonProperty("ids")
                List<String> ids,
                @JsonProperty("properties")
                List<DataExtensionProperty> properties) {
        	this.properties = properties;
            this.ids = ids;
        }
        
        @JsonProperty("properties")
		public List<DataExtensionProperty> getProperties() {
			return properties;
		}

        @JsonProperty("ids")
		public List<String> getIds() {
			return ids;
		}
        
    }
    
    /**
     * Data extension corresponding to a single record.
     * Each row in the record can be associated with a data extension.
     * 
     * @author Antonin Delpeuch
     *
     */
    static public class RecordDataExtension implements Serializable {

		private static final long serialVersionUID = 9150870996868122876L;
		final private Map<Long, DataExtension> extensions;
    	
    	public RecordDataExtension(Map<Long, DataExtension> extensions) {
    		this.extensions = extensions;
    	}
    	
    	public Map<Long, DataExtension> getExtensions() {
    		return extensions;
    	}
    	
    	@Override
    	public int hashCode() {
    		return extensions.hashCode();
    	}
    	
    	@Override
    	public boolean equals(Object other) {
    		if (!(other instanceof RecordDataExtension)) {
    			return false;
    		}
    		return ((RecordDataExtension)other).getExtensions().equals(extensions);
    	}
    }
    
    /**
     * Data extension corresponding to a single row.
     * @author Antonin Delpeuch
     *
     */
    static public class DataExtension implements Serializable {

		private static final long serialVersionUID = -6098778224771219654L;
		final public List<List<Cell>> data;
        
        public DataExtension(List<List<Cell>> data) {
            this.data = data;
        }
        
        @Override
        public int hashCode() {
        	return data.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
        	if (!(other instanceof DataExtension)) {
        		return false;
        	}
        	return ((DataExtension)other).data.equals(data);
        }
    }
    
    // Json serialization is used in PreviewExtendDataCommand
    static public class ColumnInfo implements Serializable {

		private static final long serialVersionUID = 8043235351320028446L;
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
    final private String             identifierSpace;
    final private String             schemaSpace;
    
    public ReconciledDataExtensionJob(DataExtensionConfig obj, String endpoint, String identifierSpace, String schemaSpace) {
        this.extension = obj;
        this.endpoint = endpoint;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
    }
    
    public Map<String, ReconciledDataExtensionJob.DataExtension> extend(
        Set<String> ids
    ) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(ids, extension, writer);

        String query = writer.toString();
        InputStream is = performQuery(this.endpoint, query);
        try {
        	ObjectNode o = ParsingUtilities.mapper.readValue(is, ObjectNode.class);
          
            if(columns.size() == 0) {
                // Extract the column metadata
            	List<ColumnInfo> newColumns = ParsingUtilities.mapper.convertValue(o.get("meta"), new TypeReference<List<ColumnInfo>>() {});  
            	columns.addAll(newColumns);
            }
          
            Map<String, ReconciledDataExtensionJob.DataExtension> map = new HashMap<>();
            if (o.has("rows") && o.get("rows") instanceof ObjectNode){
                ObjectNode records = (ObjectNode) o.get("rows");
                
                // for each identifier
                for (String id : ids) {
                    if (records.has(id) && records.get(id) instanceof ObjectNode) {
                        ObjectNode record = (ObjectNode) records.get(id);
                        
                        ReconciledDataExtensionJob.DataExtension ext = collectResult(record);
                        
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
        ObjectNode record
    ) {
        List<List<Cell>> rows = new ArrayList<>();

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
                	String id = val.get("id").asText();
                	String name = val.get("name").asText();
                	ReconCandidate rc = new ReconCandidate(id, name, new String[] {}, 0);
                	Recon recon = new Recon(0L, identifierSpace, schemaSpace)
                			.withMatch(rc)
                			.withJudgmentAction("auto")
                			.withJudgment(Judgment.Matched)
                			.withMatchRank(0)
                			.withService(endpoint);
                    storeCell(rows, rowindex, colindex, new Cell(name, recon));
                } else if (val.has("str")) {
                // store a bare string
                    String str = val.get("str").asText();
                    storeCell(rows, rowindex, colindex, new Cell(str, null)); 
                } else if (val.has("float")) {
                    double v = val.get("float").asDouble();
                    storeCell(rows, rowindex, colindex, new Cell(v, null));
                } else if (val.has("int")) {
                    int v = val.get("int").asInt();
                    storeCell(rows, rowindex, colindex, new Cell(v, null));
                } else if (val.has("date")) {
                    ToDate td = new ToDate();
                    String[] args = new String[1];
                    args[0] = val.get("date").asText();
                    Object v = td.call(null, args);
                    storeCell(rows, rowindex, colindex, new Cell((Serializable)v, null));
                } else if(val.has("bool")) {
                    boolean v = val.get("bool").asBoolean();
                    storeCell(rows, rowindex, colindex, new Cell(v, null));
                }
            }
            colindex++;
        }

        return new DataExtension(rows);
    }

    protected void storeCell(
        List<List<Cell>>  rows, 
        int row,
        int col,
        Cell cell
    ) {
        while (row >= rows.size()) {
            rows.add(new ArrayList<>(Collections.nCopies(columns.size(), null)));
        }
        rows.get(row).set(col, cell);
    }
    
    static protected void formulateQuery(Set<String> ids, DataExtensionConfig node, Writer writer) throws IOException {
        DataExtensionQuery query = new DataExtensionQuery(ids.stream().filter(e -> e != null).collect(Collectors.toList()), node.getProperties());
        ParsingUtilities.saveWriter.writeValue(writer, query);
    }

	public int getBatchSize() {
		return extension.getBatchSize();
	}
}
