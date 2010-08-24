package com.google.gridworks.operations;

import java.lang.reflect.Method; 
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;

import edu.mit.simile.butterfly.ButterflyModule;

public abstract class OperationRegistry {

    static final public Map<String, Class<? extends AbstractOperation>> s_opNameToClass = new HashMap<String, Class<? extends AbstractOperation>>();
    static final public Map<Class<? extends AbstractOperation>, String> s_opClassToName = new HashMap<Class<? extends AbstractOperation>, String>();
    
    static public void registerOperation(ButterflyModule module, String name, Class<? extends AbstractOperation> klass) {
        String key = module.getName() + "/" + name;
        
        s_opNameToClass.put(key, klass);
        s_opClassToName.put(klass, key);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) {
        try {
            String op = obj.getString("op");
            if (!op.contains("/")) {
                op = "core/" + op; // backward compatible
            }
            
            Class<? extends AbstractOperation> klass = OperationRegistry.s_opNameToClass.get(op);
            if (klass != null) {
                Method reconstruct = klass.getMethod("reconstruct", Project.class, JSONObject.class);
                if (reconstruct != null) {
                    return (AbstractOperation) reconstruct.invoke(null, project, obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
