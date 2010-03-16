package com.metaweb.gridworks.operations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;

public abstract class OperationRegistry {
    static public Map<String, Class<? extends AbstractOperation>> s_opNameToClass;
    static public Map<Class<? extends AbstractOperation>, String> s_opClassToName;
    
    static protected void register(String name, Class<? extends AbstractOperation> klass) {
        s_opNameToClass.put(name, klass);
        s_opClassToName.put(klass, name);
    }
    
    static {
        s_opNameToClass = new HashMap<String, Class<? extends AbstractOperation>>();
        s_opClassToName = new HashMap<Class<? extends AbstractOperation>, String>();
        
        register("recon", ReconOperation.class);
        register("recon-mark-new-topics", ReconMarkNewTopicsOperation.class);
        register("recon-match-best-candidates", ReconMatchBestCandidatesOperation.class);
        register("recon-discard-judgments", ReconDiscardJudgmentsOperation.class);
        register("recon-match-specific-topic-to-cells", ReconMatchSpecificTopicOperation.class);
        register("recon-judge-similar-cells", ReconJudgeSimilarCellsOperation.class);
        
        register("multivalued-cell-join", MultiValuedCellJoinOperation.class);
        register("multivalued-cell-split", MultiValuedCellSplitOperation.class);
        
        register("column-addition", ColumnAdditionOperation.class);
        register("column-removal", ColumnRemovalOperation.class);
        register("extend-data", ExtendDataOperation.class);
        
        register("row-star", RowStarOperation.class);
        
        register("save-protograph", SaveProtographOperation.class);
        register("text-transform", TextTransformOperation.class);
        register("mass-edit", MassEditOperation.class);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) {
        try {
            String op = obj.getString("op");
            
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
