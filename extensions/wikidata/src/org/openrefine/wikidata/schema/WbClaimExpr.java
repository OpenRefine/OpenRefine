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
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public class WbClaimExpr extends WbChangeExpr {
    
    public static String jsonType = "wbclaimexpr";
    
    private WbItemExpr subjectExpr;
    private WbSnakExpr mainSnakExpr;
    private List<WbSnakExpr> qualifierExprs;
    // TODO: references
    
    public WbClaimExpr(WbItemExpr subjectExpr,
                       WbSnakExpr mainSnakExpr,
                       List<WbSnakExpr> qualifierExprs) {
        this.subjectExpr = subjectExpr;
        this.mainSnakExpr = mainSnakExpr;
        this.qualifierExprs = qualifierExprs;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("subject");
        subjectExpr.write(writer, options);
        writer.key("mainsnak");
        mainSnakExpr.write(writer, options);
        writer.key("qualifiers");
        writer.array();
        for (WbSnakExpr expr : qualifierExprs) {
            expr.write(writer, options);
        }
        writer.endArray();
    }
    
    public static WbClaimExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject subjObj = obj.getJSONObject("subject");
        JSONObject mainSnakObj = obj.getJSONObject("mainsnak");
        JSONArray qualifiersArr = obj.getJSONArray("qualifiers");
        List<WbSnakExpr> qualifierExprs = new ArrayList<WbSnakExpr>();
        for (int i = 0; i != qualifiersArr.length(); i++) {
            qualifierExprs.add(WbSnakExpr.fromJSON(qualifiersArr.getJSONObject(i)));
        }
        return new WbClaimExpr(
                WbItemExpr.fromJSON(subjObj),
                WbSnakExpr.fromJSON(mainSnakObj),
                qualifierExprs);
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
    
    public Claim evaluate(ExpressionContext ctxt) {
        ItemIdValue subject = subjectExpr.evaluate(ctxt);
        Snak mainSnak = mainSnakExpr.evaluate(ctxt);
        List<Snak> qualifiers = new ArrayList<Snak>(qualifierExprs.size());
        List<SnakGroup> groupedQualifiers = groupSnaks(qualifiers);
        return Datamodel.makeClaim(subject, mainSnak, groupedQualifiers);
    }

}
