package com.metaweb.gridworks.model.recon;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.protograph.FreebaseType;

public class DataExtensionReconConfig extends StrictReconConfig {
    final public FreebaseType type;
    
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        JSONObject type = obj.getJSONObject("type");
        
        return new DataExtensionReconConfig(
            new FreebaseType(
                type.getString("id"),
                type.getString("name")
            )
        );
    }
    
    public DataExtensionReconConfig(FreebaseType type) {
        this.type = type;
    }

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        
        throw new NotImplementedException();
    }

    @Override
    public int getBatchSize() {
        throw new NotImplementedException();
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("mode"); writer.value("extend");
        writer.key("type"); type.write(writer, options); 
        writer.endObject();
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs) {
        throw new NotImplementedException();
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        throw new NotImplementedException();
    }

}
