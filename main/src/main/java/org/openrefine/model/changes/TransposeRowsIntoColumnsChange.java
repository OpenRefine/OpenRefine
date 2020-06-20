package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.history.Change;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowBuilder;
import org.openrefine.model.RowFilter;

public class TransposeRowsIntoColumnsChange implements Change {
	
    final protected String  _columnName;
    final protected int     _rowCount;

    public TransposeRowsIntoColumnsChange(
        String  columnName,
        int     rowCount
    ) {
        _columnName = columnName;
        _rowCount = rowCount;
    }

	@Override
	public GridState apply(GridState projectState) throws DoesNotApplyException {
        ColumnModel columnModel = projectState.getColumnModel();
        ColumnModel newColumns = new ColumnModel(Collections.emptyList());
        List<ColumnMetadata> oldColumns = columnModel.getColumns();
        
        int columnIndex = columnModel.getColumnIndexByName(_columnName);
        if (columnIndex == -1) {
        	throw new DoesNotApplyException(String.format("Column %s could not be found", _columnName));
        }
        int columnCount = oldColumns.size();
        
        for (int i = 0; i < columnCount; i++) {           
            if (i == columnIndex) {
                int newIndex = 1;
                for (int n = 0; n < _rowCount; n++) {
                    String columnName = _columnName + " " + newIndex++;
                    while (columnModel.getColumnByName(columnName) != null) {
                        columnName = _columnName + " " + newIndex++;
                    }
                    newColumns = newColumns.appendUnduplicatedColumn(new ColumnMetadata(columnName));
                }
            } else {
            	newColumns = newColumns.appendUnduplicatedColumn(oldColumns.get(i));
            }
        }
        
        int nbNewColumns = newColumns.getColumns().size();
        RowBuilder firstNewRow = null;
        List<RowBuilder> newRows = new ArrayList<>();
        for (IndexedRow indexedRow : projectState.iterateRows(RowFilter.ANY_ROW)) {
        	long r = indexedRow.getIndex();
        	int r2 = (int)(r % (long)_rowCount);
        	
        	RowBuilder newRow = RowBuilder.create(nbNewColumns);
        	newRows.add(newRow);
        	if (r2 == 0) {
        		firstNewRow = newRow;
        	}
            
            Row oldRow = indexedRow.getRow();
            
            for (int c = 0; c < oldColumns.size(); c++) {
                Cell cell = oldRow.getCell(c);
                
                if (cell != null && cell.value != null) {
                    if (c == columnIndex) {
                        firstNewRow.withCell(columnIndex + r2, cell);
                    } else if (c < columnIndex) {
                        newRow.withCell(c, cell);
                    } else {
                        newRow.withCell(c + _rowCount - 1, cell);
                    }
                }
            }
        }
        
        List<Row> rows = newRows.stream()
        		.map(rb -> rb.build(nbNewColumns))
        		.filter(row -> !row.isEmpty())
        		.collect(Collectors.toList());
		
        return projectState.getDatamodelRunner().create(newColumns, rows, projectState.getOverlayModels());
	}

	@Override
	public boolean isImmediate() {
		return true;
	}

	@Override
	public DagSlice getDagSlice() {
		// TODO Auto-generated method stub
		return null;
	}

}
