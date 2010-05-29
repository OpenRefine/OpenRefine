package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

import au.com.bytecode.opencsv.CSVWriter;


public class CsvExporter implements Exporter {

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");
    char separator;

    public CsvExporter() {
        separator = ','; //Comma separated-value is default
    }

    public CsvExporter(char separator) {
        this.separator = separator;
    }

    public void export(Project project, Properties options, Engine engine, OutputStream outputStream) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {

        boolean printColumnHeader = true;

        if (options != null) {
            String printColHead = options.getProperty("printColumnHeader");
            if(printColHead != null)
                printColumnHeader = !printColHead.toLowerCase().equals("false");
        }

        RowVisitor visitor = new RowVisitor() {
            CSVWriter csvWriter;
            boolean printColumnHeader = true;
            boolean isFirstRow = true; //the first row should also add the column headers

            public RowVisitor init(CSVWriter writer, boolean printColumnHeader){
                this.csvWriter = writer;
                this.printColumnHeader = printColumnHeader;
                return this;
            }

            public boolean visit(Project project, int rowIndex, Row row) {
                String[] cols = new String[project.columnModel.columns.size()];
                String[] vals = new String[row.cells.size()];

                int i = 0;
                for(Column col : project.columnModel.columns){
                    int cellIndex = col.getCellIndex();
                    cols[i] = col.getName();

                    Cell cell = row.cells.get(cellIndex);
                    if(cell != null){
                        vals[i] = cell.value.toString();
                    }
                    i++;
                }

                if( printColumnHeader && isFirstRow ){
                    csvWriter.writeNext(cols,false);
                    isFirstRow = false; //switch off flag
                }
                csvWriter.writeNext(vals,false);

                return false;
            }

            public void start(Project project) {
                // nothing to do
            }

            public void end(Project project) {
                try {
                    csvWriter.close();
                } catch (IOException e) {
                    logger.error("CsvExporter could not close writer : " + e.getMessage());
                }
            }

        }.init(new CSVWriter(writer, separator), printColumnHeader);

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
    }

    public String getContentType() {
        return "application/x-unknown";
    }

    public boolean takeWriter() {
        return true;
    }

}
