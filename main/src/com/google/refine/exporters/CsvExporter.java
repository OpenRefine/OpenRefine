package com.google.refine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CsvExporter implements Exporter{

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");
    char separator;

    public CsvExporter() {
        separator = ','; //Comma separated-value is default
    }

    public CsvExporter(char separator) {
        this.separator = separator;
    }

    @Override
    public void export(Project project, Properties options, Engine engine, OutputStream outputStream)
            throws IOException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        boolean printColumnHeader = true;

        if (options != null && options.getProperty("printColumnHeader") != null) {
            printColumnHeader = Boolean.parseBoolean(options.getProperty("printColumnHeader"));
        }

        RowVisitor visitor = new RowVisitor() {
            CSVWriter csvWriter;
            boolean printColumnHeader = true;
            boolean isFirstRow = true; //the first row should also add the column headers

            public RowVisitor init(CSVWriter writer, boolean printColumnHeader) {
                this.csvWriter = writer;
                this.printColumnHeader = printColumnHeader;
                return this;
            }

            public boolean visit(Project project, int rowIndex, Row row) {
                int size = project.columnModel.columns.size();

                String[] cols = new String[size];
                String[] vals = new String[size];

                int i = 0;
                for (Column col : project.columnModel.columns) {
                    int cellIndex = col.getCellIndex();
                    cols[i] = col.getName();

                    Object value = row.getCellValue(cellIndex);
                    if (value != null) {
                        vals[i] = value instanceof String ? (String) value : value.toString();
                    }
                    i++;
                }

                if (printColumnHeader && isFirstRow) {
                    csvWriter.writeNext(cols,false);
                    isFirstRow = false; //switch off flag
                }
                csvWriter.writeNext(vals,false);

                return false;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
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

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }

    @Override
    public boolean takeWriter() {
        return true;
    }

}
