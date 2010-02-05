package com.metaweb.gridworks.importers;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExcelImporter implements Importer {

	@Override
	public boolean takesReader() {
		return false;
	}
	
	@Override
	public void read(Reader reader, Project project, Properties options)
			throws Exception {
		
		throw new NotImplementedException();
	}

	@Override
	public void read(InputStream inputStream, Project project,
			Properties options) throws Exception {
		
        POIFSFileSystem fs = new POIFSFileSystem(inputStream);
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);

        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        int r = firstRow;
        
        List<Integer> 	nonBlankIndices = null;
        List<String> 	nonBlankHeaderStrings = null;
        
        /*
         *  Find the header row
         */
        for (; r <= lastRow; r++) {
            HSSFRow row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell >= 0 && firstCell <= lastCell) {
            	nonBlankIndices = new ArrayList<Integer>(lastCell - firstCell + 1);
            	nonBlankHeaderStrings = new ArrayList<String>(lastCell - firstCell + 1);
            	
                for (short c = firstCell; c <= lastCell; c++) {
                    HSSFCell cell = row.getCell(c);
                    if (cell != null) {
                        String text = cell.getStringCellValue().trim();
                        if (text.length() > 0) {
                        	nonBlankIndices.add((int) c);
                        	nonBlankHeaderStrings.add(text);
                        }
                    }
                }
                
                if (nonBlankIndices.size() > 0) {
                	r++;
                	break;
                }
            }
        }
        
        if (nonBlankIndices == null || nonBlankIndices.size() == 0) {
        	return;
        }
        
        /*
         *  Create columns
         */
        for (int c = 0; c < nonBlankIndices.size(); c++) {
        	Column column = new Column(c, nonBlankHeaderStrings.get(c));
			project.columnModel.columns.add(column);
        }
        
        /*
         *  Now process the data rows
         */
        for (; r <= lastRow; r++) {
            HSSFRow row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell >= 0 && firstCell <= lastCell) {
            	Row newRow = new Row(nonBlankIndices.size());
            	boolean hasData = false;
            	
            	for (short c = 0; c < nonBlankIndices.size(); c++) {
            		if (c < firstCell || c > lastCell) {
            			continue;
            		}
            		
        			HSSFCell cell = row.getCell(c);
        			if (cell == null) {
        				continue;
        			}
        			
            		int cellType = cell.getCellType();
            		if (cellType == HSSFCell.CELL_TYPE_ERROR || 
            			cellType == HSSFCell.CELL_TYPE_BLANK) {
            			continue;
            		}
            		
            		Object value = null;
            		if (cellType == HSSFCell.CELL_TYPE_BOOLEAN) {
            			value = cell.getBooleanCellValue();
            		} else if (cellType == HSSFCell.CELL_TYPE_NUMERIC) {
            			value = cell.getNumericCellValue();
            		} else {
                        String text = cell.getStringCellValue().trim();
                        if (text.length() > 0) {
                        	value = text;
                        }
                    }
            		
            		if (value != null) {
            			newRow.setCell(c, new Cell(value, null));
            			hasData = true;
            		}
                }
                
                if (hasData) {
                	project.rows.add(newRow);
                }
            }
        }
    }
}
