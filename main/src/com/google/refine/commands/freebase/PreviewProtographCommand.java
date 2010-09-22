package com.google.refine.commands.freebase;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.protograph.Protograph;
import com.google.refine.protograph.transpose.MqlwriteLikeTransposedNodeFactory;
import com.google.refine.protograph.transpose.Transposer;
import com.google.refine.protograph.transpose.TripleLoaderTransposedNodeFactory;
import com.google.refine.util.ParsingUtilities;

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
                TripleLoaderTransposedNodeFactory nodeFactory = new TripleLoaderTransposedNodeFactory(project, stringWriter);
                
                Transposer.transpose(project, filteredRows, protograph, protograph.getRootNode(0), nodeFactory);
                nodeFactory.flush();
                
                sb.append("\"tripleloader\" : ");
                sb.append(JSONObject.quote(stringWriter.toString()));
            }
            
            {
                StringWriter stringWriter = new StringWriter();
                MqlwriteLikeTransposedNodeFactory nodeFactory = new MqlwriteLikeTransposedNodeFactory(stringWriter);
                
                Transposer.transpose(project, filteredRows, protograph, protograph.getRootNode(0), nodeFactory);
                nodeFactory.flush();
                
                sb.append(", \"mqllike\" : ");
                sb.append(stringWriter.toString());
            }

            sb.append(" }");
            
            respond(response, sb.toString());
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
