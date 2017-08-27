package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public class WbClaimExpr implements WbChangeExpr {
    
    private WbItemExpr subjectExpr;
    private WbSnakExpr mainSnakExpr;
    private List<WbSnakExpr> qualifierExprs;
    // TODO: references
    

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
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
        writer.endObject();
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
