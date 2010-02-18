package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.operations.ApproveNewReconOperation;
import com.metaweb.gridworks.model.operations.ApproveReconOperation;
import com.metaweb.gridworks.model.operations.ColumnAdditionOperation;
import com.metaweb.gridworks.model.operations.ColumnRemovalOperation;
import com.metaweb.gridworks.model.operations.DiscardReconOperation;
import com.metaweb.gridworks.model.operations.MultiValueCellJoinOperation;
import com.metaweb.gridworks.model.operations.MultiValueCellSplitOperation;
import com.metaweb.gridworks.model.operations.ReconOperation;
import com.metaweb.gridworks.model.operations.SaveProtographOperation;
import com.metaweb.gridworks.model.operations.TextTransformOperation;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.protograph.Protograph;

public class ApplyOperationsCommand extends Command {
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Project project = getProject(request);
		String jsonString = request.getParameter("operations");
		try {
			JSONArray a = jsonStringToArray(jsonString);
			int count = a.length();
			for (int i = 0; i < count; i++) {
				JSONObject obj = a.getJSONObject(i);
				
				reconstructOperation(project, obj);
			}

			respond(response, "{ \"code\" : \"pending\" }");
		} catch (JSONException e) {
			respondException(response, e);
		}
	}
	
	protected void reconstructOperation(Project project, JSONObject obj) {
		try {
			String op = obj.getString("op");
			AbstractOperation operation = null;
			
			JSONObject engineConfig = obj.has("engineConfig") ? obj.getJSONObject("engineConfig") : null;
			String columnName = obj.has("columnName") ? obj.getString("columnName") : null;
			
			if ("approve-new-recon".equals(op)) {
				operation = new ApproveNewReconOperation(engineConfig, columnName);
			} else if ("approve-recon".equals(op)) {
				operation = new ApproveReconOperation(engineConfig, columnName);
			} else if ("add-column".equals(op)) {
				operation = new ColumnAdditionOperation(
					engineConfig,
					obj.getString("baseColumnName"),
					obj.getString("expression"),
					obj.getString("headerLabel"),
					obj.getInt("columnInsertIndex")
				);
			} else if ("remove-column".equals(op)) {
				operation = new ColumnRemovalOperation(columnName);
			} else if ("discard-recon".equals(op)) {
				operation = new DiscardReconOperation(engineConfig, columnName);
			} else if ("join-multivalued-cells".equals(op)) {
				operation = new MultiValueCellJoinOperation(
					columnName,
					obj.getString("keyColumnName"),
					obj.getString("separator")
				);
			} else if ("split-multivalued-cells".equals(op)) {
				operation = new MultiValueCellSplitOperation(
					columnName,
					obj.getString("keyColumnName"),
					obj.getString("separator"),
					obj.getString("mode")
				);
			} else if ("recon".equals(op)) {
				operation = new ReconOperation(
					engineConfig, 
					columnName,
					obj.getString("typeID")
				);
			} else if ("save-protograph".equals(op)) {
				operation = new SaveProtographOperation(
					Protograph.reconstruct(obj.getJSONObject("protograph"))
				);
			} else if ("text-transform".equals(op)) {
				operation = new TextTransformOperation(
					engineConfig, 
					columnName,
					obj.getString("expression")
				);
			}
			
			if (operation != null) {
				Process process = operation.createProcess(project, new Properties());
				
				project.processManager.queueProcess(process);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
