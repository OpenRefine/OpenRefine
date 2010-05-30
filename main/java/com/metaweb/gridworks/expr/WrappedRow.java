package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
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
            
            return new WrappedRecord(project.recordModel.getRecordOfRow(rowIndex));
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

    protected class WrappedRecord implements HasFields {
    	final Record _record;
        
        protected WrappedRecord(Record record) {
        	_record = record;
        }

        public Object getField(String name, Properties bindings) {
            if ("cells".equals(name)) {
                return new RecordCells(_record);
            }
            return null;
        }

        public boolean fieldAlsoHasFields(String name) {
            return "cells".equals(name);
        }
    }
    
    protected class RecordCells implements HasFields {
        final Record _record;
        
        protected RecordCells(Record record) {
            _record = record;
        }
        
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

        public boolean fieldAlsoHasFields(String name) {
            return true;
        }
    }
}
