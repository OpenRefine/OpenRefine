package com.google.refine.freebase.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;


public class BooleanColumnCondition implements Condition {
    final public String columnName;
    
    public BooleanColumnCondition(String columnName) {
        this.columnName = columnName;
    }
    
    @Override
    public boolean test(Project project, int rowIndex, Row row) {
        Column column = project.columnModel.getColumnByName(columnName);
        if (column != null) {
            Object o = row.getCellValue(column.getCellIndex());
            if (o != null) {
                if (o instanceof Boolean) {
                    return ((Boolean) o).booleanValue();
                } else {
                    return Boolean.parseBoolean(o.toString());
                }
            }
        }
        return false;
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("columnName"); writer.value(columnName);
        writer.endObject();
    }
}
