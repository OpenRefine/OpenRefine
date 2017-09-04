package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;


public class WbReferenceExpr extends BiJsonizable {
    public static final String jsonType = "wbreferences";
    
    List<WbSnakExpr> snakExprs;
    
    public WbReferenceExpr(List<WbSnakExpr> snakExprs) {
        this.snakExprs = snakExprs;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("snaks");
        writer.array();
        for (WbSnakExpr expr : snakExprs) {
            expr.write(writer, options);
        }
        writer.endArray();
    }
    
    public static WbReferenceExpr fromJSON(JSONObject obj) throws JSONException {
        JSONArray arr = obj.getJSONArray("snaks");
        List<WbSnakExpr> lst = new ArrayList<WbSnakExpr>(arr.length());
        for (int i = 0; i != arr.length(); i++) {
            lst.add(WbSnakExpr.fromJSON(arr.getJSONObject(i)));
        }
        return new WbReferenceExpr(lst);
    }
    
    public Reference evaluate(ExpressionContext ctxt) {
        List<SnakGroup> snakGroups = new ArrayList<SnakGroup>();
        for (WbSnakExpr expr : snakExprs) {
            List<Snak> snakList = new ArrayList<Snak>(1);
            snakList.add(expr.evaluate(ctxt));
            snakGroups.add(Datamodel.makeSnakGroup(snakList));
        }
        return Datamodel.makeReference(snakGroups);
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

}
