/*
 * Copyright (c) 2010,2013 Thomas F. Morris and other contributors
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.Sqlresponse;
import com.google.api.services.fusiontables.model.Table;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

/**
 * OpenRefine parser for Google Spreadsheets.
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class FusionTableImporter {
    static public void parse(
        String token,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        int limit,
        JSONObject options,
        List<Exception> exceptions) {
    
        Fusiontables service = FusionTableHandler.getFusionTablesService(token);
        parse(
                service,
                project,
                metadata,
                job,
                limit,
                options,
                exceptions
                );

    }
    
    static void setProgress(ImportingJob job, String fileSource, int percent) {
        job.setProgress(percent, "Reading " + fileSource);
    }
    
    
    static private class FusionTableBatchRowReader implements TableDataReader {
            final ImportingJob job;
            final String fileSource;
            
            final Fusiontables service;
            final List<FTColumnData> columns;
            final int batchSize;
            
            final String baseQuery;
            
            int nextRow = 0; // 0-based
            int batchRowStart = 0; // 0-based
            boolean end = false;
            List<List<Object>> rowsOfCells = null;
            boolean usedHeaders = false;
            
            public FusionTableBatchRowReader(ImportingJob job, String fileSource,
                    Fusiontables service, String tableId, List<FTColumnData> columns,
                    int batchSize) {
                this.job = job;
                this.fileSource = fileSource;
                this.service = service;
                this.columns = columns;
                this.batchSize = batchSize;
                
                StringBuffer sb = new StringBuffer();
                sb.append("SELECT ");
                
                boolean first = true;
                for (FTColumnData cd : columns) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append("'");
                    sb.append(cd.name);
                    sb.append("'");
                }
                sb.append(" FROM ");
                sb.append(tableId);
                
                baseQuery = sb.toString();
            }
            
            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                if (!usedHeaders) {
                    List<Object> row = new ArrayList<Object>(columns.size());
                    for (FTColumnData cd : columns) {
                        row.add(cd.name);
                    }
                    usedHeaders = true;
                    return row;
                }
                
                if (rowsOfCells == null || (nextRow >= batchRowStart + rowsOfCells.size() && !end)) {
                    int newBatchRowStart = batchRowStart + (rowsOfCells == null ? 0 : rowsOfCells.size());
                    rowsOfCells = getRowsOfCells(newBatchRowStart);
                    batchRowStart = newBatchRowStart;

                    setProgress(job, fileSource, -1 /* batchRowStart * 100 / totalRows */);
                }
                
                if (rowsOfCells != null && nextRow - batchRowStart < rowsOfCells.size()) {
                    return rowsOfCells.get(nextRow++ - batchRowStart);
                } else {
                    return null;
                }
            }
            
            
            private List<List<Object>> getRowsOfCells(int startRow) throws IOException {
                List<List<Object>> rowsOfCells = new ArrayList<List<Object>>(batchSize);
                
                String query = baseQuery + " OFFSET " + startRow + " LIMIT " + batchSize;
                
                Sqlresponse sqlresponse = FusionTableHandler.runFusionTablesSelect(service, query);
                List<List<Object>> rows = sqlresponse.getRows();
                if (rows.size() > 1) {
                    for (int i = 1; i < rows.size(); i++) {
                        List<Object> row = rows.get(i);
                        List<Object> rowOfCells = new ArrayList<Object>(row.size());
                        for (int j = 0; j < row.size() && j < columns.size(); j++) {
                            String text = String.valueOf(row.get(j));
                            if (text.isEmpty()) {
                                rowOfCells.add(null);
                            } else {
                                FTColumnData cd = columns.get(j);
                                if (cd.type == FTColumnType.NUMBER) {
                                    try {
                                        rowOfCells.add(Long.parseLong(text));
                                        continue;
                                    } catch (NumberFormatException e) {
                                        // ignore
                                    }
                                    try {
                                        double d = Double.parseDouble(text);
                                        if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                                            rowOfCells.add(d);
                                            continue;
                                        }
                                    } catch (NumberFormatException e) {
                                        // ignore
                                    }
                                }
                                
                                rowOfCells.add(text);
                            }
                        }
                        rowsOfCells.add(rowOfCells);
                    }
                }
                end = rows.size() < batchSize + 1;
                return rowsOfCells;
            }
    
    }

    static public void parse(
            Fusiontables service,
            Project project,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            JSONObject options,
            List<Exception> exceptions) {
        
        String docUrlString = JSONUtilities.getString(options, "docUrl", null);
        String id = getFTid(docUrlString); // Use GDataExtension.getFusionTableKey(url) ?
        // TODO: Allow arbitrary Fusion Tables URL instead of (in addition to?) constructing our own?
        
        try {
            List<FTColumnData> columns = new ArrayList<FusionTableImporter.FTColumnData>();
            Table table = service.table().get(id).execute();
            for (Column col : table.getColumns()) {
                FTColumnData cd = new FTColumnData();
                cd.name = col.getName();
                String type = col.getType();
                if (type.equals("STRING")) {
                    cd.type = FTColumnType.STRING;
                } else if (type.equals("NUMBER")) {
                    cd.type = FTColumnType.NUMBER;
                } else if (type.equals("DATETIME")) {
                    cd.type = FTColumnType.DATETIME;
                } else if (type.equals("LOCATION")) {
                    cd.type = FTColumnType.LOCATION;
                } else {
                    // TODO: unknown type
                    cd.type = FTColumnType.STRING;
                }
                columns.add(cd);
            }

            setProgress(job, docUrlString, -1);

            // Force these options for the next call because each fusion table
            // is strictly structured with a single line of headers.
            JSONUtilities.safePut(options, "ignoreLines", 0); // number of blank lines at the beginning to ignore
            JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

            TabularImportingParserBase.readTable(
                    project,
                    metadata,
                    job,
                    new FusionTableBatchRowReader(job, docUrlString, service, id, columns, 100),
                    docUrlString,
                    limit,
                    options,
                    exceptions
                );
            setProgress(job, docUrlString, 100);
        } catch (IOException e) {
            e.printStackTrace();
            exceptions.add(e);
        }
    }
    
    static private String getFTid(String url) {
        if (url == null) {
            return null;
        }
        int equal = url.lastIndexOf('=');
        if (equal < 0) {
            return null;
        }
        return url.substring(equal + 1);
    }
    
    static enum FTColumnType {
        STRING,
        NUMBER,
        DATETIME,
        LOCATION
    }
    
    final static class FTColumnData {
        String name;
        FTColumnType type;
    }
}