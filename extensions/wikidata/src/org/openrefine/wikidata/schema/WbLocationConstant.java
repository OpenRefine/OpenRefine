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
        double precision = 0;
        return Datamodel.makeGlobeCoordinatesValue(lat, lng, precision,
                GlobeCoordinatesValue.GLOBE_EARTH);
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
