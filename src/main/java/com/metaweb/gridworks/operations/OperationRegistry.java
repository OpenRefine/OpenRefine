package com.metaweb.gridworks.operations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.cell.MassEditOperation;
import com.metaweb.gridworks.operations.cell.MultiValuedCellJoinOperation;
import com.metaweb.gridworks.operations.cell.MultiValuedCellSplitOperation;
import com.metaweb.gridworks.operations.cell.TextTransformOperation;
import com.metaweb.gridworks.operations.column.ColumnAdditionOperation;
import com.metaweb.gridworks.operations.column.ColumnRemovalOperation;
import com.metaweb.gridworks.operations.column.ColumnRenameOperation;
import com.metaweb.gridworks.operations.column.ColumnSplitOperation;
import com.metaweb.gridworks.operations.column.ExtendDataOperation;
import com.metaweb.gridworks.operations.recon.ReconDiscardJudgmentsOperation;
import com.metaweb.gridworks.operations.recon.ReconJudgeSimilarCellsOperation;
import com.metaweb.gridworks.operations.recon.ReconMarkNewTopicsOperation;
import com.metaweb.gridworks.operations.recon.ReconMatchBestCandidatesOperation;
import com.metaweb.gridworks.operations.recon.ReconMatchSpecificTopicOperation;
import com.metaweb.gridworks.operations.recon.ReconOperation;
import com.metaweb.gridworks.operations.row.DenormalizeOperation;
import com.metaweb.gridworks.operations.row.RowFlagOperation;
import com.metaweb.gridworks.operations.row.RowRemovalOperation;
import com.metaweb.gridworks.operations.row.RowStarOperation;

public abstract class OperationRegistry {

    static final public Map<String, Class<? extends AbstractOperation>> s_opNameToClass = new HashMap<String, Class<? extends AbstractOperation>>();
    static final public Map<Class<? extends AbstractOperation>, String> s_opClassToName = new HashMap<Class<? extends AbstractOperation>, String>();
    
    static protected void register(String name, Class<? extends AbstractOperation> klass) {
        s_opNameToClass.put(name, klass);
        s_opClassToName.put(klass, name);
    }
    
    static {
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
        register("column-rename", ColumnRenameOperation.class);
        register("column-split", ColumnSplitOperation.class);
        register("extend-data", ExtendDataOperation.class);
        
        register("row-removal", RowRemovalOperation.class);
        register("row-star", RowStarOperation.class);
        register("row-flag", RowFlagOperation.class);
        
        register("save-protograph", SaveProtographOperation.class);
        register("text-transform", TextTransformOperation.class);
        register("mass-edit", MassEditOperation.class);
        
        register("denormalize", DenormalizeOperation.class);
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
