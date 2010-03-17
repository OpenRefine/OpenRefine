package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class XlsExporter implements Exporter {
    public String getContentType() {
        return "application/xls";
    }
    
    public boolean takeWriter() {
        return false;
    }
    
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        throw new NotImplementedException();
    }
    
    public void export(Project project, Properties options, Engine engine,
        OutputStream outputStream) throws IOException {
        
        Workbook wb = new HSSFWorkbook();
        Sheet s = wb.createSheet();
        wb.setSheetName(0, ProjectManager.singleton.getProjectMetadata(project.id).getName());
        
        int rowCount = 0;
        
        {
            org.apache.poi.ss.usermodel.Row r = s.createRow(rowCount++);
            
            int cellCount = 0;
            for (Column column : project.columnModel.columns) {
                org.apache.poi.ss.usermodel.Cell c = r.createCell(cellCount++);
                c.setCellValue(column.getName());
            }
        }
        
        {
            RowVisitor visitor = new RowVisitor() {
                Sheet sheet;
                int rowCount;
                
                public RowVisitor init(Sheet sheet, int rowCount) {
                    this.sheet = sheet;
                    this.rowCount = rowCount;
                    return this;
                }
                
                public boolean visit(Project project, int rowIndex, Row row, boolean contextual, boolean includeDependent) {
                    org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowCount++);
                    
                    int cellCount = 0;
                    for (Column column : project.columnModel.columns) {
                        org.apache.poi.ss.usermodel.Cell c = r.createCell(cellCount++);
                        
                        int cellIndex = column.getCellIndex();
                        if (cellIndex < row.cells.size()) {
                            Cell cell = row.cells.get(cellIndex);
                            if (cell != null && cell.value != null) {
                                Object v = cell.value;
                                if (v instanceof Number) {
                                    c.setCellValue(((Number) v).doubleValue());
                                } else if (v instanceof Boolean) {
                                    c.setCellValue(((Boolean) v).booleanValue());
                                } else if (v instanceof Date) {
                                    c.setCellValue((Date) v);
                                } else if (v instanceof Calendar) {
                                    c.setCellValue((Calendar) v);
                                } else if (v instanceof String) {
                                    c.setCellValue((String) v);
                                }
                                
                                if (cell.recon != null && cell.recon.match != null) {
                                    HSSFHyperlink hl = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
                                    hl.setLabel(cell.recon.match.topicName);
                                    hl.setAddress("http://www.freebase.com/view" + cell.recon.match.topicID);
                                    
                                    c.setHyperlink(hl);
                                }
                            }
                        }
                    }
                    return false;
                }
            }.init(s, rowCount);
            
            FilteredRows filteredRows = engine.getAllFilteredRows(true);
            filteredRows.accept(project, visitor);
        }
        
        wb.write(outputStream);
        outputStream.flush();
    }

}
