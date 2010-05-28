package com.metaweb.gridworks.importers.parsers;

import java.io.LineNumberReader;
import java.util.List;

import com.metaweb.gridworks.model.Row;

public abstract class RowParser {
    public abstract List<String> split(String line, LineNumberReader lineReader);
    
    public abstract boolean parseRow(Row row, String line, boolean guessValueType, LineNumberReader lineReader);
}
