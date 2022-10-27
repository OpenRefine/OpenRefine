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

import org.openrefine.wikibase.schema.entityvalues.SuggestedPropertyIdValue;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A constant property, that does not change depending on the row
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbPropConstant implements WbExpression<PropertyIdValue> {

    private String pid;
    private String label;
    private String datatype;

    @JsonCreator
    public WbPropConstant(@JsonProperty("pid") String pid, @JsonProperty("label") String label,
            @JsonProperty("datatype") String datatype) {
        this.pid = pid;
        this.label = label;
        this.datatype = datatype;
    }

    @Override
    public void validate(ValidationState validation) {
        if (pid == null) {
            validation.addError("Missing property id");
        }
        if (label == null) {
            validation.addError("Missing property label");
        }
    }

    @Override
    public PropertyIdValue evaluate(ExpressionContext ctxt) {
        return new SuggestedPropertyIdValue(pid, ctxt.getBaseIRI(), label);
    }

    @JsonProperty("pid")
    public String getPid() {
        return pid;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("datatype")
    public String getDatatype() {
        return datatype;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbPropConstant.class.isInstance(other)) {
            return false;
        }
        WbPropConstant otherConstant = (WbPropConstant) other;
        return pid.equals(otherConstant.getPid()) && label.equals(otherConstant.getLabel())
                && datatype.equals(otherConstant.getDatatype());
    }

    @Override
    public int hashCode() {
        return pid.hashCode() + label.hashCode();
    }

}
