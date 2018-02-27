package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.apache.commons.lang.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A constant for a geographical location. The accepted format is lat,lng or lat/lng.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbLocationConstant implements WbExpression<GlobeCoordinatesValue> {
    
    public static final double defaultPrecision = GlobeCoordinatesValue.PREC_TEN_MICRO_DEGREE;
    
    private String value;
    private GlobeCoordinatesValue parsed;
    
    @JsonCreator
    public WbLocationConstant(
            @JsonProperty("value") String origValue) throws ParseException {
        this.value = origValue;
        Validate.notNull(origValue);
        this.parsed = parse(origValue);
        Validate.notNull(this.parsed);
    }
    
    /**
     * Parses a string to a location.
     * 
     * @param expr
     *  the string to parse
     * @return
     *  the parsed location
     * @throws ParseException
     */
    public static GlobeCoordinatesValue parse(String expr) throws ParseException {
        double lat = 0;
        double lng = 0;
        double precision = defaultPrecision;
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
            throws SkipSchemaExpressionException {
        return parsed;
    }
    
    /**
     * @return the original value as a string.
     */
    @JsonProperty("value")
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !WbLocationConstant.class.isInstance(other)) {
            return false;
        }
        WbLocationConstant otherConstant = (WbLocationConstant)other;
        return value.equals(otherConstant.getValue());
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
