package com.google.refine.clustering;

import org.json.JSONObject;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public abstract class Clusterer implements Jsonizable {

    protected Project _project;
    protected int _colindex;
    protected JSONObject _config;

    public abstract void computeClusters(Engine engine);
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _project = project;
        _config = o;

        String colname = o.getString("column");
        for (Column column : project.columnModel.columns) {
            if (column.getName().equals(colname)) {
                _colindex = column.getCellIndex();
            }
        }
    }
}
