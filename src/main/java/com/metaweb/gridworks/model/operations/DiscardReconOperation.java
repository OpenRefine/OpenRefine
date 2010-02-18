package com.metaweb.gridworks.model.operations;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;

public class DiscardReconOperation extends EngineDependentMassCellOperation {
	private static final long serialVersionUID = 6799029731665369179L;

	public DiscardReconOperation(JSONObject engineConfig, String columnName) {
		super(engineConfig, columnName, false);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("op"); writer.value("discard-recon");
		writer.key("description"); writer.value("Discard recon matches in column " + _columnName);
		writer.key("engineConfig"); writer.value(getEngineConfig());
		writer.key("columnName"); writer.value(_columnName);
		writer.endObject();
	}

	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		
		return "Discard recon results for " + cellChanges.size() + 
			" cells in column " + column.getHeaderLabel();
	}

	protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);
		
		return new RowVisitor() {
			int cellIndex;
			List<CellChange> cellChanges;
			
			public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
				this.cellIndex = cellIndex;
				this.cellChanges = cellChanges;
				return this;
			}
			
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
				if (cellIndex < row.cells.size()) {
					Cell cell = row.cells.get(cellIndex);

					Cell newCell = new Cell(cell.value, null);
					
					CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
					cellChanges.add(cellChange);
				}
				return false;
			}
		}.init(column.getCellIndex(), cellChanges);
	}
}
