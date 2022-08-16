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

package com.google.refine.commands.project;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.exporters.CsvExporter;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.ExporterRegistry;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.exporters.sql.SqlExporterException;
import com.google.refine.model.Project;
import com.google.common.net.PercentEscaper;

public class ExportRowsCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger("ExportRowsCommand");

    /**
     * This command uses POST but is left CSRF-unprotected as it does not incur a state change.
     */

    @SuppressWarnings("unchecked")
    static public Properties getRequestParameters(HttpServletRequest request) {
        Properties options = new Properties();

        Enumeration<String> en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            options.put(name, request.getParameter(name));
        }
        return options;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProjectManager.singleton.setBusy(true);

        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            Properties params = getRequestParameters(request);

            String format = params.getProperty("format");
            Exporter exporter = ExporterRegistry.getExporter(format);
            if (exporter == null) {
                exporter = new CsvExporter('\t');
            }

            String contentType = params.getProperty("contentType");
            if (contentType == null) {
                contentType = exporter.getContentType();
            }
            response.setHeader("Content-Type", contentType);

            String preview = params.getProperty("preview");
            if (!"true".equals(preview)) {
                String path = request.getPathInfo();
                String filename = path.substring(path.lastIndexOf('/') + 1);
                PercentEscaper escaper = new PercentEscaper("", false);
                filename = escaper.escape(filename);
                response.setHeader("Content-Disposition", "attachment; filename=" + filename + "; filename*=utf-8' '" + filename);
            }

            if (exporter instanceof WriterExporter) {
                String encoding = params.getProperty("encoding");

                response.setCharacterEncoding(encoding != null ? encoding : "UTF-8");
                Writer writer = encoding == null ? response.getWriter() : new OutputStreamWriter(response.getOutputStream(), encoding);

                ((WriterExporter) exporter).export(project, params, engine, writer);
                writer.close();
            } else if (exporter instanceof StreamExporter) {
                response.setCharacterEncoding("UTF-8");

                OutputStream stream = response.getOutputStream();
                ((StreamExporter) exporter).export(project, params, engine, stream);
                stream.close();
//          } else if (exporter instanceof UrlExporter) {
//              ((UrlExporter) exporter).export(project, options, engine);

            } else {
                // TODO: Should this use ServletException instead of respondException?
                respondException(response, new RuntimeException("Unknown exporter type"));
            }
        } catch (Exception e) {
            // Use generic error handling rather than our JSON handling
            logger.info("error:{}", e.getMessage());
            if (e instanceof SqlExporterException) {
                response.sendError(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            }
            throw new ServletException(e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
