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

package com.google.refine.expr;

import java.util.Properties;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

public class WrappedRow implements HasFields {

    final public Project project;
    final public int rowIndex;
    final public Row row;

    public WrappedRow(Project project, int rowIndex, Row row) {
        this.project = project;
        this.rowIndex = rowIndex;
        this.row = row;
    }

    @Override
    public Object getField(String name, Properties bindings) {
        if ("cells".equals(name)) {
            return new CellTuple(project, row);
        } else if ("index".equals(name)) {
            return rowIndex;
        } else if ("record".equals(name)) {
            int rowIndex = (Integer) bindings.get("rowIndex");

            return new WrappedRecord(project.recordModel.getRecordOfRow(rowIndex));
        } else if ("columnNames".equals(name)) {
            Project project = (Project) bindings.get("project");

            return project.columnModel.getColumnNames();
        } else {
            return row.getField(name, bindings);
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
        public Object getField(String name, Properties bindings) {
            if ("cells".equals(name)) {
                return new RecordCells(_record);
            } else if ("index".equals(name)) {
                return _record.recordIndex;
            } else if ("fromRowIndex".equals(name)) {
                return _record.fromRowIndex;
            } else if ("toRowIndex".equals(name)) {
                return _record.toRowIndex;
            } else if ("rowCount".equals(name)) {
                return _record.toRowIndex - _record.fromRowIndex;
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
        public Object getField(String name, Properties bindings) {
            Column column = project.columnModel.getColumnByName(name);
            if (column != null) {
                int cellIndex = column.getCellIndex();

                HasFieldsListImpl cells = new HasFieldsListImpl();
                for (int r = _record.fromRowIndex; r < _record.toRowIndex; r++) {
                    Row row = project.rows.get(r);
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                        cells.add(new WrappedCell(project, name, cell));
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
