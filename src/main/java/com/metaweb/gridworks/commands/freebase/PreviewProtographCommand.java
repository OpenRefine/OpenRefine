package com.metaweb.gridworks.commands.freebase;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.protograph.transpose.MqlreadLikeTransposedNodeFactory;
import com.metaweb.gridworks.protograph.transpose.Transposer;
import com.metaweb.gridworks.protograph.transpose.TripleLoaderTransposedNodeFactory;
import com.metaweb.gridworks.util.ParsingUtilities;

public class PreviewProtographCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            FilteredRows filteredRows = engine.getAllFilteredRows();
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            String jsonString = request.getParameter("protograph");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            Protograph protograph = Protograph.reconstruct(json);
            
            StringBuffer sb = new StringBuffer(2048);
            sb.append("{ ");
            
            {
                StringWriter stringWriter = new StringWriter();
                TripleLoaderTransposedNodeFactory nodeFactory = new TripleLoaderTransposedNodeFactory(stringWriter);
                
                Transposer.transpose(project, filteredRows, protograph, protograph.getRootNode(0), nodeFactory);
                nodeFactory.flush();
                
                sb.append("\"tripleloader\" : ");
                sb.append(JSONObject.quote(stringWriter.toString()));
            }
            
            {
                MqlreadLikeTransposedNodeFactory nodeFactory = new MqlreadLikeTransposedNodeFactory();
                
                Transposer.transpose(project, filteredRows, protograph, protograph.getRootNode(0), nodeFactory);
                
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
