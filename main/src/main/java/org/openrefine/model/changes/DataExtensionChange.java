package org.openrefine.model.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.recon.DataExtensionReconConfig;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.model.recon.ReconType;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.operations.utils.ReconStatsAggregator;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class DataExtensionChange extends EngineDependentChange {
	
	@JsonProperty("baseColumnName")
	protected final String _baseColumnName;
	@JsonProperty("endpoint")
	protected final String _endpoint;
	@JsonProperty("identifierSpace")
	protected final String _identifierSpace;
	@JsonProperty("schemaSpace")
	private final String _schemaSpace;
	@JsonProperty("columnInsertIndex")
	private final int _columnInsertIndex;
	@JsonProperty("columnNames")
	private final List<String> _columnNames;
	@JsonProperty("columnTypes")
	private final List<ReconType> _columnTypes;

	@JsonCreator
	public DataExtensionChange(
			@JsonProperty("engineConfig")
			EngineConfig engineConfig,
			@JsonProperty("baseColumnName")
			String baseColumnName,
			@JsonProperty("endpoint")
			String endpoint,
			@JsonProperty("identifierSpace")
			String identifierSpace,
			@JsonProperty("schemaSpace")
			String schemaSpace,
			@JsonProperty("columnInsertIndex")
			int columnInsertIndex,
			@JsonProperty("columnNames")
			List<String> columnNames,
			@JsonProperty("columnTypes")
			List<ReconType> columnTypes) {
		super(engineConfig);
		_baseColumnName = baseColumnName;
		_endpoint = endpoint;
		_identifierSpace = identifierSpace;
		_schemaSpace = schemaSpace;
		_columnInsertIndex = columnInsertIndex;
		_columnNames = columnNames;
		_columnTypes = columnTypes;
	}

	@Override
	public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
		ChangeData<RecordDataExtension> changeData;
		try {
			changeData = context.getChangeData("extend", new DataExtensionSerializer());
		} catch (IOException e) {
			throw new DoesNotApplyException(String.format("Unable to retrieve change data for data extension"));
		}
		int baseColumnId = projectState.getColumnModel().getColumnIndexByName(_baseColumnName);
		if (baseColumnId == -1) {
			throw new ColumnNotFoundException(_baseColumnName);
		}
		ColumnModel newColumnModel = projectState.getColumnModel();
		for(int i = 0; i != _columnNames.size(); i++) {
			newColumnModel = newColumnModel.insertUnduplicatedColumn(_columnInsertIndex + i, new ColumnMetadata(_columnNames.get(i)));
		}
		RecordChangeDataJoiner<RecordDataExtension> joiner = new DataExtensionJoiner(baseColumnId, _columnInsertIndex, _columnNames.size());
		GridState state = projectState.join(changeData, joiner, newColumnModel);
		
		// Compute recon stats
		List<Integer> columnIndices = IntStream.range(_columnInsertIndex, _columnInsertIndex + _columnNames.size())
				.boxed().collect(Collectors.toList());
		List<ReconConfig> reconConfigs = IntStream.range(_columnInsertIndex, _columnInsertIndex + _columnNames.size())
				.mapToObj(i -> new DataExtensionReconConfig(
						_endpoint,
						_identifierSpace,
						_schemaSpace,
						_columnTypes.get(i)))
				.collect(Collectors.toList());

		return ReconStatsAggregator.updateReconStats(state, columnIndices, reconConfigs);
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
	
    public static class DataExtensionSerializer implements ChangeDataSerializer<RecordDataExtension> {

		private static final long serialVersionUID = -8334190917198142840L;

		@Override
		public String serialize(RecordDataExtension changeDataItem) {
			try {
				return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("Cell serialization failed", e);
			}
		}

		@Override
		public RecordDataExtension deserialize(String serialized) throws IOException {
			return ParsingUtilities.mapper.readValue(serialized, RecordDataExtension.class);
		}
    	
    }
	
    protected static class DataExtensionJoiner implements RecordChangeDataJoiner<RecordDataExtension> {

		private static final long serialVersionUID = 8991393046204795332L;
		private final int baseColumnId;
    	private final int columnInsertId;
    	private final int nbInsertedColumns;
    	
    	protected DataExtensionJoiner(int baseColumnId, int columnInsertId, int nbInsertedColumns) {
    		this.baseColumnId = baseColumnId;
    		this.columnInsertId = columnInsertId;
    		this.nbInsertedColumns = nbInsertedColumns;
    	}

		@Override
		public List<Row> call(Record record, RecordDataExtension changeData) {
			List<Row> newRows = new ArrayList<>();
			List<Row> oldRows = record.getRows();
			Map<Long, DataExtension> extensions = changeData.getExtensions();
			
			for(int i = 0; i != oldRows.size(); i++) {
				Row row = oldRows.get(i);
				long rowId = record.getStartRowId() + i;
				DataExtension extension = extensions.get(rowId);
				if (extension == null) {
					newRows.add(row.insertCells(columnInsertId, Collections.nCopies(nbInsertedColumns, null)));
					continue;
				}
				
				int origRow = i;
				for (List<Cell> extensionRow : extension.data) {
					Row newRow;
					if (origRow == i || (origRow < oldRows.size() && oldRows.get(origRow).isCellBlank(baseColumnId))) {
						newRow = oldRows.get(origRow);
						origRow++;
					} else {
						newRow = new Row(Collections.nCopies(row.getCells().size(), null));
					}
					List<Cell> insertedCells = extensionRow;
					if (insertedCells.size() != nbInsertedColumns) {
						insertedCells = new ArrayList<>(extensionRow);
						insertedCells.addAll(Collections.nCopies(nbInsertedColumns - insertedCells.size(), null));
					}
					newRows.add(newRow.insertCells(columnInsertId, extensionRow));
				}
			}
			return newRows;
		}
    	
    }
	
}