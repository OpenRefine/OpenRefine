package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.util.ExpressionNominalValueGrouper;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;

public class FacetCount implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3 && args[1] instanceof String && args[2] instanceof String) {
        	Object choiceValue = args[0]; // choice value to look up
        	String facetExpression = (String) args[1];
        	String columnName = (String) args[2];
        	
        	Project project = (Project) bindings.get("project");
            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                return new EvalError("No such column named " + columnName);
            }
            
            String key = "nominal-bin:" + facetExpression;
            ExpressionNominalValueGrouper grouper = (ExpressionNominalValueGrouper) column.getPrecompute(key);
            if (grouper == null) {
				try {
	            	Evaluable eval = MetaParser.parse(facetExpression);
	            	Engine engine = new Engine(project);
	            	
	            	grouper = new ExpressionNominalValueGrouper(eval, columnName, column.getCellIndex());
	            	engine.getAllRows().accept(project, grouper);
	            	
	                column.setPrecompute(key, grouper);
				} catch (ParsingException e) {
					return new EvalError("Error parsing facet expression " + facetExpression);
				}
            }
            
            return grouper.getChoiceValueCountMultiple(choiceValue);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + 
        	" expects a choice value, an expression as a string, and a column name");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the facet count corresponding to the given choice value");
        writer.key("params"); writer.value("choiceValue, string facetExpression, string columnName");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }
}
