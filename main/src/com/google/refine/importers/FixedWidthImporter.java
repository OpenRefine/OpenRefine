package com.google.refine.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class FixedWidthImporter implements ReaderImporter, StreamImporter { //TODO this class is almost an exact copy of TsvCsvImporter.  Could we combine the two, or combine common functions into a common abstract supertype?

    final static Logger logger = LoggerFactory.getLogger("FixedWidthImporter");
    
    @Override
    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
            
            //filter out tree structure data
            if("application/json".equals(contentType)||
                    "text/json".equals(contentType)||
                    "application/xml".equals(contentType) ||
                    "text/xml".equals(contentType) ||
                    "application/rss+xml".equals(contentType) ||
                    "application/atom+xml".equals(contentType) ||
                    "application/rdf+xml".equals(contentType))  //TODO add more tree data types.
                return false;
            
            return
                "text/plain".equals(contentType)
                || "text/fixed-width".equals(contentType);  //FIXME Is text/fixed-width a valid contentType?
        }
        return false;
    }

    @Override
    public void read(InputStream inputStream, Project project,
            ProjectMetadata metadata, Properties options)
            throws ImportException {
        read(new InputStreamReader(inputStream), project, metadata, options);
    }

    @Override
    public void read(Reader reader, Project project, ProjectMetadata metadata,
            Properties options) throws ImportException {
        boolean splitIntoColumns = ImporterUtilities.getBooleanOption("split-into-columns", options, true);
        String columnWidths = options.getProperty("fixed-column-widths");
        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int headerLines = ImporterUtilities.getIntegerOption("header-lines", options, 1);

        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);
        boolean guessValueType = ImporterUtilities.getBooleanOption("guess-value-type", options, true);

        LineNumberReader lnReader = new LineNumberReader(reader);
        
        
        read(lnReader, project, columnWidths,
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
    * @param columnWidths
    *           Expects a comma separated string of integers which indicate the number of characters in each line
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
    public void read(LineNumberReader lnReader, Project project,
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean splitIntoColumns) throws ImportException{
                
                int[] columnWidths = null;

                columnWidths = getColumnWidthsFromString( sep );
                
                if(columnWidths.length < 2)
                    splitIntoColumns = false;
                
                List<String> columnNames = new ArrayList<String>();
                String line = null;
                int rowsWithData = 0;

                try {
                    while ((line = lnReader.readLine()) != null) {
                        if (ignoreLines > 0) {
                            ignoreLines--;
                            continue;
                        } else if (StringUtils.isBlank(line)) {
                            continue;
                        }


                        if (headerLines > 0) {
                            //column headers
                            headerLines--;
                            
                            ArrayList<String> cells = getCells(line, columnWidths, splitIntoColumns);
                            
                            for (int c = 0; c < cells.size(); c++) {
                                String cell = cells.get(c).trim();
                                //add column even if cell is blank
                                ImporterUtilities.appendColumnName(columnNames, c, cell);
                            }
                        } else {
                            //data
                            Row row = new Row(columnNames.size());

                            ArrayList<String> cells = getCells(line, columnWidths, splitIntoColumns);

                            if( cells != null && cells.size() > 0 )
                                rowsWithData++;

                            if (skip <=0  || rowsWithData > skip){
                                //add parsed data to row
                                for(String s : cells){
                                    if (ExpressionUtils.isNonBlankData(s)) {
                                        Serializable value = guessValueType ? ImporterUtilities.parseCellValue(s) : s;
                                        row.cells.add(new Cell(value, null));
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
                } catch (IOException e) {
                    throw new ImportException("The fixed width parser could not read the next line", e);
                }

                ImporterUtilities.setupColumns(project, columnNames);
        
        }

    /**
     * Splits the line into columns
     * @param line
     * @param lnReader
     * @param splitIntoColumns
     * @return
     */
    private ArrayList<String> getCells(String line, int[] widths, boolean splitIntoColumns) {
        ArrayList<String> cells = new ArrayList<String>();
        if(splitIntoColumns){
            int columnStartCursor = 0;
            int columnEndCursor = 0;
            for(int width : widths){
                if(columnStartCursor >= line.length()){
                    cells.add(null); //FIXME is adding a null cell (to represent no data) OK?
                    continue;
                }
                
                columnEndCursor = columnStartCursor + width;
                
                if(columnEndCursor > line.length())
                    columnEndCursor = line.length();
                if(columnEndCursor <= columnStartCursor){
                    cells.add(null); //FIXME is adding a null cell (to represent no data, or a zero width column) OK? 
                    continue;
                }
                
                cells.add(line.substring(columnStartCursor, columnEndCursor));
                
                columnStartCursor = columnEndCursor;
            }
        }else{
            cells.add(line);
        }
        return cells;
    }

    /**
     * Converts the expected string of comma separated integers into an array of integers.
     * Also performs a basic sanity check on the provided data.
     * 
     * @param sep
     * A comma separated string of integers. e.g. 4,2,5,22,19
     * @return
     * @throws ServletException
     */
    public int[] getColumnWidthsFromString(String sep) throws ImportException {
        String[] splitSep = Pattern.compile(",").split(sep);

        int[] widths = new int[splitSep.length];
        for(int i = 0;  i < splitSep.length; i++){
            try{
                int parsedInt = Integer.parseInt(splitSep[i]);
                if( parsedInt < 0 )
                    throw new ImportException("A column cannot have a width of less than zero", null);
                widths[i] = parsedInt;
            }catch(NumberFormatException e){
                throw new ImportException("For a fixed column width import, the column widths must be given as a comma separated string of integers.  e.g. 1,3,5,22,19", e);
            }
        }
        return widths;
    }

}
