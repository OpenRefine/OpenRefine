package com.metaweb.gridlock;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.metaweb.gridlock.commands.Command;
import com.metaweb.gridlock.commands.CreateProjectFromUploadCommand;
import com.metaweb.gridlock.commands.GetColumnModelCommand;
import com.metaweb.gridlock.commands.GetRowsCommand;

public class GridlockServlet extends HttpServlet {
	private static final long serialVersionUID = 2386057901503517403L;
	
	static protected Map<String, Command> _commands = new HashMap<String, Command>();
	
	static {
		_commands.put("create-project-from-upload", new CreateProjectFromUploadCommand());
		_commands.put("get-column-model", new GetColumnModelCommand());
		_commands.put("get-rows", new GetRowsCommand());
	}

	@Override
	public void init() throws ServletException {
		super.init();
	}
	
	@Override
	public void destroy() {
		ProjectManager.singleton.saveAllProjects();
		ProjectManager.singleton.save();
		ProjectManager.singleton = null;
		
		super.destroy();
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ProjectManager.initialize(new File("./data"));
		
    	String commandName = request.getPathInfo().substring(1);
    	Command command = _commands.get(commandName);
    	if (command != null) {
    		command.doPost(request, response);
    	}
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ProjectManager.initialize(new File("./data"));
		
    	String commandName = request.getPathInfo().substring(1);
    	Command command = _commands.get(commandName);
    	if (command != null) {
    		command.doGet(request, response);
    	}
    }
    
    static public JSONObject evaluateJsonStringToObject(String s) throws JSONException {
    	JSONTokener t = new JSONTokener(s);
    	JSONObject o = (JSONObject) t.nextValue();
    	return o;
    }
    
    protected String encodeString(String s) {
    	return s.replace("\"", "\\\"");
    }
}
