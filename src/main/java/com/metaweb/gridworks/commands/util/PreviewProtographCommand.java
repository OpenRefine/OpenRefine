package com.metaweb.gridworks.commands.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.protograph.transpose.MqlreadLikeTransposedNodeFactory;
import com.metaweb.gridworks.protograph.transpose.Transposer;

public class PreviewProtographCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
			String jsonString = request.getParameter("protograph");
			JSONObject json = jsonStringToObject(jsonString);
			Protograph protograph = Protograph.reconstruct(json);
			
			MqlreadLikeTransposedNodeFactory nodeFactory = new MqlreadLikeTransposedNodeFactory();
			
			Transposer.transpose(project, protograph, protograph.getRootNode(0), nodeFactory);
			
			JSONArray results = nodeFactory.getJSON();
			
			respond(response, "{ \"result\" : " + results.toString() + " }");
			
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
