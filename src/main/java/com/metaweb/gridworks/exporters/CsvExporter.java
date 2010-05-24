package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

import au.com.bytecode.opencsv.CSVWriter;


public class CsvExporter implements Exporter{

    @Override
    public void export(Project project, Properties options, Engine engine, OutputStream outputStream)
            throws IOException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        {
            RowVisitor visitor = new RowVisitor() {
                CSVWriter csvWriter;
                boolean columnHeader = true; //the first row should also add the column headers

                public RowVisitor init(CSVWriter writer) {
                    this.csvWriter = writer;
                    return this;
                }

                public boolean visit(Project project, int rowIndex, Row row) {
                    String[] vals = null;

                    if( columnHeader ){
                        String[] cols = new String[project.columnModel.columns.size()];
                        for(int i = 0; i < cols.length; i++){
                            cols[i] = project.columnModel.columns.get(i).getName();
                        }
                        csvWriter.writeNext(cols);
                        columnHeader = false; //switch off flag
                    }

                    vals = new String[row.cells.size()];
                    for(int i = 0; i < vals.length; i++){
                        vals[i] = row.cells.get(i).value.toString();
                    }

                    csvWriter.writeNext(vals);
                    return false;
                }

                @Override
                public void start(Project project) {
                    // nothing to do
                }

                @Override
                public void end(Project project) {
                    // nothing to do

                }

            }.init(new CSVWriter(writer));

            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, visitor);
        }
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
