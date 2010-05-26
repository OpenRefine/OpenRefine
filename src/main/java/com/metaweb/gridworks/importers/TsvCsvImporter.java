package com.metaweb.gridworks.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import au.com.bytecode.opencsv.CSVParser;

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

        LineNumberReader lnReader = new LineNumberReader(reader);


        read(lnReader, project, sep,
    		limit, skip, ignoreLines, headerLines,
    		guessValueType, splitIntoColumns
    	);
    }

    /**
     *
     * @param lnReader
     *           LineNumberReader used to read file or string contents
     * @param project
     *           The project into which the parsed data will be added
     * @param sep
     *           The character used to denote different the break between data points
     * @param limit
     *           The maximum number of rows of data to import
     * @param skip
     *           The number of initial data rows to skip
     * @param ignoreLines
     *           The number of initial lines within the data source which should be ignored entirely
     * @param headerLines
     *           The number of lines in the data source which describe each column
     * @param guessValueType
     *           Whether the parser should try and guess the type of the value being parsed
     * @param splitIntoColumns
     *           Whether the parser should try and split the data source into columns
     * @throws IOException
     */
    public void read(LineNumberReader lnReader, Project project, String sep, int limit, int skip, int ignoreLines, int headerLines, boolean guessValueType, boolean splitIntoColumns ) throws IOException{
        CSVParser parser = (sep != null && sep.length() > 0 && splitIntoColumns) ?
                        new CSVParser(sep.toCharArray()[0]) : null;//HACK changing string to char - won't work for multi-char separators.
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

            //guess separator
            if (parser == null) {
                int tab = line.indexOf('\t');
                if (tab >= 0) {
                    parser = new CSVParser('\t');
                } else {
                    parser = new CSVParser(',');
                }
            }


            if (headerLines > 0) {
                //column headers
                headerLines--;

                ArrayList<String> cells = getCells(line, parser, lnReader, splitIntoColumns);

                for (int c = 0; c < cells.size(); c++) {
                    String cell = cells.get(c).trim();
                    //add column even if cell is blank
                    ImporterUtilities.appendColumnName(columnNames, c, cell);
                }
            } else {
                //data
                Row row = new Row(columnNames.size());

                ArrayList<String> cells = getCells(line, parser, lnReader, splitIntoColumns);

                if( cells != null && cells.size() > 0 )
                    rowsWithData++;

                if (skip <=0  || rowsWithData > skip){
                    //add parsed data to row
                    for(String s : cells){
                        s = s.trim();
                        if (ExpressionUtils.isNonBlankData(s)) {
                            row.cells.add(new Cell(s, null));
                        }else{
                            row.cells.add(null);
                        }
                    }
                    project.rows.add(row);
                    project.columnModel.setMaxCellIndex(row.cells.size());

                    ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

                    if (limit > 0 && project.rows.size() >= limit) {
                        break;
                    }
                }
            }
        }

        ImporterUtilities.setupColumns(project, columnNames);
    }

    protected ArrayList<String> getCells(String line, CSVParser parser, LineNumberReader lnReader, boolean splitIntoColumns) throws IOException{
        ArrayList<String> cells = new ArrayList<String>();
        if(splitIntoColumns){
            String[] tokens = parser.parseLineMulti(line);
            for(String s : tokens){
                cells.add(s);
            }
            while(parser.isPending()){
                tokens = parser.parseLineMulti(lnReader.readLine());
                for(String s : tokens){
                    cells.add(s);
                }
            }
        }else{
            cells.add(line);
        }
        return cells;
    }

    public void read(InputStream inputStream, Project project, Properties options) throws Exception {
        read(new InputStreamReader(inputStream), project, options);
    }

    public boolean takesReader() {
        return true;
    }

    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
            return false;
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".tsv")) {
                return true;
            }else if (fileName.endsWith(".csv")){
                return true;
            }
        }
        return false;
    }
}
