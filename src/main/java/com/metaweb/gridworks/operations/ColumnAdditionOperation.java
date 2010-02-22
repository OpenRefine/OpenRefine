package com.metaweb.gridworks.operations;

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
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellAtRow;
import com.metaweb.gridworks.model.changes.ColumnAdditionChange;

public class ColumnAdditionOperation extends EngineDependentOperation {
	private static final long serialVersionUID = -5672677479629932356L;

	final protected String	_baseColumnName;
	final protected String 	_expression;
	
	final protected String 	_headerLabel;
	final protected int		_columnInsertIndex;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ColumnAdditionOperation(
            engineConfig,
            obj.getString("baseColumnName"),
            obj.getString("expression"),
            obj.getString("headerLabel"),
            obj.getInt("columnInsertIndex")
        );
    }
    
	public ColumnAdditionOperation(
		JSONObject 	engineConfig,
		String		baseColumnName,
		String 		expression,
		String 		headerLabel, 
		int 		columnInsertIndex 
	) {
		super(engineConfig);
		
		_baseColumnName = baseColumnName;
		_expression = expression;
		
		_headerLabel = headerLabel;
		_columnInsertIndex = columnInsertIndex;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
		writer.key("description"); writer.value(getBriefDescription());
		writer.key("engineConfig"); writer.value(getEngineConfig());
		writer.key("headerLabel"); writer.value(_headerLabel);
		writer.key("columnInsertIndex"); writer.value(_columnInsertIndex);
		writer.key("baseColumnName"); writer.value(_baseColumnName);
		writer.key("expression"); writer.value(_expression);
		writer.endObject();
	}

	protected String getBriefDescription() {
        return "Create column " + _headerLabel + 
            " at index " + _columnInsertIndex + 
            " based on column " + _baseColumnName + 
            " using expression " + _expression;
	}

	protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
		return "Create new column " + _headerLabel + 
			" based on column " + column.getHeaderLabel() + 
			" by filling " + cellsAtRows.size() +
			" rows with " + _expression;
	}
	
    protected HistoryEntry createHistoryEntry(Project project) throws Exception {
        Engine engine = createEngine(project);
        
        Column column = project.columnModel.getColumnByName(_baseColumnName);
        if (column == null) {
            throw new Exception("No column named " + _baseColumnName);
        }
        
        List<CellAtRow> cellsAtRows = new ArrayList<CellAtRow>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
        filteredRows.accept(project, createRowVisitor(project, cellsAtRows));
        
        String description = createDescription(column, cellsAtRows);
        
        Change change = new ColumnAdditionChange(_headerLabel, _columnInsertIndex, cellsAtRows);
        
        return new HistoryEntry(
            project, description, this, change);
    }

	protected RowVisitor createRowVisitor(Project project, List<CellAtRow> cellsAtRows) throws Exception {
		Column column = project.columnModel.getColumnByName(_baseColumnName);
		
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
			
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
				Cell cell = row.getCell(cellIndex);

                ExpressionUtils.bind(bindings, row, cell);
				
                Object v = eval.evaluate(bindings);
                if (v != null) {
                    Cell newCell = new Cell(v, null);
				
                    cellsAtRows.add(new CellAtRow(rowIndex, newCell));
                }
                
				return false;
			}
		}.init(column.getCellIndex(), bindings, cellsAtRows, eval);
	}
}
