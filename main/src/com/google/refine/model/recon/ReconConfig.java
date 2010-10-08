package com.google.refine.model.recon;

import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;

import edu.mit.simile.butterfly.ButterflyModule;

abstract public class ReconConfig implements Jsonizable {
    static final public Map<String, List<Class<? extends ReconConfig>>> s_opNameToClass =
        new HashMap<String, List<Class<? extends ReconConfig>>>();
    
    static final public Map<Class<? extends ReconConfig>, String> s_opClassToName =
        new HashMap<Class<? extends ReconConfig>, String>();
    
    static public void registerReconConfig(ButterflyModule module, String name, Class<? extends ReconConfig> klass) {
        String key = module.getName() + "/" + name;
        
        s_opClassToName.put(klass, key);
        
        List<Class<? extends ReconConfig>> classes = s_opNameToClass.get(key);
        if (classes == null) {
            classes = new LinkedList<Class<? extends ReconConfig>>();
            s_opNameToClass.put(key, classes);
        }
        classes.add(klass);
    }
    
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        try {
            String mode = obj.getString("mode");
            
            // Backward compatibility
            if ("extend".equals(mode) || "strict".equals("mode")) {
                mode = "freebase/" + mode;
            } else if ("heuristic".equals(mode)) {
                mode = "core/standard-service"; // legacy
            } else if (!mode.contains("/")) {
                mode = "core/" + mode;
            }
            
            List<Class<? extends ReconConfig>> classes = s_opNameToClass.get(mode);
            if (classes != null && classes.size() > 0) {
                Class<? extends ReconConfig> klass = classes.get(classes.size() - 1);
                
                Method reconstruct = klass.getMethod("reconstruct", JSONObject.class);
                if (reconstruct != null) {
                    return (ReconConfig) reconstruct.invoke(null, obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
