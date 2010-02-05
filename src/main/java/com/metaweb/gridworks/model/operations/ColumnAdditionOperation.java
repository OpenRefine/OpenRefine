package com.metaweb.gridworks.model.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Parser;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellAtRow;
import com.metaweb.gridworks.model.changes.ColumnAdditionChange;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class ColumnAdditionOperation extends EngineDependentOperation {
	private static final long serialVersionUID = -5672677479629932356L;

	final protected int		_baseCellIndex;
	final protected String 	_expression;
	
	final protected String 	_headerLabel;
	final protected int		_columnInsertIndex;

	public ColumnAdditionOperation(
		JSONObject 	engineConfig,
		int			baseCellIndex,
		String 		expression,
		String 		headerLabel, 
		int 		columnInsertIndex 
	) {
		super(engineConfig);
		
		_baseCellIndex = baseCellIndex;
		_expression = expression;
		
		_headerLabel = headerLabel;
		_columnInsertIndex = columnInsertIndex;
	}

	@Override
	public Process createProcess(Project project, Properties options)
			throws Exception {
		
		Engine engine = createEngine(project);
		
		Column column = project.columnModel.getColumnByCellIndex(_baseCellIndex);
		if (column == null) {
			throw new Exception("No column corresponding to cell index " + _baseCellIndex);
		}
		
		List<CellAtRow> cellsAtRows = new ArrayList<CellAtRow>(project.rows.size());
		
		FilteredRows filteredRows = engine.getAllFilteredRows(false);
		filteredRows.accept(project, createRowVisitor(project, cellsAtRows));
		
		String description = createDescription(column, cellsAtRows);
		
		Change change = new ColumnAdditionChange(_headerLabel, _columnInsertIndex, cellsAtRows);
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}

	protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
		return "Create new column " + _headerLabel + 
			" based on column " + column.getHeaderLabel() + 
			" by filling " + cellsAtRows.size() +
			" rows with " + _expression;
	}
	
	protected RowVisitor createRowVisitor(Project project, List<CellAtRow> cellsAtRows) throws Exception {
		Evaluable eval = new Parser(_expression).getExpression();
        Properties bindings = ExpressionUtils.createBindings(project);
        
		return new RowVisitor() {
			int 				cellIndex;
			Properties 			bindings;
			List<CellAtRow> 	cellsAtRows;
			Evaluable 			eval;
			
			public RowVisitor init(int cellIndex, Properties bindings, List<CellAtRow> cellsAtRows, Evaluable eval) {
				this.cellIndex = cellIndex;
				this.bindings = bindings;
				this.cellsAtRows = cellsAtRows;
				this.eval = eval;
				return this;
			}
			
			@Override
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
				if (cellIndex < row.cells.size()) {
					Cell cell = row.cells.get(cellIndex);
					if (cell.value != null) {
		                ExpressionUtils.bind(bindings, row, cell);
						
						Cell newCell = new Cell(eval.evaluate(bindings), null);
						
						cellsAtRows.add(new CellAtRow(rowIndex, newCell));
					}
				}
				return false;
			}
		}.init(_baseCellIndex, bindings, cellsAtRows, eval);
	}
}
