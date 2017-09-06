package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

public class WbItemVariable extends WbItemExpr {
    /* An item that depends on a reconciled value in a column */
    
    public static final String jsonType = "wbitemvariable";
    
    private String columnName;
    
    public WbItemVariable(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("columnName");
        writer.value(columnName);
    }
    
    public static WbItemVariable fromJSON(JSONObject obj) throws JSONException {
        return new WbItemVariable(obj.getString("columnName"));
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) throws SkipStatementException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null && cell.recon != null) {
            Recon recon = cell.recon;
            if (recon.judgment == Recon.Judgment.Matched && cell.recon.match != null) {
                ReconCandidate match = cell.recon.match;
                return Datamodel.makeItemIdValue(match.id, ctxt.getBaseIRI());
            } else if (recon.judgment == Recon.Judgment.New) {
                return ItemIdValue.NULL;
            }
        }
        throw new SkipStatementException();
    }

    public String getJsonType() {
        return jsonType;
    }
}
