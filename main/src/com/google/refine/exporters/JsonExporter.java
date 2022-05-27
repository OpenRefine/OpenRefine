package com.google.refine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class JsonExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, final Writer writer)
            throws IOException {

        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JsonNode options) {
                try {

                    CsvSchema csv = CsvSchema.emptySchema().withHeader();
                    CsvMapper csvMapper = new CsvMapper();
                    MappingIterator<Map<?, ?>> mappingIterator =  csvMapper.reader().forType(Map.class).with(csv).readValues(input);
                    List<Map<?, ?>> list = mappingIterator.readAll();
                    System.out.println(list);

                } catch (IOException e) {
                    // Ignore
                }
            }

            @Override
            public void endFile() {
                try {
                    writer.write("List\n");
                } catch (IOException e) {
                    // Ignore
                }
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                try {
                    writer.write("{");
                    if (isHeader) {
                        for (CellData cellData : cells) {
                            writer.write("<th>");
                            writer.write((cellData != null && cellData.text != null) ? cellData.text : "");
                            writer.write("</th>");
                        }
                    } else {
                        for (CellData cellData : cells) {
                            writer.write("{");
                            if (cellData != null && cellData.text != null) {
                                if (cellData.link != null) {
                                    writer.write(",");
                                }
                                writer.write(StringEscapeUtils.escapeXml10(cellData.text));
                                if (cellData.link != null) {
                                    writer.write(",");
                                }
                            }
                            writer.write("}");
                        }
                    }
                    writer.write("}\n");
                } catch (IOException e) {
                    // Ignore
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);
    }
}

