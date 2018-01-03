package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbLocationConstant extends WbLocationExpr {
    
    private String value;
    private GlobeCoordinatesValue parsed;
    
    @JsonCreator
    public WbLocationConstant(
            @JsonProperty("value") String origValue) {
        this.value = origValue;
        try {
            this.parsed = parse(origValue);
        } catch (ParseException e) {
            this.parsed = null;
        }
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
        if (parsed == null)
            throw new SkipStatementException();
        return parsed;
    }
    
    public String getValue() {
        return value;
    }
}
