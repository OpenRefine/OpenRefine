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

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

/**
 * CsvExporter class
 */
public class CsvExporter implements WriterExporter {

    private char separator;

    public CsvExporter() {
        separator = ',';      //Comma separated-value is default
    }

    public CsvExporter(char separator) {
        this.separator = separator;
    }

    private static class Configuration {
        @JsonProperty("separator")
        protected String separator = null;
        @JsonProperty("lineSeparator")
        protected String lineSeparator = CSVWriter.DEFAULT_LINE_END;
        @JsonProperty("quoteAll")
        protected boolean quoteAll = false;
    }

    @Override
    public void export(Project project, Properties params, Engine engine, final Writer writer)
            throws IOException {

        String optionsString = (params == null) ? null : params.getProperty("options");
        com.google.refine.exporters.CsvExporter.Configuration options = new com.google.refine.exporters.CsvExporter.Configuration();
        if (optionsString != null) {
            try {
                options = ParsingUtilities.mapper.readValue(optionsString, com.google.refine.exporters.CsvExporter.Configuration.class);
            } catch (IOException e) {
                // Ignore and keep options null.
                e.printStackTrace();
            }
        }
        if (options.separator == null) {
            options.separator = Character.toString(separator);
        }

        final String separator = options.separator;
        final String lineSeparator = options.lineSeparator;
        final boolean quoteAll = options.quoteAll;

        final boolean printColumnHeader =
                params != null && params.getProperty("printColumnHeader") != null ?
                        Boolean.parseBoolean(params.getProperty("printColumnHeader")) :
                        true;

        final CSVWriter csvWriter = createWriter(writer, separator.charAt(0), lineSeparator);

        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JsonNode options) {
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                if (!isHeader || printColumnHeader) {
                    String[] strings = new String[cells.size()];
                    for (int i = 0; i < strings.length; i++) {
                        CellData cellData = cells.get(i);
                        // If file is tsv and cell text contains internal tabs, then manually escape tabs
                        if (separator.charAt(0) == '\t' && cellData != null && cellData.text != null && cellData.text.contains("\t")) {
                            final String tabString = cellData.text.replace("\t", "\\t");
                            strings[i] = tabString;
                        }
                        // If file is tsv & cell contains internal newlines, manually escape newlines
                        else if (separator.charAt(0) == '\t' && cellData != null && cellData.text != null && cellData.text.contains("\n")) {
                            final String lineString = cellData.text.replace("\n", "\\n");
                            strings[i] = lineString;
                        } else {
                            strings[i] =
                                    cellData != null && cellData.text != null ?
                                            cellData.text :
                                            "";
                        }
                    }
                    csvWriter.writeNext(strings, quoteAll);
                }
            }
        };
        CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);
        csvWriter.close();
    }

    /**
     * Create csv writer if csv file specified or tsv writer if tsv file specified
     * tsv files only escape tabs and newlines
     *
     * @param writer        Writer originally passed to export method of CsvExporter
     * @param delimiter     Delimiter originally passed to CsvExporter
     * @param lineSeparator Line terminator, has value of CSVWriter.DEFAULT_LINE_END
     * @return csvWriter    Writer properly configured for csv or tsv file export
     * @author John Fox
     */
    private CSVWriter createWriter(final Writer writer, final char delimiter, final String lineSeparator) {
        CSVWriter csvWriter;
        if (delimiter == '\t') {
            csvWriter =
                    new CSVWriter(writer, delimiter, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, lineSeparator);
        } else {
            csvWriter =
                    new CSVWriter(writer, delimiter, CSVWriter.DEFAULT_QUOTE_CHARACTER, lineSeparator);
        }
        return csvWriter;
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }
}
