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

package org.openrefine.expr;

import java.util.List;

import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

public class WrappedRow implements HasFields {

    final public ColumnModel columnModel;
    final public long rowIndex;
    final public Row row;
    final public Record record;

    /**
     * Constructor to be used when the wrapped row is used in rows mode, where no record is available.
     * 
     * @param columnModel
     * @param rowIndex
     * @param row
     */
    public WrappedRow(ColumnModel columnModel, long rowIndex, Row row) {
        this.columnModel = columnModel;
        this.rowIndex = rowIndex;
        this.row = row;
        this.record = null;
    }

    /**
     * Constructor to be used when the wrapped row is used in records mode, when the enclosing record is available.
     * 
     * @param columnModel
     * @param rowIndex
     * @param row
     * @param record
     */
    public WrappedRow(ColumnModel columnModel, long rowIndex, Row row, Record record) {
        this.columnModel = columnModel;
        this.rowIndex = rowIndex;
        this.row = row;
        this.record = record;
    }

    @Override
    public Object getField(String name) {
        if ("cells".equals(name)) {
            return new CellTuple(columnModel, row);
        } else if ("index".equals(name)) {
            return rowIndex;
        } else if ("record".equals(name)) {
            if (record == null) {
                return null;
            } else {
                return new WrappedRecord(record);
            }
        } else if ("columnNames".equals(name)) {
            return columnModel.getColumnNames();
        } else {
            return row.getField(name);
        }
    }

    @Override
    public boolean fieldAlsoHasFields(String name) {
        return row.fieldAlsoHasFields(name);
    }

    protected class WrappedRecord implements HasFields {

        final Record _record;

        protected WrappedRecord(Record record) {
            _record = record;
        }

        @Override
        public Object getField(String name) {
            if ("cells".equals(name)) {
                return new RecordCells(_record);
            } else if ("index".equals(name)) {
                // TODO remove this field or reimplement it (which comes at performance costs)
                return 0;
            } else if ("fromRowIndex".equals(name)) {
                return _record.getStartRowId();
            } else if ("toRowIndex".equals(name)) {
                return _record.getEndRowId();
            } else if ("rowCount".equals(name)) {
                return _record.size();
            }
            return null;
        }

        @Override
        public boolean fieldAlsoHasFields(String name) {
            return "cells".equals(name);
        }
    }

    protected class RecordCells implements HasFields {

        final Record _record;

        protected RecordCells(Record record) {
            _record = record;
        }

        @Override
        public Object getField(String name) {
            int columnIndex = columnModel.getColumnIndexByName(name);
            if (columnIndex != -1) {

                HasFieldsListImpl cells = new HasFieldsListImpl();
                List<Row> rows = _record.getRows();
                for (int r = 0; r < rows.size(); r++) {
                    Row row = rows.get(r);
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                        cells.add(new WrappedCell(name, cell));
                    }
                }

                return cells;
            }
            return null;
        }

        @Override
        public boolean fieldAlsoHasFields(String name) {
            return true;
        }
    }
}
