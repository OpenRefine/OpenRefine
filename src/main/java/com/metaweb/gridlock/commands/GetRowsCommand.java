package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.model.Column;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public class GetRowsCommand extends Command {
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Project project = getProject(request);
		int start = Math.min(project.rows.size(), Math.max(0, getIntegerParameter(request, "start", 0)));
		int limit = Math.min(project.rows.size() - start, Math.max(0, getIntegerParameter(request, "limit", 20)));
		
		try {
			JSONObject o = new JSONObject();
		
			List<JSONObject> a = new ArrayList<JSONObject>(limit);
			try {
				for (int r = start; r < start + limit && r < project.rows.size(); r++) {
					Row row = project.rows.get(r);
					
					a.add(row.getJSON());
				}
			} catch (JSONException e) {
				respondException(response, e);
			}
			o.put("start", start);
			o.put("limit", limit);
			o.put("rows", a);
		
			respondJSON(response, o);
		} catch (JSONException e) {
			respondException(response, e);
		}
	}
}
