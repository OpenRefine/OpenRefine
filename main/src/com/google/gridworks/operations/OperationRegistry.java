package com.google.gridworks.operations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.cell.BlankDownOperation;
import com.google.gridworks.operations.cell.FillDownOperation;
import com.google.gridworks.operations.cell.MassEditOperation;
import com.google.gridworks.operations.cell.MultiValuedCellJoinOperation;
import com.google.gridworks.operations.cell.MultiValuedCellSplitOperation;
import com.google.gridworks.operations.cell.TextTransformOperation;
import com.google.gridworks.operations.cell.TransposeColumnsIntoRowsOperation;
import com.google.gridworks.operations.cell.TransposeRowsIntoColumnsOperation;
import com.google.gridworks.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.gridworks.operations.column.ColumnAdditionOperation;
import com.google.gridworks.operations.column.ColumnMoveOperation;
import com.google.gridworks.operations.column.ColumnRemovalOperation;
import com.google.gridworks.operations.column.ColumnRenameOperation;
import com.google.gridworks.operations.column.ColumnSplitOperation;
import com.google.gridworks.operations.column.ExtendDataOperation;
import com.google.gridworks.operations.recon.ImportQADataOperation;
import com.google.gridworks.operations.recon.ReconDiscardJudgmentsOperation;
import com.google.gridworks.operations.recon.ReconJudgeSimilarCellsOperation;
import com.google.gridworks.operations.recon.ReconMarkNewTopicsOperation;
import com.google.gridworks.operations.recon.ReconMatchBestCandidatesOperation;
import com.google.gridworks.operations.recon.ReconMatchSpecificTopicOperation;
import com.google.gridworks.operations.recon.ReconOperation;
import com.google.gridworks.operations.row.DenormalizeOperation;
import com.google.gridworks.operations.row.RowFlagOperation;
import com.google.gridworks.operations.row.RowRemovalOperation;
import com.google.gridworks.operations.row.RowReorderOperation;
import com.google.gridworks.operations.row.RowStarOperation;

public abstract class OperationRegistry {

    static final public Map<String, Class<? extends AbstractOperation>> s_opNameToClass = new HashMap<String, Class<? extends AbstractOperation>>();
    static final public Map<Class<? extends AbstractOperation>, String> s_opClassToName = new HashMap<Class<? extends AbstractOperation>, String>();
    
    static public void register(String name, Class<? extends AbstractOperation> klass) {
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
        register("fill-down", FillDownOperation.class);
        register("blank-down", BlankDownOperation.class);
        register("transpose-columns-into-rows", TransposeColumnsIntoRowsOperation.class);
        register("transpose-rows-into-columns", TransposeRowsIntoColumnsOperation.class);
        
        register("column-addition", ColumnAdditionOperation.class);
        register("column-removal", ColumnRemovalOperation.class);
        register("column-rename", ColumnRenameOperation.class);
        register("column-move", ColumnMoveOperation.class);
        register("column-split", ColumnSplitOperation.class);
        register("extend-data", ExtendDataOperation.class);
        register("column-addition-by-fetching-urls", ColumnAdditionByFetchingURLsOperation.class);
        
        register("row-removal", RowRemovalOperation.class);
        register("row-star", RowStarOperation.class);
        register("row-flag", RowFlagOperation.class);
        register("row-reorder", RowReorderOperation.class);
        
        register("save-protograph", SaveProtographOperation.class);
        register("text-transform", TextTransformOperation.class);
        register("mass-edit", MassEditOperation.class);
        
        register("import-qa-data", ImportQADataOperation.class);
        
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
