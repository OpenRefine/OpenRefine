/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.operations.utils.CellValueSplitter;

public class ColumnSplitOperation extends RowMapOperation {

    public static enum Mode {
        @JsonProperty("lengths")
        Lengths, @JsonProperty("separator")
        Separator
    }

    final protected String _columnName;
    final protected boolean _guessCellType;
    final protected boolean _removeOriginalColumn;
    final protected Mode _mode;

    final protected String _separator;
    final protected Boolean _regex;
    final protected Integer _maxColumns;

    final protected int[] _fieldLengths;

    // initialized lazily
    protected CellValueSplitter _splitter = null;

    @JsonCreator
    public static ColumnSplitOperation deserialize(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("guessCellType") boolean guessCellType,
            @JsonProperty("removeOriginalColumn") boolean removeOriginalColumn,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") Boolean regex,
            @JsonProperty("maxColumns") Integer maxColumns,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if (Mode.Separator.equals(mode)) {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    separator,
                    regex,
                    maxColumns);
        } else {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    fieldLengths);
        }
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            String separator,
            boolean regex,
            int maxColumns) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = Mode.Separator;
        _separator = separator;
        _regex = regex;
        _maxColumns = maxColumns;

        _fieldLengths = null;
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            int[] fieldLengths) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = Mode.Lengths;
        _separator = null;
        _regex = null;
        _maxColumns = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("guessCellType")
    public boolean getGuessCellType() {
        return _guessCellType;
    }

    @JsonProperty("removeOriginalColumn")
    public boolean getRemoveOriginalColumn() {
        return _removeOriginalColumn;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }

    @JsonProperty("separator")
    @JsonInclude(Include.NON_NULL)
    public String getSeparator() {
        return _separator;
    }

    @JsonProperty("regex")
    @JsonInclude(Include.NON_NULL)
    public Boolean getRegex() {
        return _regex;
    }

    @JsonProperty("maxColumns")
    @JsonInclude(Include.NON_NULL)
    public Integer getMaxColumns() {
        return _maxColumns;
    }

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    public String getDescription() {
        return "Split column " + _columnName +
                (Mode.Separator.equals(_mode) ? " by separator" : " by field lengths");
    }

    protected CellValueSplitter getSplitter() {
        if (_splitter == null) {
            _splitter = CellValueSplitter.construct(_mode, _separator, _regex, _fieldLengths, _maxColumns);
        }
        return _splitter;
    }

    @Override
    public GridMap getGridMap(Grid state, ChangeContext context) throws OperationException {
        ColumnModel origColumnModel = state.getColumnModel();
        int origColumnIdx = origColumnModel.getRequiredColumnIndex(_columnName);

        // Create an aggregator which counts the number of columns generated
        // by the splitting settings.
        Engine engine = getEngine(state, context.getProjectId());
        int nbColumns = engine.aggregateFilteredRows(buildAggregator(getSplitter(), origColumnIdx), 0);
        if (_maxColumns != null && _maxColumns > 0) {
            nbColumns = Math.min(nbColumns, _maxColumns);
        }

        // Build new column model
        List<String> columnNames = new ArrayList<>(nbColumns);
        int columnNameIndex = 1;
        for (int i = 0; i < nbColumns; i++) {
            while (true) {
                String newColumnName = _columnName + " " + columnNameIndex++;
                if (origColumnModel.getColumnByName(newColumnName) == null) {
                    columnNames.add(newColumnName);
                    break;
                }
            }
        }

        int startColumnIdx = _removeOriginalColumn ? origColumnIdx : origColumnIdx + 1;
        List<ColumnMetadata> origColumns = origColumnModel.getColumns();
        List<ColumnMetadata> newColumns = new ArrayList<>(origColumns.subList(0, startColumnIdx));
        newColumns.addAll(columnNames.stream().map(n -> new ColumnMetadata(n)).collect(Collectors.toList()));
        newColumns.addAll(origColumns.subList(origColumnIdx + 1, origColumns.size()));
        ColumnModel newColumnModel = new ColumnModel(newColumns);

        return new GridMap(
                newColumnModel,
                mapper(getSplitter(), origColumnIdx, nbColumns, _removeOriginalColumn, _guessCellType, origColumnModel.getKeyColumnIndex()),
                negativeMapper(origColumnIdx, nbColumns, _removeOriginalColumn, origColumnModel.getKeyColumnIndex()),
                state.getOverlayModels());
    }

    // for visibility in tests
    @Override
    protected Engine getEngine(Grid grid, long projectId) {
        return super.getEngine(grid, projectId);
    }

    protected static RowInRecordMapper negativeMapper(int columnIdx, int nbColumns, boolean removeOrigColumn, int keyColumnIdx) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 467330557649346821L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Row newRow = row.insertCells(columnIdx + 1, Collections.nCopies(nbColumns, null));
                if (removeOrigColumn) {
                    newRow = newRow.removeCell(columnIdx);
                }
                return newRow;
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO adapt for cases where the key column idx is not 0 and is adjusted after the operation
                return columnIdx > keyColumnIdx || (columnIdx == keyColumnIdx && !removeOrigColumn);
            }

        };
    }

    protected static RowInRecordMapper mapper(CellValueSplitter splitter, int columnIdx, int nbColumns, boolean removeOrigColumn,
            boolean guessCellType, int keyColumnIndex) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -5552242219011530334L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                // Split the cell
                Serializable value = row.getCellValue(columnIdx);
                List<String> split;
                if (!(value instanceof String)) {
                    split = Collections.emptyList();
                } else {
                    split = splitter.split((String) value);
                }

                // Insert the split values in the row
                List<Cell> splitCells = new ArrayList<>(nbColumns);
                for (int i = 0; i != nbColumns; i++) {
                    Serializable cellValue = null;
                    if (i < split.size()) {
                        cellValue = guessCellType ? ImporterUtilities.parseCellValue(split.get(i)) : split.get(i);
                    }
                    splitCells.add(new Cell(cellValue, null));
                }

                Row newRow = row.insertCells(columnIdx + 1, splitCells);
                if (removeOrigColumn) {
                    newRow = newRow.removeCell(columnIdx);
                }
                return newRow;
            }

            @Override
            public boolean preservesRecordStructure() {
                return keyColumnIndex < columnIdx || (keyColumnIndex == columnIdx && !removeOrigColumn);
            }

        };
    }

    /**
     * Aggregator to compute the maximum number of values generated by a splitting configuration
     * 
     * @author Antonin Delpeuch
     *
     */
    protected static RowAggregator<Integer> buildAggregator(CellValueSplitter splitter, int columnIndex) {
        return new RowAggregator<Integer>() {

            private static final long serialVersionUID = -5885231185365433813L;

            @Override
            public Integer sum(Integer first, Integer second) {
                return Math.max(first, second);
            }

            @Override
            public Integer withRow(Integer state, long rowId, Row row) {
                Object val = row.getCellValue(columnIndex);
                if (!(val instanceof String)) {
                    return state;
                } else {
                    List<String> splits = splitter.split((String) val);
                    if (state < splits.size()) {
                        return splits.size();
                    } else {
                        return state;
                    }
                }
            }
        };
    }

}
