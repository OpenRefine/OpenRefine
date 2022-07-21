/*

Copyright 2011, Google Inc.
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

package com.google.refine.importing;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingManager.Format;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class DefaultImportingController implements ImportingController {

    protected RefineServlet servlet;

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        throw new ServletException("GET verb not implemented");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * The uploaded file is in the POST body as a "file part". If we call request.getParameter() then the POST body
         * will get read and we won't have a chance to parse the body ourselves. This is why we have to parse the URL
         * for parameters ourselves.
         */
        Properties parameters = ParsingUtilities.parseUrlParameters(request);
        String subCommand = parameters.getProperty("subCommand");
        if ("load-raw-data".equals(subCommand)) {
            doLoadRawData(request, response, parameters);
        } else if ("update-file-selection".equals(subCommand)) {
            doUpdateFileSelection(request, response, parameters);
        } else if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("update-format-and-options".equals(subCommand)) {
            doUpdateFormatAndOptions(request, response, parameters);
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }
    }

    private void doLoadRawData(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("new".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job already started; cannot load more data");
            return;
        }

        ImportingUtilities.loadDataAndPrepareJob(
                request, response, parameters, job, config);
        job.touch();
        job.updating = false;
    }

    private void doUpdateFileSelection(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        ArrayNode fileSelectionArray = ParsingUtilities.evaluateJsonStringToArrayNode(
                request.getParameter("fileSelection"));

        ImportingUtilities.updateJobWithNewFileSelection(job, fileSelectionArray);

        replyWithJobData(request, response, job);
        job.touch();
        job.updating = false;
    }

    private void doUpdateFormatAndOptions(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        String format = request.getParameter("format");
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        ImportingUtilities.previewParse(job, format, optionObj, exceptions);

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            if (exceptions.size() == 0) {
                job.project.update(); // update all internal models, indexes, caches, etc.

                writer.writeStringField("status", "ok");
            } else {
                writer.writeStringField("status", "error");
                writer.writeArrayFieldStart("errors");
                writeErrors(writer, exceptions);
                writer.writeEndArray();
            }
            writer.writeEndObject();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            w.flush();
            w.close();
        }
        job.touch();
        job.updating = false;
    }

    private void doInitializeParserUI(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        String format = request.getParameter("format");
        Format formatRecord = ImportingManager.formatToRecord.get(format);
        if (formatRecord != null && formatRecord.parser != null) {
            ObjectNode options = formatRecord.parser.createParserUIInitializationData(
                    job, job.getSelectedFileRecords(), format);
            ObjectNode result = ParsingUtilities.mapper.createObjectNode();
            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "options", options);

            Command.respondJSON(response, result);
        } else {
            HttpUtilities.respond(response, "error", "Unrecognized format or format has no parser");
        }
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        job.touch();
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        String format = request.getParameter("format");
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        ImportingUtilities.createProject(job, format, optionObj, exceptions, false);

        HttpUtilities.respond(response, "ok", "done");
    }

    protected static class JobResponse {

        @JsonProperty("code")
        protected String code;
        @JsonProperty("job")
        protected ImportingJob job;

        protected JobResponse(String code, ImportingJob job) {
            this.code = code;
            this.job = job;
        }

    }

    /**
     * return the job to the front end.
     * 
     * @param request
     * @param response
     * @param job
     * @throws ServletException
     * @throws IOException
     */
    private void replyWithJobData(HttpServletRequest request, HttpServletResponse response, ImportingJob job)
            throws ServletException, IOException {

        Writer w = response.getWriter();
        ParsingUtilities.defaultWriter.writeValue(w, new JobResponse("ok", job));
        w.flush();
        w.close();
    }

    static public void writeErrors(JsonGenerator writer, List<Exception> exceptions) throws IOException {
        for (Exception e : exceptions) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            writer.writeStartObject();
            writer.writeStringField("message", e.getLocalizedMessage());
            writer.writeStringField("stack", sw.toString());
            writer.writeEndObject();
        }
    }

    static public ArrayNode convertErrorsToJsonArray(List<Exception> exceptions) {
        ArrayNode a = ParsingUtilities.mapper.createArrayNode();
        for (Exception e : exceptions) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            ObjectNode o = ParsingUtilities.mapper.createObjectNode();
            JSONUtilities.safePut(o, "message", e.getLocalizedMessage());
            JSONUtilities.safePut(o, "stack", sw.toString());
            JSONUtilities.append(a, o);
        }
        return a;
    }

}
