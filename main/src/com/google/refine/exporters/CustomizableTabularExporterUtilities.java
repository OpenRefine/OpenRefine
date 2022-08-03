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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.commons.validator.routines.UrlValidator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    final static private String fullIso8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    static public void exportRows(
            final Project project,
            final Engine engine,
            Properties params,
            final TabularSerializer serializer) {

        String optionsString = (params != null) ? params.getProperty("options") : null;
        JsonNode optionsTemp = null;
        if (optionsString != null) {
            try {
                optionsTemp = ParsingUtilities.mapper.readTree(optionsString);
            } catch (IOException e) {
                // Ignore and keep options null.
            }
        }
        final JsonNode options = optionsTemp;

        final boolean outputColumnHeaders = options == null ? true : JSONUtilities.getBoolean(options, "outputColumnHeaders", true);
        final boolean outputEmptyRows = options == null ? false : JSONUtilities.getBoolean(options, "outputBlankRows", true);
        final int limit = options == null ? -1 : JSONUtilities.getInt(options, "limit", -1);

        final List<String> columnNames;
        final Map<String, CellFormatter> columnNameToFormatter = new HashMap<String, CustomizableTabularExporterUtilities.CellFormatter>();

        List<JsonNode> columnOptionArray = options == null ? null : JSONUtilities.getArray(options, "columns");
        if (columnOptionArray == null) {
            List<Column> columns = project.columnModel.columns;

            columnNames = new ArrayList<String>(columns.size());
            for (Column column : columns) {
                String name = column.getName();
                columnNames.add(name);
                columnNameToFormatter.put(name, new CellFormatter());
            }
        } else {
            int count = columnOptionArray.size();

            columnNames = new ArrayList<String>(count);
            for (int i = 0; i < count; i++) {
                JsonNode columnOptions = columnOptionArray.get(i);
                if (columnOptions != null) {
                    String name = JSONUtilities.getString(columnOptions, "name", null);
                    if (name != null) {
                        columnNames.add(name);
                        try {
                            columnNameToFormatter.put(name, ParsingUtilities.mapper.treeToValue(columnOptions, ColumnOptions.class));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
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
        public void startFile(JsonNode options) {
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
        @JsonProperty("entity-name")
        ENTITY_NAME, @JsonProperty("entity-id")
        ENTITY_ID, @JsonProperty("cell-content")
        CELL_CONTENT
    }

    private enum DateFormatMode {
        @JsonProperty("iso-8601")
        ISO_8601, @JsonProperty("locale-short")
        SHORT_LOCALE, @JsonProperty("locale-medium")
        MEDIUM_LOCALE, @JsonProperty("locale-long")
        LONG_LOCALE, @JsonProperty("locale-full")
        FULL_LOCALE, @JsonProperty("custom")
        CUSTOM
    }

    static private class ReconSettings {

        @JsonProperty("output")
        ReconOutputMode outputMode = ReconOutputMode.ENTITY_NAME;
        @JsonProperty("blankUnmatchedCells")
        boolean blankUnmatchedCells = false;
        @JsonProperty("linkToEntityPages")
        boolean linkToEntityPages = true;
    }

    static private class DateSettings {

        @JsonProperty("format")
        DateFormatMode formatMode = DateFormatMode.ISO_8601;
        @JsonProperty("custom")
        String custom = null;
        @JsonProperty("useLocalTimeZone")
        boolean useLocalTimeZone = false;
        @JsonProperty("omitTime")
        boolean omitTime = false;
    }

    static public class ColumnOptions extends CellFormatter {

        @JsonProperty("name")
        String columnName;
    }

    static public class CellFormatter {

        @JsonProperty("reconSettings")
        ReconSettings recon = new ReconSettings();
        @JsonProperty("dateSettings")
        DateSettings date = new DateSettings();

        // SQLExporter parameter to convert null cell value to empty string
        @JsonProperty("nullValueToEmptyStr")
        boolean includeNullFieldValue = false;

        DateFormat dateFormatter;
        String[] urlSchemes = { "http", "https", "ftp" };
        UrlValidator urlValidator = new UrlValidator(urlSchemes);

        Map<String, String> identifierSpaceToUrl = null;

        @JsonCreator
        CellFormatter(
                @JsonProperty("reconSettings") ReconSettings reconSettings,
                @JsonProperty("dateSettings") DateSettings dateSettings,
                @JsonProperty("nullValueToEmptyStr") boolean includeNullFieldValue) {
            if (reconSettings != null) {
                recon = reconSettings;
            }
            if (dateSettings != null) {
                date = dateSettings;
            }
            setup();
        }

        CellFormatter() {
            setup();
        }

        private void setup() {
            if (date.formatMode == DateFormatMode.CUSTOM &&
                    (date.custom == null || date.custom.isEmpty())) {
                date.formatMode = DateFormatMode.ISO_8601;
            }

            switch (date.formatMode) {
                case SHORT_LOCALE:
                    dateFormatter = date.omitTime ? SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
                            : SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
                    break;
                case MEDIUM_LOCALE:
                    dateFormatter = date.omitTime ? SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
                            : SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM);
                    break;
                case LONG_LOCALE:
                    dateFormatter = date.omitTime ? SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG)
                            : SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG);
                    break;
                case FULL_LOCALE:
                    dateFormatter = date.omitTime ? SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL)
                            : SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.FULL);
                    break;
                case CUSTOM:
                    dateFormatter = new SimpleDateFormat(date.custom);
                    break;

                default:
                    dateFormatter = date.omitTime ? new SimpleDateFormat("yyyy-MM-dd") : new SimpleDateFormat(fullIso8601);
            }

            if (!date.useLocalTimeZone) {
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
                        if (this.recon.outputMode == ReconOutputMode.ENTITY_NAME) {
                            text = recon.match.name;
                        } else if (this.recon.outputMode == ReconOutputMode.ENTITY_ID) {
                            text = recon.match.id;
                        } // else: output cell content

                        if (this.recon.linkToEntityPages) {
                            buildIdentifierSpaceToUrlMap();

                            String service = recon.service;
                            String viewUrl = identifierSpaceToUrl.get(service);
                            if (viewUrl != null) {
                                link = StringUtils.replace(viewUrl, "{{id}}", recon.match.id);
                            }
                        }
                    } else if (this.recon.blankUnmatchedCells) {
                        return null;
                    }
                }

                Object value = cell.value;
                if (value != null) {
                    if (text == null) {
                        if (value instanceof String) {
                            text = (String) value;

                            if (text.contains(":") && urlValidator.isValid(text)) {
                                // Extra check for https://github.com/OpenRefine/OpenRefine/issues/2213
                                try {
                                    link = new URI(text).toString();
                                } catch (URISyntaxException e) {
                                    ;
                                }
                            }
                        } else if (value instanceof OffsetDateTime) {
                            text = ((OffsetDateTime) value).format(DateTimeFormatter.ISO_INSTANT);
                        } else {
                            text = value.toString();
                        }
                    }
                    return new CellData(column.getName(), value, text, link);
                }
            } else {// added for sql exporter

                if (includeNullFieldValue) {
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
            ArrayNode services = (ArrayNode) ps.get("reconciliation.standardServices");
            if (services != null) {
                int count = services.size();

                for (int i = 0; i < count; i++) {
                    ObjectNode service = (ObjectNode) services.get(i);
                    ObjectNode view = JSONUtilities.getObject(service, "view");
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
