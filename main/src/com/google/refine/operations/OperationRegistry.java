package com.google.refine.operations;

import java.lang.reflect.Method; 
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

import edu.mit.simile.butterfly.ButterflyModule;

public abstract class OperationRegistry {

    static final public Map<String, List<Class<? extends AbstractOperation>>> s_opNameToClass =
        new HashMap<String, List<Class<? extends AbstractOperation>>>();
    
    static final public Map<Class<? extends AbstractOperation>, String> s_opClassToName =
        new HashMap<Class<? extends AbstractOperation>, String>();
    
    static public void registerOperation(ButterflyModule module, String name, Class<? extends AbstractOperation> klass) {
        String key = module.getName() + "/" + name;
        
        s_opClassToName.put(klass, key);
        
        List<Class<? extends AbstractOperation>> classes = s_opNameToClass.get(key);
        if (classes == null) {
            classes = new LinkedList<Class<? extends AbstractOperation>>();
            s_opNameToClass.put(key, classes);
        }
        classes.add(klass);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) {
        try {
            String op = obj.getString("op");
            if (!op.contains("/")) {
                op = "core/" + op; // backward compatible
            }
            
            List<Class<? extends AbstractOperation>> classes = s_opNameToClass.get(op);
            if (classes != null && classes.size() > 0) {
                Class<? extends AbstractOperation> klass = classes.get(classes.size() - 1);
                
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
