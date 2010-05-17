package com.metaweb.gridworks.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.metaweb.gridworks.importers.parsers.TsvCsvRowParser;
import com.metaweb.gridworks.importers.parsers.NonSplitRowParser;
import com.metaweb.gridworks.importers.parsers.RowParser;
import com.metaweb.gridworks.importers.parsers.SeparatorRowParser;
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

        LineNumberReader lnReader = new LineNumberReader(reader);
        RowParser parser = (sep != null && sep.length() > 0 && splitIntoColumns) ?
                new SeparatorRowParser(sep) : null;
                
        read(parser, lnReader, project, sep, 
    		limit, skip, ignoreLines, headerLines, 
    		guessValueType, splitIntoColumns
    	);
    }

    public void read(RowParser parser, LineNumberReader lnReader, Project project, String sep, int limit, int skip, int ignoreLines, int headerLines, boolean guessValueType, boolean splitIntoColumns ) throws IOException{
        List<String> columnNames = new ArrayList<String>();
        String line = null;
        int rowsWithData = 0;

        while ((line = lnReader.readLine()) != null) {
            if (ignoreLines > 0) {
                ignoreLines--;
                continue;
            } else if (StringUtils.isBlank(line)) {
                continue;
            }

            if (parser == null) {
                if (splitIntoColumns) {
                    int tab = line.indexOf('\t');
                    if (tab >= 0) {
                        sep = "\t";
                        parser = new TsvCsvRowParser('\t');
                    } else {
                        sep = ",";
                        parser = new TsvCsvRowParser(',');
                    }
                } else {
                    parser = new NonSplitRowParser();
                }
            }

            if (headerLines > 0) {
                headerLines--;

                List<String> cells = parser.split(line, lnReader);
                for (int c = 0; c < cells.size(); c++) {
                    String cell = cells.get(c).trim();

                    ImporterUtilities.appendColumnName(columnNames, c, cell);
                }
            } else {
                Row row = new Row(columnNames.size());

                if (parser.parseRow(row, line, guessValueType, lnReader)) {
                    rowsWithData++;

                    if (skip <= 0 || rowsWithData > skip) {
                        project.rows.add(row);
                        project.columnModel.setMaxCellIndex(row.cells.size());

                        ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

                        if (limit > 0 && project.rows.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }

        ImporterUtilities.setupColumns(project, columnNames);
    }

    public void read(InputStream inputStream, Project project, Properties options) throws Exception {
        throw new UnsupportedOperationException();
    }

    public boolean takesReader() {
        return true;
    }
}
