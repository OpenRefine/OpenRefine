package com.metaweb.gridworks.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import au.com.bytecode.opencsv.CSVReader;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class TsvCsvImporter implements Importer {
    public void read(Reader reader, Project project, Properties options) throws Exception {
        boolean splitIntoColumns = ImporterUtilities.getBooleanOption("split-into-columns", options, true);

        String sep = options.getProperty("separator"); // auto-detect if not present
        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int headerLines = ImporterUtilities.getIntegerOption("header-lines", options, 1);

        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);
        boolean guessValueType = ImporterUtilities.getBooleanOption("guess-value-type", options, true);

        // default expected format is CSV
        char separator = (sep != null && sep.length() == 1 && splitIntoColumns) ? sep.toCharArray()[0] : ','; 

        CSVReader CsvReader = new CSVReader(reader, separator);
        read(CsvReader, project, limit, skip, ignoreLines, headerLines, guessValueType);
    }

    /**
     *
     * @param reader
     * @param project
     * @param limit - negative for no limit.
     * @param skip
     * @param ignoreLines
     * @param headerLines
     * @param guessValueType
     * @throws IOException
     */
    public void read(CSVReader reader, Project project, int limit, int skip, int ignoreLines, int headerLines, boolean guessValueType ) throws IOException {
        
        // prevent logic errors below when negative numbers are introduced by defaulting to zero (except limit which is negative to indicate no limit)
        if (skip < 0) skip = 0;
        if (ignoreLines < 0) ignoreLines = 0;
        if (headerLines < 0) headerLines = 0;

        List<String> columnNames = new ArrayList<String>();
        String [] nextLine;
        int lineCounter = 0;
        while ((nextLine = reader.readNext()) != null) {
            lineCounter++;

            if (limit > 0 && lineCounter > limit + ignoreLines + headerLines + skip) break;
            if (ignoreLines > 0 && lineCounter <= ignoreLines) continue; // initial non-blank lines
            if (headerLines > 0 && lineCounter <= ignoreLines + headerLines && lineCounter > ignoreLines) {
                // deal with column headers
                for (int c = 0; c < nextLine.length; c++) {
                    String cell = nextLine[c].trim();
                    ImporterUtilities.appendColumnName(columnNames, c, cell);
                }
            } else {
                // a data line (or a line below the header)
                if (skip > 0 && lineCounter <= ignoreLines + headerLines + skip) continue; // skip initial data lines

                // data line
                Row row = new Row(columnNames.size());
                project.rows.add(row);
                project.columnModel.setMaxCellIndex(row.cells.size());
                for (String s : nextLine) {
                    Serializable value = guessValueType ? ImporterUtilities.parseCellValue(s) : s;

                    if (ExpressionUtils.isNonBlankData(value)) {
                        row.cells.add(new Cell(value, null));
                    } else {
                        row.cells.add(null);
                    }
                }
                ImporterUtilities.ensureColumnsInRowExist(columnNames, row);
            }
        }

        ImporterUtilities.setupColumns(project, columnNames);
    }

    protected void DealWithHeaders(String[] nextLine, List<String> columnNames){

    }

    public void read(InputStream inputStream, Project project, Properties options) throws Exception {
        throw new UnsupportedOperationException();
    }

    public boolean takesReader() {
        return true;
    }
}
