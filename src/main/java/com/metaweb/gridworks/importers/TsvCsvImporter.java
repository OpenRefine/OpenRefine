package com.metaweb.gridworks.importers;

import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;

import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class TsvCsvImporter implements Importer {

    public void read(Reader reader, Project project, Properties options, int skip, int limit)
            throws Exception {
        
        LineNumberReader lnReader = new LineNumberReader(reader);
        try {
            String         sep = null; // auto-detect TSV or CSV
            String         line = null;
            boolean     first = true;
            int         cellCount = 1;
            
            int rowsWithData = 0;
            while ((line = lnReader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                
                if (sep == null) {
                    int tab = line.indexOf('\t');
                    if (tab >= 0) {
                        sep = "\t";
                    } else {
                        sep = ",";
                    }
                }
                
                if (first) {
                    String[] cells = line.split(sep);
                    
                    first = false;
                    for (int c = 0; c < cells.length; c++) {
                        String cell = cells[c];
                        if (cell.startsWith("\"") && cell.endsWith("\"")) {
                            cell = cell.substring(1, cell.length() - 1);
                        }
                        
                        Column column = new Column(c, cell);
                        
                        project.columnModel.columns.add(column);
                    }
                    
                    cellCount = cells.length;
                } else {
                    Row row = new Row(cellCount);
                    
                    if ((sep.charAt(0) == ',') ? ImporterUtilities.parseCSVIntoRow(row, line) : ImporterUtilities.parseTSVIntoRow(row, line)) {
                        rowsWithData++;
                        
                        if (skip <= 0 || rowsWithData > skip) {
                            project.rows.add(row);
                            project.columnModel.setMaxCellIndex(Math.max(project.columnModel.getMaxCellIndex(), row.cells.size()));
                            
                            if (limit > 0 && project.rows.size() >= limit) {
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            lnReader.close();
        }
    }

    public void read(InputStream inputStream, Project project,
            Properties options, int skip, int limit) throws Exception {
        
        throw new NotImplementedException();
    }

    public boolean takesReader() {
        return true;
    }
}
