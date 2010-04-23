package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.util.ParsingUtilities;

/**
 * The super class of all calls that the client side can invoke, most of which
 * are AJAX calls.
 */
public abstract class Command {
    
    final static protected Logger logger = LoggerFactory.getLogger("command");
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        
        throw new NotImplementedException();
    };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        
        throw new NotImplementedException();
    };
    
    /**
     * Utility function to get the browsing engine's configuration as a JSON object
     * from the "engine" request parameter, most often in the POST body.
     * 
     * @param request
     * @return
     * @throws Exception
     */
    static protected JSONObject getEngineConfig(HttpServletRequest request) throws Exception {
        String json = request.getParameter("engine");
        return (json == null) ? null : ParsingUtilities.evaluateJsonStringToObject(json);
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
    static protected Engine getEngine(HttpServletRequest request, Project project) throws Exception {
        Engine engine = new Engine(project);
        String json = request.getParameter("engine");
        if (json != null) {
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject(json);
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
    static protected Project getProject(HttpServletRequest request) throws ServletException {
        try {
            Project p = ProjectManager.singleton.getProject(Long.parseLong(request.getParameter("project")));
            if (p != null) {
                return p;
            }
        } catch (Exception e) {
            // ignore
        }
        throw new ServletException("Missing or bad project URL parameter");
    }
    
    static protected int getIntegerParameter(HttpServletRequest request, String name, int def) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (Exception e) {
            // ignore
        }
        return def;
    }
    
    static protected JSONObject getJsonParameter(HttpServletRequest request, String name) {
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
        throws IOException {
        
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Writer w = response.getWriter();
        w.write(content);
        w.flush();
        w.close();
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
        
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        
        o.write(writer, options);
        w.flush();
        w.close();
    }
    
    static protected void respondException(HttpServletResponse response, Exception e) 
        throws IOException {
        
        logger.warn("Exception caught", e);

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
    
    static protected void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }
    
}
