/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import java.text.ParseException;

import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
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
    public WbLocationConstant(@JsonProperty("value") String origValue) {
        this.value = origValue;
    }

    @Override
    public void validate(ValidationState validation) {
        if (value == null) {
            validation.addError("Empty geographical coordinates value");
        }
        try {
            parsed = parse(value);
        } catch (ParseException e) {
            validation.addError("Invalid geographical coordinates: '" + value + "'");
        }
    }

    /**
     * Parses a string to a location.
     * 
     * @param expr
     *            the string to parse
     * @return the parsed location
     * @throws ParseException
     */
    public static GlobeCoordinatesValue parse(String expr)
            throws ParseException {
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
                return Datamodel.makeGlobeCoordinatesValue(lat, lng, precision, GlobeCoordinatesValue.GLOBE_EARTH);
            } catch (NumberFormatException e) {
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
        if (other == null || !WbLocationConstant.class.isInstance(other)) {
            return false;
        }
        WbLocationConstant otherConstant = (WbLocationConstant) other;
        return value.equals(otherConstant.getValue());
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
