package com.metaweb.gridworks.clustering;

import org.json.JSONObject;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;

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
