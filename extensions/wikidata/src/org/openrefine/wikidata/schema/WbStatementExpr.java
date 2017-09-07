package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class WbStatementExpr extends BiJsonizable {
    
    public static final String jsonType = "wbstatementexpr";
    
    private WbValueExpr mainSnakValueExpr;
    private List<WbSnakExpr> qualifierExprs;
    private List<WbReferenceExpr> referenceExprs;
    // TODO: references
    
    public WbStatementExpr(WbValueExpr mainSnakValueExpr,
                       List<WbSnakExpr> qualifierExprs,
                       List<WbReferenceExpr> referenceExprs) {
        this.mainSnakValueExpr = mainSnakValueExpr;
        this.qualifierExprs = qualifierExprs;
        this.referenceExprs = referenceExprs;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("value");
        mainSnakValueExpr.write(writer, options);
        writer.key("qualifiers");
        writer.array();
        for (WbSnakExpr expr : qualifierExprs) {
            expr.write(writer, options);
        }
        writer.endArray();
    }
    
    public static WbStatementExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject mainSnakObj = obj.getJSONObject("value");
        
        List<WbSnakExpr> qualifierExprs = new ArrayList<WbSnakExpr>();
        if (obj.has("qualifiers")) {
            JSONArray qualifiersArr = obj.getJSONArray("qualifiers");
            for (int i = 0; i != qualifiersArr.length(); i++) {
                qualifierExprs.add(WbSnakExpr.fromJSON(qualifiersArr.getJSONObject(i)));
            }
        }
        
        List<WbReferenceExpr> referenceExprs = new ArrayList<WbReferenceExpr>();
        if (obj.has("references")) {
            JSONArray referencesArr = obj.getJSONArray("references");
            for (int i = 0; i != referencesArr.length(); i++) {
                referenceExprs.add(WbReferenceExpr.fromJSON(referencesArr.getJSONObject(i)));
            }
        }
        return new WbStatementExpr(
                WbValueExpr.fromJSON(mainSnakObj),
                qualifierExprs,
                referenceExprs);
    }
    
    public static List<SnakGroup> groupSnaks(List<Snak> snaks) {
        Map<PropertyIdValue, List<Snak> > map = new HashMap<PropertyIdValue, List<Snak>>();
        for (Snak snak : snaks) {
            PropertyIdValue pid = snak.getPropertyId();
            if (!map.containsKey(pid)) {
                map.put(pid, new ArrayList<Snak>());
            }
            map.get(pid).add(snak);
        }
        List<SnakGroup> snakGroups = new ArrayList<SnakGroup>();
        for (List<Snak> snaksGroup : map.values()) {
            snakGroups.add(Datamodel.makeSnakGroup(snaksGroup));
        }
        return snakGroups;
    }
    
    public Statement evaluate(ExpressionContext ctxt, ItemIdValue subject, PropertyIdValue propertyId) throws SkipStatementException {
        Value mainSnakValue = mainSnakValueExpr.evaluate(ctxt);
        Snak mainSnak = Datamodel.makeValueSnak(propertyId, mainSnakValue);
        List<Snak> qualifiers = new ArrayList<Snak>(qualifierExprs.size());
        for (WbSnakExpr qExpr : qualifierExprs) {
            qualifiers.add(qExpr.evaluate(ctxt));
        }
        List<SnakGroup> groupedQualifiers = groupSnaks(qualifiers);
        Claim claim = Datamodel.makeClaim(subject, mainSnak, groupedQualifiers);
        List<Reference> references = new ArrayList<Reference>();
        StatementRank rank = StatementRank.NORMAL;
        return Datamodel.makeStatement(claim, references, rank, "");
    }

    public String getJsonType() {
        return jsonType;
    }
}
