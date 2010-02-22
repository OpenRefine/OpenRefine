package com.metaweb.gridworks.operations;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Parser;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;

public class TextTransformOperation extends EngineDependentMassCellOperation {
	private static final long serialVersionUID = -7698202759999537298L;

	final protected String _expression;
	
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new TextTransformOperation(
            engineConfig,
            obj.getString("columnName"),
            obj.getString("expression")
        );
    }
    
	public TextTransformOperation(JSONObject engineConfig, String columnName, String expression) {
		super(engineConfig, columnName, true);
		_expression = expression;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
		writer.key("description"); writer.value(getBriefDescription());
		writer.key("engineConfig"); writer.value(getEngineConfig());
		writer.key("columnName"); writer.value(_columnName);
		writer.key("expression"); writer.value(_expression);
		writer.endObject();
	}

	protected String getBriefDescription() {
		return "Text transform on cells in column " + _columnName + " using expression " + _expression;
	}

	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		
		return "Text transform on " + cellChanges.size() + 
			" cells in column " + column.getHeaderLabel() + ": " + _expression;
	}

	protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);
		
		Evaluable eval = new Parser(_expression).getExpression();
        Properties bindings = ExpressionUtils.createBindings(project);
        
		return new RowVisitor() {
			int 				cellIndex;
			Properties 			bindings;
			List<CellChange> 	cellChanges;
			Evaluable 			eval;
			
			public RowVisitor init(int cellIndex, Properties bindings, List<CellChange> cellChanges, Evaluable eval) {
				this.cellIndex = cellIndex;
				this.bindings = bindings;
				this.cellChanges = cellChanges;
				this.eval = eval;
				return this;
			}
			
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
				Cell cell = row.getCell(cellIndex);

                ExpressionUtils.bind(bindings, row, rowIndex, cell);
				
                Object v = eval.evaluate(bindings);
                if ((cell != null && cell.value != null) || v != null) {
                    Cell newCell = new Cell(v, cell.recon);
				
    				CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
    				cellChanges.add(cellChange);
                }
                
				return false;
			}
		}.init(column.getCellIndex(), bindings, cellChanges, eval);
	}
}
