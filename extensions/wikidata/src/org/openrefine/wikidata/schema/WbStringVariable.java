package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.google.refine.model.Cell;

public class WbStringVariable extends WbStringExpr {
    public static final String jsonType = "wbstringvariable";
    
    private String columnName;
    
    public WbStringVariable(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public StringValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null) {
            return Datamodel.makeStringValue(cell.value.toString());
        }
        throw new SkipStatementException();
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("columnName");
        writer.value(columnName);
    }
    
    public static WbStringVariable fromJSON2(JSONObject obj) throws JSONException {
        return new WbStringVariable(obj.getString("columnName"));
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

}
