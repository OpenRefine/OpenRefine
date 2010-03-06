package com.metaweb.gridworks.model.recon;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.util.ParsingUtilities;

public class GuidBasedReconConfig extends StrictReconConfig {
    private static final long serialVersionUID = 1857895989346775294L;

    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        return new GuidBasedReconConfig();
    }
    
    public GuidBasedReconConfig() {
    }

    static protected class GuidBasedReconJob extends ReconJob {
        String guid;
        
        public int getKey() {
            return guid.hashCode();
        }
    }

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        
        GuidBasedReconJob job = new GuidBasedReconJob();
        String s = cell.value.toString();
        
        if (s.startsWith("/guid/")) {
            s = "#" + s.substring(6);
        } else if (!s.startsWith("#")) {
            s = "#" + s;
        }
        
        job.guid = s;
        
        return job;
    }

    @Override
    public int getBatchSize() {
        return 10;
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        return "Reconcile cells in column " + columnName + " as Freebase IDs";
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("mode"); writer.value("strict");
        writer.key("match"); writer.value("id"); 
        writer.endObject();
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs) {
        List<Recon> recons = new ArrayList<Recon>(jobs.size());
        Map<String, Recon> guidToRecon = new HashMap<String, Recon>();
        
        try {
            String query = null;
            {
                StringWriter stringWriter = new StringWriter();
                JSONWriter jsonWriter = new JSONWriter(stringWriter);
                
                jsonWriter.object();
                jsonWriter.key("query");
                    jsonWriter.array();
                    jsonWriter.object();
                    
                        jsonWriter.key("id"); jsonWriter.value(null);
                        jsonWriter.key("name"); jsonWriter.value(null);
                        jsonWriter.key("guid"); jsonWriter.value(null);
                        jsonWriter.key("type"); jsonWriter.array(); jsonWriter.endArray();
                        
                        jsonWriter.key("guid|=");
                            jsonWriter.array();
                            for (ReconJob job : jobs) {
                                jsonWriter.value(((GuidBasedReconJob) job).guid);
                            }
                            jsonWriter.endArray();
                        
                    jsonWriter.endObject();
                    jsonWriter.endArray();
                jsonWriter.endObject();
                
                query = stringWriter.toString();
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append(s_mqlreadService + "?query=");
            sb.append(ParsingUtilities.encode(query));
            
            URL url = new URL(sb.toString());
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
                JSONArray results = o.getJSONArray("result");
                int count = results.length();
                
                for (int i = 0; i < count; i++) {
                    JSONObject result = results.getJSONObject(i);
                    
                    String guid = result.getString("guid");
                    
                    JSONArray types = result.getJSONArray("type");
                    String[] typeIDs = new String[types.length()];
                    for (int j = 0; j < typeIDs.length; j++) {
                        typeIDs[j] = types.getString(j);
                    }
                    
                    ReconCandidate candidate = new ReconCandidate(
                        result.getString("id"),
                        guid,
                        result.getString("name"),
                        typeIDs,
                        100
                    );
                    
                    Recon recon = new Recon();
                    recon.addCandidate(candidate);
                    recon.match = candidate;
                    recon.judgment = Judgment.Matched;
                    
                    guidToRecon.put(guid, recon);
                }
            } finally {
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        for (int i = 0; i < jobs.size(); i++) {
            String guid = ((GuidBasedReconJob) jobs.get(i)).guid;
            Recon recon = guidToRecon.get(guid);
            recons.add(recon);
        }
        
        return recons;
    }
}
