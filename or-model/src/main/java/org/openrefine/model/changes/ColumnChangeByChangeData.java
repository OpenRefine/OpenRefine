package org.openrefine.model.changes;

import java.io.IOException;

import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Adds a new column based on data fetched from an external process.
 * If no column name is supplied, then the change will replace the column
 * at the given index instead.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ColumnChangeByChangeData implements Change {
	
	private final String      _changeDataId;
	private final int         _columnIndex;
	private final String      _columnName;
	
	@JsonCreator
	public ColumnChangeByChangeData(
			@JsonProperty("changeDataId")
			String changeDataId,
			@JsonProperty("columnIndex")
			int columnIndex,
			@JsonProperty("columnName")
			String columnName) {
		_changeDataId = changeDataId;
		_columnIndex = columnIndex;
		_columnName = columnName;
	}
	
	@JsonProperty("changeDataId")
	public String getChangeDataId() {
		return _changeDataId;
	}
	
	@JsonProperty("columnIndex")
	public int getColumnIndex() {
		return _columnIndex;
	}
	
	@JsonProperty("columnName")
	public String getColumnName() {
		return _columnName;
	}

	@Override
	public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
		ChangeData<Cell> changeData = null;
		try {
			changeData = context.getChangeData(_changeDataId, new CellChangeDataSerializer());
		} catch (IOException e) {
			throw new DoesNotApplyException(String.format("Unable to retrieve change data '%s'", _changeDataId));
		}
		RowChangeDataJoiner<Cell> joiner = new Joiner(_columnIndex, _columnName != null);
		ColumnModel columnModel = projectState.getColumnModel();
		if (_columnName != null) {
    		ColumnMetadata column = new ColumnMetadata(_columnName);
    		try {
    			columnModel = projectState.getColumnModel().insertColumn(_columnIndex, column);
    		} catch(ModelException e) {
    			throw new Change.DoesNotApplyException(
    					String.format("A column with name '{}' cannot be added as the name conflicts with an existing column", _columnName));
    		}
		}
		return projectState.join(changeData, joiner, columnModel);
	}

	@Override
	public boolean isImmediate() {
		return false;
	}

	@Override
	public DagSlice getDagSlice() {
		// TODO Auto-generated method stub
		return null;
	}

    public static class Joiner implements RowChangeDataJoiner<Cell> {
        
        private static final long serialVersionUID = 8332780210267820528L;
        private final int _columnIndex;
        private final boolean _add;
        
        public Joiner(int columnIndex, boolean add) {
            _columnIndex = columnIndex;
            _add = add;
        }

        @Override
        public Row call(long rowId, Row row, Cell cell) {
            if (_add) {
                return row.insertCell(_columnIndex, cell);
            } else {
                return row.withCell(_columnIndex, cell);
            }
        }
        
    }
	
}