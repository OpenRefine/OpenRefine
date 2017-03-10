package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.commands.expr.PreviewExpressionCommand;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class PreviewRdfValueExpressionCommand extends PreviewExpressionCommand{

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
            Project project = getProject(request);
            
            String columnName = request.getParameter("columnName");
            String uri = request.getParameter("isUri");
            boolean isUri = uri!=null && uri.equals("1") ? true:false;
            
            String expression = request.getParameter("expression");
            String rowIndicesString = request.getParameter("rowIndices");
            if (rowIndicesString == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
                return;
            }
            
            String baseUri = request.getParameter("baseUri");
            URI base;
            try{
            	base = new URI(baseUri);
            }catch(URISyntaxException ex){
            	respond(response, "{ \"code\" : \"error\", \"message\" : \"Invalie Base URI\" }");
                return;
            }
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONArray rowIndices = ParsingUtilities.evaluateJsonStringToArray(rowIndicesString);
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            if(isUri){
            	respondUriPreview(project, writer, rowIndices, expression, columnName, base);
            }else{
            	respondLiteralPreview(project, writer, rowIndices, expression, columnName);
            }
        } catch (Exception e) {
            respondException(response, e);
        }
	}
	
	private void respondUriPreview(Project project, JSONWriter writer, JSONArray rowIndices, String expression, String columnName, URI base) throws JSONException{
		int length = rowIndices.length();
        
        writer.object();
        
        try {
            writer.key("results"); writer.array();
            String[] absolutes = new String[length];
            for (int i = 0; i < length; i++) {
                Object result = null;
                absolutes[i] = null;
                int rowIndex = rowIndices.getInt(i);
                if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                    Row row = project.rows.get(rowIndex);
                    result = Util.evaluateExpression(project, expression, columnName, row, rowIndex); 
                }
                
                if (result == null) {
                    writer.value(null);
                } else if (ExpressionUtils.isError(result)) {
                    writer.object();
                    writer.key("message"); writer.value(((EvalError) result).message);
                    writer.endObject();
                } else {
                	StringBuffer sb = new StringBuffer();
                    writeValue(sb, result, false);
                    writer.value(sb.toString());
                    //prepare absolute value                    
                	if (result.getClass().isArray()) {
                		int lngth = Array.getLength(result);
                		StringBuilder resolvedUrisVal = new StringBuilder("[");
                		for(int k=0;k<lngth;k++){
                			resolvedUrisVal.append(Util.resolveUri(base,Array.get(result, k).toString()));
                			if(k<lngth-1){
                				resolvedUrisVal.append(",");
                			}
                		}
                		resolvedUrisVal.append("]");
                		absolutes[i] = resolvedUrisVal.toString();
                	} else {
                        absolutes[i] = Util.resolveUri(base,sb.toString());
                	}
                }
            }
            writer.endArray();
            
            //writing the absolutes
            writer.key("absolutes"); writer.array();
            for (int i = 0; i < length; i++) {
            	writer.value(absolutes[i]);
            }
            writer.endArray();
            writer.key("code"); writer.value("ok");
        } catch (ParsingException e) {
        	writer.endArray();
            writer.key("code"); writer.value("error");
            writer.key("type"); writer.value("parser");
            writer.key("message"); writer.value(e.getMessage());
        } catch (Exception e) {
        	writer.endArray();
            writer.key("code"); writer.value("error");
            writer.key("type"); writer.value("other");
            writer.key("message"); writer.value(e.getMessage());
        }
        
        writer.endObject();

	}
	
	
	private void respondLiteralPreview(Project project, JSONWriter writer, JSONArray rowIndices, String expression, String columnName) throws JSONException{
		int length = rowIndices.length();
        
        writer.object();
        
        try {
            writer.key("results"); writer.array();
            for (int i = 0; i < length; i++) {
                Object result = null;
                int rowIndex = rowIndices.getInt(i);
                if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                    Row row = project.rows.get(rowIndex);
                    result = Util.evaluateExpression(project, expression, columnName, row, rowIndex); 
                }
                
                if (result == null) {
                    writer.value(null);
                } else if (ExpressionUtils.isError(result)) {
                    writer.object();
                    writer.key("message"); writer.value(((EvalError) result).message);
                    writer.endObject();
                } else {
                    StringBuffer sb = new StringBuffer();
                    writeValue(sb, result, false);
                    writer.value(sb.toString());
                }
            }
            writer.endArray();
            
            writer.key("code"); writer.value("ok");
        } catch (ParsingException e) {
            writer.key("code"); writer.value("error");
            writer.key("type"); writer.value("parser");
            writer.key("message"); writer.value(e.getMessage());
        } catch (Exception e) {
        	writer.endArray();
            writer.key("code"); writer.value("error");
            writer.key("type"); writer.value("other");
            writer.key("message"); writer.value(e.getMessage());
        }
        
        writer.endObject();

	}
}
