package com.metaweb.gridworks.model.operations;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Parser;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;

public class TextTransformOperation extends EngineDependentMassCellOperation {
	private static final long serialVersionUID = -7698202759999537298L;

	final protected String _expression;
	
	public TextTransformOperation(JSONObject engineConfig, int cellIndex, String expression) {
		super(engineConfig, cellIndex);
		_expression = expression;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		
		return "Text transform on " + cellChanges.size() + 
			" cells in column " + column.getHeaderLabel() + ": " + _expression;
	}

	@Override
	protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
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
			
			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				if (cellIndex < row.cells.size()) {
					Cell cell = row.cells.get(cellIndex);
					if (cell.value != null) {
		                ExpressionUtils.bind(bindings, row, cell);
						
						Cell newCell = new Cell(eval.evaluate(bindings), cell.recon);
						
						CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
						cellChanges.add(cellChange);
					}
				}
				return false;
			}
		}.init(_cellIndex, bindings, cellChanges, eval);
	}
}
