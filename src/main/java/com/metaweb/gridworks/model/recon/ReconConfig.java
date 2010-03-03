package com.metaweb.gridworks.model.recon;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;

abstract public class ReconConfig implements Serializable, Jsonizable {
    private static final long serialVersionUID = -4831409797104437854L;
    
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        String mode = obj.getString("mode");
        if ("heuristic".equals(mode)) {
        	return HeuristicReconConfig.reconstruct(obj);
        } else if ("strict".equals(mode)) {
        	return StrictReconConfig.reconstruct(obj);
        }
        return null;
    }
    
    abstract public int getBatchSize();
    
	abstract public String getBriefDescription(Project project, String columnName);
	
	abstract public ReconJob createJob(
		Project 	project, 
		int 		rowIndex, 
		Row 		row,
		String		columnName,
		Cell		cell
	);
    
	abstract public List<Recon> batchRecon(List<ReconJob> jobs);
}
