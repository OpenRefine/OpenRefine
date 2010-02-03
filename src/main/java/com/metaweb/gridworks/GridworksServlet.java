package com.metaweb.gridworks;

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

import com.metaweb.gridworks.commands.ApproveNewReconcileCommand;
import com.metaweb.gridworks.commands.ApproveReconcileCommand;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.commands.ComputeFacetsCommand;
import com.metaweb.gridworks.commands.CreateProjectFromUploadCommand;
import com.metaweb.gridworks.commands.DiscardReconcileCommand;
import com.metaweb.gridworks.commands.DoTextTransformCommand;
import com.metaweb.gridworks.commands.GetAllProjectMetadataCommand;
import com.metaweb.gridworks.commands.GetColumnModelCommand;
import com.metaweb.gridworks.commands.GetHistoryCommand;
import com.metaweb.gridworks.commands.GetProcessesCommand;
import com.metaweb.gridworks.commands.GetProjectMetadataCommand;
import com.metaweb.gridworks.commands.GetRowsCommand;
import com.metaweb.gridworks.commands.ReconcileCommand;
import com.metaweb.gridworks.commands.UndoRedoCommand;

public class GridworksServlet extends HttpServlet {
	private static final long serialVersionUID = 2386057901503517403L;
	
	static protected Map<String, Command> _commands = new HashMap<String, Command>();
	
	static {
		_commands.put("create-project-from-upload", new CreateProjectFromUploadCommand());
		
		_commands.put("get-project-metadata", new GetProjectMetadataCommand());
		_commands.put("get-all-project-metadata", new GetAllProjectMetadataCommand());
		
		_commands.put("get-column-model", new GetColumnModelCommand());
		_commands.put("get-rows", new GetRowsCommand());
		_commands.put("get-processes", new GetProcessesCommand());
		_commands.put("get-history", new GetHistoryCommand());
		
		_commands.put("undo-redo", new UndoRedoCommand());
		_commands.put("compute-facets", new ComputeFacetsCommand());
		_commands.put("do-text-transform", new DoTextTransformCommand());
		
		_commands.put("reconcile", new ReconcileCommand());
		_commands.put("approve-reconcile", new ApproveReconcileCommand());
		_commands.put("approve-new-reconcile", new ApproveNewReconcileCommand());
		_commands.put("discard-reconcile", new DiscardReconcileCommand());
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
