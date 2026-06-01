/*

Copyright 2010, Google Inc.
Copyright 2024, OpenRefine contributors
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
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public class MarkdownExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "text/markdown";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, final Writer writer)
            throws IOException {

        TabularSerializer serializer = new TabularSerializer() {

            @Override
            public void startFile(JsonNode options) {
                // A Markdown table has no preamble; output begins with the header row.
            }

            @Override
            public void endFile() {
                // A Markdown table has no trailer.
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                try {
                    writer.write("|");
                    for (CellData cellData : cells) {
                        writer.write(" ");
                        writer.write((cellData != null && cellData.text != null) ? escapeMarkdown(cellData.text) : "");
                        writer.write(" |");
                    }
                    writer.write("\n");

                    if (isHeader) {
                        writer.write("|");
                        for (int i = 0; i < cells.size(); i++) {
                            writer.write(" --- |");
                        }
                        writer.write("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);
    }

    /**
     * Escapes characters that would otherwise break a Markdown table cell: pipes are the cell delimiter, and a cell may
     * not span multiple lines.
     */
    static private String escapeMarkdown(String text) {
        return text.replace("|", "\\|").replaceAll("[\r\n]+", " ");
    }
}
