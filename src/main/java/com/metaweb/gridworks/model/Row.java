package com.metaweb.gridworks.model;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.ParsingUtilities;

public class Row implements HasFields, Jsonizable {
    public boolean             flagged;
    public boolean             starred;
    final public List<Cell>    cells;
    
    transient public int            recordIndex = -1; // -1 for rows that are not main record rows
    transient public List<Integer>  contextRows;
    transient public int[]          contextRowSlots;
    transient public int[]          contextCellSlots;
    
    public Row(int cellCount) {
        cells = new ArrayList<Cell>(cellCount);
    }
    
    public Row dup() {
        Row row = new Row(cells.size());
        row.flagged = flagged;
        row.starred = starred;
        row.cells.addAll(cells);
        return row;
    }
    
    public Object getField(String name, Properties bindings) {
        if ("flagged".equals(name)) {
            return flagged;
        } else if ("starred".equals(name)) {
            return starred;
        } else if ("cells".equals(name)) {
            return new Cells();
        } else if ("index".equals(name)) {
            return bindings.get("rowIndex");
        } else if ("record".equals(name)) {
        	int rowIndex = (Integer) bindings.get("rowIndex");
        	int recordRowIndex = (contextRows != null && contextRows.size() > 0) ?
        			contextRows.get(0) : rowIndex;
        	
            return new Record(recordRowIndex, rowIndex);
        } else if ("columnNames".equals(name)) {
        	Project project = (Project) bindings.get("project");
        	
        	return project.columnModel.getColumnNames();
        }
        return null;
    }
    
    public boolean isEmpty() {
        for (Cell cell : cells) {
            if (cell != null && cell.value != null && !isValueBlank(cell.value)) {
                return false;
            }
        }
        return true;
    }
    
    public Cell getCell(int cellIndex) {
        if (cellIndex < cells.size()) {
            return cells.get(cellIndex);
        } else {
            return null;
        }
    }
    
    public Object getCellValue(int cellIndex) {
        if (cellIndex < cells.size()) {
            Cell cell = cells.get(cellIndex);
            if (cell != null) {
                return cell.value;
            }
        }
        return null;
    }
    
    public boolean isCellBlank(int cellIndex) {
        return isValueBlank(getCellValue(cellIndex));
    }
    
    protected boolean isValueBlank(Object value) {
        return value == null || !(value instanceof String) || ((String) value).trim().length() == 0;
    }
    
    public void setCell(int cellIndex, Cell cell) {
        if (cellIndex < cells.size()) {
            cells.set(cellIndex, cell);
        } else {
            while (cellIndex > cells.size()) {
                cells.add(null);
            }
            cells.add(cell);
        }
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("flagged"); writer.value(flagged);
        writer.key("starred"); writer.value(starred);
        
        writer.key("cells"); writer.array();
        for (Cell cell : cells) {
            if (cell != null) {
                cell.write(writer, options);
            } else {
                writer.value(null);
            }
        }
        writer.endArray();
        
        if (!"save".equals(options.getProperty("mode"))) {
            if (recordIndex >= 0) {
                writer.key("j"); writer.value(recordIndex);
            }
            
            if (options.containsKey("rowIndex")) {
                writer.key("i"); writer.value(options.get("rowIndex"));
            }
            if (options.containsKey("extra")) {
                Properties extra = (Properties) options.get("extra");
                if (extra != null) {
                    for (Object key : extra.keySet()) {
                        writer.key((String) key);
                        writer.value(extra.get(key));
                    }
                }
            }
        }
        
        writer.endObject();
    }
    
    public void save(Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public Row load(String s) throws Exception {
        return s.length() == 0 ? null : load(ParsingUtilities.evaluateJsonStringToObject(s));
    }
    
    static public Row load(JSONObject obj) throws Exception {
        JSONArray a = obj.getJSONArray("cells");
        int count = a.length();
        
        Row row = new Row(count);
        for (int i = 0; i < count; i++) {
            if (!a.isNull(i)) {
                JSONObject o = a.getJSONObject(i);
                
                row.setCell(i, Cell.load(o));
            }
        }
        
        if (obj.has("starred")) {
            row.starred = obj.getBoolean("starred");
        }
        if (obj.has("flagged")) {
            row.flagged = obj.getBoolean("flagged");
        }
        
        return row;
    }
    
    protected class Cells implements HasFields {
        public Object getField(String name, Properties bindings) {
            Project project = (Project) bindings.get("project");
            Column column = project.columnModel.getColumnByName(name);
            if (column != null) {
                int cellIndex = column.getCellIndex();
                return getCell(cellIndex);
            }
            return null;
        }
        
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
    }
    
    protected class RecordCells implements HasFields {
    	final int _recordRowIndex;
    	
    	protected RecordCells(int recordRowIndex) {
    		_recordRowIndex = recordRowIndex;
    	}
    	
		public Object getField(String name, Properties bindings) {
            Project project = (Project) bindings.get("project");
            Column column = project.columnModel.getColumnByName(name);
            if (column != null) {
            	Row recordRow = project.rows.get(_recordRowIndex);
                int cellIndex = column.getCellIndex();
                
                CellTuple cells = new CellTuple();
                
            	int recordIndex = recordRow.recordIndex;
            	int count = project.rows.size();
            	for (int r = _recordRowIndex; r < count; r++) {
            		Row row = project.rows.get(r);
            		if (row.recordIndex > recordIndex) {
            			break;
            		}
            		
            		Cell cell = row.getCell(cellIndex);
            		if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
            			cells.add(cell);
            		}
            	}
            	
                return cells;
            }
            return null;
		}
    }
    
    protected class CellTuple extends ArrayList<Cell> implements HasFields {
		private static final long serialVersionUID = -651032866647686293L;

		public Object getField(String name, Properties bindings) {
			Object[] r = new Object[this.size()];
			for (int i = 0; i < r.length; i++) {
				r[i] = this.get(i).getField(name, bindings);
			}
			return r;
		}
    }
}
