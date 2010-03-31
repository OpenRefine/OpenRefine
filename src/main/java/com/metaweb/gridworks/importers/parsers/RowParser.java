package com.metaweb.gridworks.importers.parsers;

import com.metaweb.gridworks.model.Row;

public abstract class RowParser {
    
    public abstract boolean parseRow(Row row, String line);
}
