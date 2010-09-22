package com.google.refine.model.recon;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.protograph.FreebaseType;

public class DataExtensionReconConfig extends StrictReconConfig {
    final public FreebaseType type;
    
    private final static String WARN = "Not implemented";
    
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
        throw new RuntimeException(WARN);
    }

    @Override
    public int getBatchSize() {
        throw new RuntimeException(WARN);
    }

    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("mode"); writer.value("extend");
        writer.key("type"); type.write(writer, options); 
        writer.endObject();
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID) {
        throw new RuntimeException(WARN);
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        throw new RuntimeException(WARN);
    }
}
