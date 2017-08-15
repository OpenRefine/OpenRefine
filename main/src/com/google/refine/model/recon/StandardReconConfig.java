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

package com.google.refine.model.recon;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.RecordModel.RowDependency;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class StandardReconConfig extends ReconConfig {
    final static Logger logger = LoggerFactory.getLogger("refine-standard-recon");
    
    static public class ColumnDetail {
        final public String columnName;
        final public String propertyName;
        final public String propertyID;
        
        public ColumnDetail(String columnName, String propertyName, String propertyID) {
            this.columnName = columnName;
            this.propertyName = propertyName;
            this.propertyID = propertyID;
        }
    }
    
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        List<ColumnDetail> columnDetails = null;
        if (obj.has("columnDetails")) {
            JSONArray columnDetailsA = obj.getJSONArray("columnDetails");
            int l = columnDetailsA.length();
            
            columnDetails = new ArrayList<ColumnDetail>(l);
            for (int i = 0; i < l; i++) {
                JSONObject o = columnDetailsA.getJSONObject(i);
                
                if (o.has("property")) { // legacy
                    JSONObject p = o.getJSONObject("property");
                    columnDetails.add(new ColumnDetail(
                        o.getString("column"),
                        p.has("name") ? p.getString("name") : null,
                        p.has("id") ? p.getString("id") : null
                    ));
                } else {
                    columnDetails.add(new ColumnDetail(
                        o.getString("column"),
                        o.has("propertyName") ? o.getString("propertyName") : null,
                        o.has("propertyID") ? o.getString("propertyID") : null
                    ));
                }
            }
        } else {
            columnDetails = new ArrayList<ColumnDetail>();
        }
        
        JSONObject t = obj.has("type") && !obj.isNull("type") ? obj.getJSONObject("type") : null;
        
        String limitString = obj.has("limit") && !obj.isNull("limit") ? obj.getString("limit") : "";
        int limit = "".equals(limitString) ? 0 : Integer.parseInt(limitString); 
        
        return new StandardReconConfig(
            obj.getString("service"),
            obj.has("identifierSpace") ? obj.getString("identifierSpace") : null,
            obj.has("schemaSpace") ? obj.getString("schemaSpace") : null,
            t == null ? null : t.getString("id"),
            t == null ? null : (t.has("name") ? t.getString("name") : null),
            obj.getBoolean("autoMatch"),
            columnDetails,
            limit
        );
    }
    
    static protected class StandardReconJob extends ReconJob {
        String text;
        String code;
        
        @Override
        public int getKey() {
            return code.hashCode();
        }
    }
    
    final public String     service;
    final public String     identifierSpace;
    final public String     schemaSpace;
    
    final public String     typeID;
    final public String     typeName;
    final public boolean    autoMatch;
    final public List<ColumnDetail> columnDetails;
    final private int limit;

    public StandardReconConfig(
            String service,
            String identifierSpace,
            String schemaSpace,
            
            String typeID, 
            String typeName,
            boolean autoMatch,
            List<ColumnDetail> columnDetails
        ) {
        this(service, identifierSpace, schemaSpace, typeID, typeName, autoMatch, columnDetails, 0);
    }
    
    
    /**
     * @param service
     * @param identifierSpace
     * @param schemaSpace
     * @param typeID
     * @param typeName
     * @param autoMatch
     * @param columnDetails
     * @param limit maximum number of results to return (0 = default)
     */
    public StandardReconConfig(
        String service,
        String identifierSpace,
        String schemaSpace,
        String typeID, 
        String typeName,
        boolean autoMatch,
        List<ColumnDetail> columnDetails,
        int limit
    ) {
        this.service = service;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        
        this.typeID = typeID;
        this.typeName = typeName;
        this.autoMatch = autoMatch;
        this.columnDetails = columnDetails;
        this.limit = limit;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("mode"); writer.value("standard-service");
        writer.key("service"); writer.value(service);
        writer.key("identifierSpace"); writer.value(identifierSpace);
        writer.key("schemaSpace"); writer.value(schemaSpace);
        writer.key("type");
            if (typeID == null) {
                writer.value(null);
            } else {
                writer.object();
                writer.key("id"); writer.value(typeID);
                writer.key("name"); writer.value(typeName);
                writer.endObject();
            }
        writer.key("autoMatch"); writer.value(autoMatch);
        writer.key("columnDetails");
            writer.array();
            for (ColumnDetail c : columnDetails) {
                writer.object();
                writer.key("column"); writer.value(c.columnName);
                writer.key("propertyName"); writer.value(c.propertyName);
                writer.key("propertyID"); writer.value(c.propertyID);
                writer.endObject();
            }
            writer.endArray();
          writer.key("limit"); writer.value(limit);
        writer.endObject();
    }

    @Override
    public int getBatchSize() {
        return 10;
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        return "Reconcile cells in column " + columnName + " to type " + typeID;
    }
    
    public ReconJob createSimpleJob(String query) {
        /* Same as createJob, but for simpler queries
         * without any properties. This is much easier
         * to generate as there is no need for a Project,
         * Row and Cell: this means the job can be created
         * outside the usual context of reconciliation (e.g.
         * in an importer).
         */
        StandardReconJob job = new StandardReconJob();
        try {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            jsonWriter.object();
            jsonWriter.key("query");
            jsonWriter.value(query);
            jsonWriter.endObject();
            job.text = query;
            job.code = stringWriter.toString();
            return job;
        } catch (JSONException _) {
            return null;
        }
    }

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        
        StandardReconJob job = new StandardReconJob();

        try {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            
            jsonWriter.object();
                jsonWriter.key("query"); jsonWriter.value(cell.value.toString());
                if (typeID != null) {
                    jsonWriter.key("type"); jsonWriter.value(typeID);
                    jsonWriter.key("type_strict"); jsonWriter.value("should");
                }
                
                if (columnDetails.size() > 0) {
                    jsonWriter.key("properties");
                    jsonWriter.array();
                    
                    for (ColumnDetail c : columnDetails) {
                        int detailCellIndex = project.columnModel.getColumnByName(c.columnName).getCellIndex();
                        
                        Cell cell2 = row.getCell(detailCellIndex);
                        if (cell2 == null || !ExpressionUtils.isNonBlankData(cell2.value)) {
                            int cellIndex = project.columnModel.getColumnByName(columnName).getCellIndex();
                            
                            RowDependency rd = project.recordModel.getRowDependency(rowIndex);
                            if (rd != null && rd.cellDependencies != null) {
                                int contextRowIndex = rd.cellDependencies[cellIndex].rowIndex;
                                if (contextRowIndex >= 0 && contextRowIndex < project.rows.size()) {
                                    Row row2 = project.rows.get(contextRowIndex);
                                    
                                    cell2 = row2.getCell(detailCellIndex);
                                }
                            }
                        }
                        
                        if (cell2 != null && ExpressionUtils.isNonBlankData(cell2.value)) {
                            jsonWriter.object();
                            
                            jsonWriter.key("pid"); jsonWriter.value(c.propertyID);
                            jsonWriter.key("v");
                            if (cell2.recon != null && cell2.recon.match != null) {
                                jsonWriter.object();
                                jsonWriter.key("id"); jsonWriter.value(cell2.recon.match.id);
                                jsonWriter.key("name"); jsonWriter.value(cell2.recon.match.name);
                                jsonWriter.endObject();
                            } else if (cell2.value instanceof Calendar) {
                                jsonWriter.value(ParsingUtilities.dateToString(((Calendar) cell2.value).getTime()));
                            } else if (cell2.value instanceof Date) {
                                jsonWriter.value(ParsingUtilities.dateToString((Date) cell2.value));
                            } else {
                                jsonWriter.value(cell2.value.toString());
                            }
                            
                            jsonWriter.endObject();
                        }
                    }
                    
                    jsonWriter.endArray();
                }
                
            // Only send limit if it's non-default to preserve backward compatibility with
            // services which might choke on this
            if (limit != 0) {
                jsonWriter.key("limit"); jsonWriter.value(limit);
            }
                
            jsonWriter.endObject();
            
            job.text = cell.value.toString();
            job.code = stringWriter.toString();
        } catch (JSONException e) {
            //
        }
        return job;
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID) {
        List<Recon> recons = new ArrayList<Recon>(jobs.size());
        
        StringWriter stringWriter = new StringWriter();
        
        stringWriter.write("{");
        for (int i = 0; i < jobs.size(); i++) {
            StandardReconJob job = (StandardReconJob) jobs.get(i);
            if (i > 0) {
                stringWriter.write(",");
            }
            stringWriter.write("\"q" + i + "\":");
            stringWriter.write(job.code);
        }
        stringWriter.write("}");
        String queriesString = stringWriter.toString();
        
        try {
            URL url = new URL(service);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setConnectTimeout(30000);
                connection.setDoOutput(true);
                
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                try {
                    String body = "queries=" + ParsingUtilities.encode(queriesString);
                    
                    dos.writeBytes(body);
                } finally {
                    dos.flush();
                    dos.close();
                }
                
                connection.connect();
            }
            
            if (connection.getResponseCode() >= 400) {
                InputStream is = connection.getErrorStream();
                logger.error("Failed  - code:" 
                        + Integer.toString(connection.getResponseCode()) 
                        + " message: " + is == null ? ""
                                : ParsingUtilities.inputStreamToString(is));
            } else {
                InputStream is = connection.getInputStream();
                try {
                    String s = ParsingUtilities.inputStreamToString(is);
                    JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);

                    for (int i = 0; i < jobs.size(); i++) {
                        StandardReconJob job = (StandardReconJob) jobs.get(i);
                        Recon recon = null;

                        String text = job.text;
                        String key = "q" + i;
                        if (o.has(key)) {
                            JSONObject o2 = o.getJSONObject(key);
                            if (o2.has("result")) {
                                JSONArray results = o2.getJSONArray("result");

                                recon = createReconServiceResults(text, results, historyEntryID);
                            } else {
                                logger.warn("Service error for text: " + text + "\n  Job code: " + job.code + "\n  Response: " + o2.toString());
                            }
                        } else {
                            logger.warn("Service error for text: " + text + "\n  Job code: " + job.code);
                        }

                        if (recon != null) {
                            recon.service = service;
                        }
                        recons.add(recon);
                    }
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to batch recon with load:\n" + queriesString, e);
        }

        while (recons.size() < jobs.size()) {
            Recon recon = new Recon(historyEntryID, identifierSpace, schemaSpace);
            recon.service = service;
            recon.identifierSpace = identifierSpace;
            recon.schemaSpace = schemaSpace;

            recons.add(recon);
        }
        
        return recons;
    }
    
    @Override
    public Recon createNewRecon(long historyEntryID) {
        Recon recon = new Recon(historyEntryID, identifierSpace, schemaSpace);
        recon.service = service;
        return recon;
    }

    protected Recon createReconServiceResults(String text, JSONArray results, long historyEntryID) {
        Recon recon = new Recon(historyEntryID, identifierSpace, schemaSpace);
        try {
            int length = results.length();
            int count = 0;
            for (int i = 0; i < length; i++) {
                JSONObject result = results.getJSONObject(i);
                if (!result.has("name")) {
                    continue;
                }
                
                JSONArray types = result.getJSONArray("type");
                String[] typeIDs = new String[types.length()];
                for (int j = 0; j < typeIDs.length; j++) {
                    Object type = types.get(j);
                    typeIDs[j] = type instanceof String ? (String) type :
                        ((JSONObject) type).getString("id");
                }
                
                double score = result.getDouble("score");
                ReconCandidate candidate = new ReconCandidate(
                    result.getString("id"),
                    result.getString("name"),
                    typeIDs,
                    score
                );
                
                if (autoMatch && i == 0 && result.has("match") && result.getBoolean("match")) {
                    recon.match = candidate;
                    recon.matchRank = 0;
                    recon.judgment = Judgment.Matched;
                    recon.judgmentAction = "auto";
                }
                
                recon.addCandidate(candidate);
                count++;
            }
            
            if (count > 0) {
                ReconCandidate candidate = recon.candidates.get(0);
                
                recon.setFeature(Recon.Feature_nameMatch, text.equalsIgnoreCase(candidate.name));
                recon.setFeature(Recon.Feature_nameLevenshtein, 
                        StringUtils.getLevenshteinDistance(StringUtils.lowerCase(text), StringUtils.lowerCase(candidate.name)));
                recon.setFeature(Recon.Feature_nameWordDistance, wordDistance(text, candidate.name));
                
                recon.setFeature(Recon.Feature_typeMatch, false);
                if (this.typeID != null) {
                    for (String typeID : candidate.types) {
                        if (this.typeID.equals(typeID)) {
                            recon.setFeature(Recon.Feature_typeMatch, true);
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return recon;
    }
    
    static protected double wordDistance(String s1, String s2) {
        Set<String> words1 = breakWords(s1);
        Set<String> words2 = breakWords(s2);
        return words1.size() >= words2.size() ? wordDistance(words1, words2) : wordDistance(words2, words1);
    }
    
    static protected double wordDistance(Set<String> longWords, Set<String> shortWords) {
        if (longWords.size() == 0) {
            return 0.0;
        }

        double common = 0;
        for (String word : shortWords) {
            if (longWords.contains(word)) {
                common++;
            }
        }
        return common / longWords.size();
    }
    
    static final protected Set<String> s_stopWords = new HashSet<String>();
    static {
        // FIXME: This is English specific
        s_stopWords.add("the");
        s_stopWords.add("a");
        s_stopWords.add("and");
        s_stopWords.add("of");
        s_stopWords.add("on");
        s_stopWords.add("in");
        s_stopWords.add("at");
        s_stopWords.add("by");
    }
    
    static protected Set<String> breakWords(String s) {
        String[] words = s.toLowerCase().split("\\s+");
        
        Set<String> set = new HashSet<String>(words.length);
        for (String word : words) {
            if (!s_stopWords.contains(word)) {
                set.add(word);
            }
        }
        return set;
    }
}
