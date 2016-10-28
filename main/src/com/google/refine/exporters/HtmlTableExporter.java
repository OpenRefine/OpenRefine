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
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public class HtmlTableExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, final Writer writer)
        throws IOException {
        
        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JSONObject options) {
                try {
                    writer.write("<html>\n");
                    writer.write("<head>\n");
                    writer.write("<title>"); 
                    writer.write(ProjectManager.singleton.getProjectMetadata(project.id).getName());
                    writer.write("</title>\n");
                    writer.write("<meta charset=\"utf-8\" />\n");
                    writer.write("</head>\n");
    
                    writer.write("<body>\n");
                    writer.write("<table>\n");
                } catch (IOException e) {
                    // Ignore
                }
            }

            @Override
            public void endFile() {
                try {
                    writer.write("</table>\n");
                    writer.write("</body>\n");
                    writer.write("</html>\n");
                } catch (IOException e) {
                    // Ignore
                }
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                try {
                    writer.write("<tr>");
                    if (isHeader) {
                        for (CellData cellData : cells) {
                            writer.write("<th>");
                            writer.write((cellData != null && cellData.text != null) ? cellData.text : "");
                            writer.write("</th>");
                        }
                    } else {
                        for (CellData cellData : cells) {
                            writer.write("<td>");
                            if (cellData != null && cellData.text != null) {
                                if (cellData.link != null) {
                                    writer.write("<a href=\"");
                                    // TODO: The escape below looks wrong, but is probably harmless in most cases
                                    writer.write(StringEscapeUtils.escapeHtml(cellData.link));
                                    writer.write("\">");
                                }
                                writer.write(StringEscapeUtils.escapeXml(cellData.text));
                                if (cellData.link != null) {
                                    writer.write("</a>");
                                }
                            }
                            writer.write("</td>");
                        }
                    }
                    writer.write("</tr>\n");
                } catch (IOException e) {
                    // Ignore
                }
            }
        };
        
        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);
    }
}
