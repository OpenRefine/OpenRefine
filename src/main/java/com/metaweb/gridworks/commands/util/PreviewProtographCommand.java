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
import com.metaweb.gridworks.protograph.transpose.TripleLoaderTransposedNodeFactory;

public class PreviewProtographCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
			
			String jsonString = request.getParameter("protograph");
			JSONObject json = jsonStringToObject(jsonString);
			Protograph protograph = Protograph.reconstruct(json);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{ ");
			
			{
				TripleLoaderTransposedNodeFactory nodeFactory = new TripleLoaderTransposedNodeFactory();
				
				Transposer.transpose(project, protograph, protograph.getRootNode(0), nodeFactory);
				
				sb.append("\"tripleloader\" : ");
				sb.append(JSONObject.quote(nodeFactory.getLoad()));
			}
			
			{
				MqlreadLikeTransposedNodeFactory nodeFactory = new MqlreadLikeTransposedNodeFactory();
				
				Transposer.transpose(project, protograph, protograph.getRootNode(0), nodeFactory);
				
				JSONArray results = nodeFactory.getJSON();
				
				sb.append(", \"mqllike\" : ");
				sb.append(results.toString());
			}

			sb.append(" }");
			
			respond(response, sb.toString());
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
