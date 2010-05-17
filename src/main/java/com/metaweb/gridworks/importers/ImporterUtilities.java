package com.metaweb.gridworks.importers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
        if (text.length() > 0) {
            if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) {
                return text.substring(1, text.length() - 1);
            }

            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
            }

            try {
                double d = Double.parseDouble(text);
                if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                    return d;
                }
            } catch (NumberFormatException e) {
            }
            text = text.trim();
        }
        return text;
    }

    static public int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return value;
    }

    static public boolean getBooleanOption(String name, Properties options, boolean def) {
        boolean value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Boolean.parseBoolean(s);
            } catch (Exception e) {
            }
        }
        return value;
    }

    static public void appendColumnName(List<String> columnNames, int index, String name) {
        name = name.trim();

        while (columnNames.size() <= index) {
            columnNames.add("");
        }

        if (!name.isEmpty()) {
            String oldName = columnNames.get(index);
            if (!oldName.isEmpty()) {
                name = oldName + " " + name;
            }

            columnNames.set(index, name);
        }
    }

    static public void ensureColumnsInRowExist(List<String> columnNames, Row row) {
        int count = row.cells.size();
        while (count > columnNames.size()) {
            columnNames.add("");
        }
    }

    static public void setupColumns(Project project, List<String> columnNames) {
        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        for (int c = 0; c < columnNames.size(); c++) {
            String cell = columnNames.get(c).trim();
            if (cell.isEmpty()) {
                cell = "Column";
            } else if (cell.startsWith("\"") && cell.endsWith("\"")) {
                cell = cell.substring(1, cell.length() - 1).trim(); //FIXME is trimming quotation marks appropriate?
            }

            if (nameToIndex.containsKey(cell)) {
                int index = nameToIndex.get(cell);
                nameToIndex.put(cell, index + 1);

                cell = cell.contains(" ") ? (cell + " " + index) : (cell + index);
            } else {
                nameToIndex.put(cell, 2);
            }

            Column column = new Column(c, cell);

            project.columnModel.columns.add(column);
        }
    }

}
