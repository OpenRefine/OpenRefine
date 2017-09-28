package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Cell;


public class WbLanguageVariable extends WbLanguageExpr {
    public static final String jsonType = "wblanguagevariable";
    
    private String _columnName;
    
    public WbLanguageVariable(String columnName) {
        _columnName = columnName;
    }
    
    @Override
    public String evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(_columnName);
        if (cell != null) {
            // TODO some validation here?
            return cell.value.toString();
        }
        throw new SkipStatementException();
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("columnName");
        writer.value(_columnName);
    }
    
    public static WbLanguageVariable fromJSON(JSONObject obj) throws JSONException {
        return new WbLanguageVariable(obj.getString("columnName"));
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

}
