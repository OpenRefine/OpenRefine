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

import org.openrefine.wikibase.schema.entityvalues.SuggestedItemIdValue;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an item that does not vary, it is independent of the row.
 */
public class WbItemConstant implements WbExpression<ItemIdValue> {

    private String qid;
    private String label;

    @JsonCreator
    public WbItemConstant(@JsonProperty("qid") String qid, @JsonProperty("label") String label) {
        this.qid = qid;
        this.label = label;
    }

    @Override
    public void validate(ValidationState validation) {
        if (qid == null) {
            validation.addError("No entity id provided");
        } else {
            try {
                EntityIdValueImpl.guessEntityTypeFromId(qid);
            } catch (IllegalArgumentException e) {
                validation.addError("Invalid entity id format: '" + qid + "'");
            }
        }
        if (label == null) {
            validation.addError("No entity label provided");
        }
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) {
        return new SuggestedItemIdValue(qid, ctxt.getBaseIRI(), label);
    }

    @JsonProperty("qid")
    public String getQid() {
        return qid;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbItemConstant.class.isInstance(other)) {
            return false;
        }
        WbItemConstant otherConstant = (WbItemConstant) other;
        return (qid.equals(otherConstant.getQid()) && label.equals(otherConstant.getLabel()));
    }

    @Override
    public int hashCode() {
        return qid.hashCode() + label.hashCode();
    }
}
