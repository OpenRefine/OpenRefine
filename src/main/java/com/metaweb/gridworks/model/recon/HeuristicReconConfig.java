package com.metaweb.gridworks.model.recon;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.util.ParsingUtilities;

public class HeuristicReconConfig extends ReconConfig {
    static public class ColumnDetail {
        final public String columnName;
        final public FreebaseProperty property;
        
        public ColumnDetail(String columnName, FreebaseProperty property) {
            this.columnName = columnName;
            this.property = property;
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
                JSONObject p = o.getJSONObject("property");
                
                columnDetails.add(new ColumnDetail(
                    o.getString("column"),
                    new FreebaseProperty(
                        p.getString("id"),
                        p.getString("name")
                    )
                ));
            }
        } else {
            columnDetails = new ArrayList<ColumnDetail>();
        }
        
        JSONObject t = obj.getJSONObject("type");
        
        return new HeuristicReconConfig(
            obj.getString("service"),
            t.getString("id"),
            t.getString("name"),
            obj.getBoolean("autoMatch"),
            columnDetails
        );
    }
    
    static protected class HeuristicReconJob extends ReconJob {
        String text;
        String code;
        
        public int getKey() {
            return code.hashCode();
        }
    }
    
    final public String     service; // either "recon" or "relevance"
    final public String     typeID;
    final public String     typeName;
    final public boolean    autoMatch;
    final public List<ColumnDetail> columnDetails;
    
    public HeuristicReconConfig(
        String service,
        String typeID, 
        String typeName,
        boolean autoMatch,
        List<ColumnDetail> columnDetails
    ) {
        this.service = service;
        this.typeID = typeID;
        this.typeName = typeName;
        this.autoMatch = autoMatch;
        this.columnDetails = columnDetails;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("mode"); writer.value("heuristic"); 
        writer.key("service"); writer.value(service); 
        writer.key("type"); 
            writer.object();
            writer.key("id"); writer.value(typeID);
            writer.key("name"); writer.value(typeName);
            writer.endObject();
        writer.key("autoMatch"); writer.value(autoMatch);
        writer.key("columnDetails");
            writer.array();
            for (ColumnDetail c : columnDetails) {
                writer.object();
                writer.key("column"); writer.value(c.columnName);
                writer.key("property"); c.property.write(writer, options);
                writer.endObject();
            }
            writer.endArray();
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

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        
        HeuristicReconJob job = new HeuristicReconJob();
        if ("relevance".equals(service)) {
            job.code = job.text = cell.value.toString();
        } else {
            try {
                StringWriter stringWriter = new StringWriter();
                JSONWriter jsonWriter = new JSONWriter(stringWriter);
                
                jsonWriter.object();
                    jsonWriter.key("/type/object/name"); jsonWriter.value(cell.value.toString());
                    jsonWriter.key("/type/object/type"); jsonWriter.value(typeID);
                    
                    for (ColumnDetail c : columnDetails) {
                        int detailCellIndex = project.columnModel.getColumnByName(c.columnName).getCellIndex();
                        
                        Cell cell2 = row.getCell(detailCellIndex);
                        if (cell2 == null || !ExpressionUtils.isNonBlankData(cell2.value)) {
                            int cellIndex = project.columnModel.getColumnByName(columnName).getCellIndex();
                            if (row.contextRowSlots != null && cellIndex < row.contextRowSlots.length) {
                                int contextRowIndex = row.contextRowSlots[cellIndex];
                                if (contextRowIndex >= 0 && contextRowIndex < project.rows.size()) {
                                    Row row2 = project.rows.get(contextRowIndex);
                                    
                                    cell2 = row2.getCell(detailCellIndex);
                                }
                            }
                        }
                        if (cell2 != null && ExpressionUtils.isNonBlankData(cell2.value)) {
                            jsonWriter.key(c.property.id);
                            
                            if (cell2.recon != null && cell2.recon.match != null) {
                                jsonWriter.object();
                                jsonWriter.key("id"); jsonWriter.value(cell2.recon.match.topicID);
                                jsonWriter.key("name"); jsonWriter.value(cell2.recon.match.topicName);
                                jsonWriter.endObject();
                            } else {
                                jsonWriter.value(cell2.value.toString());
                            }
                        }
                    }
                jsonWriter.endObject();
                
                job.text = cell.value.toString();
                job.code = stringWriter.toString();
            } catch (JSONException e) {
                //
            }
        }
        return job;
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs) {
        if ("relevance".equals(service)) {
            return batchReconUsingRelevance(jobs);
        } else {
            return batchReconUsingReconService(jobs);
        }
    }
    
    protected List<Recon> batchReconUsingRelevance(List<ReconJob> jobs) {
        List<Recon> recons = new ArrayList<Recon>(jobs.size());
        
        try {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            
            jsonWriter.object();
            for (int i = 0; i < jobs.size(); i++) {
                HeuristicReconJob job = (HeuristicReconJob) jobs.get(i);
                
                jsonWriter.key("q" + i + ":search");
                
                jsonWriter.object();
                    jsonWriter.key("query"); jsonWriter.value(job.text);
                    jsonWriter.key("limit"); jsonWriter.value(3);
                    jsonWriter.key("type"); jsonWriter.value(typeID);
                    jsonWriter.key("type_strict"); jsonWriter.value("should");
                    jsonWriter.key("type_exclude"); jsonWriter.value("/common/image");
                    jsonWriter.key("domain_exclude"); jsonWriter.value("/freebase");
                jsonWriter.endObject();
            }
            jsonWriter.endObject();
            
            StringBuffer sb = new StringBuffer(1024);
            sb.append("http://api.freebase.com/api/service/search?indent=1&queries=");
            sb.append(ParsingUtilities.encode(stringWriter.toString()));
            
            URL url = new URL(sb.toString());
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
                
                for (int i = 0; i < jobs.size(); i++) {
                    HeuristicReconJob job = (HeuristicReconJob) jobs.get(i);
                    
                    String text = job.text;
                    String key = "q" + i + ":search";
                    if (!o.has(key)) {
                        continue;
                    }
                    
                    Recon recon = null;
                    
                    JSONObject o2 = o.getJSONObject(key);
                    if (o2.has("result")) {
                        JSONArray results = o2.getJSONArray("result");
                        
                        recon = createReconFromRelevanceResults(text, results);
                    } else {
                        recon = new Recon();
                    }
                    
                    recons.add(recon);
                }
            } finally {
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return recons;
    }

    protected Recon createReconFromRelevanceResults(String text, JSONArray results) {
        Recon recon = new Recon();
        try {
            int length = results.length();
            int count = 0;
            for (int i = 0; i < length && count < 3; i++) {
                JSONObject result = results.getJSONObject(i);
                if (!result.has("name")) {
                    continue;
                }
                
                JSONArray types = result.getJSONArray("type");
                String[] typeIDs = new String[types.length()];
                for (int j = 0; j < typeIDs.length; j++) {
                    typeIDs[j] = types.getJSONObject(j).getString("id");
                }
                
                double score = result.getDouble("relevance:score");
                ReconCandidate candidate = new ReconCandidate(
                    result.getString("id"),
                    result.getString("guid"),
                    result.getString("name"),
                    typeIDs,
                    score
                );
                
                recon.addCandidate(candidate);
                count++;
            }
            
            if (count > 0) {
            	ReconCandidate candidate = recon.candidates.get(0);
            	
                recon.setFeature(Recon.Feature_nameMatch, text.equalsIgnoreCase(candidate.topicName));
                recon.setFeature(Recon.Feature_nameLevenshtein, StringUtils.getLevenshteinDistance(text, candidate.topicName));
                recon.setFeature(Recon.Feature_nameWordDistance, wordDistance(text, candidate.topicName));
                
                recon.setFeature(Recon.Feature_typeMatch, false);
                for (String typeID : candidate.typeIDs) {
                    if (this.typeID.equals(typeID)) {
                        recon.setFeature(Recon.Feature_typeMatch, true);
                        if (autoMatch && candidate.score >= 100) {
                        	if (count == 1 || 
                        		candidate.score / recon.candidates.get(1).score >= 1.5) {
                        		
                        		recon.match = candidate;
                        		recon.judgment = Judgment.Matched;
                        	}
                        }
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return recon;
    }
    
    static final String s_reconService = "http://data.labs.freebase.com/recon/query";
    
    protected List<Recon> batchReconUsingReconService(List<ReconJob> jobs) {
        List<Recon> recons = new ArrayList<Recon>(jobs.size());
        
        for (int i = 0; i < jobs.size(); i++) {
            HeuristicReconJob job = (HeuristicReconJob) jobs.get(i);
            Recon recon = null;
            try {
                StringBuffer sb = new StringBuffer();
                sb.append(s_reconService + "?limit=5&q=");
                sb.append(ParsingUtilities.encode(job.code));
                
                URL url = new URL(sb.toString());
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
                
                InputStream is = connection.getInputStream();
                try {
                    String s = ParsingUtilities.inputStreamToString(is);
                    JSONArray a = ParsingUtilities.evaluateJsonStringToArray(s);
                
                    recon = createReconFromReconResults(job.text, a);
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (recon == null) {
                recon = new Recon();
            }
            recons.add(recon);
        }
        
        return recons;
    }

    protected Recon createReconFromReconResults(String text, JSONArray results) {
        Recon recon = new Recon();
        try {
            int length = results.length();
            int count = 0;
            for (int i = 0; i < length && count < 3; i++) {
                JSONObject result = results.getJSONObject(i);
                if (!result.has("name")) {
                    continue;
                }
                
                String id = result.getString("id");
                JSONArray names = result.getJSONArray("name");
                double score = result.getDouble("score");
                
                JSONArray types = result.getJSONArray("type");
                String[] typeIDs = new String[types.length()];
                for (int j = 0; j < typeIDs.length; j++) {
                    typeIDs[j] = types.getString(j);
                }
                
                ReconCandidate candidate = new ReconCandidate(
                    id,
                    "#" + id.substring(6),
                    names.getString(0),
                    typeIDs,
                    score
                );
                
                // best match
                if (i == 0) {
                    recon.setFeature(Recon.Feature_nameMatch, text.equalsIgnoreCase(candidate.topicName));
                    recon.setFeature(Recon.Feature_nameLevenshtein, StringUtils.getLevenshteinDistance(text, candidate.topicName));
                    recon.setFeature(Recon.Feature_nameWordDistance, wordDistance(text, candidate.topicName));
                    
                    recon.setFeature(Recon.Feature_typeMatch, false);
                    for (String typeID : candidate.typeIDs) {
                        if (this.typeID.equals(typeID)) {
                            recon.setFeature(Recon.Feature_typeMatch, true);
                            if (autoMatch && result.has("match") && result.getBoolean("match")) {
                                recon.match = candidate;
                                recon.judgment = Judgment.Matched;
                            }
                            break;
                        }
                    }
                }
                
                recon.addCandidate(candidate);
                count++;
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
