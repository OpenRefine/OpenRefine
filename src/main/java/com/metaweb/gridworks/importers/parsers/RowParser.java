package com.metaweb.gridworks.importers.parsers;

import java.util.List;

import com.metaweb.gridworks.model.Row;

public abstract class RowParser {
    public abstract List<String> split(String line);
    
    public abstract boolean parseRow(Row row, String line, boolean guessValueType);
}
