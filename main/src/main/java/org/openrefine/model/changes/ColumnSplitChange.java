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

package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

public class ColumnSplitChange extends RowMapChange {

    public static enum Mode {
        @JsonProperty("lengths")
        Lengths, @JsonProperty("separator")
        Separator
    }

    final protected String _columnName;
    final protected boolean _removeOriginalColumn;
    final protected CellValueSplitter _splitter;
    final protected boolean _guessCellType;
    final protected Mode _mode;
    final protected String _separator;
    final protected Boolean _regex;
    final protected Integer _maxColumns;
    final protected int[] _fieldLengths;

    @JsonCreator
    public ColumnSplitChange(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") Boolean regex,
            @JsonProperty("maxColumns") Integer maxColumns,
            @JsonProperty("fieldLengths") int[] fieldLengths,
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("removeOriginalColumn") boolean removeOriginalColumn,
            @JsonProperty("guessCellType") boolean guessCellType) {
        super(engineConfig);
        _columnName = columnName;
        _mode = mode;
        _separator = separator;
        _regex = regex;
        _maxColumns = maxColumns;
        _fieldLengths = fieldLengths;
        _removeOriginalColumn = removeOriginalColumn;
        _guessCellType = guessCellType;
        _splitter = CellValueSplitter.construct(mode, separator, regex, fieldLengths, maxColumns);
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
    public boolean isImmediate() {
        return true;
    }

    @Override
    public GridMap getGridMap(GridState state) throws DoesNotApplyException {
        ColumnModel origColumnModel = state.getColumnModel();
        int origColumnIdx = columnIndex(origColumnModel, _columnName);

        // Create an aggregator which counts the number of columns generated
        // by the splitting settings.
        Engine engine = getEngine(state);
        int nbColumns = engine.aggregateFilteredRows(buildAggregator(_splitter, origColumnIdx), 0);
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
                mapper(_splitter, origColumnIdx, nbColumns, _removeOriginalColumn, _guessCellType),
                negativeMapper(origColumnIdx, nbColumns, _removeOriginalColumn),
                state.getOverlayModels());
    }

    protected static RowMapper negativeMapper(int columnIdx, int nbColumns, boolean removeOrigColumn) {
        return new RowMapper() {

            private static final long serialVersionUID = 467330557649346821L;

            @Override
            public Row call(long rowId, Row row) {
                Row newRow = row.insertCells(columnIdx + 1, Collections.nCopies(nbColumns, null));
                if (removeOrigColumn) {
                    newRow = newRow.removeCell(columnIdx);
                }
                return newRow;
            }

        };
    }

    protected static RowMapper mapper(CellValueSplitter splitter, int columnIdx, int nbColumns, boolean removeOrigColumn,
            boolean guessCellType) {
        return new RowMapper() {

            private static final long serialVersionUID = -5552242219011530334L;

            @Override
            public Row call(long rowId, Row row) {
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

        };
    }

    /**
     * Provides different ways to split a cell value into multiple strings.
     * 
     * @author Antonin Delpeuch
     *
     */
    public interface CellValueSplitter extends Serializable {

        public List<String> split(String source);

        /**
         * Constructs a CellValueSplitter according to the supplied settings.
         * 
         * @param mode
         *            whether to split by separator or fixed lengths
         * @param separator
         *            the separator to use (set to null if using lengths)
         * @param regex
         *            whether to interpret the separator as a regular expression
         * @param fieldLengths
         *            the lengths of the fields to extract (set to null if using a separator)
         * @param maxColumns
         *            the maximum number of values to extract (ignored for lengths)
         */
        public static CellValueSplitter construct(Mode mode, String separator, Boolean regex, int[] fieldLengths,
                Integer maxColumns) {
            if (Mode.Lengths.equals(mode)) {
                return CellValueSplitter.splitByLengths(fieldLengths);
            } else {
                if (regex) {
                    Pattern pattern = Pattern.compile(separator);
                    return CellValueSplitter.splitByRegex(pattern, maxColumns == null ? 0 : maxColumns);
                } else {
                    return CellValueSplitter.splitBySeparator(separator, maxColumns == null ? 0 : maxColumns);
                }
            }
        }

        public static CellValueSplitter splitByLengths(int[] lengths) {
            return new CellValueSplitter() {

                private static final long serialVersionUID = -8087516195285863794L;

                @Override
                public List<String> split(String source) {
                    List<String> results = new ArrayList<>(lengths.length + 1);

                    int lastIndex = 0;
                    for (int length : lengths) {
                        int from = lastIndex;
                        int to = Math.min(from + length, source.length());

                        results.add(source.substring(from, to));

                        lastIndex = to;
                    }

                    return results;
                }

            };
        }

        public static CellValueSplitter splitBySeparator(String separator, int maxColumns) {
            return new CellValueSplitter() {

                private static final long serialVersionUID = 5678119132735565975L;

                @Override
                public List<String> split(String source) {
                    return Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(source, separator, 0));
                }

            };
        }

        public static CellValueSplitter splitByRegex(Pattern regex, int maxColumns) {
            return new CellValueSplitter() {

                private static final long serialVersionUID = -6979838040900570895L;

                @Override
                public List<String> split(String source) {
                    String[] split = regex.split(source);
                    return Arrays.asList(split);
                }

            };

        }
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
