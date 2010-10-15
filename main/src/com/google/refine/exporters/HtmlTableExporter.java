package com.google.refine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class HtmlTableExporter implements WriterExporter {
    public String getContentType() {
        return "text/html";
    }
    
    public boolean takeWriter() {
        return true;
    }
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
        throw new RuntimeException("Not implemented");
    }
    
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        writer.write("<html>\n");
        writer.write("<head><title>"); 
            writer.write(ProjectManager.singleton.getProjectMetadata(project.id).getName());
            writer.write("</title></head>\n");
        
        writer.write("<body>\n");
        writer.write("<table>\n");
        
        writer.write("<tr>");
        {
            for (Column column : project.columnModel.columns) {
                writer.write("<th>");
                writer.write(column.getName());
                writer.write("</th>");
            }
        }
        writer.write("</tr>\n");
        
        {
            RowVisitor visitor = new RowVisitor() {
                Writer writer;
                
                public RowVisitor init(Writer writer) {
                    this.writer = writer;
                    return this;
                }
                
                @Override
                public void start(Project project) {
                	// nothing to do
                }
                
                @Override
                public void end(Project project) {
                	// nothing to do
                }
                
                public boolean visit(Project project, int rowIndex, Row row) {
                    try {
                        writer.write("<tr>");
                        
                        for (Column column : project.columnModel.columns) {
                            writer.write("<td>");
                            
                            int cellIndex = column.getCellIndex();
                            if (cellIndex < row.cells.size()) {
                                Cell cell = row.cells.get(cellIndex);
                                if (cell != null && cell.value != null) {
                                    Object v = cell.value;
                                    writer.write(v instanceof String ? ((String) v) : v.toString());
                                }
                            }
                            
                            writer.write("</td>");
                        }
                        
                        writer.write("</tr>\n");
                    } catch (IOException e) {
                        // ignore
                    }
                    return false;
                }
            }.init(writer);
            
            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, visitor);
        }
        
        writer.write("</table>\n");
        writer.write("</body>\n");
        writer.write("</html>\n");
    }

}
