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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.model.ColumnModel;
import org.openrefine.util.ParsingUtilities;

public class CsvExporter extends EngineDependentExporter {

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");
    private Configuration config;
    private char separator;
    private boolean printColumnHeader;
    private CSVWriter csvWriter;

    public CsvExporter() {
        this.separator = ',';
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
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public void startFile(JsonNode options, Properties params, ColumnModel columnModel, Writer writer) {
        config = new Configuration();
        if (options != null) {
            try {
                config = ParsingUtilities.mapper.treeToValue(options, Configuration.class);
            } catch (IOException e) {
                ;
            }
        }
        if (config.separator == null) {
            config.separator = Character.toString(separator);
        }
        printColumnHeader = (params != null && params.getProperty("printColumnHeader") != null)
                ? Boolean.parseBoolean(params.getProperty("printColumnHeader"))
                : true;
        csvWriter = new CSVWriter(writer, config.separator.charAt(0), CSVWriter.DEFAULT_QUOTE_CHARACTER, config.lineSeparator);
    }

    @Override
    public void endFile() throws IOException {
        csvWriter.close();
    }

    @Override
    public void addRow(List<CellData> cells, boolean isHeader) {
        if (!isHeader || printColumnHeader) {
            String[] strings = new String[cells.size()];
            for (int i = 0; i < strings.length; i++) {
                CellData cellData = cells.get(i);
                strings[i] = (cellData != null && cellData.text != null) ? cellData.text : "";
            }
            csvWriter.writeNext(strings, config.quoteAll);
        }

    }
}
