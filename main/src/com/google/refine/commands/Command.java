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
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.HttpResponder;
import com.google.refine.RefineServlet;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.browsing.Engine;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

/**
 * The super class of all calls that the client side can invoke, most of which
 * are AJAX calls.
 */
public abstract class Command implements HttpResponder {

    final static protected Logger logger = LoggerFactory.getLogger("command");

    protected RefineServlet servlet;
    
    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    /**
     * Utility function to get the browsing engine's configuration as a JSON object
     * from the "engine" request parameter, most often in the POST body.
     *
     * @param request
     * @return
     * @throws JSONException
     */
    static protected JSONObject getEngineConfig(HttpServletRequest request)
    throws JSONException {
        if (request == null) throw new IllegalArgumentException("parameter 'request' should not be null");

        String json = request.getParameter("engine");
        try{
            return (json == null) ? null : ParsingUtilities.evaluateJsonStringToObject(json);
        } catch (JSONException e){
            logger.debug( json + " could not be parsed to JSON");
            return null;
        }
    }

    /**
     * Utility function to reconstruct the browsing engine from the "engine" request parameter,
     * most often in the POST body.
     *
     * @param request
     * @param project
     * @return
     * @throws Exception
     */
    static protected Engine getEngine(HttpServletRequest request, Project project)
    throws Exception {
        if (request == null) throw new IllegalArgumentException("parameter 'request' should not be null");
        if (project == null) throw new IllegalArgumentException("parameter 'project' should not be null");

        Engine engine = new Engine(project);
        JSONObject o = getEngineConfig(request);
        if (o != null)
            engine.initializeFromJSON(o);
        return engine;
    }

    /**
     * Utility method for retrieving the Project object having the ID specified
     * in the "project" URL parameter.
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
     * Utility method for retrieving the ProjectMetadata object having the ID specified
     * in the "project" URL parameter.
     *
     * @param request
     * @return
     * @throws ServletException
     */
    protected ProjectMetadata getProjectMetadata(HttpServletRequest request) throws ServletException {
        if (request == null) throw new IllegalArgumentException("parameter 'request' should not be null");
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
    
    static protected void performProcessAndRespond(
        HttpServletRequest request,
        HttpServletResponse response,
        Project project,
        Process process
    ) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        HistoryEntry historyEntry = project.processManager.queueProcess(process);
        if (historyEntry != null) {
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            Properties options = new Properties();

            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("historyEntry"); historyEntry.write(writer, options);
            writer.endObject();

            w.flush();
            w.close();
        } else {
            HttpUtilities.respond(response, "{ \"code\" : \"pending\" }");
        }
    }

    protected void respondWithErrorPage(
        HttpServletRequest request, 
        HttpServletResponse response, 
        String message, 
        Throwable e
    ) {
        HttpUtilities.respondWithErrorPage(servlet, request, response, message, e);
    }

}
