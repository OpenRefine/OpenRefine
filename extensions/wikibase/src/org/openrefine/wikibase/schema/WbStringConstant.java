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

import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WbStringConstant implements WbExpression<StringValue> {

    private String value;

    @JsonCreator
    public WbStringConstant(@JsonProperty("value") String value) {
        this.value = value == null ? value : value.trim();
    }

    @Override
    public void validate(ValidationState validation) {
        if (value == null || value.isEmpty()) {
            // for now we don't accept empty strings
            // because in the variable counterpart of this expression, they are skipped
            validation.addError("Empty value");
        }
    }

    @Override
    public StringValue evaluate(ExpressionContext ctxt) {
        return Datamodel.makeStringValue(value);
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbStringConstant.class.isInstance(other)) {
            return false;
        }
        return value.equals(((WbStringConstant) other).getValue());
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
