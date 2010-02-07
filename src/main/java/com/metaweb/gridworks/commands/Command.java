package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.Project;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

public abstract class Command {
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	throw new NotImplementedException();
    };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	throw new NotImplementedException();
    };
    
    protected Project getProject(HttpServletRequest request) throws ServletException {
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
    
    protected int getIntegerParameter(HttpServletRequest request, String name, int def) throws ServletException {
    	try {
	    	return Integer.parseInt(request.getParameter(name));
    	} catch (Exception e) {
    		// ignore
    	}
    	return def;
    }
    
    protected void respond(HttpServletResponse response, String content) throws IOException {
    	response.setStatus(HttpServletResponse.SC_OK);
    	
    	OutputStream os = response.getOutputStream();
    	OutputStreamWriter osw = new OutputStreamWriter(os);
    	try {
    		osw.write(content);
    	} finally {
    		osw.flush();
    		osw.close();
    	}
    }
    
    protected void respondJSON(HttpServletResponse response, Jsonizable o) throws IOException, JSONException {
    	respondJSON(response, o, new Properties());
    }
    
    protected void respondJSON(HttpServletResponse response, Jsonizable o, Properties options) throws IOException, JSONException {
    	response.setCharacterEncoding("UTF-8");
    	response.setHeader("Content-Type", "application/json");
    	
		JSONWriter writer = new JSONWriter(response.getWriter());
		
		o.write(writer, options);
    }
    
    protected void respondException(HttpServletResponse response, Exception e) throws IOException {
        e.printStackTrace();
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
			
	    	response.setHeader("Content-Type", "application/json");
			respond(response, o.toString());
		} catch (JSONException e1) {
	   		e.printStackTrace(response.getWriter());
		}
    }
    
    protected void redirect(HttpServletResponse response, String url) throws IOException {
    	response.setStatus(HttpServletResponse.SC_OK);
    	
    	String content = "<html><head><meta http-equiv=\"refresh\" content=\"1;url=" + url + "\"></head><body></body></html>";
    	response.getWriter().print(content);
	}
    
    protected String readFileUpload(HttpServletRequest request, Properties properties) throws IOException {
    	StringBuffer sb = new StringBuffer();
		try {
			MultipartParser parser = new MultipartParser(request, 20 * 1024 * 1024);
			Part part = null;
			while ((part = parser.readNextPart()) != null) {
				
				if (part.isFile()) {
					Reader reader = new InputStreamReader(((FilePart) part).getInputStream());
					LineNumberReader lnr = new LineNumberReader(reader);
					try {
						String line = null;
						while ((line = lnr.readLine()) != null) {
							sb.append(line);
							sb.append('\n');
						}
					} finally {
						lnr.close();
					}
				} else if (part.isParam()) {
					ParamPart paramPart = (ParamPart) part;
					properties.put(part.getName(), paramPart.getStringValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sb.toString();
    }
    
    protected JSONObject getJsonParameter(HttpServletRequest request, String name) {
    	String value = request.getParameter(name);
    	if (value != null) {
    		try {
				JSONObject o = jsonStringToObject(value);
				
				return o;
			} catch (JSONException e) {
			}
    	}
    	return null;
    }
    
    protected JSONObject jsonStringToObject(String s) throws JSONException {
    	JSONTokener t = new JSONTokener(s);
    	JSONObject o = (JSONObject) t.nextValue();
    	return o;
    }
    
    protected JSONArray jsonStringToArray(String s) throws JSONException {
    	JSONTokener t = new JSONTokener(s);
    	JSONArray a = (JSONArray) t.nextValue();
    	return a;
    }
    
    protected JSONObject getEngineConfig(HttpServletRequest request) throws Exception {
		String json = request.getParameter("engine");
		if (json != null) {
			return jsonStringToObject(json);
		}
		return null;
    }
    
    protected Engine getEngine(HttpServletRequest request, Project project) throws Exception {
		Engine engine = new Engine(project);
		String json = request.getParameter("engine");
		if (json != null) {
			JSONObject o = jsonStringToObject(json);
			engine.initializeFromJSON(o);
		}
		return engine;
    }
}
