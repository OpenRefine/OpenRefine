package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class WrappedRow implements HasFields {
    final public Project project;
    final public int rowIndex;
    final public Row row;
    
    public WrappedRow(Project project, int rowIndex, Row row) {
        this.project = project;
        this.rowIndex = rowIndex;
        this.row = row;
    }
    
    public Object getField(String name, Properties bindings) {
        if ("cells".equals(name)) {
            return new CellTuple(project, row);
        } else if ("index".equals(name)) {
            return rowIndex;
        } else if ("record".equals(name)) {
            int rowIndex = (Integer) bindings.get("rowIndex");
            int recordRowIndex = (row.contextRows != null && row.contextRows.size() > 0) ?
                    row.contextRows.get(0) : rowIndex;
            
            return new Record(recordRowIndex, rowIndex);
        } else if ("columnNames".equals(name)) {
            Project project = (Project) bindings.get("project");
            
            return project.columnModel.getColumnNames();
        } else {
            return row.getField(name, bindings);
        }
    }

    public boolean fieldAlsoHasFields(String name) {
        return row.fieldAlsoHasFields(name);
    }

    protected class Record implements HasFields {
        final int _recordRowIndex;
        final int _currentRowIndex;
        
        protected Record(int recordRowIndex, int currentRowIndex) {
            _recordRowIndex = recordRowIndex;
            _currentRowIndex = currentRowIndex;
        }

        public Object getField(String name, Properties bindings) {
            if ("cells".equals(name)) {
                return new RecordCells(_recordRowIndex);
            }
            return null;
        }

        public boolean fieldAlsoHasFields(String name) {
            return "cells".equals(name);
        }
    }
    
    protected class RecordCells implements HasFields {
        final int _recordRowIndex;
        
        protected RecordCells(int recordRowIndex) {
            _recordRowIndex = recordRowIndex;
        }
        
        public Object getField(String name, Properties bindings) {
            Column column = project.columnModel.getColumnByName(name);
            if (column != null) {
                Row recordRow = project.rows.get(_recordRowIndex);
                int cellIndex = column.getCellIndex();
                
                HasFieldsListImpl cells = new HasFieldsListImpl();
                
                int recordIndex = recordRow.recordIndex;
                int count = project.rows.size();
                for (int r = _recordRowIndex; r < count; r++) {
                    Row row = project.rows.get(r);
                    if (row.recordIndex > recordIndex) {
                        break;
                    }
                    
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                        cells.add(new WrappedCell(project, name, cell));
                    }
                }
                
                return cells;
            }
            return null;
        }

        public boolean fieldAlsoHasFields(String name) {
            return true;
        }
    }
}
