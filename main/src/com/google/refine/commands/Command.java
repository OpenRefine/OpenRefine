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

package com.google.refine.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

/**
 * The super class of all calls that the client side can invoke, most of which are AJAX calls.
 */
public abstract class Command {

    final static protected Logger logger = LoggerFactory.getLogger("command");

    final static public CSRFTokenFactory csrfFactory = new CSRFTokenFactory(3600, 32);

    protected RefineServlet servlet;

    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    public void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    /**
     * Whether each request to this command should be logged. For some commands that can get called too frequently, such
     * as GetProcessesCommand, logging is very distracting.
     */
    public boolean logRequests() {
        return true;
    }

    /**
     * Utility function to get the browsing engine's configuration as a JSON object from the "engine" request parameter,
     * most often in the POST body.
     *
     * @param request
     * @return
     */
    static protected EngineConfig getEngineConfig(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }

        String json = request.getParameter("engine");
        return (json == null) ? null : EngineConfig.reconstruct(json);
    }

    /**
     * Utility function to reconstruct the browsing engine from the "engine" request parameter, most often in the POST
     * body.
     *
     * @param request
     * @param project
     * @return
     * @throws Exception
     */
    static protected Engine getEngine(HttpServletRequest request, Project project)
            throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        if (project == null) {
            throw new IllegalArgumentException("parameter 'project' should not be null");
        }

        Engine engine = new Engine(project);
        EngineConfig c = getEngineConfig(request);
        if (c != null) {
            engine.initializeFromConfig(c);
        }
        return engine;
    }

    /**
     * Utility method for retrieving the Project object having the ID specified in the "project" URL parameter.
     *
     * @param request
     * @return
     * @throws ServletException
     */
    protected Project getProject(HttpServletRequest request) throws ServletException {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        String param = request.getParameter("project");
        if (param == null || "".equals(param)) {
            throw new ServletException("Can't find project: missing ID parameter");
        }
        Long id;
        try {
            id = Long.parseLong(param);
        } catch (NumberFormatException e) {
            throw new ServletException("Can't find project: badly formatted id #", e);
        }
        Project p = ProjectManager.singleton.getProject(id);
        if (p != null) {
            return p;
        } else {
            throw new ServletException("Failed to find project id #" + param + " - may be corrupt");
        }
    }

    /**
     * Utility method for retrieving the ProjectMetadata object having the ID specified in the "project" URL parameter.
     *
     * @param request
     * @return
     * @throws ServletException
     */
    protected ProjectMetadata getProjectMetadata(HttpServletRequest request) throws ServletException {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        try {
            ProjectMetadata pm = ProjectManager.singleton.getProjectMetadata(Long.parseLong(request.getParameter("project")));
            if (pm != null) {
                return pm;
            }
        } catch (Exception e) {
            // ignore
        }
        throw new ServletException("Can't find project metadata: missing or bad URL parameter");
    }

    static protected int getIntegerParameter(HttpServletRequest request, String name, int def) {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (Exception e) {
            // ignore
        }
        return def;
    }

    /**
     * Utility method for retrieving the CSRF token stored in the "csrf_token" parameter of the request, and checking
     * that it is valid.
     *
     * @param request
     * @return
     * @throws ServletException
     */
    protected boolean hasValidCSRFToken(HttpServletRequest request) throws ServletException {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        try {
            String token = request.getParameter("csrf_token");
            return token != null && csrfFactory.validToken(token);
        } catch (Exception e) {
            // ignore
        }
        throw new ServletException("Can't find CSRF token: missing or bad URL parameter");
    }

    /**
     * Checks the validity of a CSRF token, without reading the whole POST body. Useful when we need to control how the
     * POST body is read (for instance if it contains files).
     */
    protected boolean hasValidCSRFTokenAsGET(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        Map<String, String> options = ParsingUtilities.parseParameters(request);
        String token = options.get("csrf_token");
        return token != null && csrfFactory.validToken(token);
    }

    protected static class HistoryEntryResponse {

        @JsonProperty("code")
        protected String getCode() {
            return "ok";
        }

        @JsonProperty("historyEntry")
        protected HistoryEntry historyEntry;

        protected HistoryEntryResponse(HistoryEntry entry) {
            historyEntry = entry;
        }
    }

    static protected void performProcessAndRespond(
            HttpServletRequest request,
            HttpServletResponse response,
            Project project,
            Process process) throws Exception {

        HistoryEntry historyEntry = project.processManager.queueProcess(process);
        if (historyEntry != null) {
            respondJSON(response, new HistoryEntryResponse(historyEntry));
        } else {
            respondCodePending(response);
        }
    }

    static protected void respondStatusOk(HttpServletResponse response) throws IOException {
        respondStatusOk(response, null);
    }

    static protected void respondStatusOk(HttpServletResponse response, String message) throws IOException {
        respondStatus(response, "ok", message);
    }

    // TODO: This seems like unnecessary redundancy, but can be reviewed along with the rest of the API responses
    static protected void respondOkDone(HttpServletResponse response) throws IOException {
        respondStatusOk(response, "done");
    }

    static protected void respondCodePending(HttpServletResponse response) throws IOException {
        respondJSON(response, Map.of("code", "pending"));
    }

    static protected void respond(HttpServletResponse response, String content)
            throws IOException, ServletException {
        HttpUtilities.respond(response, content);
    }

    /**
     * @deprecated for 3.8 renamed to {@link #respondStatus(HttpServletResponse, String, String)} to disambiguate the
     *             from the standard respond which sets `code` instead of `status`
     */
    @Deprecated
    static protected void respond(HttpServletResponse response, String status, String message)
            throws IOException {
        respondStatus(response, status, message);
    }

    static protected void respondStatus(HttpServletResponse response, String status, String message)
            throws IOException {
        // NOTE: This method sets "status", not "code" in the reply
        HttpUtilities.respond(response, status, message);
    }

    static protected void respondStatusError(HttpServletResponse response, String message)
            throws IOException {
        // NOTE: This method sets "status", not "code" in the reply
        respondStatus(response, "error", message);
    }

    public static void respondJSON(HttpServletResponse response, Object o)
            throws IOException {

        respondJSON(response, o, new Properties());
    }

    // TODO: options parameter is ignored here, but the only usage is an empty Properties object
    @Deprecated
    static protected void respondJSON(
            HttpServletResponse response, Object o, Properties options)
            throws IOException {

        // TODO: Inline the HttpUtilities code here when deprecation period expires
        HttpUtilities.respondJSON(response, o, options);
    }

    static protected void respondCSRFError(HttpServletResponse response) throws IOException {
        respondCodeError(response, "Missing or invalid csrf_token parameter");
    }

    static protected void respondCodeError(HttpServletResponse response, String message) throws IOException {
        respondCodeError(response, message, HttpServletResponse.SC_BAD_REQUEST);
    }

    static protected void respondCodeError(HttpServletResponse response, String message, int statusCode) throws IOException {
        respondCodeError(response, message, statusCode, null);
    }

    static protected void respondCodeError(HttpServletResponse response, String message, int statusCode, String stackTrace)
            throws IOException {
        // TODO: We always return OK for now as has been the historical practice, but would like to return meaningful
        // HTTP status codes in the future.
//        response.setStatus(statusCode);
        Map<String, String> responseJSON = new HashMap<>();
        responseJSON.put("code", "error");
        if (message != null) {
            responseJSON.put("message", message);
        }
        if (stackTrace != null) {
            responseJSON.put("stack", stackTrace);
        }
        respondJSON(response, responseJSON);
    }

    static protected void respondException(HttpServletResponse response, Exception e)
            throws IOException, ServletException {

        logger.warn("Exception caught", e);
        e.printStackTrace();

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }

        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            // FIXME: status is currently ignored, but switch this when supported
//            respondCodeError(response, e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sw.toString());
            respondCodeError(response, e.toString(), HttpServletResponse.SC_OK, sw.toString());
        }
    }

    /**
     * Used by @link DefaultImportingController} and GData importer to report non-fatal exceptions
     */
    static protected void respondStatusErrors(HttpServletResponse response, List<Exception> exceptions)
            throws IOException, ServletException {
        respondJSON(response, Map.of("status", "error", "errors", exceptions));
    }

    protected void respondWithErrorPage(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            Throwable e) {
        // TODO: inline implementation from {@link HttpUtilities#respondWithErrorPage} when deprecation period expires
        HttpUtilities.respondWithErrorPage(servlet, request, response, message, e);
    }

    static protected void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }

}
