package com.metaweb.gridworks.model.recon;

import java.io.Writer;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;

abstract public class ReconConfig implements Jsonizable {
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        String mode = obj.getString("mode");
        if ("heuristic".equals(mode)) {
            return HeuristicReconConfig.reconstruct(obj);
        } else if ("strict".equals(mode)) {
            return StrictReconConfig.reconstruct(obj);
        } else if ("extend".equals(mode)) {
            return DataExtensionReconConfig.reconstruct(obj);
        }
        return null;
    }
    
    abstract public int getBatchSize();
    
    abstract public String getBriefDescription(Project project, String columnName);
    
    abstract public ReconJob createJob(
        Project     project, 
        int         rowIndex, 
        Row         row,
        String      columnName,
        Cell        cell
    );
    
    abstract public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID);
    
    public void save(Writer writer) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, new Properties());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
