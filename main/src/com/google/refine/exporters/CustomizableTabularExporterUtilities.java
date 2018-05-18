/*

Copyright 2010, Google Inc.
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

package com.google.refine.exporters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.exporters.TabularSerializer.CellData;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

abstract public class CustomizableTabularExporterUtilities {
    static public void exportRows(
        final Project project,
        final Engine engine,
        Properties params,
        final TabularSerializer serializer) {
        
        String optionsString = (params != null) ? params.getProperty("options") : null;
        JSONObject optionsTemp = null;
        if (optionsString != null) {
            try {
                optionsTemp = ParsingUtilities.evaluateJsonStringToObject(optionsString);
            } catch (JSONException e) {
                // Ignore and keep options null.
            }
        }
        final JSONObject options = optionsTemp;
        
        final boolean outputColumnHeaders = options == null ? true :
            JSONUtilities.getBoolean(options, "outputColumnHeaders", true);
        final boolean outputEmptyRows = options == null ? false :
            JSONUtilities.getBoolean(options, "outputBlankRows", true);
        final int limit = options == null ? -1 :
            JSONUtilities.getInt(options, "limit", -1);
        
        final List<String> columnNames;
        final Map<String, CellFormatter> columnNameToFormatter =
            new HashMap<String, CustomizableTabularExporterUtilities.CellFormatter>();
        
        JSONArray columnOptionArray = options == null ? null :
            JSONUtilities.getArray(options, "columns");
        if (columnOptionArray == null) {
            List<Column> columns = project.columnModel.columns;
            
            columnNames = new ArrayList<String>(columns.size());
            for (Column column : columns) {
                String name = column.getName();
                columnNames.add(name);
                columnNameToFormatter.put(name, new CellFormatter());
            }
        } else {
            int count = columnOptionArray.length();
            
            columnNames = new ArrayList<String>(count);
            for (int i = 0; i < count; i++) {
                JSONObject columnOptions = JSONUtilities.getObjectElement(columnOptionArray, i);
                if (columnOptions != null) {
                    String name = JSONUtilities.getString(columnOptions, "name", null);
                    if (name != null) {
                        columnNames.add(name);
                        columnNameToFormatter.put(name, new CellFormatter(columnOptions));
                    }
                }
            }
        }
        
        RowVisitor visitor = new RowVisitor() {
            int rowCount = 0;
            
            @Override
            public void start(Project project) {
                serializer.startFile(options);
                if (outputColumnHeaders) {
                    List<CellData> cells = new ArrayList<TabularSerializer.CellData>(columnNames.size());
                    for (String name : columnNames) {
                        cells.add(new CellData(name, name, name, null));
                    }
                    serializer.addRow(cells, true);
                }
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                List<CellData> cells = new ArrayList<TabularSerializer.CellData>(columnNames.size());
                int nonNullCount = 0;
                
                for (String columnName : columnNames) {
                    Column column = project.columnModel.getColumnByName(columnName);
                    CellFormatter formatter = columnNameToFormatter.get(columnName);
                    CellData cellData = formatter.format(
                        project,
                        column,
                        row.getCell(column.getCellIndex()));
                    
                    cells.add(cellData);
                    if (cellData != null) {
                        nonNullCount++;
                    }
                }
                
                if (nonNullCount > 0 || outputEmptyRows) {
                    serializer.addRow(cells, false);
                    rowCount++;
                }
                
                return limit > 0 && rowCount >= limit;
            }

            @Override
            public void end(Project project) {
                serializer.endFile();
            }
        };

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
    }
    
    static public int[] countColumnsRows(
            final Project project,
            final Engine engine,
            Properties params) {
        RowCountingTabularSerializer serializer = new RowCountingTabularSerializer();
        exportRows(project, engine, params, serializer);
        return new int[] { serializer.columns, serializer.rows };
    }
    
    static private class RowCountingTabularSerializer implements TabularSerializer {
        int columns;
        int rows;
        
        @Override
        public void startFile(JSONObject options) {
        }

        @Override
        public void endFile() {
        }

        @Override
        public void addRow(List<CellData> cells, boolean isHeader) {
            columns = Math.max(columns, cells.size());
            rows++;
        }
    }
    
    private enum ReconOutputMode {
        ENTITY_NAME,
        ENTITY_ID,
        CELL_CONTENT
    }
    private enum DateFormatMode {
        ISO_8601,
        SHORT_LOCALE,
        MEDIUM_LOCALE,
        LONG_LOCALE,
        FULL_LOCALE,
        CUSTOM
    }
    
    final static private String fullIso8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    
    static private class CellFormatter {
        ReconOutputMode recon_outputMode = ReconOutputMode.ENTITY_NAME;
        boolean recon_blankUnmatchedCells = false;
        boolean recon_linkToEntityPages = true;
        
        DateFormatMode date_formatMode = DateFormatMode.ISO_8601;
        String date_custom = null;
        boolean date_useLocalTimeZone = false;
        boolean date_omitTime = false;
        
        DateFormat dateFormatter;
        
        Map<String, String> identifierSpaceToUrl = null;
        
        //SQLExporter parameter to convert null cell value to empty string
        boolean includeNullFieldValue = false;
        
        CellFormatter() {
            dateFormatter = new SimpleDateFormat(fullIso8601);
        }
        
        CellFormatter(JSONObject options) {
            JSONObject reconSettings = JSONUtilities.getObject(options, "reconSettings");
            includeNullFieldValue = JSONUtilities.getBoolean(options, "nullValueToEmptyStr", false);
            if (reconSettings != null) {
                String reconOutputString = JSONUtilities.getString(reconSettings, "output", null);
                if ("entity-name".equals(reconOutputString)) {
                    recon_outputMode = ReconOutputMode.ENTITY_NAME;
                } else if ("entity-id".equals(reconOutputString)) {
                    recon_outputMode = ReconOutputMode.ENTITY_ID;
                } else if ("cell-content".equals(reconOutputString)) {
                    recon_outputMode = ReconOutputMode.CELL_CONTENT;
                }
                
                recon_blankUnmatchedCells = JSONUtilities.getBoolean(reconSettings, "blankUnmatchedCells", recon_blankUnmatchedCells);
                recon_linkToEntityPages = JSONUtilities.getBoolean(reconSettings, "linkToEntityPages", recon_linkToEntityPages);
            }
            JSONObject dateSettings = JSONUtilities.getObject(options, "dateSettings");
            if (dateSettings != null) {
                String dateFormatString = JSONUtilities.getString(dateSettings, "format", null);
                if ("iso-8601".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.ISO_8601;
                } else if ("locale-short".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.SHORT_LOCALE;
                } else if ("locale-medium".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.MEDIUM_LOCALE;
                } else if ("locale-long".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.LONG_LOCALE;
                } else if ("locale-full".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.FULL_LOCALE;
                } else if ("custom".equals(dateFormatString)) {
                    date_formatMode = DateFormatMode.CUSTOM;
                }
                
                date_custom = JSONUtilities.getString(dateSettings, "custom", null);
                date_useLocalTimeZone = JSONUtilities.getBoolean(dateSettings, "useLocalTimeZone", date_useLocalTimeZone);
                date_omitTime = JSONUtilities.getBoolean(dateSettings, "omitTime", date_omitTime);
                
                if (date_formatMode == DateFormatMode.CUSTOM &&
                    (date_custom == null || date_custom.isEmpty())) {
                    date_formatMode = DateFormatMode.ISO_8601;
                }
            }
            
            switch (date_formatMode) {
            case SHORT_LOCALE:
                dateFormatter = date_omitTime ?
                    SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT) :
                    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
                break;
            case MEDIUM_LOCALE:
                dateFormatter = date_omitTime ?
                    SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM) :
                    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM);
                break;
            case LONG_LOCALE:
                dateFormatter = date_omitTime ?
                    SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG) :
                    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG);
                break;
            case FULL_LOCALE:
                dateFormatter = date_omitTime ?
                    SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL) :
                    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.FULL);
                break;
            case CUSTOM:
                dateFormatter = new SimpleDateFormat(date_custom);
                break;
                
            default:
                dateFormatter = date_omitTime ?
                    new SimpleDateFormat("yyyy-MM-dd") :
                    new SimpleDateFormat(fullIso8601);
            }
            
            if (!date_useLocalTimeZone) {
                dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
        }
        
        CellData format(Project project, Column column, Cell cell) {
            if (cell != null) {
                String link = null;
                String text = null;
                
                if (cell.recon != null) {
                    Recon recon = cell.recon;
                    if (recon.judgment == Recon.Judgment.Matched) {
                        if (recon_outputMode == ReconOutputMode.ENTITY_NAME) {
                            text = recon.match.name;
                        } else if (recon_outputMode == ReconOutputMode.ENTITY_ID) {
                            text = recon.match.id;
                        } // else: output cell content
                        
                        if (recon_linkToEntityPages) {
                            buildIdentifierSpaceToUrlMap();
                            
                            String service = recon.service;
                            String viewUrl = identifierSpaceToUrl.get(service);
                            if (viewUrl != null) {
                                link = StringUtils.replace(viewUrl, "{{id}}", recon.match.id);
                            }
                        }
                    } else if (recon_blankUnmatchedCells) {
                        return null;
                    }
                }
                
                Object value = cell.value;
                if (value != null) {
                    if (text == null) {
                        if (value instanceof String) {
                            text = (String) value;
                        } else if (value instanceof OffsetDateTime) {
                            text = ((OffsetDateTime) value).format(DateTimeFormatter.ISO_INSTANT);
                        } else {
                            text = value.toString();
                        }
                    }
                    return new CellData(column.getName(), value, text, link);
                }
            }else {//added for sql exporter
            
                if(includeNullFieldValue) {
                    return new CellData(column.getName(), "", "", "");
                }
                
            }
            return null;
        }
        
        void buildIdentifierSpaceToUrlMap() {
            if (identifierSpaceToUrl != null) {
                return;
            }

            identifierSpaceToUrl = new HashMap<String, String>();
            
            PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
            JSONArray services = (JSONArray) ps.get("reconciliation.standardServices");
            if (services != null) {
                int count = services.length();
                
                for (int i = 0; i < count; i++) {
                    JSONObject service = JSONUtilities.getObjectElement(services, i);
                    JSONObject view = JSONUtilities.getObject(service, "view");
                    if (view != null) {
                        String url = JSONUtilities.getString(service, "url", null);
                        String viewUrl = JSONUtilities.getString(view, "url", null);
                        if (url != null && viewUrl != null) {
                            identifierSpaceToUrl.put(url, viewUrl);
                        }
                    }
                }
            }
        }
    }
}