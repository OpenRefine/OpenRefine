package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.ReconCandidate;

public class WbItemVariable implements WbItemExpr {
    /* An item that depends on a reconciled value in a column */
    
    private String columnName;
    
    public WbItemVariable(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("columnName");
        writer.value(columnName);
        writer.endObject();

    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null && cell.recon != null && cell.recon.match != null) {
            ReconCandidate match = cell.recon.match;
            return ItemIdValueImpl.create(match.id, ctxt.getBaseIRI());
        }
        return null;
    }

}
