package org.openrefine.wikidata.schema;

import java.text.ParseException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.google.refine.model.Cell;


public class WbDateVariable extends WbDateExpr {
    public static final String jsonType = "wbdatevariable";
    
    private String _columnName;
    
    public WbDateVariable(String columnName) {
        _columnName = columnName;
    }

    @Override
    public TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(_columnName);
        if (cell != null) {
            try {
                // TODO accept parsed dates (without converting them to strings)
                return WbDateConstant.parse(cell.value.toString());
            } catch (ParseException e) {
            }
        }
        throw new SkipStatementException();
    }
    
    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("columnName");
        writer.value(_columnName);
    }
    
    public static WbDateVariable fromJSON(JSONObject obj) throws JSONException {
        return new WbDateVariable(obj.getString("columnName"));
    }
    
    @Override
    public String getJsonType() {
        return jsonType;
    }
}
