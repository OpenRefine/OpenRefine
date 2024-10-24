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
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.net.PercentEscaper;
import org.apache.commons.lang3.StringUtils;
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

public class ExportRowsCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger("ExportRowsCommand");

    /**
     * This command uses POST but is left CSRF-unprotected as it does not incur a state change. TODO: add CSRF
     * protection anyway, as it does not cost much and could still have prevented an XSS vulnerability
     */

    @Deprecated(since = "3.9")
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
        // This command triggers evaluation expression and therefore requires CSRF-protection
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        ProjectManager.singleton.setBusy(true);

        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            Map<String, String> params = getParameters(request);

            String format = params.get("format");
            Exporter exporter = ExporterRegistry.getExporter(format);
            if (exporter == null) {
                exporter = new CsvExporter('\t');
            }

            response.setHeader("Content-Type", exporter.getContentType());
            // in case the content-type is text/html, to avoid XSS attacks
            response.setHeader("Content-Security-Policy", "script-src 'none'; connect-src 'none'");

            String preview = params.get("preview");
            if (!"true".equals(preview)) {
                String path = request.getPathInfo();
                String filename = path.substring(path.lastIndexOf('/') + 1);
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.contains("Safari/") && !userAgent.contains("Chrome/")
                        && !userAgent.contains("Chromium/")) {
                    // Safari doesn't support rfc5897 and just wants straight UTF-8, but strip any controls to avoid
                    // complaints about potential request/response splitting attacks
                    response.setHeader("Content-Disposition", "attachment; filename=" + filename.replaceAll("\\p{Cntrl}", ""));
                } else {
                    // We use the full suite of rc5987 safe characters even though some of them might not make sense
                    // in a filename. The browser will drop any unsafe characters before saving the file.
                    PercentEscaper escaper = new PercentEscaper("!#$&+-.^_`|~", false);
                    // Fallback printable ASCII filename in case browser doesn't understand filename*
                    // (percent encoded, just in case)
                    String asciiFilename = escaper.escape(StringUtils.stripAccents(filename).replaceAll("[^ -~]", " "));
                    String rfc5987Filename = escaper.escape(filename);
                    response.setHeader("Content-Disposition",
                            "attachment; filename=" + asciiFilename + "; filename*=UTF-8' '" + rfc5987Filename);
                }
            }

            if (exporter instanceof WriterExporter) {
                String encoding = params.get("encoding");

                response.setCharacterEncoding(encoding != null ? encoding : "UTF-8");
                Writer writer = encoding == null ? response.getWriter() : new OutputStreamWriter(response.getOutputStream(), encoding);

                ((WriterExporter) exporter).export(project, params, engine, writer);
                writer.close();
            } else if (exporter instanceof StreamExporter) {
                response.setCharacterEncoding("UTF-8");

                OutputStream stream = response.getOutputStream();
                ((StreamExporter) exporter).export(project, params, engine, stream);
                stream.close();
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
