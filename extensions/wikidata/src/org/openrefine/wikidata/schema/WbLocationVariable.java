package org.openrefine.wikidata.schema;

import java.text.ParseException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.google.refine.model.Cell;


public class WbLocationVariable extends WbLocationExpr {
    public static final String jsonType = "wblocationvariable";
    
    private String columnName;
    
    public WbLocationVariable(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public GlobeCoordinatesValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null) {
            String expr = cell.value.toString();
            try {
                return WbLocationConstant.parse(expr);
            } catch (ParseException e) {
            }
        }
        throw new SkipStatementException();
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("columnName");
        writer.value(columnName);
    }
    
    public static WbLocationVariable fromJSON(JSONObject obj) throws JSONException {
        return new WbLocationVariable(obj.getString("columnName"));
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }
}
