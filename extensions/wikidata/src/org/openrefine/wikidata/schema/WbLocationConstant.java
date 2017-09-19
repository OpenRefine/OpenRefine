package org.openrefine.wikidata.schema;

import java.text.ParseException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;


public class WbLocationConstant extends WbLocationExpr {
    public static final String jsonType = "wblocationconstant";
    
    private String _origValue;
    private GlobeCoordinatesValue _parsed;
    
    public WbLocationConstant(String origValue) {
        _origValue = origValue;
        _parsed = null;
    }
    
    public static GlobeCoordinatesValue parse(String expr) throws ParseException {
        double lat = 0;
        double lng = 0;
        double precision = GlobeCoordinatesValue.PREC_TEN_MICRO_DEGREE;
        String[] parts = expr.split("[,/]");
        if (parts.length >= 2 && parts.length <= 3) {
           try {
           lat = Double.parseDouble(parts[0]);
           lng = Double.parseDouble(parts[1]);
           if (parts.length == 3) {
               precision = Double.parseDouble(parts[2]);
           }
           return Datamodel.makeGlobeCoordinatesValue(lat, lng, precision,
                   GlobeCoordinatesValue.GLOBE_EARTH);
           } catch(NumberFormatException e) {
               ;
           }
        }
        throw new ParseException("Invalid globe coordinates", 0);
    }

    @Override
    public GlobeCoordinatesValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        if (_parsed == null)
            throw new SkipStatementException();
        return _parsed;
    }
    
    public static WbLocationConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbLocationConstant(obj.getString("value"));
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("value");
        writer.value(_origValue);     
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }  
}
