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

package org.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.expr.ExpressionUtils;

public class RecordModel {

    final static Logger logger = LoggerFactory.getLogger("RecordModel");

    final static public class CellDependency {

        final public int rowIndex;
        final public int cellIndex;

        public CellDependency(int rowIndex, int cellIndex) {
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
        }

        @Override
        public String toString() {
            return rowIndex + "," + cellIndex;
        }
    }

    final static public class RowDependency {

        public int recordIndex;
        public CellDependency[] cellDependencies;
        public List<Integer> contextRows;

        @Override
        public String toString() {
            return "Idx: " + recordIndex + " CellDeps: " + Arrays.toString(cellDependencies) + " Rows:" + contextRows;
        }
    }

    protected List<RowDependency> _rowDependencies;
    protected List<Record> _records;

    public RowDependency getRowDependency(int rowIndex) {
        return _rowDependencies != null && rowIndex >= 0 && rowIndex < _rowDependencies.size() ? _rowDependencies.get(rowIndex) : null;
    }

    @JsonIgnore
    public int getRecordCount() {
        return _records.size();
    }

    public Record getRecord(int recordIndex) {
        return _records != null && recordIndex >= 0 && recordIndex < _records.size() ? _records.get(recordIndex) : null;
    }

    public Record getRecordOfRow(int rowIndex) {
        RowDependency rd = getRowDependency(rowIndex);
        if (rd != null) {
            if (rd.recordIndex < 0) {
                rd = getRowDependency(rd.contextRows.get(0));
            }
            return getRecord(rd.recordIndex);
        }
        return null;
    }

    @JsonProperty("hasRecords")
    public boolean hasRecords() {
        return _records != null && _rowDependencies != null &&
                _records.size() < _rowDependencies.size();
    }

    static protected class KeyedGroup {

        int[] cellIndices;
        int keyCellIndex;

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i : cellIndices) {
                sb.append(i).append(',');
            }
            return "key: " + keyCellIndex + " cells: " + sb.toString();
        }
    }

    synchronized public void update(Project project) {
        synchronized (project) {
            List<Row> rows = project.rows;
            int rowCount = rows.size();

            ColumnModel columnModel = project.columnModel;
            List<KeyedGroup> keyedGroups = computeKeyedGroups(columnModel);
            int groupCount = keyedGroups.size();

            int[] lastNonBlankRowsByGroup = new int[keyedGroups.size()];
            for (int i = 0; i < lastNonBlankRowsByGroup.length; i++) {
                lastNonBlankRowsByGroup[i] = -1;
            }

            _rowDependencies = new ArrayList<RowDependency>(rowCount);

            int recordIndex = 0;
            for (int r = 0; r < rowCount; r++) {
                Row row = rows.get(r);
                RowDependency rowDependency = new RowDependency();

                for (int g = 0; g < groupCount; g++) {
                    KeyedGroup group = keyedGroups.get(g);

                    if (!ExpressionUtils.isNonBlankData(row.getCellValue(keyedGroups.get(0).keyCellIndex)) &&
                            !ExpressionUtils.isNonBlankData(row.getCellValue(group.keyCellIndex))) {
                        int contextRowIndex = lastNonBlankRowsByGroup[g];
                        if (contextRowIndex >= 0) {
                            for (int dependentCellIndex : group.cellIndices) {
                                if (ExpressionUtils.isNonBlankData(row.getCellValue(dependentCellIndex))) {
                                    setRowDependency(
                                            project,
                                            rowDependency,
                                            dependentCellIndex,
                                            contextRowIndex,
                                            group.keyCellIndex);
                                }
                            }
                        }
                    } else {
                        lastNonBlankRowsByGroup[g] = r;
                    }
                }

                if (rowDependency.cellDependencies != null && rowDependency.cellDependencies.length > 0) {
                    rowDependency.recordIndex = -1;
                    rowDependency.contextRows = new ArrayList<Integer>();
                    for (CellDependency cd : rowDependency.cellDependencies) {
                        if (cd != null) {
                            rowDependency.contextRows.add(cd.rowIndex);
                        }
                    }
                    Collections.sort(rowDependency.contextRows);
                } else {
                    rowDependency.recordIndex = recordIndex++;
                }

                _rowDependencies.add(rowDependency);
            }

            _records = new ArrayList<Record>(recordIndex);
            if (recordIndex > 0) {
                recordIndex = 0;

                int recordRowIndex = 0;
                for (int r = 1; r < rowCount; r++) {
                    RowDependency rd = _rowDependencies.get(r);
                    if (rd.recordIndex >= 0) {
                        _records.add(new Record(recordRowIndex, r, recordIndex++));

                        recordIndex = rd.recordIndex;
                        recordRowIndex = r;
                    }
                }

                _records.add(new Record(recordRowIndex, rowCount, recordIndex++));
            }
        }
    }

    protected List<KeyedGroup> computeKeyedGroups(ColumnModel columnModel) {
        List<KeyedGroup> keyedGroups = new ArrayList<KeyedGroup>();

        int count = columnModel.getMaxCellIndex() + 1;
        if (count > 0 && columnModel.getKeyColumnIndex() < columnModel.columns.size()) {
            KeyedGroup rootKeyedGroup = new KeyedGroup();

            rootKeyedGroup.cellIndices = new int[count - 1];
            rootKeyedGroup.keyCellIndex = columnModel.columns.get(columnModel.getKeyColumnIndex()).getCellIndex();

            for (int i = 0; i < count; i++) {
                if (i < rootKeyedGroup.keyCellIndex) {
                    rootKeyedGroup.cellIndices[i] = i;
                } else if (i > rootKeyedGroup.keyCellIndex) {
                    rootKeyedGroup.cellIndices[i - 1] = i;
                }
            }
            keyedGroups.add(rootKeyedGroup);
        }

        return keyedGroups;
    }

    protected void setRowDependency(
            Project project,
            RowDependency rowDependency,
            int cellIndex,
            int contextRowIndex,
            int contextCellIndex) {
        if (rowDependency.cellDependencies == null) {
            int count = project.columnModel.getMaxCellIndex() + 1;

            rowDependency.cellDependencies = new CellDependency[count];
        }

        rowDependency.cellDependencies[cellIndex] = new CellDependency(contextRowIndex, contextCellIndex);
    }

}
