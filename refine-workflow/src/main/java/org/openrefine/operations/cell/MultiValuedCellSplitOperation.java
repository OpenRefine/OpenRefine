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

package org.openrefine.operations.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.column.ColumnSplitOperation.Mode;
import org.openrefine.operations.utils.CellValueSplitter;

/**
 * Splits the value of a cell and spreads the splits on the following rows, while respecting the record structure. The
 * keyColumnName can be used to specify which column should be treated as record key (although this parameter has never
 * been exposed in the UI as of 2020-05).
 */
public class MultiValuedCellSplitOperation implements Operation {

    final protected String _columnName;
    final protected String _keyColumnName;
    final protected Mode _mode;
    final protected String _separator;
    final protected Boolean _regex;

    final protected int[] _fieldLengths;

    @JsonCreator
    public static MultiValuedCellSplitOperation deserialize(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") boolean regex,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if (Mode.Separator.equals(mode)) {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    separator,
                    regex);
        } else {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    fieldLengths);
        }
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            String separator,
            boolean regex) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;
        _separator = separator;
        _mode = Mode.Separator;
        _regex = regex;

        _fieldLengths = null;
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            int[] fieldLengths) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;

        _mode = Mode.Lengths;
        _separator = null;
        _regex = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
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

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    public String getDescription() {
        return "Split multi-valued cells in column " + _columnName;
    }

    @Override
    public Change createChange() throws ParsingException {
        return new MultiValuedCellSplitChange();
    }

    public class MultiValuedCellSplitChange implements Change {

        private final CellValueSplitter splitter;

        public MultiValuedCellSplitChange() {
            this.splitter = CellValueSplitter.construct(_mode, _separator, _regex, _fieldLengths, null);
        }

        @Override
        public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
            ColumnModel columnModel = projectState.getColumnModel();
            int columnIdx = columnModel.getColumnIndexByName(_columnName);
            if (columnIdx == -1) {
                throw new DoesNotApplyException(
                        String.format("Column '%s' does not exist", _columnName));
            }
            int keyColumnIdx = _keyColumnName == null ? 0 : columnModel.getColumnIndexByName(_keyColumnName);
            if (keyColumnIdx == -1) {
                throw new DoesNotApplyException(
                        String.format("Key column '%s' does not exist", _keyColumnName));
            }
            if (keyColumnIdx != columnModel.getKeyColumnIndex()) {
                projectState = projectState.withColumnModel(columnModel.withKeyColumnIndex(keyColumnIdx));
            }
            return projectState.mapRecords(recordMapper(columnIdx, splitter), columnModel);
        }

        @Override
        public boolean isImmediate() {
            return true;
        }

    }

    protected static RecordMapper recordMapper(int columnIdx, CellValueSplitter splitter) {
        return new RecordMapper() {

            private static final long serialVersionUID = -6481651649785541256L;

            @Override
            public List<Row> call(Record record) {
                List<String> splits = Collections.emptyList();
                List<Row> origRows = record.getRows();
                List<Row> newRows = new ArrayList<>(origRows.size());
                int rowSize = 0;

                for (int i = 0; i != origRows.size(); i++) {
                    Row row = origRows.get(i);
                    rowSize = row.getCells().size();

                    if (row.isCellBlank(columnIdx)) {
                        if (!splits.isEmpty()) {
                            // We have space to add an accumulated split value
                            String splitValue = splits.remove(0);
                            newRows.add(row.withCell(columnIdx, new Cell(splitValue, null)));
                        } else {
                            // We leave the row as it is
                            newRows.add(row);
                        }
                    } else {
                        // We have a non-empty value to split

                        // We need to exhaust the queue of splits to insert first
                        for (String splitValue : splits) {
                            Row newRow = new Row(Collections.nCopies(rowSize, (Cell) null));
                            newRows.add(newRow.withCell(columnIdx, new Cell(splitValue, null)));
                        }
                        splits = Collections.emptyList();

                        // Split the current value
                        Serializable cellValue = origRows.get(i).getCellValue(columnIdx);
                        if (!(cellValue instanceof String)) {
                            newRows.add(row);
                        } else {
                            // we use a linked list because we pop elements one by one later on
                            splits = new LinkedList<>(splitter.split((String) cellValue));
                            Cell firstSplit = null;
                            if (!splits.isEmpty()) {
                                firstSplit = new Cell(splits.remove(0), null);
                            }
                            newRows.add(row.withCell(columnIdx, firstSplit));
                        }
                    }
                }

                //  Exhaust any remaining splits at the end
                for (String splitValue : splits) {
                    Row newRow = new Row(Collections.nCopies(rowSize, (Cell) null));
                    newRows.add(newRow.withCell(columnIdx, new Cell(splitValue, null)));
                }

                return newRows;
            }

            @Override
            public boolean preservesRecordStructure() {
                return false;
            }

        };
    }
}
