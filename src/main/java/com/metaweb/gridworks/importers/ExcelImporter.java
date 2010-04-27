package com.metaweb.gridworks.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;

public class ExcelImporter implements Importer {
    final protected boolean _xmlBased;
    
    public ExcelImporter(boolean xmlBased) {
        _xmlBased = xmlBased;
    }

    public boolean takesReader() {
        return false;
    }
    
    public void read(Reader reader, Project project, Properties options) throws Exception {
        throw new NotImplementedException();
    }

    public void read(InputStream inputStream, Project project, Properties options) throws Exception {
        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);
        
        Workbook wb = null;
        try {
            wb = _xmlBased ? 
                new XSSFWorkbook(inputStream) : 
                new HSSFWorkbook(new POIFSFileSystem(inputStream));
        } catch (IOException e) {
            throw new IOException(
                "Attempted to parse file as Excel file but failed. " +
                "Try to use Excel to re-save the file as a different Excel version or as TSV and upload again.",
                e
            );
        }
        
        Sheet sheet = wb.getSheetAt(0);

        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        int r = firstRow;
        
        List<Integer>    nonBlankIndices = null;
        List<String>     nonBlankHeaderStrings = null;
        
        /*
         *  Find the header row
         */
        for (; r <= lastRow; r++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            } else if (ignoreLines > 0) {
                ignoreLines--;
                continue;
            }
            
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell >= 0 && firstCell <= lastCell) {
                nonBlankIndices = new ArrayList<Integer>(lastCell - firstCell + 1);
                nonBlankHeaderStrings = new ArrayList<String>(lastCell - firstCell + 1);
                
                for (int c = firstCell; c <= lastCell; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
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
        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        for (int c = 0; c < nonBlankIndices.size(); c++) {
        	String cell = nonBlankHeaderStrings.get(c);
            if (nameToIndex.containsKey(cell)) {
            	int index = nameToIndex.get(cell);
            	nameToIndex.put(cell, index + 1);
            	
            	cell = cell.contains(" ") ? (cell + " " + index) : (cell + index);
            } else {
            	nameToIndex.put(cell, 2);
            }
            
            Column column = new Column(c, cell);
            project.columnModel.columns.add(column);
        }
        
        /*
         *  Now process the data rows
         */
        int rowsWithData = 0;
        Map<String, Recon> reconMap = new HashMap<String, Recon>();
        
        for (; r <= lastRow; r++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell >= 0 && firstCell <= lastCell) {
                Row newRow = new Row(nonBlankIndices.size());
                boolean hasData = false;
                
                for (int c = 0; c < nonBlankIndices.size(); c++) {
                    if (c < firstCell || c > lastCell) {
                        continue;
                    }
                    
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell == null) {
                        continue;
                    }
                    
                    int cellType = cell.getCellType();
                    if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR || 
                        cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK) {
                        continue;
                    }
                    if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA) {
                        cellType = cell.getCachedFormulaResultType();
                    }
                    
                    Serializable value = null;
                    if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN) {
                        value = cell.getBooleanCellValue();
                    } else if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC) {
                        double d = cell.getNumericCellValue();
                        
                        if (HSSFDateUtil.isCellDateFormatted(cell)) {
                            value = HSSFDateUtil.getJavaDate(d);
                        } else {
                            value = d;
                        }
                    } else {
                        String text = cell.getStringCellValue().trim();
                        if (text.length() > 0) {
                            value = text;
                        }
                    }
                    
                    if (value != null) {
                        Recon recon = null;
                        
                        Hyperlink hyperlink = cell.getHyperlink();
                        if (hyperlink != null) {
                            String url = hyperlink.getAddress();
                            
                            if (url.startsWith("http://") || 
                                url.startsWith("https://")) {
                                
                                final String sig = "freebase.com/view";
                                
                                int i = url.indexOf(sig);
                                if (i > 0) {
                                    String id = url.substring(i + sig.length());
                                    
                                    int q = id.indexOf('?');
                                    if (q > 0) {
                                        id = id.substring(0, q);
                                    }
                                    int h = id.indexOf('#');
                                    if (h > 0) {
                                        id = id.substring(0, h);
                                    }
                                    
                                    if (reconMap.containsKey(id)) {
                                    	recon = reconMap.get(id);
	                                    recon.judgmentBatchSize++;
                                    } else {
	                                    recon = new Recon();
	                                    recon.service = "import";
	                                    recon.match = new ReconCandidate(id, "", value.toString(), new String[0], 100);
	                                    recon.matchRank = 0;
	                                    recon.judgment = Judgment.Matched;
	                                    recon.judgmentAction = "auto";
	                                    recon.judgmentBatchSize = 1;
	                                    recon.addCandidate(recon.match);
	                                    
	                                    reconMap.put(id, recon);
                                    }
                                    
                                }
                            }
                        }
                        
                        newRow.setCell(c, new Cell(value, recon));
                        hasData = true;
                    }
                }
                
                if (hasData) {
                    rowsWithData++;
                    
                    if (skip <= 0 || rowsWithData > skip) {
                        project.rows.add(newRow);
                        project.columnModel.setMaxCellIndex(newRow.cells.size());
                        
                        if (limit > 0 && project.rows.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
