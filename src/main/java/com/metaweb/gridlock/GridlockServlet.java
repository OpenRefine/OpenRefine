package com.metaweb.gridlock;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GridlockServlet extends HttpServlet {
	private static final long serialVersionUID = 2386057901503517403L;
	
	static private boolean s_verbose = false;

	@Override
	public void init() throws ServletException {
		super.init();
		
		s_verbose = "true".equalsIgnoreCase(System.getProperty("verbose"));
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
    
    static public JSONObject evaluateJsonStringToObject(String s) throws JSONException {
    	JSONTokener t = new JSONTokener(s);
    	JSONObject o = (JSONObject) t.nextValue();
    	return o;
    }
    
    protected String encodeString(String s) {
    	return s.replace("\"", "\\\"");
    }
    
    protected void logException(Exception e) {
    	if (s_verbose) {
    		e.printStackTrace();
    	}
    }
}
