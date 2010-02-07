package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;

public class Row implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = -689264211730915507L;
	
	public boolean		       flagged;
	public boolean		       starred;
	final public List<Cell>    cells;
	
	transient public List<Integer> 	contextRows;
	transient public int[] 			contextRowSlots;
	transient public int[] 			contextCellSlots;
	
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
	
	@Override
	public Object getField(String name, Properties bindings) {
		if ("flagged".equals(name)) {
			return flagged;
		} else if ("starred".equals(name)) {
			return starred;
		} else if ("cells".equals(name)) {
			return new Cells();
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
	
	public class Cells implements HasFields {
		private Cells() {};

		@Override
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

	@Override
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
		
		writer.endObject();
	}
}
