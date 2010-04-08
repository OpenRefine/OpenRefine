package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class HtmlTableExporter implements Exporter {
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
                
                public boolean visit(Project project, int rowIndex, Row row, boolean contextual, boolean includeDependent) {
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
            
            FilteredRows filteredRows = engine.getAllFilteredRows(true);
            filteredRows.accept(project, visitor);
        }
        
        writer.write("</table>\n");
        writer.write("</body>\n");
        writer.write("</html>\n");
    }

}
