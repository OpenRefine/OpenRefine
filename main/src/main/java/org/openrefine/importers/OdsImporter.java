/*

Copyright 2011, Thomas F. Morris
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.openrefine.ProjectMetadata;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class OdsImporter extends InputStreamImporter { 
    final static Logger logger = LoggerFactory.getLogger("open office");
    
    private final TabularParserHelper tabularParserHelper;

    public OdsImporter(DatamodelRunner runner) {
        super(runner);
        tabularParserHelper = new TabularParserHelper(runner);
    }

    
    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        ArrayNode sheetRecords = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(options, "sheetRecords", sheetRecords);
        OdfDocument odfDoc = null;
        try {
            for (int index = 0;index < fileRecords.size();index++) {
                ImportingFileRecord fileRecord = fileRecords.get(index);
                File file = fileRecord.getFile(job.getRawDataDir());
                InputStream is = new FileInputStream(file);
                odfDoc = OdfDocument.loadDocument(is);
                List<OdfTable> tables = odfDoc.getTableList();
                int sheetCount = tables.size();
    
                for (int i = 0; i < sheetCount; i++) {
                    OdfTable sheet = tables.get(i);
                    int rows = sheet.getRowCount();
    
                    ObjectNode sheetRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(sheetRecord, "name",  file.getName() + "#" + sheet.getTableName());
                    JSONUtilities.safePut(sheetRecord, "fileNameAndSheetIndex", file.getName() + "#" + i);
                    JSONUtilities.safePut(sheetRecord, "rows", rows);
                    if (rows > 0) {
                        JSONUtilities.safePut(sheetRecord, "selected", true);
                    } else {
                        JSONUtilities.safePut(sheetRecord, "selected", false);
                    }
                    JSONUtilities.append(sheetRecords, sheetRecord);
                }
            }
        } catch (FileNotFoundException e) {
            logger.info("File not found",e);
        } catch (Exception e) {
            // ODF throws *VERY* wide exceptions
            logger.info("Error reading ODF spreadsheet",e);
        } finally {
            if (odfDoc != null) {
                odfDoc.close();
            }
        }
        return options;
    }
    

    @Override
    public GridState parseOneFile(
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            long limit,
            ObjectNode options
    ) throws Exception {
        OdfDocument odfDoc;
        try {
            odfDoc = OdfDocument.loadDocument(inputStream);
        } catch (Exception e) { // Ugh! could they throw any wider exception?
            throw e;
        }

        List<OdfTable> tables = odfDoc.getTableList();
        List<GridState> grids = new ArrayList<>();

        ArrayNode sheets = JSONUtilities.getArray(options, "sheets");
        for(int i=0;i<sheets.size();i++)  {
            String[] fileNameAndSheetIndex = new String[2];
            ObjectNode sheetObj = JSONUtilities.getObjectElement(sheets, i);
            // value is fileName#sheetIndex
            fileNameAndSheetIndex = sheetObj.get("fileNameAndSheetIndex").asText().split("#");
        
            if (!fileNameAndSheetIndex[0].equals(fileSource))
                continue;
            
            final OdfTable table = tables.get(Integer.parseInt(fileNameAndSheetIndex[1]));
            final int lastRow = table.getRowCount();

            TableDataReader dataReader = new TableDataReader() {
                int nextRow = 0;
                Map<String, Recon> reconMap = new HashMap<String, Recon>();

                @Override
                public List<Object> getNextRowOfCells() throws IOException {
                    if (nextRow > lastRow) {
                        return null;
                    }

                    List<Object> cells = new ArrayList<Object>();
                    OdfTableRow row = table.getRowByIndex(nextRow++);
                    if (row != null) {
                        int lastCell = row.getCellCount();
                        for (int cellIndex = 0; cellIndex <= lastCell; cellIndex++) {
                            Cell cell = null;

                            OdfTableCell sourceCell = row.getCellByIndex(cellIndex);
                            if (sourceCell != null) {
                                cell = extractCell(sourceCell, reconMap);
                            }
                            cells.add(cell);
                        }
                    }
                    return cells;
                }
            };

            grids.add(tabularParserHelper.parseOneFile(
                    metadata,
                    job,
                    fileSource + "#" + table.getTableName(),
                    dataReader,
                    limit,
                    options
            ));
        }
        
        return mergeGridStates(grids);
    }

    static protected Serializable extractCell(OdfTableCell cell) {
        // TODO: how can we tell if a cell contains an error?
        //String formula = cell.getFormula();

        Serializable value = null;
        // "boolean", "currency", "date", "float", "percentage", "string" or "time"
        String cellType = cell.getValueType();
        if ("boolean".equals(cellType)) {
            value = cell.getBooleanValue();
        } else if ("float".equals(cellType)) {
            value = cell.getDoubleValue();
        } else if ("date".equals(cellType)) {
            value = cell.getDateValue();
        } else if ("currency".equals(cellType)) {
            value = cell.getCurrencyValue();
        } else if ("percentage".equals(cellType)) {
            value = cell.getPercentageValue();
        } else if ("string".equals(cellType)) {
            value = cell.getStringValue();
        } else if (cellType == null) {
            value = cell.getDisplayText();
            if ("".equals(value)) {
                value = null;
            } else {
                logger.info("Null cell type with non-empty value: " + value);                
            }
        } else {
            logger.info("Unexpected cell type " + cellType);
            value = cell.getDisplayText();
        }
        return value;
    }

    static protected Cell extractCell(OdfTableCell cell, Map<String, Recon> reconMap) {
        Serializable value = extractCell(cell);

        if (value != null) {
            Recon recon = null;

            String hyperlink = ""; // TODO: cell.getHyperlink();
            if (hyperlink != null) {
                String url = hyperlink; // TODO: hyperlink.getAddress();

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
                        } else {
                        	ReconCandidate candidate = new ReconCandidate(id, value.toString(), new String[0], 100);
                            recon = new Recon(0, null, null)
                            		.withService("import")
                            		.withMatch(candidate)
                            		.withMatchRank(0)
                            		.withJudgment(Judgment.Matched)
                            		.withJudgmentAction("auto")
                            		.withCandidate(candidate);

                            reconMap.put(id, recon);
                        }
                    }
                }
            }
            return new Cell(value, recon);
        } else {
            return null;
        }
    }

} 