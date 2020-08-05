package org.openrefine.model.changes;

import java.io.IOException;

import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ColumnAdditionByChangeData implements Change {
	
	private final String      _changeDataId;
	private final int         _columnIndex;
	private final String      _columnName;
	
	@JsonCreator
	public ColumnAdditionByChangeData(
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
		RowChangeDataJoiner<Cell> joiner = new Joiner(_columnIndex);
		ColumnModel columnModel;
		ColumnMetadata column = new ColumnMetadata(_columnName);
		try {
			columnModel = projectState.getColumnModel().insertColumn(_columnIndex, column);
		} catch(ModelException e) {
			throw new Change.DoesNotApplyException(
					String.format("A column with name '{}' cannot be added as the name conflicts with an existing column", _columnName));
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

    public static class CellChangeDataSerializer implements ChangeDataSerializer<Cell> {

        private static final long serialVersionUID = 606360403156779037L;

        @Override
        public String serialize(Cell changeDataItem) {
            try {
                return ParsingUtilities.mapper.writeValueAsString(changeDataItem);
            } catch (JsonProcessingException e) {
                // does not happen, Cells are always serializable
                return null;
            }
        }

        @Override
        public Cell deserialize(String serialized) throws IOException {
            return ParsingUtilities.mapper.readValue(serialized, Cell.class);
        }
        
    }
	
    public static class Joiner implements RowChangeDataJoiner<Cell> {
        
        private static final long serialVersionUID = 8332780210267820528L;
        private final int _columnIndex;
        
        public Joiner(int columnIndex) {
            _columnIndex = columnIndex;
        }

        @Override
        public Row call(long rowId, Row row, Cell cell) {
            return row.insertCell(_columnIndex, cell);
        }
        
    }
	
}