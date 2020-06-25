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

package org.openrefine.exporters;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import org.openrefine.ProjectManager;
import org.openrefine.exporters.EngineDependentExporter.CellData;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.recon.Recon;
import org.openrefine.preference.PreferenceStore;
import org.openrefine.util.JSONUtilities;

abstract public class CustomizableTabularExporterUtilities {

    final static private String fullIso8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

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

        CellData format(ColumnMetadata column, Cell cell) {
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
