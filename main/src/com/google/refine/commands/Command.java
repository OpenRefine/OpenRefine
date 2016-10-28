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
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.browsing.Engine;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

/**
 * The super class of all calls that the client side can invoke, most of which
 * are AJAX calls.
 */
public abstract class Command {

    final static protected Logger logger = LoggerFactory.getLogger("command");

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

    public void doPut(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        throw new UnsupportedOperationException();
    };

    /**
     * Whether each request to this command should be logged. For some commands
     * that can get called too frequently, such as GetProcessesCommand, logging
     * is very distracting.
     */
    public boolean logRequests() {
        return true;
    }

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
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        
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
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        if (project == null) {
            throw new IllegalArgumentException("parameter 'project' should not be null");
        }

        Engine engine = new Engine(project);
        JSONObject o = getEngineConfig(request);
        if (o != null) {
            engine.initializeFromJSON(o);
        }
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

    static protected JSONObject getJsonParameter(HttpServletRequest request, String name) {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        String value = request.getParameter(name);
        if (value != null) {
            try {
                return ParsingUtilities.evaluateJsonStringToObject(value);
            } catch (JSONException e) {
                logger.warn("error getting json parameter",e);
            }
        }
        return null;
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
            respond(response, "{ \"code\" : \"pending\" }");
        }
    }

    static protected void respond(HttpServletResponse response, String content)
        throws IOException, ServletException {

        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Writer w = response.getWriter();
        if (w != null) {
            w.write(content);
            w.flush();
            w.close();
        } else {
            throw new ServletException("response returned a null writer");
        }
    }

    static protected void respond(HttpServletResponse response, String status, String message)
        throws IOException, JSONException {

        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        writer.object();
        writer.key("status"); writer.value(status);
        writer.key("message"); writer.value(message);
        writer.endObject();
        w.flush();
        w.close();
    }

    static protected void respondJSON(HttpServletResponse response, Jsonizable o)
        throws IOException, JSONException {

        respondJSON(response, o, new Properties());
    }

    static protected void respondJSON(
            HttpServletResponse response, Jsonizable o, Properties options)
            throws IOException, JSONException {

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-cache");

        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);

        o.write(writer, options);
        w.flush();
        w.close();
    }

    static protected void respondException(HttpServletResponse response, Exception e)
        throws IOException, ServletException {

        logger.warn("Exception caught", e);

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }

        try {
            JSONObject o = new JSONObject();
            o.put("code", "error");
            o.put("message", e.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();

            o.put("stack", sw.toString());

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            respond(response, o.toString());
        } catch (JSONException e1) {
            e.printStackTrace(response.getWriter());
        }
    }
    
    protected void respondWithErrorPage(
        HttpServletRequest request, 
        HttpServletResponse response, 
        String message, 
        Throwable e
    ) {
        VelocityContext context = new VelocityContext();
        
        context.put("message", message);
        
        if (e != null) {
            StringWriter writer = new StringWriter();
            
            e.printStackTrace(new PrintWriter(writer));
            
            context.put("stack", writer.toString());
        } else {
            context.put("stack", "");
        }
        
        try {
            servlet.getModule("core").sendTextFromTemplate(
                request, response, context, "error.vt", "UTF-8", "text/html", true);
            
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    static protected void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }

}
