package com.google.refine.clustering;

import org.json.JSONObject;

import com.google.refine.Jsonizable;
import com.google.refine.model.Project;

/**
 * Represents the configuration data for a clusterer.
 * @author Antonin Delpeuch
 *
 */
public abstract class ClustererConfig implements Jsonizable {
    
    protected String columnName;
    
    /**
     * Reads the configuration from a JSON payload (TODO: delete)
     * @param o
     */
    public void initializeFromJSON(JSONObject o) {
        columnName = o.getString("column");
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    /**
     * Instantiate the configuration on a particular project.
     * @param project
     * @return
     */
    public abstract Clusterer apply(Project project);
}
