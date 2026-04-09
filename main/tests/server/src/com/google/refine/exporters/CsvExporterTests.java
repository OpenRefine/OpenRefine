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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.univocity.parsers.common.AbstractWriter;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import com.univocity.parsers.tsv.TsvFormat;
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class CsvExporterTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    StringWriter writer;
    Project project;
    Engine engine;
    Map<String, String> options;

    // System Under Test
    CsvExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new CsvExporter();
        writer = new StringWriter();
        project = new Project();
        engine = new Engine(project);
        options = mock(Map.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        writer = null;
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleCsv() throws IOException {
        CreateGrid(2, 2);

        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "column0,column1\n" +
                "row0cell0,row0cell1\n" +
                "row1cell0,row1cell1\n");
    }

    @Test
    public void exportSimpleCsvNoHeader() throws IOException {
        CreateGrid(2, 2);
        when(options.get("printColumnHeader")).thenReturn("false");

        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "row0cell0,row0cell1\n" +
                "row1cell0,row1cell1\n");

        verify(options, times(2)).get("printColumnHeader");
    }

    @Test
    public void exportSimpleCsvCustomLineSeparator() throws IOException {
        CreateGrid(2, 2);
        when(options.get("options")).thenReturn("{\"lineSeparator\":\"X\"}");

        SUT.export(project, options, engine, writer);

        Assert.assertEquals(writer.toString(), "column0,column1X" +
                "row0cell0,row0cell1X" +
                "row1cell0,row1cell1X");
    }

    @Test
    public void exportSimpleCsvQuoteAll() throws IOException {
        CreateGrid(2, 2);
        when(options.get("options")).thenReturn("{\"quoteAll\":\"true\"}");

        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "\"column0\",\"column1\"\n" +
                "\"row0cell0\",\"row0cell1\"\n" +
                "\"row1cell0\",\"row1cell1\"\n");
    }

    @Test
    public void exportCsvWithLineBreaks() throws IOException {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line\n\n\nbreak", null));
        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"line\n\n\nbreak\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithComma() throws IOException {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("with, comma", null));
        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"with, comma\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithQuote() throws IOException {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line has \"quote\"", null));
        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"line has \"\"quote\"\"\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithEmptyCells() throws IOException {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        SUT.export(project, options, engine, writer);

        assertEqualsSystemLineEnding(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,,row1cell2\n" +
                ",row2cell1,row2cell2\n");
    }

    @Test
    void exportLegacyExporter() throws IOException {
        WriterExporter exporter = new LegacyTestExporter();
        exporter.export(project, options, engine, writer);

    }

    // all date type cells are in unified format
    /**
     * @Ignore
     * @Test public void exportDateColumnsPreVersion28(){ CreateGrid(1,2); Calendar calendar = Calendar.getInstance();
     *       Date date = new Date();
     *
     *       when(options.getProperty("printColumnHeader")).thenReturn("false"); project.rows.get(0).cells.set(0, new
     *       Cell(calendar, null)); project.rows.get(0).cells.set(1, new Cell(date, null));
     *
     *       try { SUT.export(project, options, engine, writer); } catch (IOException e) { Assert.fail(); }
     *
     *       String expectedOutput = ParsingUtilities.instantToLocalDateTimeString(calendar.toInstant()) + "," +
     *       ParsingUtilities.instantToLocalDateTimeString(date.toInstant()) + "\n";
     *
     *       assertEqualsSystemLineEnding(writer.toString(), expectedOutput); }
     */
    // helper methods

    protected void CreateColumns(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void CreateGrid(int noOfRows, int noOfColumns) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    // This is a copy of the CSV exporter as it existed before the API update
    class LegacyTestExporter implements WriterExporter {

        CsvFormat DEFAULT_FORMAT = new CsvWriterSettings().getFormat();
        TsvFormat TSV_FORMAT = new TsvWriterSettings().getFormat();
        char DEFAULT_SEPARATOR = DEFAULT_FORMAT.getDelimiter();
        String DEFAULT_LINE_ENDING = DEFAULT_FORMAT.getLineSeparatorString();

        final Logger logger = LoggerFactory.getLogger("CsvExporter");
        char separator;

        public LegacyTestExporter() {
            separator = ','; // Comma separated-value is default
        }

        public LegacyTestExporter(char separator) {
            this.separator = separator;
        }

        private class Configuration {

            @JsonProperty("separator")
            protected String separator = null;
            @JsonProperty("lineSeparator")
            protected String lineSeparator = DEFAULT_LINE_ENDING;
            @JsonProperty("quoteAll")
            protected boolean quoteAll = false;
        }

        @Override
        public void export(Project project, Properties params, Engine engine, final Writer writer)
                throws IOException {

            String optionsString = (params == null) ? null : params.getProperty("options");
            Configuration options = new Configuration();
            if (optionsString != null) {
                try {
                    options = ParsingUtilities.mapper.readValue(optionsString, Configuration.class);
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

            final boolean printColumnHeader = (params != null && params.getProperty("printColumnHeader") != null)
                    ? Boolean.parseBoolean(params.getProperty("printColumnHeader"))
                    : true;

            AbstractWriter csvWriter;
            if ("\t".equals(separator)) {
                TsvWriterSettings tsvSettings = new TsvWriterSettings();
                csvWriter = new TsvWriter(writer, tsvSettings);
            } else {
                CsvWriterSettings settings = new CsvWriterSettings();
                settings.setQuoteAllFields(quoteAll); // CSV only
                settings.getFormat().setLineSeparator(lineSeparator);
                settings.getFormat().setDelimiter(separator);

                // Required for our test exportCsvWithQuote which wants the value "line has \"quote\""
                // to be exported as "\"line has \"\"quote\"\"", although the default of literal value
                // without the extra quoting is arguably cleaner
                settings.setEscapeUnquotedValues(true);
                settings.setQuoteEscapingEnabled(true);

                csvWriter = new CsvWriter(writer, settings);
            }

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
                        Stream<String> strings = cells.stream()
                                .map(cellData -> (cellData != null && cellData.text != null) ? cellData.text : "");
                        csvWriter.writeRow(strings.toArray());
                    }
                }
            };

            CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);

        }

        @Override
        public String getContentType() {
            return "text/plain";
        }
    }

}
